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

    /**
     * Lê uma linha no formato de {@link #linhaPersistida()} (o do antigo {@code dados/auditoria.log}).
     * O resumo pode conter " | ", por isso as 3 primeiras e as 2 últimas partes são fixas.
     *
     * @return {@code null} se a linha não estiver nesse formato
     */
    public static EntradaAuditoria deLinhaPersistida(String linha) {
        String[] partes = linha.split(" \\| ", -1);
        if (partes.length < 6
            || !partes[partes.length - 2].startsWith("anterior=")
            || !partes[partes.length - 1].startsWith("hash=")) {
            return null;
        }
        try {
            String resumo = String.join(" | ", java.util.Arrays.copyOfRange(partes, 3, partes.length - 2));
            return new EntradaAuditoria(
                Long.parseLong(partes[0].trim()),
                Instant.parse(partes[1].trim()),
                partes[2].trim(),
                resumo.trim(),
                partes[partes.length - 2].substring("anterior=".length()).trim(),
                partes[partes.length - 1].substring("hash=".length()).trim()
            );
        } catch (RuntimeException formatoInvalido) {
            return null;
        }
    }
}
