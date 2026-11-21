package br.edu.unipampa.usina.historico;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lógica de consulta sobre o histórico operacional em banco — porte de
 * {@code mvp/.../historicooperacional/RegistroHistorico.java} (métodos {@code agruparPorDia}/
 * {@code resumir}) e de {@code SnapshotOperacional.statusDerivado()}/{@code temFalhaDeSensor()},
 * adaptados pra operar sobre {@link HistoricoOperacional} (campos de sensor em string
 * pipe-separada, não {@code List<String>}).
 */
final class HistoricoConsulta {
    private HistoricoConsulta() { }

    static String statusDerivado(HistoricoOperacional linha) {
        if (!linha.getSensoresEmAlarme().isEmpty()) {
            return "CRITICO";
        }
        if (!linha.getSensoresReconhecidos().isEmpty() || !linha.getSensoresEmObservacao().isEmpty()) {
            return "ATENCAO";
        }
        return "ESTAVEL";
    }

    static boolean temFalhaDeSensor(HistoricoOperacional linha) {
        return !linha.getSensoresEmFalha().isEmpty();
    }

    static List<String> separar(String campo) {
        if (campo == null || campo.isBlank()) {
            return List.of();
        }
        return List.of(campo.split("\\|"));
    }

    /**
     * RF16 — agrupa o histórico em saídas de operação: um resumo por (dia local + operador).
     * Mesma regra de {@code RegistroHistorico.agruparPorDia()} no mvp/.
     */
    static List<ResumoDia> agruparPorDia(List<HistoricoOperacional> linhas, FiltroHistorico filtro, ZoneId fuso) {
        Map<String, List<HistoricoOperacional>> porDiaEOperador = new LinkedHashMap<>();
        for (HistoricoOperacional linha : linhas) {
            if (!filtro.aceita(linha)) {
                continue;
            }
            LocalDate dia = linha.getInstante().atZone(fuso).toLocalDate();
            porDiaEOperador
                .computeIfAbsent(dia + "\u0000" + linha.getOperador(), chave -> new ArrayList<>())
                .add(linha);
        }

        List<ResumoDia> resumos = new ArrayList<>();
        for (List<HistoricoOperacional> grupo : porDiaEOperador.values()) {
            resumos.add(resumir(grupo, fuso));
        }
        resumos.sort(
            Comparator.comparing(ResumoDia::dia).thenComparing(ResumoDia::primeiroRegistro).reversed());
        return List.copyOf(resumos);
    }

    private static ResumoDia resumir(List<HistoricoOperacional> grupo, ZoneId fuso) {
        HistoricoOperacional primeiro = grupo.get(0);
        HistoricoOperacional ultimo = grupo.get(grupo.size() - 1);
        int comAlarme = 0;
        int emObservacao = 0;
        int comFalha = 0;
        Set<String> envolvidos = new LinkedHashSet<>();
        String pior = "ESTAVEL";
        for (HistoricoOperacional linha : grupo) {
            if (!linha.getSensoresEmAlarme().isEmpty()) {
                comAlarme++;
                envolvidos.addAll(separar(linha.getSensoresEmAlarme()));
            }
            if (!linha.getSensoresEmObservacao().isEmpty()) {
                emObservacao++;
            }
            if (temFalhaDeSensor(linha)) {
                comFalha++;
                envolvidos.addAll(separar(linha.getSensoresEmFalha()));
            }
            pior = piorEntre(pior, statusDerivado(linha));
        }
        return new ResumoDia(
            primeiro.getInstante().atZone(fuso).toLocalDate(),
            primeiro.getOperador(),
            primeiro.getOrigemOperador(),
            primeiro.getInstante(),
            ultimo.getInstante(),
            grupo.size(),
            comAlarme,
            emObservacao,
            comFalha,
            List.copyOf(envolvidos),
            pior
        );
    }

    private static String piorEntre(String atual, String candidato) {
        return severidade(candidato) > severidade(atual) ? candidato : atual;
    }

    private static int severidade(String status) {
        return switch (status) {
            case "CRITICO" -> 2;
            case "ATENCAO" -> 1;
            default -> 0;
        };
    }
}
