package br.edu.unipampa.usina.app;

import br.edu.unipampa.usina.alarmes.AlarmeFacade;
import br.edu.unipampa.usina.alarmes.AlarmeFactory;
import br.edu.unipampa.usina.alarmes.AvaliadorFaixaSegura;
import br.edu.unipampa.usina.apiestado.ApiEstadoHttpServer;
import br.edu.unipampa.usina.apiestado.EstadoAgregador;
import br.edu.unipampa.usina.apiestado.EstadoJson;
import br.edu.unipampa.usina.auditorialogs.AuditoriaSubscriber;
import br.edu.unipampa.usina.auditorialogs.EntradaAuditoria;
import br.edu.unipampa.usina.auditorialogs.RegistroAuditoria;
import br.edu.unipampa.usina.contingencia.FonteHttpContingencia;
import br.edu.unipampa.usina.contingencia.ProtocoloRemoto;
import br.edu.unipampa.usina.contingencia.SincronizadorContingencia;
import br.edu.unipampa.usina.controlereator.ReatorFacade;
import br.edu.unipampa.usina.controlereator.Sensor;
import br.edu.unipampa.usina.historicooperacional.OperadorDeServico;
import br.edu.unipampa.usina.historicooperacional.RegistroHistorico;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import br.edu.unipampa.usina.persistencia.ConexaoBanco;
import br.edu.unipampa.usina.segurancaacesso.AcessoFacade;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Issue #99 — protocolo de contingência da Central ligado ao EventBus do núcleo: ativar trava as
 * operações de rotina, entra na auditoria e no estado; encerrar destrava; Central fora do ar não
 * destrava (falha segura).
 */
public final class TestesContingencia {
    private TestesContingencia() {}

