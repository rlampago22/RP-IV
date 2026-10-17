package br.edu.unipampa.usina.apiestado;

import br.edu.unipampa.usina.alarmes.AlarmeFacade;
import br.edu.unipampa.usina.controlereator.ReatorFacade;
import br.edu.unipampa.usina.segurancaacesso.AcessoFacade;
import br.edu.unipampa.usina.segurancaacesso.RegistroAcesso;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stub HTTP (JDK {@code com.sun.net.httpserver} — Zero External Dependencies) que expõe o
 * estado derivado do EventBus para o frontend React (T01–T05, Opção A).
 * Rotas e formato documentados em docs/api-estado-contrato.md.
 */
public final class ApiEstadoHttpServer {
    private static final Pattern ALARME_RECONHECER = Pattern.compile("^/api/alarmes/([^/]+)/reconhecer$");
    private static final Pattern ALARME_RESOLVER = Pattern.compile("^/api/alarmes/([^/]+)/resolver$");
    private static final Pattern CENARIO_PATTERN = Pattern.compile("^/api/cenarios/([^/]+)$");

    private final EstadoAgregador estado;
    private final AlarmeFacade alarmes;
    private final ReatorFacade reator;
    private final AcessoFacade acesso;
    private final int porta;
    private HttpServer server;

    public ApiEstadoHttpServer(EstadoAgregador estado, AlarmeFacade alarmes, ReatorFacade reator, int porta) {
        this(estado, alarmes, reator, null, porta);
    }

    public ApiEstadoHttpServer(
        EstadoAgregador estado,
        AlarmeFacade alarmes,
        ReatorFacade reator,
        AcessoFacade acesso,
        int porta
    ) {
        this.estado = estado;
        this.alarmes = alarmes;
        this.reator = reator;
        this.acesso = acesso;
        this.porta = porta;
    }

    public void iniciar() throws IOException {
        server = HttpServer.create(new InetSocketAddress(porta), 0);
        server.createContext("/api/estado", this::tratarEstado);
        server.createContext("/api/tempo-real/iniciar", exchange -> tratarTempoReal(exchange, true));
        server.createContext("/api/tempo-real/pausar", exchange -> tratarTempoReal(exchange, false));
        server.createContext("/api/cenarios/", this::tratarCenario);
        server.createContext("/api/demo/anomalia", this::tratarAnomaliaDemo);
        server.createContext("/api/alarmes/", this::tratarAcaoAlarme);
        server.createContext("/api/acesso/", this::tratarAcesso);
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

    /**
     * T05 (#52) + seeds T02 (#50): {@code POST /api/cenarios/{normal|observacao|falha|critico}}.
     * Normal/Observação usam {@link CenariosMedicao} (histórico previsível).
     */
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
        estado.definirTempoReal(false);
        switch (cenario) {
            case "normal" -> CenariosMedicao.aplicarNormal(reator);
            case "observacao" -> CenariosMedicao.aplicarObservacao(reator);
            case "falha" -> reator.simularFalhaSensor(
                2L,
                "Falha de comunicacao de telemetria no barramento"
            );
            case "critico" -> aplicarAnomaliaCritica();
            default -> {
                responder(exchange, 400, "{\"erro\":\"cenario desconhecido: " + cenario + "\"}");
                return;
            }
        }
        responder(exchange, 200, EstadoJson.escrever(estado.snapshot()));
    }

    /** Alias do roteiro José (#66): mesmo efeito de {@code POST /api/cenarios/critico}. */
    private void tratarAnomaliaDemo(HttpExchange exchange) throws IOException {
        if (comCorsEPreflight(exchange, "POST")) {
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            responder(exchange, 405, "{\"erro\":\"metodo nao suportado\"}");
            return;
        }
        estado.definirTempoReal(false);
        aplicarAnomaliaCritica();
        responder(exchange, 200, EstadoJson.escrever(estado.snapshot()));
    }

    /** Temp crítica; fluxo baixo só se o sensor 4 estiver cadastrado (demo completa). */
    private void aplicarAnomaliaCritica() {
        reator.receberLeitura(1L, 372.0);
        if (reator.obterSensor(4L) != null) {
            reator.receberLeitura(4L, 420.0);
        }
    }

