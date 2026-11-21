package br.edu.unipampa.usina.historico;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * RF16/RF17 — consulta do histórico operacional, agora em banco. Porte das rotas
 * {@code /api/historico}, {@code /api/historico/dias} e {@code /api/historico/exportar} de
 * {@code mvp/.../apiestado/ApiEstadoHttpServer.java}: mesmos parâmetros, mesmo contrato JSON
 * (docs/api-estado-contrato.md) — o {@code mvp/} passa a fazer proxy pra cá, com fallback local
 * se este serviço estiver fora do ar.
 */
@RestController
@RequestMapping("/api/historico")
public class HistoricoConsultaController {
    private static final ZoneId FUSO_OPERACAO = ZoneId.of("America/Sao_Paulo");
    private static final int LIMITE_PADRAO = 500;
    private static final int LIMITE_MAXIMO = 5000;

    private final HistoricoOperacionalRepository repositorio;

    public HistoricoConsultaController(HistoricoOperacionalRepository repositorio) {
        this.repositorio = repositorio;
    }

    @GetMapping
    public ResponseEntity<String> consultar(
        @RequestParam(required = false) String de,
        @RequestParam(required = false) String ate,
        @RequestParam(required = false) String temperaturaMin,
        @RequestParam(required = false) String temperaturaMax,
        @RequestParam(required = false) String pressaoMin,
        @RequestParam(required = false) String pressaoMax,
        @RequestParam(required = false) String radiacaoMin,
        @RequestParam(required = false) String radiacaoMax,
        @RequestParam(required = false) String fluxoMin,
        @RequestParam(required = false) String fluxoMax,
        @RequestParam(required = false) String operador,
        @RequestParam(required = false) String reator,
        @RequestParam(required = false) String limite
    ) {
        try {
            FiltroHistorico filtro = filtroDe(
                de, ate, temperaturaMin, temperaturaMax, pressaoMin, pressaoMax,
                radiacaoMin, radiacaoMax, fluxoMin, fluxoMax, operador, reator
            );
            int teto = Math.min(inteiro(limite, LIMITE_PADRAO), LIMITE_MAXIMO);
            List<HistoricoOperacional> casaram = buscarOrdenado(filtro.reatorId()).stream()
                .filter(filtro::aceita)
                .toList();
            boolean truncado = casaram.size() > teto;
            List<HistoricoOperacional> retornados = truncado ? casaram.subList(0, teto) : casaram;
            return json(HistoricoJson.escrever(retornados, casaram.size(), truncado, filtro));
        } catch (FiltroHistorico.FiltroInvalidoException invalido) {
            return erro(invalido.getMessage());
        }
    }

    @GetMapping("/dias")
    public ResponseEntity<String> consultarDias(
        @RequestParam(required = false) String de,
        @RequestParam(required = false) String ate,
        @RequestParam(required = false) String temperaturaMin,
        @RequestParam(required = false) String temperaturaMax,
        @RequestParam(required = false) String pressaoMin,
        @RequestParam(required = false) String pressaoMax,
        @RequestParam(required = false) String radiacaoMin,
        @RequestParam(required = false) String radiacaoMax,
        @RequestParam(required = false) String fluxoMin,
        @RequestParam(required = false) String fluxoMax,
        @RequestParam(required = false) String operador,
        @RequestParam(required = false) String reator
    ) {
        try {
            FiltroHistorico filtro = filtroDe(
                de, ate, temperaturaMin, temperaturaMax, pressaoMin, pressaoMax,
                radiacaoMin, radiacaoMax, fluxoMin, fluxoMax, operador, reator
            );
            List<ResumoDia> dias =
                HistoricoConsulta.agruparPorDia(buscarOrdenado(filtro.reatorId()), filtro, FUSO_OPERACAO);
            return json(HistoricoJson.escreverDias(dias, filtro, FUSO_OPERACAO));
        } catch (FiltroHistorico.FiltroInvalidoException invalido) {
            return erro(invalido.getMessage());
        }
    }

    @GetMapping("/exportar")
    public ResponseEntity<String> exportar(
        @RequestParam(required = false) String de,
        @RequestParam(required = false) String ate,
        @RequestParam(required = false) String temperaturaMin,
        @RequestParam(required = false) String temperaturaMax,
        @RequestParam(required = false) String pressaoMin,
        @RequestParam(required = false) String pressaoMax,
        @RequestParam(required = false) String radiacaoMin,
        @RequestParam(required = false) String radiacaoMax,
        @RequestParam(required = false) String fluxoMin,
        @RequestParam(required = false) String fluxoMax,
        @RequestParam(required = false) String operador,
        @RequestParam(required = false) String reator
    ) {
        try {
            FiltroHistorico filtro = filtroDe(
                de, ate, temperaturaMin, temperaturaMax, pressaoMin, pressaoMax,
                radiacaoMin, radiacaoMax, fluxoMin, fluxoMax, operador, reator
            );
            List<HistoricoOperacional> casaram = buscarOrdenado(filtro.reatorId()).stream()
                .filter(filtro::aceita)
                .toList();
            String csv = HistoricoOperacional.CABECALHO_CSV + "\n"
                + casaram.stream().map(HistoricoOperacional::linhaCsv).reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"historico-operacional-"
                    + Instant.now().toString().replace(':', '-').replace('.', '-') + ".csv\"")
                .body("﻿" + csv);
        } catch (FiltroHistorico.FiltroInvalidoException invalido) {
            return erro(invalido.getMessage());
        }
    }

    private List<HistoricoOperacional> buscarOrdenado(String reatorId) {
        List<HistoricoOperacional> linhas = reatorId == null
            ? repositorio.findAllByOrderByInstanteAsc()
            : repositorio.findAllByReatorIdOrderByInstanteAsc(reatorId);
        return linhas.stream().sorted(Comparator.comparing(HistoricoOperacional::getInstante)).toList();
    }

    private static FiltroHistorico filtroDe(
        String de, String ate, String temperaturaMin, String temperaturaMax,
        String pressaoMin, String pressaoMax, String radiacaoMin, String radiacaoMax,
        String fluxoMin, String fluxoMax, String operador, String reator
    ) {
        return new FiltroHistorico(
            instante(de, "de"),
            instante(ate, "ate"),
            new FiltroHistorico.Faixa(decimal(temperaturaMin, "temperaturaMin"), decimal(temperaturaMax, "temperaturaMax")),
            new FiltroHistorico.Faixa(decimal(pressaoMin, "pressaoMin"), decimal(pressaoMax, "pressaoMax")),
            new FiltroHistorico.Faixa(decimal(radiacaoMin, "radiacaoMin"), decimal(radiacaoMax, "radiacaoMax")),
            new FiltroHistorico.Faixa(decimal(fluxoMin, "fluxoMin"), decimal(fluxoMax, "fluxoMax")),
            operador,
            reator
        );
    }

    private static Instant instante(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
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
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(valor);
        } catch (NumberFormatException invalido) {
            throw new FiltroHistorico.FiltroInvalidoException("Parametro " + campo + " deve ser numerico.");
        }
    }

    private static int inteiro(String valor, int padrao) {
        if (valor == null || valor.isBlank()) {
            return padrao;
        }
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException invalido) {
            throw new FiltroHistorico.FiltroInvalidoException("Parametro limite deve ser inteiro.");
        }
    }

    private static ResponseEntity<String> json(String corpo) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(corpo);
    }

    private static ResponseEntity<String> erro(String mensagem) {
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
            .body("{\"erro\":\"" + mensagem.replace("\"", "\\\"") + "\"}");
    }
}
