package br.edu.unipampa.usina.app;

import br.edu.unipampa.usina.alarmes.AlarmeFacade;
import br.edu.unipampa.usina.alarmes.AlarmeFactory;
import br.edu.unipampa.usina.alarmes.AvaliadorFaixaSegura;
import br.edu.unipampa.usina.apiestado.ApiEstadoHttpServer;
import br.edu.unipampa.usina.apiestado.EstadoAgregador;
import br.edu.unipampa.usina.controlereator.ReatorFacade;
import br.edu.unipampa.usina.historicooperacional.OperadorDeServico;
import br.edu.unipampa.usina.historicooperacional.RegistroHistorico;
import br.edu.unipampa.usina.historicooperacional.SnapshotOperacional;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Testes HTTP da consulta ao histórico operacional (RF16, fase 2):
 * filtros por período, faixa e operador, combinação E, limite e recusa de critério inválido.
 */
public final class TestesApiHistorico {
    private static final Instant T0 = Instant.parse("2026-10-05T12:00:00Z");

    private TestesApiHistorico() {}

    public static void main(String[] args) throws Exception {
        System.out.println("================================================================================");
        System.out.println(" TESTES HTTP DA CONSULTA AO HISTORICO (RF16 fase 2)");
        System.out.println("================================================================================");

        Path pasta = Files.createTempDirectory("rpiv-api-historico");
        RegistroHistorico historico = new RegistroHistorico(pasta.resolve("historico.csv"));
        semear(historico);

        EventBus eventBus = new EventBus();
        EstadoAgregador estado = new EstadoAgregador(eventBus);
        AlarmeFacade alarmes = new AlarmeFacade(eventBus, new AvaliadorFaixaSegura(), new AlarmeFactory());
        ReatorFacade reator = new ReatorFacade(eventBus);

        OperadorDeServico operadorDeServico = new OperadorDeServico();
        int porta = obterPortaLivre();
        ApiEstadoHttpServer servidor =
            new ApiEstadoHttpServer(estado, alarmes, reator, historico, operadorDeServico, porta);
        servidor.iniciar();

        try {
            HttpClient cliente = HttpClient.newHttpClient();
            String base = "http://localhost:" + porta + "/api/historico";

            // 1. Sem filtro devolve tudo.
            HttpResponse<String> tudo = get(cliente, base);
            exigir(tudo.statusCode() == 200, "Consulta sem filtro deve responder HTTP 200.");
            exigir(tudo.body().contains("\"total\":5"), "Sem filtro, o total deve ser 5.");
            exigir(tudo.body().contains("\"truncado\":false"), "Sem filtro e sem limite, nao deve truncar.");

            // 2. Periodo recorta o intervalo pedido.
            HttpResponse<String> periodo = get(cliente, base
                + "?de=" + T0.plusSeconds(3) + "&ate=" + T0.plusSeconds(9));
            exigir(periodo.body().contains("\"total\":3"),
                "Periodo de 3s a 9s deve devolver 3 registros.");

            // 3. Faixa de um parametro filtra por valor.
            HttpResponse<String> quentes = get(cliente, base + "?temperaturaMin=312");
            exigir(quentes.body().contains("\"total\":2"),
                "Temperatura minima 312 deve devolver 2 registros.");

            // 4. O ponto da modelagem por ciclo: dois parametros combinam com E.
            HttpResponse<String> combinado = get(cliente, base
                + "?temperaturaMin=310&temperaturaMax=313&pressaoMin=154&pressaoMax=156");
            exigir(combinado.statusCode() == 200, "Filtro combinado deve responder HTTP 200.");
            exigir(combinado.body().contains("\"total\":3"),
                "Temperatura 310-313 E pressao 154-156 deve devolver 3 registros.");

            // 5. Operador casa por trecho e ignora caixa.
            HttpResponse<String> porOperador = get(cliente, base + "?operador=ana");
            exigir(porOperador.body().contains("\"total\":2"),
                "Busca por 'ana' deve achar os 2 registros de Ana Operadora.");
            exigir(porOperador.body().contains("\"origemOperador\":\"DECLARADO_PELA_UI\""),
                "A resposta deve expor a origem da identificacao do operador.");

            // 6. Status derivado vem calculado na resposta.
            HttpResponse<String> criticos = get(cliente, base + "?temperaturaMin=320");
            exigir(criticos.body().contains("\"status\":\"CRITICO\""),
                "A resposta deve trazer o status derivado de cada linha.");

            // 7. Limite trunca e avisa.
            HttpResponse<String> limitado = get(cliente, base + "?limite=2");
            exigir(limitado.body().contains("\"total\":5") && limitado.body().contains("\"retornados\":2"),
                "Com limite 2, total deve seguir 5 e retornados ser 2.");
            exigir(limitado.body().contains("\"truncado\":true"),
                "Resultado cortado pelo limite deve marcar truncado.");

            // 8. Criterios invalidos viram 400, nao 500.
            exigir(get(cliente, base + "?temperaturaMin=abc").statusCode() == 400,
                "Faixa nao numerica deve responder HTTP 400.");
            exigir(get(cliente, base + "?de=ontem").statusCode() == 400,
                "Data fora do ISO-8601 deve responder HTTP 400.");
            exigir(get(cliente, base + "?temperaturaMin=320&temperaturaMax=300").statusCode() == 400,
                "Faixa invertida deve responder HTTP 400.");
            exigir(get(cliente, base + "?de=" + T0.plusSeconds(9) + "&ate=" + T0).statusCode() == 400,
                "Periodo invertido deve responder HTTP 400.");

            // 9. Metodo nao suportado.
            HttpResponse<String> post = cliente.send(
                HttpRequest.newBuilder(URI.create(base)).POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
            exigir(post.statusCode() == 405, "POST em /api/historico deve responder HTTP 405.");

            // 10. Turno: declarar o operador de servico.
            String turno = "http://localhost:" + porta + "/api/turno";
            HttpResponse<String> declarado = post(cliente,
                turno + "?operador=Ana%20Operadora&papeis=OPERADOR_REATOR%7CSUPERVISAO_CENTRAL");
            exigir(declarado.statusCode() == 200, "Declarar turno deve responder HTTP 200.");
            exigir(declarado.body().contains("\"operador\":\"Ana Operadora\""),
                "A resposta deve confirmar o operador declarado.");
            exigir(declarado.body().contains("\"OPERADOR_REATOR\"")
                    && declarado.body().contains("\"SUPERVISAO_CENTRAL\""),
                "A resposta deve confirmar os papeis declarados.");
            exigir(operadorDeServico.nome().equals("Ana Operadora"),
                "O holder deve passar a conhecer o operador de servico.");

            // 11. A rota HTTP nunca pode afirmar sessao validada — so declaracao.
            exigir(declarado.body().contains("\"origem\":\"DECLARADO_PELA_UI\""),
                "A rota deve marcar a origem como DECLARADO_PELA_UI, nunca SESSAO_VALIDADA.");
            exigir(operadorDeServico.origem() == OperadorDeServico.Origem.DECLARADO_PELA_UI,
                "Nenhum parametro da requisicao pode elevar a origem para SESSAO_VALIDADA.");

            // 12. Papeis separados por virgula tambem sao aceitos (forma que o fetch gera sem escape).
            HttpResponse<String> porVirgula = post(cliente,
                turno + "?operador=Bruno%20Rocha&papeis=OPERADOR_REATOR,GUARDA_ACESSO");
            exigir(porVirgula.body().contains("\"OPERADOR_REATOR\"")
                    && porVirgula.body().contains("\"GUARDA_ACESSO\""),
                "Papeis separados por virgula devem ser aceitos.");

            // 13. Sem nome, recusa.
            exigir(post(cliente, turno).statusCode() == 400,
                "Declarar turno sem operador deve responder HTTP 400.");

            // 14. RF17 — exportacao devolve CSV para download, com o mesmo formato do arquivo.
            String exportar = base + "/exportar";
            HttpResponse<String> csv = get(cliente, exportar);
            exigir(csv.statusCode() == 200, "Exportacao deve responder HTTP 200.");
            exigir(csv.headers().firstValue("Content-Type").orElse("").contains("text/csv"),
                "Exportacao deve declarar Content-Type text/csv.");
            exigir(csv.headers().firstValue("Content-Disposition").orElse("").contains("attachment"),
                "Exportacao deve vir como attachment para o navegador baixar.");
            exigir(csv.headers().firstValue("Content-Disposition").orElse("").contains(".csv"),
                "O nome do arquivo deve terminar em .csv.");
            exigir(csv.body().contains(SnapshotOperacional.CABECALHO),
                "O CSV exportado deve usar o mesmo cabecalho do arquivo persistido.");
            exigir(contarLinhas(csv.body()) == 6,
                "Sem filtro, o CSV deve ter cabecalho + 5 registros.");

            // 15. A exportacao respeita o filtro.
            HttpResponse<String> csvFiltrado = get(cliente, exportar + "?temperaturaMin=312");
            exigir(contarLinhas(csvFiltrado.body()) == 3,
                "Com temperatura minima 312, o CSV deve ter cabecalho + 2 registros.");

            // 16. A exportacao ignora o limite da tela: arquivo cortado em silencio seria pior.
            HttpResponse<String> csvComLimite = get(cliente, exportar + "?limite=2");
            exigir(contarLinhas(csvComLimite.body()) == 6,
                "O parametro limite nao deve truncar a exportacao.");

            // 17. Criterio invalido na exportacao tambem vira 400.
            exigir(get(cliente, exportar + "?de=ontem").statusCode() == 400,
                "Exportacao com data invalida deve responder HTTP 400.");

            // 18. Agregacao por dia: uma saida por (dia local + operador).
            HttpResponse<String> dias = get(cliente, base + "/dias");
            exigir(dias.statusCode() == 200, "Agregacao por dia deve responder HTTP 200.");
            exigir(dias.body().contains("\"fusoHorario\":\"America/Sao_Paulo\""),
                "A resposta deve declarar o fuso usado no agrupamento.");
            exigir(dias.body().contains("\"total\":3"),
                "Os 5 registros semeados viram 3 saidas: sem operador, Ana e Bruno.");
            exigir(dias.body().contains("\"operador\":\"Ana Operadora\"")
                    && dias.body().contains("\"operador\":\"Bruno Rocha\""),
                "Cada operador do mesmo dia deve virar uma saida propria.");
            exigir(dias.body().contains("\"piorStatus\":\"CRITICO\""),
                "A saida com alarme ativo deve reportar pior status CRITICO.");
            exigir(dias.body().contains("\"ciclosComAlarme\":1"),
                "A saida do Bruno deve contar 1 ciclo com alarme ativo.");

            // 19. O filtro de operador tambem vale na agregacao.
            HttpResponse<String> diasDaAna = get(cliente, base + "/dias?operador=ana");
            exigir(diasDaAna.body().contains("\"total\":1"),
                "Filtrar a agregacao por operador deve devolver so a saida dele.");

            // 20. Liberar devolve ao estado nao identificado.
            HttpResponse<String> liberado = post(cliente, turno + "/liberar");
            exigir(liberado.statusCode() == 200, "Liberar turno deve responder HTTP 200.");
            exigir(liberado.body().contains("\"origem\":\"NAO_IDENTIFICADO\""),
                "Apos liberar, a origem deve voltar a NAO_IDENTIFICADO.");
            exigir(!operadorDeServico.identificado(),
                "Apos liberar, o holder nao deve ter operador identificado.");

            System.out.println("\n[SUCESSO] Consulta HTTP do historico + turno (RF16 fase 2) passou com 100% de exito!");
        } finally {
            servidor.parar();
        }
    }

    /** Cinco ciclos conhecidos, para os filtros terem resultado previsivel. */
    private static void semear(RegistroHistorico historico) {
        historico.registrar(linha(T0, 310.0, 155.0, 2.4, 1100.0, 0, false, ""));
        historico.registrar(linha(T0.plusSeconds(3), 312.0, 154.0, 2.5, 1095.0, 0, false, "Ana Operadora"));
        historico.registrar(linha(T0.plusSeconds(6), 311.0, 156.0, 2.4, 1090.0, 0, true, "Ana Operadora"));
        historico.registrar(linha(T0.plusSeconds(9), 309.0, 153.0, 2.3, 1105.0, 0, false, "Bruno Rocha"));
        historico.registrar(linha(T0.plusSeconds(12), 325.0, 161.0, 2.6, 1080.0, 1, false, "Bruno Rocha"));
    }

    private static SnapshotOperacional linha(
        Instant instante, double temp, double pressao, double radiacao, double fluxo,
        int alarmesAtivos, boolean observacao, String operador
    ) {
        boolean identificado = !operador.isEmpty();
        return new SnapshotOperacional(
            instante, temp, pressao, radiacao, fluxo,
            alarmesAtivos > 0 ? List.of("2:PRESSAO") : List.of(),
            List.of(),
            observacao ? List.of("1:TEMPERATURA") : List.of(),
            List.of(),
            operador,
            identificado ? List.of("OPERADOR_REATOR") : List.of(),
            identificado
                ? OperadorDeServico.Origem.DECLARADO_PELA_UI
                : OperadorDeServico.Origem.NAO_IDENTIFICADO
        );
    }

    private static HttpResponse<String> get(HttpClient cliente, String url) throws Exception {
        return cliente.send(
            HttpRequest.newBuilder(URI.create(url)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    }

    /** Conta linhas não vazias, ignorando o BOM que o Excel precisa. */
    private static int contarLinhas(String csv) {
        return (int) csv.replace("﻿", "").lines().filter(l -> !l.isBlank()).count();
    }

    private static HttpResponse<String> post(HttpClient cliente, String url) throws Exception {
        return cliente.send(
            HttpRequest.newBuilder(URI.create(url)).POST(HttpRequest.BodyPublishers.noBody()).build(),
            HttpResponse.BodyHandlers.ofString());
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
