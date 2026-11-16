package br.edu.unipampa.usina.infraestruturaeventos;

import java.time.Instant;

/** Contrato comum dos fatos publicados no barramento de eventos. */
public interface EventoDominio {
    /**
     * Reator assumido pelos eventos criados sem reator explícito. É o reator que o sistema
     * supervisionava antes de passar a 5 — histórico operacional, T01 e T02 continuam nele.
     */
    String REATOR_PRINCIPAL = "R-01";

    String tipo();
    Instant ocorridoEm();
    String resumo();

    /** Normaliza o id do reator: nulo ou vazio vira o reator principal. */
    static String reatorOuPrincipal(String reatorId) {
        return reatorId == null || reatorId.isBlank() ? REATOR_PRINCIPAL : reatorId;
    }
}
