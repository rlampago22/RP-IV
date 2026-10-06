package br.edu.unipampa.usina.apiestado;

import br.edu.unipampa.usina.alarmes.AlarmeFacade;
import br.edu.unipampa.usina.controlereator.ReatorFacade;
import br.edu.unipampa.usina.historicooperacional.FiltroHistorico;
import br.edu.unipampa.usina.historicooperacional.OperadorDeServico;
import br.edu.unipampa.usina.historicooperacional.RegistroHistorico;
import br.edu.unipampa.usina.historicooperacional.ResultadoConsulta;
import br.edu.unipampa.usina.historicooperacional.SnapshotOperacional;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /** Fuso do relatório de operação: o dia do histórico é o dia local, não o dia UTC. */
    private static final ZoneId FUSO_OPERACAO = ZoneId.of("America/Sao_Paulo");

    private final EstadoAgregador estado;
    private final AlarmeFacade alarmes;
    private final ReatorFacade reator;
    private final RegistroHistorico historico;
    private final OperadorDeServico operadorDeServico;
    private final int porta;
    private HttpServer server;

    public ApiEstadoHttpServer(
        EstadoAgregador estado,
        AlarmeFacade alarmes,
        ReatorFacade reator,
        RegistroHistorico historico,
        OperadorDeServico operadorDeServico,
        int porta
    ) {
        this.estado = estado;
        this.alarmes = alarmes;
        this.reator = reator;
        this.historico = historico;
        this.operadorDeServico = operadorDeServico;
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
        server.createContext("/api/historico", this::tratarHistorico);
        server.createContext("/api/historico/dias", this::tratarHistoricoDias);
        server.createContext("/api/historico/exportar", this::tratarExportarHistorico);
        server.createContext("/api/turno", this::tratarTurno);
        server.createContext("/api/turno/liberar", this::tratarLiberarTurno);
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

    /** RF16 — consulta filtrada do histórico operacional. Critérios na query string. */
    private void tratarHistorico(HttpExchange exchange) throws IOException {
        if (comCorsEPreflight(exchange, "GET")) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responder(exchange, 405, "{\"erro\":\"metodo nao suportado\"}");
            return;
        }
        Map<String, String> parametros = parametrosDaQuery(exchange.getRequestURI().getRawQuery());
        try {
            FiltroHistorico filtro = filtroDe(parametros);
            int limite = inteiro(parametros.get("limite"), RegistroHistorico.LIMITE_PADRAO);
            ResultadoConsulta resultado = historico.consultar(filtro, limite);
            responder(exchange, 200, HistoricoJson.escrever(resultado, filtro));
        } catch (FiltroHistorico.FiltroInvalidoException invalido) {
            responder(exchange, 400, "{\"erro\":" + jsonTexto(invalido.getMessage()) + "}");
        }
    }

    /**
     * RF16 — saídas de operação: um resumo por (dia local + operador), para o primeiro nível do
     * T06. O dia é calculado em {@link #FUSO_OPERACAO}, não em UTC.
     */
    private void tratarHistoricoDias(HttpExchange exchange) throws IOException {
        if (comCorsEPreflight(exchange, "GET")) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responder(exchange, 405, "{\"erro\":\"metodo nao suportado\"}");
            return;
        }
        try {
            FiltroHistorico filtro = filtroDe(parametrosDaQuery(exchange.getRequestURI().getRawQuery()));
            responder(exchange, 200, HistoricoJson.escreverDias(
                historico.agruparPorDia(filtro, FUSO_OPERACAO), filtro, FUSO_OPERACAO));
        } catch (FiltroHistorico.FiltroInvalidoException invalido) {
            responder(exchange, 400, "{\"erro\":" + jsonTexto(invalido.getMessage()) + "}");
        }
    }

    /** RF17 — baixa o recorte filtrado como CSV, no mesmo formato do arquivo persistido. */
    private void tratarExportarHistorico(HttpExchange exchange) throws IOException {
        if (comCorsEPreflight(exchange, "GET")) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            responder(exchange, 405, "{\"erro\":\"metodo nao suportado\"}");
            return;
        }
        Map<String, String> parametros = parametrosDaQuery(exchange.getRequestURI().getRawQuery());
        try {
            FiltroHistorico filtro = filtroDe(parametros);
            StringBuilder csv = new StringBuilder(SnapshotOperacional.CABECALHO).append('\n');
            for (SnapshotOperacional registro : historico.exportar(filtro)) {
                csv.append(registro.linhaCsv()).append('\n');
            }
            responderCsv(exchange, nomeDoArquivo(), csv.toString());
        } catch (FiltroHistorico.FiltroInvalidoException invalido) {
            responder(exchange, 400, "{\"erro\":" + jsonTexto(invalido.getMessage()) + "}");
        }
    }

    private static String nomeDoArquivo() {
        return "historico-operacional-"
            + Instant.now().toString().replace(':', '-').replace('.', '-')
            + ".csv";
    }

    /**
     * Declara quem está de serviço, para o histórico registrar o operador junto das medições.
     *
     * <p>A origem é <b>sempre</b> {@code DECLARADO_PELA_UI}: esta rota não valida sessão nenhuma
     * (a autenticação roda em outro processo), então ela não pode ter o poder de afirmar
     * {@code SESSAO_VALIDADA} — senão um {@code curl} se passaria por identidade verificada.
     */
    private void tratarTurno(HttpExchange exchange) throws IOException {
        if (comCorsEPreflight(exchange, "POST")) {
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            responder(exchange, 405, "{\"erro\":\"metodo nao suportado\"}");
            return;
        }
        Map<String, String> parametros = parametrosDaQuery(exchange.getRequestURI().getRawQuery());
        String nome = parametros.get("operador");
        if (nome == null || nome.isBlank()) {
            responder(exchange, 400, "{\"erro\":\"informe o parametro operador\"}");
            return;
        }
        operadorDeServico.identificar(
            nome,
            papeisDeParametro(parametros.get("papeis")),
            OperadorDeServico.Origem.DECLARADO_PELA_UI
        );
        responder(exchange, 200, turnoJson());
    }

    private void tratarLiberarTurno(HttpExchange exchange) throws IOException {
        if (comCorsEPreflight(exchange, "POST")) {
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            responder(exchange, 405, "{\"erro\":\"metodo nao suportado\"}");
            return;
        }
        operadorDeServico.liberar();
        responder(exchange, 200, turnoJson());
    }

    private String turnoJson() {
        StringBuilder papeis = new StringBuilder("[");
        List<String> lista = operadorDeServico.papeis();
        for (int i = 0; i < lista.size(); i++) {
            if (i > 0) {
                papeis.append(',');
            }
            papeis.append(jsonTexto(lista.get(i)));
        }
        papeis.append(']');
        Instant desde = operadorDeServico.identificadoEm();
        return "{\"operador\":" + jsonTexto(operadorDeServico.nome())
            + ",\"papeis\":" + papeis
            + ",\"origem\":" + jsonTexto(operadorDeServico.origem().name())
            + ",\"identificadoEm\":" + (desde == null ? "null" : jsonTexto(desde.toString()))
            + "}";
    }

    private static List<String> papeisDeParametro(String valor) {
        if (valor == null || valor.isBlank()) {
            return List.of();
        }
        return List.of(valor.split("[|,]"));
    }

    private static FiltroHistorico filtroDe(Map<String, String> p) {
        return new FiltroHistorico(
            instante(p.get("de"), "de"),
            instante(p.get("ate"), "ate"),
            new FiltroHistorico.Faixa(decimal(p.get("temperaturaMin"), "temperaturaMin"),
                decimal(p.get("temperaturaMax"), "temperaturaMax")),
            new FiltroHistorico.Faixa(decimal(p.get("pressaoMin"), "pressaoMin"),
                decimal(p.get("pressaoMax"), "pressaoMax")),
            new FiltroHistorico.Faixa(decimal(p.get("radiacaoMin"), "radiacaoMin"),
                decimal(p.get("radiacaoMax"), "radiacaoMax")),
            new FiltroHistorico.Faixa(decimal(p.get("fluxoMin"), "fluxoMin"),
                decimal(p.get("fluxoMax"), "fluxoMax")),
            p.get("operador")
        );
    }

    private static Map<String, String> parametrosDaQuery(String query) {
        Map<String, String> parametros = new HashMap<>();
        if (query == null || query.isBlank()) {
            return parametros;
        }
        for (String par : query.split("&")) {
            int igual = par.indexOf('=');
            if (igual <= 0) {
                continue;
            }
            String chave = URLDecoder.decode(par.substring(0, igual), StandardCharsets.UTF_8);
            String valor = URLDecoder.decode(par.substring(igual + 1), StandardCharsets.UTF_8);
            if (!valor.isBlank()) {
                parametros.put(chave, valor);
            }
        }
        return parametros;
    }

    private static Instant instante(String valor, String campo) {
        if (valor == null) {
            return null;
        }
        try {
            return Instant.parse(valor);
        } catch (RuntimeException invalido) {
            throw new FiltroHistorico.FiltroInvalidoException(
                "Parametro " + campo + " deve ser data ISO-8601 (ex.: 2026-10-05T12:00:00Z).");
        }
    }

    private static Double decimal(String valor, String campo) {
        if (valor == null) {
            return null;
        }
        try {
            return Double.parseDouble(valor);
        } catch (NumberFormatException invalido) {
            throw new FiltroHistorico.FiltroInvalidoException("Parametro " + campo + " deve ser numerico.");
        }
    }

    private static int inteiro(String valor, int padrao) {
        if (valor == null) {
            return padrao;
        }
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException invalido) {
            throw new FiltroHistorico.FiltroInvalidoException("Parametro limite deve ser inteiro.");
        }
    }

    private static String jsonTexto(String valor) {
        return "\"" + (valor == null ? "" : valor.replace("\\", "\\\\").replace("\"", "\\\"")) + "\"";
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

    /**
     * Devolve um arquivo para download. O BOM UTF-8 faz o Excel reconhecer a codificação — sem
     * ele, nomes de operador acentuados (ex.: "Supervisão Central") saem corrompidos na planilha.
     */
    private void responderCsv(HttpExchange exchange, String nomeArquivo, String csv) throws IOException {
        byte[] corpo = csv.getBytes(StandardCharsets.UTF_8);
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        exchange.getResponseHeaders().add("Content-Type", "text/csv; charset=utf-8");
        exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + nomeArquivo + "\"");
        exchange.sendResponseHeaders(200, bom.length + corpo.length);
        try (OutputStream saida = exchange.getResponseBody()) {
            saida.write(bom);
            saida.write(corpo);
        }
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
