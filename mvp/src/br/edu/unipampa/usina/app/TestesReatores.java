package br.edu.unipampa.usina.app;

import br.edu.unipampa.usina.alarmes.AlarmeFacade;
import br.edu.unipampa.usina.alarmes.AlarmeFactory;
import br.edu.unipampa.usina.alarmes.AvaliadorFaixaSegura;
import br.edu.unipampa.usina.apiestado.AlarmeEstado;
import br.edu.unipampa.usina.apiestado.ApiEstadoHttpServer;
import br.edu.unipampa.usina.apiestado.EstadoAgregador;
import br.edu.unipampa.usina.apiestado.EstadoJson;
import br.edu.unipampa.usina.apiestado.EstadoSnapshot;
import br.edu.unipampa.usina.apiestado.ReatorEstado;
import br.edu.unipampa.usina.apiestado.SensorReatorEstado;
import br.edu.unipampa.usina.apiestado.StatusReator;
import br.edu.unipampa.usina.controlereator.ReatorFacade;
import br.edu.unipampa.usina.controlereator.Sensor;
import br.edu.unipampa.usina.historicooperacional.HistoricoSubscriber;
import br.edu.unipampa.usina.historicooperacional.OperadorDeServico;
import br.edu.unipampa.usina.historicooperacional.RegistroHistorico;
import br.edu.unipampa.usina.historicooperacional.SnapshotOperacional;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import br.edu.unipampa.usina.persistencia.ConexaoBanco;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Testes do monitoramento de 5 reatores: estado separado por reator, lâmpada de nível,
 * alarme com reator de origem, compatibilidade dos campos do reator principal e isolamento do
 * histórico operacional.
 */
public final class TestesReatores {
    private TestesReatores() {}

    public static void main(String[] args) throws Exception {
        System.out.println("================================================================================");
        System.out.println(" TESTES DOS 5 REATORES (painel de monitoramento)");
        System.out.println("================================================================================");

        cenarios(ConexaoBanco.emMemoria("testes-reatores-" + System.nanoTime()));
        cenariosPorReatorViaHttp(ConexaoBanco.emMemoria("testes-reatores-http-" + System.nanoTime()));

        System.out.println("\n[SUCESSO] Testes dos 5 reatores passaram com 100% de exito!");
    }

