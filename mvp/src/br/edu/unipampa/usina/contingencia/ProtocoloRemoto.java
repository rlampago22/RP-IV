package br.edu.unipampa.usina.contingencia;

/** Uma emergência com protocolo, como a Central (backend, porta 8081) informa. */
public record ProtocoloRemoto(
    long emergenciaId,
    String status,
    String protocolo,
    String nome,
    String ativadoPor,
    String encerradoPor
) {
    public static final String ATIVO = "PROTOCOLO_ATIVO";

    public boolean ativo() {
        return ATIVO.equals(status);
    }
}