    public static void main(String[] args) throws Exception {
        System.out.println("=== Testes do protocolo de contingencia no nucleo (#99) ===");

        EventBus eventBus = new EventBus();
        RegistroAuditoria auditoria = new RegistroAuditoria(ConexaoBanco.emMemoria("testes-contingencia-" + System.nanoTime()));
        new AuditoriaSubscriber(eventBus, auditoria);
        EstadoAgregador estado = new EstadoAgregador(eventBus);
        AlarmeFacade alarmes = new AlarmeFacade(eventBus, new AvaliadorFaixaSegura(), new AlarmeFactory());
        AcessoFacade acesso = new AcessoFacade(eventBus);
        acesso.bloquearDuranteContingencia(estado::isContingenciaAtiva);
        ReatorFacade reator = new ReatorFacade(eventBus);
        reator.registrarSensor(new Sensor(1L, "TEMPERATURA", "Celsius", 0.0, 350.0, 20.0, 325.0));
        reator.registrarSensor(new Sensor(2L, "PRESSAO", "bar", 0.0, 160.0, 10.0, 155.0));
        reator.registrarSensor(new Sensor(3L, "RADIACAO", "mSv/h", 0.0, 5.0, 0.1, 4.2));
        reator.registrarSensor(new Sensor(4L, "FLUXO_RESFRIAMENTO", "m3/h", 500.0, 1500.0, 600.0, 1400.0));

        // Dublê da Central: a lista que GET /api/interno/contingencia devolveria.
        List<ProtocoloRemoto> central = new ArrayList<>();
        AtomicBoolean centralFora = new AtomicBoolean(false);
        SincronizadorContingencia sincronizador = new SincronizadorContingencia(eventBus, () -> {
            if (centralFora.get()) {
                throw new IOException("Connection refused");
            }
            return List.copyOf(central);
        });

        int porta = obterPortaLivre();
        RegistroHistorico historico = new RegistroHistorico(
            Files.createTempDirectory("rpiv-contingencia").resolve("historico.csv"));
        ApiEstadoHttpServer servidor = new ApiEstadoHttpServer(
            estado, alarmes, reator, acesso, historico, new OperadorDeServico(), auditoria, porta);
        servidor.iniciar();
        HttpClient cliente = HttpClient.newHttpClient();
        String base = "http://localhost:" + porta;

        try {
            // 1. Operação normal
            exigir(sincronizador.sincronizar() && !estado.isContingenciaAtiva(),
                "CONT-01: Sem protocolo na Central o nucleo opera normalmente.");
            exigir(estado.definirTempoReal(true), "CONT-01: Tempo real liga normalmente.");
            exigir(post(cliente, base + "/api/cenarios/normal").statusCode() == 200,
                "CONT-01: Cenario de rotina responde 200 sem protocolo ativo.");
            estado.definirTempoReal(true);

            // 2. Ativação na Central -> evento no barramento -> trava
            central.add(new ProtocoloRemoto(7, "PROTOCOLO_ATIVO", "DESLIGAMENTO_EMERGENCIAL",
                "Desligamento emergencial do reator", "operador@usina.local", null));
            sincronizador.sincronizar();
            exigir(estado.isContingenciaAtiva(), "CONT-02: Protocolo ativo na Central poe o nucleo em contingencia.");
            exigir(!estado.isTempoRealAtivo(), "CONT-02: Ativar o protocolo corta a simulacao em tempo real.");
            exigir(!estado.definirTempoReal(true) && !estado.isTempoRealAtivo(),
                "CONT-02: Religar o tempo real e recusado durante a contingencia.");
            exigir(contar(auditoria, "PROTOCOLO_CONTINGENCIA_ATIVADO") == 1,
                "CONT-02: A ativacao entra na trilha de auditoria (1 vez).");
            sincronizador.sincronizar();
            exigir(contar(auditoria, "PROTOCOLO_CONTINGENCIA_ATIVADO") == 1,
                "CONT-02: Consultas seguintes nao repetem o evento de ativacao.");
            String json = EstadoJson.escrever(estado.snapshot());
            exigir(json.contains("\"contingencia\":{\"ativa\":true")
                    && json.contains("\"protocolo\":\"DESLIGAMENTO_EMERGENCIAL\""),
                "CONT-02: /api/estado informa o protocolo ativo.");

            // 3. Rotas de rotina travadas; consultas e alarmes seguem
            HttpResponse<String> cenario = post(cliente, base + "/api/cenarios/critico");
            exigir(cenario.statusCode() == 423 && cenario.body().contains("Desligamento emergencial do reator"),
                "CONT-03: Cenario durante a contingencia responde 423 dizendo qual protocolo trava.");
            exigir(post(cliente, base + "/api/demo/anomalia").statusCode() == 423,
                "CONT-03: Anomalia de demonstracao tambem e travada.");
            exigir(post(cliente, base + "/api/tempo-real/iniciar").statusCode() == 423,
                "CONT-03: POST /api/tempo-real/iniciar e travado.");
            exigir(post(cliente, base + "/api/tempo-real/pausar").statusCode() == 200,
                "CONT-03: Pausar o tempo real continua permitido.");
            exigir(get(cliente, base + "/api/estado").statusCode() == 200,
                "CONT-03: Consulta do estado continua liberada.");
            HttpResponse<String> acessoNovo = postJson(cliente, base + "/api/acesso/solicitar",
                "{\"credencial\":\"SUP-01\",\"area\":\"SALA-CONTROLE\"}");
            exigir(acessoNovo.statusCode() == 200 && acessoNovo.body().contains(AcessoFacade.RESULTADO_BLOQUEADO),
                "CONT-03: Novo acesso e bloqueado (registrado como " + AcessoFacade.RESULTADO_BLOQUEADO + ").");
            exigir(contar(auditoria, "ACESSO_REGISTRADO") == 1,
                "CONT-03: A tentativa de acesso bloqueada continua auditada.");

            // 4. Central fora do ar: falha segura, nao destrava
            centralFora.set(true);
            exigir(!sincronizador.sincronizar() && estado.isContingenciaAtiva(),
                "CONT-04: Central fora do ar mantem o nucleo travado (falha segura).");
            centralFora.set(false);

            // 5. Encerramento na Central -> destrava
            central.set(0, new ProtocoloRemoto(7, "ENCERRADA", "DESLIGAMENTO_EMERGENCIAL",
                "Desligamento emergencial do reator", "operador@usina.local", "supervisao@usina.local"));
            sincronizador.sincronizar();
            exigir(!estado.isContingenciaAtiva(), "CONT-05: Encerrar a emergencia tira o nucleo da contingencia.");
            exigir(auditoria.consultar().stream().anyMatch(e -> e.tipoEvento().equals("PROTOCOLO_CONTINGENCIA_ENCERRADO")
                    && e.resumo().contains("encerradoPor=\"supervisao@usina.local\"")),
                "CONT-05: O encerramento entra na auditoria com quem encerrou.");
            exigir(post(cliente, base + "/api/cenarios/normal").statusCode() == 200,
                "CONT-05: Cenarios voltam a funcionar.");
            exigir(estado.definirTempoReal(true), "CONT-05: Tempo real pode ser religado.");
            exigir(acesso.solicitarAcesso("SUP-01", "SALA-CONTROLE").getResultado().equals("SUCESSO"),
                "CONT-05: Acessos voltam a seguir a regra normal.");
            exigir(auditoria.verificarIntegridade(), "CONT-05: Cadeia SHA-256 da auditoria segue integra.");

            // 6. Leitura do JSON real da rota interna da Central (Jackson: nulls, acentos, escapes)
            testarFonteHttp();
        } finally {
            servidor.parar();
            auditoria.close();
        }
        System.out.println("[SUCESSO] Protocolo de contingencia ligado ao EventBus do nucleo.");
    }