    private static void cenarios(ConexaoBanco banco) {
        EventBus eventBus = new EventBus();
        RegistroHistorico historico = new RegistroHistorico(banco);
        new HistoricoSubscriber(eventBus, historico, new OperadorDeServico());
        new AlarmeFacade(eventBus, new AvaliadorFaixaSegura(), new AlarmeFactory());
        EstadoAgregador estado = new EstadoAgregador(eventBus);

        ReatorFacade r01 = reator(eventBus, "R-01");
        ReatorFacade r03 = reator(eventBus, "R-03");

        // 1. Leituras normais nos dois: cada reator aparece separado, e os campos antigos são do R-01.
        cicloNormal(r01);
        cicloNormal(r03);
        EstadoSnapshot inicial = estado.snapshot();
        exigir(inicial.reatores().size() == 2, "Devem existir 2 reatores no snapshot.");
        exigir(inicial.reatores().get(0).id().equals("R-01") && inicial.reatores().get(1).id().equals("R-03"),
            "Reatores devem vir ordenados por id.");
        exigir(inicial.sensores().size() == 4, "Campo sensores (compatibilidade) deve ter so os 4 do R-01, nao 8.");
        exigir(nivel(inicial, "R-01") == StatusReator.ESTAVEL && nivel(inicial, "R-03") == StatusReator.ESTAVEL,
            "Leituras na faixa ideal devem deixar as duas lampadas verdes.");

        // 2. Radiação do R-03 na faixa de atenção: só a lâmpada dele fica amarela.
        r03.receberLeitura(3L, 4.5);
        EstadoSnapshot atencao = estado.snapshot();
        exigir(nivel(atencao, "R-03") == StatusReator.ATENCAO, "R-03 com radiacao 4.5 deve ficar ATENCAO.");
        exigir(nivel(atencao, "R-01") == StatusReator.ESTAVEL, "R-01 nao deve ser afetado pelo R-03.");
        exigir(atencao.status() == StatusReator.ESTAVEL, "Status do principal nao deve mudar por observacao do R-03.");

        // 3. Radiação do R-03 acima do limite: lâmpada vermelha e alarme com reator de origem.
        r03.receberLeitura(3L, 5.3);
        EstadoSnapshot critico = estado.snapshot();
        exigir(nivel(critico, "R-03") == StatusReator.CRITICO, "R-03 acima do limite deve ficar CRITICO.");
        exigir(reator(critico, "R-03").alarmesAtivos() == 1, "R-03 deve ter 1 alarme ativo.");
        exigir(reator(critico, "R-01").alarmesAtivos() == 0, "R-01 nao deve ter alarme.");
        AlarmeEstado alarme = critico.alarmes().get(0);
        exigir("R-03".equals(alarme.reatorId()), "Alarme deve registrar o reator de origem.");
        exigir(alarme.mensagem().startsWith("R-03"), "Mensagem do alarme deve citar o reator.");
        exigir(critico.status() == StatusReator.ESTAVEL,
            "Status do principal (badge do T01) nao deve ir a CRITICO por alarme de outro reator.");

        // 4. Radiação volta ao normal: a lâmpada volta ao verde, mas o alarme segue ativo até ser tratado.
        r03.receberLeitura(3L, 2.4);
        EstadoSnapshot recuperado = estado.snapshot();
        exigir(nivel(recuperado, "R-03") == StatusReator.ESTAVEL, "Lampada deve voltar ao verde com a leitura normal.");
        exigir(reator(recuperado, "R-03").alarmesAtivos() == 1, "Alarme deve continuar ativo ate ser reconhecido.");

        // 5. Série guarda instante e valor de cada leitura.
        SensorReatorEstado radiacao = reator(recuperado, "R-03").sensores().stream()
            .filter(s -> s.id() == 3L).findFirst().orElseThrow();
        exigir(radiacao.serie().size() == 4, "Serie da radiacao do R-03 deve ter 4 pontos.");
        exigir(radiacao.serie().get(2).valor() == 5.3, "Serie deve manter a ordem das leituras.");
        exigir(!radiacao.serie().get(3).instante().isBefore(radiacao.serie().get(0).instante()),
            "Serie deve ir da mais antiga a mais recente.");

        // 6. Série tem teto: não cresce sem limite com o tempo real ligado.
        for (int i = 0; i < 100; i++) {
            r03.receberLeitura(1L, 310.0);
        }
        SensorReatorEstado temperatura = reator(estado.snapshot(), "R-03").sensores().stream()
            .filter(s -> s.id() == 1L).findFirst().orElseThrow();
        exigir(temperatura.serie().size() == 60, "Serie deve parar em 60 pontos.");
        exigir(estado.snapshot().sensores().get(0).historico().size() <= 6,
            "Historico curto do T02 deve continuar com no maximo 6 valores.");

        // 7. Histórico operacional (CSV) cobre os 5 reatores (corrigido em 16/11): ciclo completo
        // do R-03 grava sua própria linha, sem se misturar com a do R-01.
        int linhasAntes = historico.tamanho();
        cicloNormal(r03);
        exigir(historico.tamanho() == linhasAntes + 1, "Ciclo do R-03 deve gravar no historico do R-03.");
        exigir("R-03".equals(ultima(historico).reatorId()), "Linha gravada deve apontar para o R-03.");
        cicloNormal(r01);
        exigir(historico.tamanho() == linhasAntes + 2, "Ciclo do R-01 deve continuar gravando separadamente.");
        exigir("R-01".equals(ultima(historico).reatorId()), "Linha gravada deve apontar para o R-01.");

        // 8. Contrato JSON leva os reatores e o reator do alarme.
        String json = EstadoJson.escrever(estado.snapshot());
        exigir(json.contains("\"reatores\":[{\"id\":\"R-01\""), "JSON deve conter a lista de reatores.");
        exigir(json.contains("\"nivel\":"), "JSON deve conter o nivel de cada reator.");
        exigir(json.contains("\"serie\":[["), "JSON deve conter a serie como pares [instante, valor].");
        exigir(json.contains("\"reatorId\":\"R-03\""), "JSON do alarme deve conter o reator.");

        // 9. Reator sem id explícito continua sendo o principal (compatibilidade).
        exigir(new ReatorFacade(eventBus).getReatorId().equals("R-01"), "Facade sem id deve ser o R-01.");
    }

