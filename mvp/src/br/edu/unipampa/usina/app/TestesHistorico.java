package br.edu.unipampa.usina.app;

import br.edu.unipampa.usina.historicooperacional.HistoricoSubscriber;
import br.edu.unipampa.usina.historicooperacional.OperadorDeServico;
import br.edu.unipampa.usina.historicooperacional.RegistroHistorico;
import br.edu.unipampa.usina.historicooperacional.SnapshotOperacional;
import br.edu.unipampa.usina.infraestruturaeventos.AlarmeEmitido;
import br.edu.unipampa.usina.infraestruturaeventos.AlarmeReconhecido;
import br.edu.unipampa.usina.infraestruturaeventos.AlarmeResolvido;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import br.edu.unipampa.usina.infraestruturaeventos.FalhaSensorDetectada;
import br.edu.unipampa.usina.infraestruturaeventos.MedicaoRegistrada;
import br.edu.unipampa.usina.infraestruturaeventos.ObservacaoRegistrada;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Testes da persistência do histórico operacional (Fase 1 de RF16 / RNF09):
 * amostragem por instante, derivação de status e recuperação após reinício.
 */
public final class TestesHistorico {
    private static final Instant T0 = Instant.parse("2026-10-05T12:00:00Z");

    private TestesHistorico() {}

    public static void main(String[] args) throws IOException {
        System.out.println("================================================================================");
        System.out.println(" TESTES DO HISTORICO OPERACIONAL (RF16 fase 1 / RNF09)");
        System.out.println("================================================================================");

        Path pasta = Files.createTempDirectory("rpiv-historico");
        Path arquivo = pasta.resolve("historico-operacional.csv");
        try {
            cenarios(arquivo);
        } finally {
            apagar(pasta);
        }

        System.out.println("\n[SUCESSO] Testes do historico operacional passaram com 100% de exito!");
    }

