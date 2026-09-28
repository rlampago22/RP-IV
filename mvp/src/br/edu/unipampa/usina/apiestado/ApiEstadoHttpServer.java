package br.edu.unipampa.usina.apiestado;

import br.edu.unipampa.usina.alarmes.AlarmeFacade;
import br.edu.unipampa.usina.controlereator.ReatorFacade;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stub HTTP (JDK {@code com.sun.net.httpserver} — Zero External Dependencies) que expõe o
 * estado derivado do EventBus para o frontend React (T01 Overview, Opção A).
 * Rotas e formato documentados em docs/api-estado-contrato.md.
 */
public final class ApiEstadoHttpServer {
    private static final Pattern ALARME_RECONHECER = Pattern.compile("^/api/alarmes/([^/]+)/reconhecer$");
    private static final Pattern ALARME_RESOLVER = Pattern.compile("^/api/alarmes/([^/]+)/resolver$");
    private static final Pattern CENARIO_PATTERN = Pattern.compile("^/api/cenarios/([^/]+)$");

    private final EstadoAgregador estado;
    private final AlarmeFacade alarmes;
    private final ReatorFacade reator;
    private final int porta;
    private HttpServer server;

    public ApiEstadoHttpServer(EstadoAgregador estado, AlarmeFacade alarmes, int porta) {
        this(estado, alarmes, null, porta);
    }

    public ApiEstadoHttpServer(EstadoAgregador estado, AlarmeFacade alarmes, ReatorFacade reator, int porta) {
        this.estado = estado;
        this.alarmes = alarmes;
        this.reator = reator;
        this.porta = porta;
    }

    public void iniciar() throws IOException {
        server = HttpServer.create(new InetSocketAddress(porta), 0);
        server.createContext("/api/estado", this::tratarEstado);
        server.createContext("/api/tempo-real/iniciar", exchange -> tratarTempoReal(exchange, true));
        server.createContext("/api/tempo-real/pausar", exchange -> tratarTempoReal(exchange, false));
        server.createContext("/api/alarmes/", this::tratarAcaoAlarme);
        server.createContext("/api/cenarios/", this::tratarCenario);
        server.setExecutor(null);
        server.start();
        System.out.println("[API ESTADO] Ouvindo em http://localhost:" + porta + "/api/estado");
    }

    public void parar() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void tratarEstado(HttpExchange exchange) throws IOException {
        if (comCorsEPreflight(exchange, "GET")) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responder(exchange, 405, "{\"erro\":\"metodo nao suportado\"}");
            return;
        }
        responder(exchange, 200, EstadoJson.escrever(estado.snapshot()));
    }

    private void tratarTempoReal(HttpExchange exchange, boolean ativo) throws IOException {
        if (comCorsEPreflight(exchange, "POST")) {
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            responder(exchange, 405, "{\"erro\":\"metodo nao suportado\"}");
            return;
        }
        estado.definirTempoReal(ativo);
        responder(exchange, 200, EstadoJson.escrever(estado.snapshot()));
    }

    private void tratarAcaoAlarme(HttpExchange exchange) throws IOException {
        if (comCorsEPreflight(exchange, "POST")) {
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            responder(exchange, 405, "{\"erro\":\"metodo nao suportado\"}");
            return;
        }

        String caminho = exchange.getRequestURI().getPath();
        Matcher reconhecer = ALARME_RECONHECER.matcher(caminho);
        if (reconhecer.matches()) {
            boolean ok = alarmes.reconhecerAlarme(
                reconhecer.group(1),
                "Operador de Reator (UI Web)",
                "Reconhecido via T01 Overview"
            );
            responder(exchange, ok ? 200 : 404, EstadoJson.escrever(estado.snapshot()));
            return;
        }

        Matcher resolver = ALARME_RESOLVER.matcher(caminho);
        if (resolver.matches()) {
            boolean ok = alarmes.resolverAlarme(
                resolver.group(1),
                "Engenheiro de Turno (UI Web)",
                "Parametros normalizados via T01 Overview"
            );
            responder(exchange, ok ? 200 : 404, EstadoJson.escrever(estado.snapshot()));
            return;
        }

        responder(exchange, 404, "{\"erro\":\"rota nao encontrada\"}");
    }

    private void tratarCenario(HttpExchange exchange) throws IOException {
        if (comCorsEPreflight(exchange, "POST")) {
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            responder(exchange, 405, "{\"erro\":\"metodo nao suportado\"}");
            return;
        }

        String caminho = exchange.getRequestURI().getPath();
        Matcher matcher = CENARIO_PATTERN.matcher(caminho);
        if (!matcher.matches()) {
            responder(exchange, 404, "{\"erro\":\"rota de cenario nao encontrada\"}");
            return;
        }

        String cenario = matcher.group(1).toLowerCase();
        if (reator != null) {
            switch (cenario) {
                case "normal" -> {
                    reator.receberLeitura(1L, 310.5);
                    reator.receberLeitura(2L, 155.0);
                    reator.receberLeitura(3L, 2.4);
                    reator.receberLeitura(4L, 1100.0);
                }
                case "observacao" -> {
                    // Valor em faixa de atenção (acima de 325 °C), gera ObservacaoRegistrada sem alarme crítico
                    reator.receberLeitura(1L, 328.0);
                }
                case "falha" -> {
                    // Simula perda de sinal de telemetria no sensor de pressão (UC01 Exceção)
                    reator.simularFalhaSensor(2L, "Falha de comunicacao de telemetria no barramento");
                }
                case "critico" -> {
                    // Anomalia crítica violando limiar seguro (temperatura 372 °C e fluxo de resfriamento 420 m³/h)
                    reator.receberLeitura(1L, 372.0);
                    reator.receberLeitura(4L, 420.0);
                }
                default -> {
                    responder(exchange, 400, "{\"erro\":\"cenario desconhecido: " + cenario + "\"}");
                    return;
                }
            }
        }
        responder(exchange, 200, EstadoJson.escrever(estado.snapshot()));
    }

    /** Adiciona cabeçalhos CORS e resolve o preflight OPTIONS. Retorna true se a requisição já foi respondida. */
    private boolean comCorsEPreflight(HttpExchange exchange, String metodoPermitido) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", metodoPermitido + ", OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private void responder(HttpExchange exchange, int status, String corpoJson) throws IOException {
        byte[] bytes = corpoJson.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream saida = exchange.getResponseBody()) {
            saida.write(bytes);
        }
    }
}