    /** Aba Demo com reator escolhido: o cenário cai só naquele reator e não pausa os outros. */
    private static void cenariosPorReatorViaHttp(ConexaoBanco banco) throws Exception {
        EventBus eventBus = new EventBus();
        AlarmeFacade alarmes = new AlarmeFacade(eventBus, new AvaliadorFaixaSegura(), new AlarmeFactory());
        EstadoAgregador estado = new EstadoAgregador(eventBus);
        ReatorFacade r01 = reator(eventBus, "R-01");
        ReatorFacade r04 = reator(eventBus, "R-04");
        cicloNormal(r01);
        cicloNormal(r04);

        List<String> segurados = new ArrayList<>();
        int porta;
        try (ServerSocket socket = new ServerSocket(0)) {
            porta = socket.getLocalPort();
        }
        ApiEstadoHttpServer servidor = new ApiEstadoHttpServer(
            estado, alarmes, r01, null, new RegistroHistorico(banco), new OperadorDeServico(), porta);
        servidor.registrarReatores(Map.of("R-01", r01, "R-04", r04), segurados::add);
        servidor.iniciar();
        try {
            HttpClient cliente = HttpClient.newHttpClient();
            String base = "http://localhost:" + porta;
            estado.definirTempoReal(true);

            // 10. Crítico no R-04: só ele vai a CRITICO, a simulação dele é segurada e o tempo real segue ligado.
            HttpResponse<String> critico = post(cliente, base + "/api/cenarios/critico?reator=R-04");
            exigir(critico.statusCode() == 200, "Cenario critico com reator deve responder 200.");
            EstadoSnapshot apos = estado.snapshot();
            exigir(nivel(apos, "R-04") == StatusReator.CRITICO, "Cenario critico deve deixar o R-04 CRITICO.");
            exigir(nivel(apos, "R-01") == StatusReator.ESTAVEL, "Cenario no R-04 nao deve afetar o R-01.");
            exigir(apos.alarmes().stream().allMatch(a -> "R-04".equals(a.reatorId())),
                "Alarmes do cenario devem ser do R-04.");
            exigir(segurados.equals(List.of("R-04")), "Simulacao do R-04 (e so dele) deve ser segurada.");
            exigir(estado.isTempoRealAtivo(), "Cenario com reator nao deve pausar o tempo real da usina.");

            // 11. Observação no R-04 deixa a lâmpada dele amarela.
            exigir(post(cliente, base + "/api/cenarios/observacao?reator=R-04").statusCode() == 200,
                "Cenario observacao com reator deve responder 200.");
            exigir(nivel(estado.snapshot(), "R-04") == StatusReator.ATENCAO, "Observacao deve deixar o R-04 ATENCAO.");

            // 12. Reator inexistente: 404, nada muda.
            exigir(post(cliente, base + "/api/cenarios/critico?reator=R-09").statusCode() == 404,
                "Reator desconhecido deve responder 404.");

            // 13. Sem reator: comportamento original (R-01 e tempo real pausado).
            exigir(post(cliente, base + "/api/cenarios/critico").statusCode() == 200,
                "Cenario sem reator deve continuar funcionando.");
            exigir(nivel(estado.snapshot(), "R-01") == StatusReator.CRITICO, "Cenario sem reator deve cair no R-01.");
            exigir(!estado.isTempoRealAtivo(), "Cenario sem reator deve pausar o tempo real, como antes.");
        } finally {
            servidor.parar();
        }
    }

    private static HttpResponse<String> post(HttpClient cliente, String endereco) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create(endereco))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        return cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }

    private static ReatorFacade reator(EventBus eventBus, String id) {
        ReatorFacade reator = new ReatorFacade(eventBus, id);
        reator.registrarSensor(new Sensor(1L, "TEMPERATURA", "Celsius", 0.0, 350.0, 20.0, 325.0));
        reator.registrarSensor(new Sensor(2L, "PRESSAO", "bar", 0.0, 160.0, 10.0, 155.0));
        reator.registrarSensor(new Sensor(3L, "RADIACAO", "mSv/h", 0.0, 5.0, 0.1, 4.2));
        reator.registrarSensor(new Sensor(4L, "FLUXO_RESFRIAMENTO", "m3/h", 500.0, 1500.0, 600.0, 1400.0));
        return reator;
    }

    private static void cicloNormal(ReatorFacade reator) {
        reator.receberLeitura(1L, 310.0);
        reator.receberLeitura(2L, 150.0);
        reator.receberLeitura(3L, 2.4);
        reator.receberLeitura(4L, 1100.0);
    }

    private static SnapshotOperacional ultima(RegistroHistorico registro) {
        List<SnapshotOperacional> todas = registro.consultar();
        return todas.get(todas.size() - 1);
    }

    private static ReatorEstado reator(EstadoSnapshot snapshot, String id) {
        return snapshot.reatores().stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow();
    }

    private static StatusReator nivel(EstadoSnapshot snapshot, String id) {
        return reator(snapshot, id).nivel();
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            System.err.println("[FALHA] " + mensagem);
            throw new AssertionError(mensagem);
        }
        System.out.println("[OK] " + mensagem);
    }
}