    private void tratarAcaoAlarme(HttpExchange exchange) throws IOException {
        if (comCorsEPreflight(exchange, "POST")) {
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            responder(exchange, 405, "{\"erro\":\"metodo nao suportado\"}");
            return;
        }

        String corpo = lerCorpo(exchange);
        String caminho = exchange.getRequestURI().getPath();
        Matcher reconhecer = ALARME_RECONHECER.matcher(caminho);
        if (reconhecer.matches()) {
            String operador = lerCampoJson(corpo, "operador", "Operador de Reator (UI Web)");
            String justificativa = lerCampoJson(corpo, "justificativa", "Reconhecido via T01 Overview");
            boolean ok = alarmes.reconhecerAlarme(reconhecer.group(1), operador, justificativa);
            responder(exchange, ok ? 200 : 404, EstadoJson.escrever(estado.snapshot()));
            return;
        }

        Matcher resolver = ALARME_RESOLVER.matcher(caminho);
        if (resolver.matches()) {
            String responsavel = lerCampoJson(corpo, "operador", "Engenheiro de Turno (UI Web)");
            if ("Engenheiro de Turno (UI Web)".equals(responsavel)) {
                responsavel = lerCampoJson(corpo, "responsavel", responsavel);
            }
            String solucao = lerCampoJson(corpo, "solucao", "Parametros normalizados via T01 Overview");
            boolean ok = alarmes.resolverAlarme(resolver.group(1), responsavel, solucao);
            responder(exchange, ok ? 200 : 404, EstadoJson.escrever(estado.snapshot()));
            return;
        }

        responder(exchange, 404, "{\"erro\":\"rota nao encontrada\"}");
    }

    private void tratarAcesso(HttpExchange exchange) throws IOException {
        String caminho = exchange.getRequestURI().getPath();
        if (caminho.endsWith("/solicitar")) {
            if (comCorsEPreflight(exchange, "POST")) {
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                responder(exchange, 405, "{\"erro\":\"metodo nao suportado\"}");
                return;
            }
            if (acesso == null) {
                responder(exchange, 503, "{\"erro\":\"modulo de acesso indisponivel\"}");
                return;
            }
            String corpo = lerCorpo(exchange);
            String credencial = lerCampoJson(corpo, "credencial", "");
            String area = lerCampoJson(corpo, "area", "");
            RegistroAcesso registro = acesso.solicitarAcesso(credencial, area);
            responder(exchange, 200, escreverRegistro(registro));
            return;
        }
        if (caminho.endsWith("/registros")) {
            if (comCorsEPreflight(exchange, "GET")) {
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                responder(exchange, 405, "{\"erro\":\"metodo nao suportado\"}");
                return;
            }
            if (acesso == null) {
                responder(exchange, 503, "{\"erro\":\"modulo de acesso indisponivel\"}");
                return;
            }
            responder(exchange, 200, escreverLista(acesso.listarRegistros()));
            return;
        }
        if (comCorsEPreflight(exchange, "GET")) {
            return;
        }
        responder(exchange, 404, "{\"erro\":\"rota de acesso nao encontrada\"}");
    }

    private static String escreverRegistro(RegistroAcesso registro) {
        return "{"
            + "\"id\":\"" + escapar(registro.getId()) + "\","
            + "\"credencial\":\"" + escapar(registro.getCredencial()) + "\","
            + "\"area\":\"" + escapar(registro.getArea()) + "\","
            + "\"resultado\":\"" + escapar(registro.getResultado()) + "\","
            + "\"ocorridoEm\":\"" + registro.getOcorridoEm() + "\""
            + "}";
    }

    private static String escreverLista(List<RegistroAcesso> registros) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < registros.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escreverRegistro(registros.get(i)));
        }
        return sb.append(']').toString();
    }

    private static String lerCorpo(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String lerCampoJson(String json, String campo, String padrao) {
        if (json == null || json.isBlank()) {
            return padrao;
        }
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(campo) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            String valor = matcher.group(1).trim();
            return valor.isEmpty() ? padrao : valor;
        }
        return padrao;
    }

    private static String escapar(String valor) {
        return valor == null ? "" : valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }

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
