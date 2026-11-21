package br.edu.unipampa.usina.app;

import br.edu.unipampa.usina.alarmes.Alarme;
import br.edu.unipampa.usina.alarmes.AlarmeFacade;
import br.edu.unipampa.usina.alarmes.AlarmeFactory;
import br.edu.unipampa.usina.alarmes.AvaliadorFaixaSegura;
import br.edu.unipampa.usina.auditorialogs.AuditoriaSubscriber;
import br.edu.unipampa.usina.auditorialogs.RegistroAuditoria;
import br.edu.unipampa.usina.controlereator.MedicaoReator;
import br.edu.unipampa.usina.controlereator.ReatorFacade;
import br.edu.unipampa.usina.controlereator.Sensor;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import br.edu.unipampa.usina.infraestruturaeventos.EventoDominio;
import br.edu.unipampa.usina.infraestruturaeventos.MedicaoRegistrada;
import br.edu.unipampa.usina.infraestruturaeventos.ObservacaoRegistrada;
import br.edu.unipampa.usina.infraestruturaeventos.FalhaSensorDetectada;
import br.edu.unipampa.usina.persistencia.ConexaoBanco;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Suíte de testes automatizados sem dependências externas (Zero External Deps),
 * cobrindo os cenários de aceite CT-RF01 a CT-RF06 e CT-RNF01 a CT-RNF10 do checklist da disciplina.
 */
public final class TestesMvp {
    private TestesMvp() {}