    private static void cenarios(Path arquivo) {
        EventBus eventBus = new EventBus();
        RegistroHistorico registro = new RegistroHistorico(arquivo);
        OperadorDeServico operador = new OperadorDeServico();
        new HistoricoSubscriber(eventBus, registro, operador);

        // 1. Ciclo incompleto nao grava: linha parcial nao serve ao relatorio.
        eventBus.publicar(medicao(T0, 1L, "TEMPERATURA", "Celsius", 310.5));
        eventBus.publicar(medicao(T0, 2L, "PRESSAO", "bar", 155.0));
        eventBus.publicar(medicao(T0, 3L, "RADIACAO", "mSv/h", 2.4));
        exigir(registro.tamanho() == 0, "Ciclo incompleto (3 de 4) nao deve gravar linha.");

        // 2. Ao fechar o ciclo, grava uma linha com os 4 valores daquele ciclo.
        eventBus.publicar(medicao(T0, 4L, "FLUXO_RESFRIAMENTO", "m3/h", 1100.0));
        exigir(registro.tamanho() == 1, "Ciclo completo deve gravar exatamente uma linha.");

        SnapshotOperacional primeira = registro.consultar().get(0);
        exigir(primeira.temperatura() == 310.5 && primeira.pressao() == 155.0
                && primeira.radiacao() == 2.4 && primeira.fluxoResfriamento() == 1100.0,
            "A linha deve conter os 4 parametros do mesmo ciclo.");
        exigir("ESTAVEL".equals(primeira.statusDerivado()),
            "Sem alarme nem observacao, o status derivado deve ser ESTAVEL.");

        // 3. Comecar o ciclo seguinte sem fechar nao gera linha nova.
        Instant t1 = T0.plusSeconds(3);
        eventBus.publicar(medicao(t1, 1L, "TEMPERATURA", "Celsius", 312.0));
        eventBus.publicar(medicao(t1, 2L, "PRESSAO", "bar", 156.0));
        exigir(registro.tamanho() == 1, "Ciclo iniciado e nao fechado nao deve gravar linha.");

        // 4. Repetir um sensor nao fecha o ciclo — precisa dos quatro tipos distintos.
        eventBus.publicar(medicao(t1, 1L, "TEMPERATURA", "Celsius", 312.4));
        exigir(registro.tamanho() == 1, "Repetir o mesmo sensor nao deve fechar o ciclo.");

        // 5. Fechado o ciclo, a linha traz os quatro valores novos — nenhum herdado do anterior.
        eventBus.publicar(medicao(t1, 3L, "RADIACAO", "mSv/h", 2.6));
        eventBus.publicar(medicao(t1, 4L, "FLUXO_RESFRIAMENTO", "m3/h", 1080.0));
        exigir(registro.tamanho() == 2, "Fechar o ciclo deve gravar a segunda linha.");
        SnapshotOperacional segunda = registro.consultar().get(1);
        exigir(segunda.temperatura() == 312.4 && segunda.pressao() == 156.0
                && segunda.radiacao() == 2.6 && segunda.fluxoResfriamento() == 1080.0,
            "Os 4 valores da linha devem ser do ciclo que fechou, nao do anterior.");

        // 6. Alarme ativo leva o status derivado a CRITICO.
        eventBus.publicar(new AlarmeEmitido(
            t1, "ALM-01", 2L, "CRITICA", "PRESSAO fora da faixa", List.of("Operador de Reator")));
        publicarCiclo(eventBus, T0.plusSeconds(6), 313.0, 161.0, 2.5, 1075.0);
        SnapshotOperacional comAlarme = ultima(registro);
        exigir(comAlarme.sensoresEmAlarme().equals(List.of("2:PRESSAO")),
            "A linha deve dizer QUAL sensor esta em alarme, nao apenas quantos.");
        exigir("CRITICO".equals(comAlarme.statusDerivado()), "Alarme ativo deve derivar status CRITICO.");

        // 7. Reconhecer move o alarme para reconhecidos e o status para ATENCAO.
        eventBus.publicar(new AlarmeReconhecido(T0.plusSeconds(7), "ALM-01", "Operador", "Validado"));
        publicarCiclo(eventBus, T0.plusSeconds(9), 312.5, 158.0, 2.5, 1078.0);
        SnapshotOperacional reconhecido = ultima(registro);
        exigir(reconhecido.sensoresEmAlarme().isEmpty()
                && reconhecido.sensoresReconhecidos().equals(List.of("2:PRESSAO")),
            "Reconhecer deve mover o sensor de 'em alarme' para 'reconhecidos'.");
        exigir("ATENCAO".equals(reconhecido.statusDerivado()),
            "Alarme reconhecido e nao resolvido deve derivar ATENCAO.");

        // 8. Resolver limpa o contexto e volta a ESTAVEL.
        eventBus.publicar(new AlarmeResolvido(T0.plusSeconds(10), "ALM-01", "Engenheiro", "Normalizado"));
        publicarCiclo(eventBus, T0.plusSeconds(12), 311.0, 155.0, 2.4, 1095.0);
        exigir("ESTAVEL".equals(ultima(registro).statusDerivado()),
            "Alarme resolvido deve devolver o status a ESTAVEL.");

        // 9. Observacao preventiva sobrevive ao resto do ciclo e deriva ATENCAO.
        // Ordem igual a do ReatorFacade: a medicao do sensor limpa, a observacao dele marca em seguida.
        Instant t4 = T0.plusSeconds(15);
        eventBus.publicar(medicao(t4, 1L, "TEMPERATURA", "Celsius", 326.0));
        eventBus.publicar(new ObservacaoRegistrada(
            t4, 1L, "TEMPERATURA", "Celsius", 326.0, "Fora da faixa ideal"));
        eventBus.publicar(medicao(t4, 2L, "PRESSAO", "bar", 154.0));
        eventBus.publicar(medicao(t4, 3L, "RADIACAO", "mSv/h", 2.4));
        eventBus.publicar(medicao(t4, 4L, "FLUXO_RESFRIAMENTO", "m3/h", 1100.0));
        SnapshotOperacional comObservacao = ultima(registro);
        exigir(comObservacao.sensoresEmObservacao().equals(List.of("1:TEMPERATURA")),
            "A linha deve dizer qual sensor esta em observacao.");
        exigir("ATENCAO".equals(comObservacao.statusDerivado()),
            "Observacao pendente deve derivar ATENCAO.");

        // 9b. Falha com o ciclo parado grava linha na hora — e este e o caso das rotas de cenario,
        // que pausam o tempo real. Antes da correcao, a falha nao deixava rastro nenhum.
        int antesDaFalha = registro.tamanho();
        Instant t5 = T0.plusSeconds(18);
        eventBus.publicar(new FalhaSensorDetectada(
            t5, 2L, "PRESSAO", "Falha de comunicacao", List.of("Manutencao")));
        exigir(registro.tamanho() == antesDaFalha + 1,
            "Falha com ciclo parado deve gravar uma linha imediatamente.");

        SnapshotOperacional comFalha = ultima(registro);
        exigir(comFalha.sensoresEmFalha().equals(List.of("2:PRESSAO")),
            "A linha deve registrar qual sensor falhou.");
        exigir(comFalha.temFalhaDeSensor(), "A linha deve sinalizar falha de sensor.");
        exigir(comFalha.pressao() == 154.0,
            "A linha mantem a ultima leitura conhecida do sensor que falhou.");
        // A observacao do cenario anterior segue ativa (so a proxima medicao do sensor 1 a limpa),
        // por isso o status aqui e ATENCAO. O ponto e que a falha nao o elevou a CRITICO.
        exigir(comFalha.sensoresEmObservacao().equals(List.of("1:TEMPERATURA")),
            "A observacao anterior continua valendo na linha da falha.");
        exigir(!"CRITICO".equals(comFalha.statusDerivado()),
            "Falha de sensor nao deve elevar o status a CRITICO — e eixo proprio, como no EstadoAgregador.");

        // 9c. Falha e evento, nao estado: a proxima linha nao repete a falha ja registrada.
        publicarCiclo(eventBus, T0.plusSeconds(21), 311.0, 155.0, 2.4, 1100.0);
        exigir(ultima(registro).sensoresEmFalha().isEmpty(),
            "Falha ja registrada nao deve se repetir na linha seguinte.");

        // 9d. Alarme emitido com o ciclo parado tambem grava na hora.
        int antesDoAlarme = registro.tamanho();
        eventBus.publicar(new AlarmeEmitido(T0.plusSeconds(24), "ALM-02", 4L, "CRITICA",
            "FLUXO fora da faixa", List.of("Operador de Reator")));
        exigir(registro.tamanho() == antesDoAlarme + 1,
            "Alarme com ciclo parado deve gravar uma linha imediatamente.");
        exigir(ultima(registro).sensoresEmAlarme().equals(List.of("4:FLUXO_RESFRIAMENTO")),
            "A linha da transicao deve apontar o sensor que alarmou.");

        // 9e. Transicao com ciclo parcial tambem grava — o cenario critico do T05 mexe em 2 de 4
        // sensores com o tempo real pausado, e esse ciclo nunca fecharia.
        int antesDoParcial = registro.tamanho();
        eventBus.publicar(medicao(T0.plusSeconds(27), 1L, "TEMPERATURA", "Celsius", 372.0));
        eventBus.publicar(new AlarmeEmitido(T0.plusSeconds(27), "ALM-03", 1L, "CRITICA",
            "TEMPERATURA fora da faixa", List.of("Operador de Reator")));
        exigir(registro.tamanho() == antesDoParcial + 1,
            "Alarme em ciclo parcial deve gravar, senao o cenario critico some do historico.");
        exigir(ultima(registro).sensoresEmAlarme().contains("1:TEMPERATURA"),
            "A linha deve apontar o sensor do alarme do ciclo parcial.");

        // 10. Operador de servico: sem identificacao, a linha nao afirma operador nenhum.
        exigir(primeira.operador().isEmpty(),
            "Sem identificacao, o campo operador deve ficar vazio.");
        exigir(primeira.origemOperador() == OperadorDeServico.Origem.NAO_IDENTIFICADO,
            "Sem identificacao, a origem deve ser NAO_IDENTIFICADO.");

        // 11. Identificado, o nome, os papeis e a origem entram na linha do ciclo seguinte.
        operador.identificar(
            "Ana Operadora",
            List.of("OPERADOR_REATOR", "SUPERVISAO_CENTRAL"),
            OperadorDeServico.Origem.DECLARADO_PELA_UI
        );
        publicarCiclo(eventBus, T0.plusSeconds(18), 310.0, 154.0, 2.4, 1100.0);
        SnapshotOperacional comOperador = ultima(registro);
        exigir("Ana Operadora".equals(comOperador.operador()),
            "A linha deve registrar o operador de servico identificado.");
        exigir(comOperador.papeisOperador().equals(List.of("OPERADOR_REATOR", "SUPERVISAO_CENTRAL")),
            "A linha deve registrar os papeis do operador.");
        exigir(comOperador.origemOperador() == OperadorDeServico.Origem.DECLARADO_PELA_UI,
            "A origem deve distinguir nome declarado de sessao validada.");

        // 12. Fim de turno volta ao estado nao identificado.
        operador.liberar();
        publicarCiclo(eventBus, T0.plusSeconds(21), 309.0, 153.0, 2.3, 1098.0);
        SnapshotOperacional semOperador = ultima(registro);
        exigir(semOperador.operador().isEmpty() && !operador.identificado(),
            "Apos liberar o turno, a linha nao deve afirmar operador.");

        // 13. Identificacao exige nome e origem explicita.
        exigirErro(() -> operador.identificar("", List.of(), OperadorDeServico.Origem.DECLARADO_PELA_UI),
            "Identificar sem nome deve ser recusado.");
        exigirErro(() -> operador.identificar("Alguem", List.of(), OperadorDeServico.Origem.NAO_IDENTIFICADO),
            "NAO_IDENTIFICADO nao pode ser usado como origem de identificacao.");

        // 14. Ida e volta pelo CSV preserva os campos, inclusive operador e papeis.
        SnapshotOperacional reconstruida = SnapshotOperacional.deLinhaCsv(primeira.linhaCsv());
        exigir(reconstruida != null && reconstruida.equals(primeira),
            "Serializar e desserializar a linha CSV deve preservar todos os campos.");
        SnapshotOperacional reconstruidaComOperador =
            SnapshotOperacional.deLinhaCsv(comOperador.linhaCsv());
        exigir(reconstruidaComOperador != null && reconstruidaComOperador.equals(comOperador),
            "O round-trip deve preservar operador, papeis e origem.");
        exigir(SnapshotOperacional.deLinhaCsv(SnapshotOperacional.CABECALHO) == null,
            "A linha de cabecalho nao deve virar snapshot.");
        exigir(SnapshotOperacional.deLinhaCsv("lixo;malformado") == null,
            "Linha malformada deve ser ignorada em vez de quebrar a leitura.");

        // 10. RNF09: reinicio recupera tudo o que foi gravado.
        int gravadas = registro.tamanho();
        RegistroHistorico novaSessao = new RegistroHistorico(arquivo);
        exigir(novaSessao.tamanho() == gravadas,
            "Reinicio deve recuperar todas as " + gravadas + " linhas do arquivo.");
        exigir(novaSessao.consultar().get(0).equals(primeira),
            "A primeira linha recuperada deve ser identica a gravada.");

        // 11. Cabecalho gravado uma unica vez.
        long cabecalhos = lerLinhas(arquivo).stream()
            .filter(l -> l.startsWith("instante"))
            .count();
        exigir(cabecalhos == 1, "O cabecalho CSV deve aparecer exatamente uma vez.");
    }