    private static void testarFonteHttp() throws Exception {
        String resposta = "[{\"emergenciaId\":12,\"status\":\"PROTOCOLO_ATIVO\",\"protocolo\":\"EVACUACAO_AREA\","
            + "\"nome\":\"Evacuação da área\",\"ativadoPor\":\"admin@usina.local\",\"ativadoEm\":\"2026-11-20T23:00:00Z\","
            + "\"encerradoPor\":null,\"encerradoEm\":null},"
            + "{\"emergenciaId\":3,\"status\":\"ENCERRADA\",\"protocolo\":\"CONTENCAO_VAZAMENTO\","
            + "\"nome\":\"Conten\\u00e7\\u00e3o \\\"vazamento\\\"\",\"ativadoPor\":\"operador@usina.local\","
            + "\"ativadoEm\":\"2026-11-19T10:00:00Z\",\"encerradoPor\":\"supervisao@usina.local\",\"encerradoEm\":\"2026-11-19T11:00:00Z\"}]";
        int porta = obterPortaLivre();
        HttpServer centralFalsa = HttpServer.create(new InetSocketAddress("localhost", porta), 0);
        centralFalsa.createContext("/api/interno/contingencia", troca -> {
            byte[] corpo = resposta.getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(200, corpo.length);
            try (OutputStream saida = troca.getResponseBody()) {
                saida.write(corpo);
            }
        });
        centralFalsa.start();
        try {
            List<ProtocoloRemoto> lidos =
                new FonteHttpContingencia("http://localhost:" + porta + "/api/interno/contingencia").consultar();
            exigir(lidos.size() == 2 && lidos.get(0).ativo() && lidos.get(0).emergenciaId() == 12
                    && "Evacuação da área".equals(lidos.get(0).nome()) && lidos.get(0).encerradoPor() == null,
                "CONT-06: Le o JSON da Central (UTF-8, null) corretamente.");
            exigir(!lidos.get(1).ativo() && "Contenção \"vazamento\"".equals(lidos.get(1).nome())
                    && "supervisao@usina.local".equals(lidos.get(1).encerradoPor()),
                "CONT-06: Le escapes \\uXXXX e aspas do JSON.");
        } finally {
            centralFalsa.stop(0);
        }
        boolean recusou;
        try {
            new FonteHttpContingencia("http://localhost:" + obterPortaLivre() + "/api/interno/contingencia").consultar();
            recusou = false;
        } catch (IOException esperado) {
            recusou = true;
        }
        exigir(recusou, "CONT-06: Central fora do ar vira IOException (o sincronizador mantem o estado).");
    }

    private static long contar(RegistroAuditoria auditoria, String tipo) {
        return auditoria.consultar().stream().map(EntradaAuditoria::tipoEvento).filter(tipo::equals).count();
    }

    private static HttpResponse<String> post(HttpClient cliente, String endereco) throws IOException, InterruptedException {
        return postJson(cliente, endereco, "");
    }

    private static HttpResponse<String> postJson(HttpClient cliente, String endereco, String corpo)
        throws IOException, InterruptedException {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create(endereco))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8))
            .build();
        return cliente.send(requisicao, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> get(HttpClient cliente, String endereco) throws IOException, InterruptedException {
        return cliente.send(HttpRequest.newBuilder(URI.create(endereco)).GET().build(),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static int obterPortaLivre() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            System.err.println("[FALHA] " + mensagem);
            throw new AssertionError(mensagem);
        }
        System.out.println("[OK] " + mensagem);
    }
}
