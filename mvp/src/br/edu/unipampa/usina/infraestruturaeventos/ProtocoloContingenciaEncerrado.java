package br.edu.unipampa.usina.infraestruturaeventos;

import java.time.Instant;

/** RF10 — a emergência do protocolo foi encerrada na Central; o núcleo volta à operação normal. */
public record ProtocoloContingenciaEncerrado(
    Instant ocorridoEm,
    long emergenciaId,
    String protocolo,
    String encerradoPor
) implements EventoDominio {
    @Override
    public String tipo() {
        return "PROTOCOLO_CONTINGENCIA_ENCERRADO";
    }

    @Override
    public String resumo() {
        return "emergencia=" + emergenciaId
            + " protocolo=" + protocolo
            + " encerradoPor=\"" + encerradoPor + "\"";
    }
}