    private static MedicaoRegistrada medicao(
        Instant instante, long sensorId, String tipo, String unidade, double valor
    ) {
        return new MedicaoRegistrada(instante, sensorId, tipo, unidade, valor, 0.0, 400.0);
    }

    /** Publica um ciclo completo dos 4 sensores RF-1, o que fecha uma linha do histórico. */
    private static void publicarCiclo(
        EventBus eventBus, Instant instante,
        double temperatura, double pressao, double radiacao, double fluxo
    ) {
        eventBus.publicar(medicao(instante, 1L, "TEMPERATURA", "Celsius", temperatura));
        eventBus.publicar(medicao(instante, 2L, "PRESSAO", "bar", pressao));
        eventBus.publicar(medicao(instante, 3L, "RADIACAO", "mSv/h", radiacao));
        eventBus.publicar(medicao(instante, 4L, "FLUXO_RESFRIAMENTO", "m3/h", fluxo));
    }

    private static SnapshotOperacional ultima(RegistroHistorico registro) {
        List<SnapshotOperacional> todas = registro.consultar();
        return todas.get(todas.size() - 1);
    }

    private static List<String> lerLinhas(Path arquivo) {
        try {
            return Files.readAllLines(arquivo);
        } catch (IOException error) {
            throw new IllegalStateException("Nao foi possivel reler o arquivo de historico.", error);
        }
    }

    private static void apagar(Path pasta) {
        try (Stream<Path> caminhos = Files.walk(pasta)) {
            caminhos.sorted(Comparator.reverseOrder()).forEach(caminho -> {
                try {
                    Files.deleteIfExists(caminho);
                } catch (IOException ignorada) {
                    // Pasta temporária; falha de limpeza não invalida o teste.
                }
            });
        } catch (IOException ignorada) {
            // idem
        }
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            System.err.println("[FALHA] " + mensagem);
            throw new AssertionError(mensagem);
        }
        System.out.println("[OK] " + mensagem);
    }

    private static void exigirErro(Runnable acao, String mensagem) {
        try {
            acao.run();
        } catch (IllegalArgumentException | IllegalStateException esperada) {
            System.out.println("[OK] " + mensagem);
            return;
        }
        System.err.println("[FALHA] " + mensagem);
        throw new AssertionError(mensagem);
    }
}
