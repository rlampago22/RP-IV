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
import br.edu.unipampa.usina.infraestruturaeventos.MedicaoRegistrada;
import br.edu.unipampa.usina.infraestruturaeventos.ObservacaoRegistrada;
import br.edu.unipampa.usina.infraestruturaeventos.FalhaSensorDetectada;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Suíte de testes automatizados sem dependências externas (Zero External Deps),
 * cobrindo RF01–RF06 e os RNFs diretamente verificáveis nesta versão local.
 */
public final class TestesMvp {
    private TestesMvp() {}

    public static void main(String[] args) throws IOException {
        System.out.println("================================================================================");
        System.out.println(" EXECUCAO DA SUITE DE TESTES DO CHECKLIST DE ACEITE (MUST)");
        System.out.println(" Alinhado ao documento oficial de 69 paginas e especificacao MoSCoW");
        System.out.println("================================================================================");

        Path logTeste = Path.of("dados", "teste-auditoria-suite.log");
        Files.deleteIfExists(logTeste);

        EventBus eventBus = new EventBus();
        RegistroAuditoria registro = new RegistroAuditoria(logTeste);
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
        boolean reconhecido = alarmes.reconhecerAlarme(alarmeEmitido.getId(), "Operador Alvaro", "Verificado e em analise");
        exigir(reconhecido && alarmeEmitido.isReconhecido(), "UC01: Alarme deve ter sido reconhecido pelo operador.");
        boolean resolvido = alarmes.resolverAlarme(alarmeEmitido.getId(), "Engenheiro Bruno", "Pressao e temp normalizadas");
        exigir(resolvido && alarmeEmitido.isResolvido(), "UC01: Alarme deve ter sido encerrado/resolvido.");

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

        // 9. CT-RNF05-01: Auditabilidade e Encadeamento SHA-256
        exigir(registro.verificarIntegridade(), "CT-RNF05-01: Cadeia SHA-256 em memoria deve ser integra.");
        exigir(registro.verificarIntegridadeArquivo(), "CT-RNF05-01: Cadeia SHA-256 no arquivo fisico deve ser integra.");

        // 10. CT-RNF09-01: Persistencia segura e continuidade multi-sessao
        int totalAntes = registro.consultar().size();
        RegistroAuditoria segundaSessao = new RegistroAuditoria(logTeste);
        exigir(segundaSessao.consultar().size() == totalAntes,
            "CT-RNF09-01: Reinicializacao deve recuperar todas as " + totalAntes + " entradas.");
        exigir(segundaSessao.verificarIntegridadeArquivo(),
            "CT-RNF09-01: Arquivo carregado na nova sessao deve ser integro.");

        // 11. RNF-03 / RNF-05: Deteccao comprovada de Adulteracao fisica (Tamper Check)
        Path logAdulterado = Path.of("dados", "teste-tamper.log");
        Files.copy(logTeste, logAdulterado, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        List<String> linhas = Files.readAllLines(logAdulterado, StandardCharsets.UTF_8);
        if (!linhas.isEmpty()) {
            // Modifica um byte/caractere em uma linha existente
            String linhaAlterada = linhas.get(0).replace("310.5", "999.9");
            linhas.set(0, linhaAlterada);
            Files.write(logAdulterado, linhas, StandardCharsets.UTF_8);

            RegistroAuditoria auditoriaAdulterada = new RegistroAuditoria(logAdulterado);
            exigir(!auditoriaAdulterada.verificarIntegridadeArquivo(),
                "RNF-03: A alteracao fisica de 1 caractere no log DEVE ser detectada como FALHA.");
        }
        Files.deleteIfExists(logAdulterado);
        Files.deleteIfExists(logTeste);

        System.out.println("\n[SUCESSO] 11 cenarios do recorte MVP passaram (RF01-RF06 e RNFs verificaveis localmente).");
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            System.err.println("[FALHA] " + mensagem);
            throw new AssertionError(mensagem);
        }
        System.out.println("[OK] " + mensagem);
    }
}
