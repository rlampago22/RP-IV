package br.edu.unipampa.usina.auditorialogs;

import java.time.Instant;

public record EntradaAuditoria(
    long sequencia,
    Instant ocorridoEm,
    String tipoEvento,
    String resumo,
    String hashAnterior,
    String hash
) {
    public String linhaPersistida() {
        return sequencia + " | " + ocorridoEm + " | " + tipoEvento
            + " | " + resumo + " | anterior=" + hashAnterior + " | hash=" + hash;
    }

    public String hashCurto() {
        return hash.substring(0, 12);
    }
}
