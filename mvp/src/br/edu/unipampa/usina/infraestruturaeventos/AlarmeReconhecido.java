package br.edu.unipampa.usina.infraestruturaeventos;

import java.time.Instant;

/**
 * Representa o evento do UC01 em que operadores e engenheiros monitoram e validam o alarme.
 */
public record AlarmeReconhecido(
    Instant ocorridoEm,
    String alarmeId,
    String operador,
    String justificativa
) implements EventoDominio {

    @Override
    public String tipo() {
        return "ALARME_RECONHECIDO";
    }

    @Override
    public String resumo() {
        return "alarme=" + alarmeId
            + " operador=\"" + operador + "\""
            + " justificativa=\"" + justificativa + "\"";
    }
}