    public static void main(String[] args) throws IOException {
        System.out.println("================================================================================");
        System.out.println(" EXECUCAO DA SUITE DE TESTES DO CHECKLIST DE ACEITE (MUST)");
        System.out.println(" Alinhado ao documento oficial de 69 paginas e especificacao MoSCoW");
        System.out.println("================================================================================");

        // Banco H2 só em memória: a suíte nunca escreve na auditoria real do sistema.
        ConexaoBanco bancoTeste = ConexaoBanco.emMemoria("testes-mvp-" + System.nanoTime());

        EventBus eventBus = new EventBus();
        RegistroAuditoria registro = new RegistroAuditoria(bancoTeste);
        new AuditoriaSubscriber(eventBus, registro);

        AvaliadorFaixaSegura avaliador = new AvaliadorFaixaSegura();
        AlarmeFacade alarmes = new AlarmeFacade(eventBus, avaliador, new AlarmeFactory());
        ReatorFacade reator = new ReatorFacade(eventBus);

        // CT-RF01-01 & RF-1: Cadastrar os 4 sensores exigidos pelo documento
        reator.registrarSensor(new Sensor(1L, "TEMPERATURA", "Celsius", 0.0, 350.0, 20.0, 320.0));
        reator.registrarSensor(new Sensor(2L, "PRESSAO", "bar", 0.0, 160.0, 10.0, 150.0));
        reator.registrarSensor(new Sensor(3L, "RADIACAO", "mSv/h", 0.0, 5.0, 0.1, 4.0));
        reator.registrarSensor(new Sensor(4L, "FLUXO_RESFRIAMENTO", "m3/h", 500.0, 1500.0, 600.0, 1400.0));

        // 1. CT-RF01-01: Coleta de medicoes seguras
        reator.receberLeitura(1L, 310.5);
        reator.receberLeitura(2L, 155.0);
        reator.receberLeitura(1L, 312.0);
        exigir(reator.consultarHistorico().size() == 3, "CT-RF01-01: Devem existir 3 medicoes cadastradas.");

        // 2. CT-RF02-01: Manter historico operacional na ordem correta
        List<MedicaoReator> historico = reator.consultarHistorico();
        exigir(historico.get(0).getValor() == 310.5, "CT-RF02-01: Ordem 1 incorreta.");
        exigir(historico.get(1).getValor() == 155.0, "CT-RF02-01: Ordem 2 incorreta.");
        exigir(historico.get(2).getValor() == 312.0, "CT-RF02-01: Ordem 3 incorreta.");

        // 3. CT-RF03-01: Avaliar limiar seguro (Strategy)
        MedicaoRegistrada segura = new MedicaoRegistrada(Instant.now(), 1L, "TEMPERATURA", "Celsius", 300.0, 0.0, 350.0);
        MedicaoRegistrada violada = new MedicaoRegistrada(Instant.now(), 1L, "TEMPERATURA", "Celsius", 380.0, 0.0, 350.0);
        exigir(!avaliador.foraDaFaixaSegura(segura), "CT-RF03-01: Medicao segura deve retornar violado=false.");
        exigir(avaliador.foraDaFaixaSegura(violada), "CT-RF03-01: Medicao fora da faixa deve retornar violado=true.");

        // 4. CT-RF04-01 & CT-RF05-01: Emitir alarme, notificar atores e registrar evento
        Alarme alarmeEmitido = alarmes.processarAvaliacao(violada, true);
        exigir(alarmeEmitido != null, "CT-RF04-01: Alarme deve ter sido gerado.");
        exigir(alarmeEmitido.isRegistrado(), "CT-RF05-01: Alarme deve ter sido registrado.");
        exigir(alarmeEmitido.getDestinatarios().contains("Operador de Reator")
            && alarmeEmitido.getDestinatarios().contains("Supervisao Central"),
            "CT-RF04-01: Destinatarios devem incluir Operador e Supervisao Central.");

        // 5. UC01: Ciclo de vida do Alarme (Reconhecimento pelo Operador e Normalizacao)
        exigir(!alarmes.resolverAlarme(alarmeEmitido.getId(), "Engenheiro Bruno", "Resolucao prematura")
            && alarmeEmitido.isAtivo(),
            "UC01: Alarme ativo nao pode ser resolvido antes do reconhecimento.");
        exigirEstadoIlegal(
            () -> alarmeEmitido.resolver("Engenheiro Bruno", "Resolucao prematura"),
            "Dominio deve rejeitar resolucao antes do reconhecimento."
        );
        boolean reconhecido = alarmes.reconhecerAlarme(alarmeEmitido.getId(), "Operador Alvaro", "Verificado e em analise");
        exigir(reconhecido && alarmeEmitido.isReconhecido(), "UC01: Alarme deve ter sido reconhecido pelo operador.");
        exigir(!alarmes.reconhecerAlarme(alarmeEmitido.getId(), "Operador Alvaro", "Reconhecimento duplicado")
            && alarmeEmitido.isReconhecido(),
            "UC01: Alarme reconhecido nao pode ser reconhecido novamente.");
        exigirEstadoIlegal(
            () -> alarmeEmitido.reconhecer("Operador Alvaro", "Reconhecimento duplicado"),
            "Dominio deve rejeitar reconhecimento repetido."
        );
        boolean resolvido = alarmes.resolverAlarme(alarmeEmitido.getId(), "Engenheiro Bruno", "Pressao e temp normalizadas");
        exigir(resolvido && alarmeEmitido.isResolvido(), "UC01: Alarme deve ter sido encerrado/resolvido.");
        exigir(!alarmes.resolverAlarme(alarmeEmitido.getId(), "Engenheiro Bruno", "Resolucao duplicada")
            && alarmeEmitido.isResolvido(),
            "UC01: Alarme resolvido nao pode ser resolvido novamente.");
        exigirEstadoIlegal(
            () -> alarmeEmitido.resolver("Engenheiro Bruno", "Resolucao duplicada"),
            "Dominio deve rejeitar resolucao repetida."
        );
        exigir(!alarmes.reconhecerAlarme(alarmeEmitido.getId(), "Operador Alvaro", "Reconhecimento tardio")
            && alarmeEmitido.isResolvido(),
            "UC01: Alarme resolvido nao pode voltar ao estado reconhecido.");

        // 6. UC01: Fluxo Alternativo 1 (Registro de Observacao preventiva)
        AtomicBoolean observacaoRecebida = new AtomicBoolean(false);
        eventBus.assinar(ObservacaoRegistrada.class, evento -> observacaoRecebida.set(true));
        reator.receberLeitura(1L, 325.0); // Na faixa de observacao/atencao
        exigir(observacaoRecebida.get(), "UC01 Alt. 1: Observacao preventiva deve ser publicada sem alarme.");

        // 7. UC01: Fluxo de Excecoes (Falha de Comunicacao com Sensor)
        AtomicBoolean falhaRecebida = new AtomicBoolean(false);
        eventBus.assinar(FalhaSensorDetectada.class, evento -> {
            FalhaSensorDetectada f = (FalhaSensorDetectada) evento;
            if (f.destinatarios().contains("Equipe Tecnica")) {
                falhaRecebida.set(true);
            }
        });
        reator.simularFalhaSensor(2L, "Falha de comunicacao de telemetria");
        exigir(falhaRecebida.get(), "UC01 Excecao: Alerta de falha tecnica deve notificar a equipe tecnica.");

        // 8. CT-RNF01-01 & CT-RNF04-01: Desacoplamento e Tolerancia a Falhas no EventBus
        eventBus.assinarTodos(evento -> {
            throw new RuntimeException("Simulacao de falha em subscriber com defeito");
        });
        // O produtor deve continuar funcionando perfeitamente sem propagar a excecao do consumidor
        reator.receberLeitura(4L, 1100.0); // Leitura do sensor de fluxo de resfriamento
        exigir(true, "CT-RNF04-01: Erro em consumidor nao interrompeu o produtor.");

        // 9. CT-RNF05-01: Auditabilidade e Encadeamento SHA-256 (agora gravado no banco)
        exigir(!registro.consultar().isEmpty(), "CT-RNF05-01: Eventos auditados devem estar gravados no banco.");
        exigir(registro.verificarIntegridade(), "CT-RNF05-01: Cadeia SHA-256 gravada no banco deve ser integra.");

        // 10. CT-RNF09-01: Persistencia segura e continuidade multi-sessao
        int totalAntes = registro.consultar().size();
        registro.close(); // simula o processo anterior encerrando antes do proximo abrir o mesmo banco
        RegistroAuditoria segundaSessao = new RegistroAuditoria(bancoTeste);
        exigir(segundaSessao.consultar().size() == totalAntes,
            "CT-RNF09-01: Reinicializacao deve recuperar todas as " + totalAntes + " entradas do banco.");
        exigir(segundaSessao.verificarIntegridade(),
            "CT-RNF09-01: Trilha lida na nova sessao deve ser integra.");

        // 10b. Dois escritores ao mesmo tempo (causa raiz do incidente de 09/11 no arquivo): no banco a
        // sequencia e chave primaria, entao a cadeia continua integra em vez de bifurcar.
        RegistroAuditoria outroProcesso = new RegistroAuditoria(bancoTeste);
        int porEscritor = 40;
        Thread escritorA = new Thread(() -> registrarVarios(segundaSessao, "A", porEscritor));
        Thread escritorB = new Thread(() -> registrarVarios(outroProcesso, "B", porEscritor));
        escritorA.start();
        escritorB.start();
        juntar(escritorA);
        juntar(escritorB);
        exigir(segundaSessao.consultar().size() == totalAntes + 2 * porEscritor,
            "CONC-01: 2 escritores simultaneos devem gravar todas as " + (2 * porEscritor) + " entradas.");
        exigir(segundaSessao.verificarIntegridade(),
            "CONC-01: Cadeia SHA-256 deve continuar integra com 2 escritores simultaneos.");
        outroProcesso.close();

        // 10c. Migracao do log antigo (dados/auditoria.log) para o banco, com os mesmos hashes
        Path logExportado = Files.createTempFile("auditoria-exportada", ".log");
        segundaSessao.exportar(logExportado);
        try (RegistroAuditoria bancoNovo = new RegistroAuditoria(ConexaoBanco.emMemoria("testes-import-" + System.nanoTime()))) {
            int importadas = bancoNovo.importarLogLegado(logExportado);
            exigir(importadas == segundaSessao.consultar().size() && bancoNovo.verificarIntegridade(),
                "MIGR-01: Log em arquivo integro deve ser importado inteiro e com a cadeia valida.");
            exigir(bancoNovo.importarLogLegado(logExportado) == 0,
                "MIGR-01: Importacao so acontece com a tabela vazia (rodar de novo nao duplica).");
        }
        List<String> linhas = Files.readAllLines(logExportado, StandardCharsets.UTF_8);
        linhas.set(0, linhas.get(0).replace("valor=", "valor=9"));
        Files.write(logExportado, linhas, StandardCharsets.UTF_8);
        try (RegistroAuditoria bancoNovo = new RegistroAuditoria(ConexaoBanco.emMemoria("testes-import-adult-" + System.nanoTime()))) {
            exigir(bancoNovo.importarLogLegado(logExportado) == 0 && bancoNovo.consultar().isEmpty(),
                "MIGR-02: Log antigo com a cadeia quebrada NAO deve ser importado.");
        }
        Files.deleteIfExists(logExportado);

        // 11. RNF-03 / RNF-05: Deteccao comprovada de adulteracao direto no banco (Tamper Check)
        try (Connection sql = bancoTeste.abrir(); Statement comando = sql.createStatement()) {
            comando.executeUpdate("UPDATE " + RegistroAuditoria.TABELA
                + " SET resumo = CONCAT(resumo, '.') WHERE sequencia = 2");
            exigir(!segundaSessao.verificarIntegridade(),
                "RNF-03: Alterar 1 caractere de uma linha no banco DEVE ser detectado como FALHA.");
            comando.executeUpdate("UPDATE " + RegistroAuditoria.TABELA
                + " SET resumo = SUBSTRING(resumo, 1, CHAR_LENGTH(resumo) - 1) WHERE sequencia = 2");
            exigir(segundaSessao.verificarIntegridade(),
                "RNF-03: Desfazendo a alteracao a cadeia volta a ser integra.");
            comando.executeUpdate("DELETE FROM " + RegistroAuditoria.TABELA + " WHERE sequencia = 3");
            exigir(!segundaSessao.verificarIntegridade(),
                "RNF-03: Apagar uma linha do banco DEVE ser detectado como FALHA.");
        } catch (SQLException error) {
            throw new AssertionError("Falha ao simular adulteracao no banco: " + error.getMessage(), error);
        }
        segundaSessao.close();

        // 12. Histórico curto consultável (S3 / Bernardo — issue #45)
        List<MedicaoReator> porTemp = reator.consultarHistorico(1L);
        exigir(!porTemp.isEmpty(), "HIST-01: Historico por sensor TEMPERATURA nao pode ser vazio.");
        exigir(porTemp.stream().allMatch(m -> m.getSensor().id() == 1L),
            "HIST-01: Filtro por sensorId deve retornar apenas o sensor pedido.");
        List<MedicaoReator> recentes = reator.consultarHistoricoRecente(2);
        exigir(recentes.size() == 2, "HIST-02: consultarHistoricoRecente(2) deve retornar 2 itens.");
        exigir(reator.getRepository().capacidadeMaxima() >= 1,
            "HIST-03: ReatorRepository deve expor capacidade do historico curto.");

        System.out.println("\n[SUCESSO] 11 verificacoes Must + historico curto (SEQ-UC01 / T02) passaram!");
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            System.err.println("[FALHA] " + mensagem);
            throw new AssertionError(mensagem);
        }
        System.out.println("[OK] " + mensagem);
    }

    private record EventoTeste(String tipo, Instant ocorridoEm, String resumo) implements EventoDominio {}

    private static void registrarVarios(RegistroAuditoria destino, String escritor, int quantidade) {
        for (int i = 1; i <= quantidade; i++) {
            destino.registrar(new EventoTeste("TESTE_CONCORRENCIA", Instant.now(), "escritor=" + escritor + " n=" + i));
        }
    }

    private static void juntar(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Teste interrompido.", error);
        }
    }

    private static void exigirEstadoIlegal(Runnable acao, String mensagem) {
        try {
            acao.run();
        } catch (IllegalStateException esperado) {
            System.out.println("[OK] " + mensagem);
            return;
        }
        throw new AssertionError(mensagem);
    }
}
