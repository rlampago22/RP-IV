package br.edu.unipampa.usina.historico;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Uma saída de operação: um dia (no fuso local) sob responsabilidade de um operador. Porte de
 * {@code mvp/.../historicooperacional/ResumoDia.java} — mesmas regras, ver lá a justificativa.
 */
public record ResumoDia(
    LocalDate dia,
    String operador,
    HistoricoOperacional.OrigemOperador origemOperador,
    Instant primeiroRegistro,
    Instant ultimoRegistro,
    int ciclos,
    int ciclosComAlarme,
    int ciclosEmObservacao,
    int ciclosComFalha,
    List<String> sensoresEnvolvidos,
    String piorStatus
) {
    public ResumoDia {
        sensoresEnvolvidos = sensoresEnvolvidos == null ? List.of() : List.copyOf(sensoresEnvolvidos);
    }
}
