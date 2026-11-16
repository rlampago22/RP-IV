package br.edu.unipampa.usina.infraestruturaeventos;

import java.time.Instant;
import java.util.List;

public record AlarmeEmitido(
    String reatorId,
    Instant ocorridoEm,
    String alarmeId,
    long sensorId,
    String severidade,
    String mensagem,
    List<String> destinatarios
) implements EventoDominio {

    public AlarmeEmitido {
        reatorId = EventoDominio.reatorOuPrincipal(reatorId);
        destinatarios = List.copyOf(destinatarios);
    }

    /** Alarme do reator principal. */
    public AlarmeEmitido(
        Instant ocorridoEm,
        String alarmeId,
        long sensorId,
        String severidade,
        String mensagem,
        List<String> destinatarios
    ) {
        this(REATOR_PRINCIPAL, ocorridoEm, alarmeId, sensorId, severidade, mensagem, destinatarios);
    }

    @Override
    public String tipo() {
        return "ALARME_EMITIDO";
    }

    @Override
    public String resumo() {
        return "reator=" + reatorId
            + " alarme=" + alarmeId
            + " sensor=" + sensorId
            + " severidade=" + severidade
            + " destinatarios=" + String.join(", ", destinatarios)
            + " mensagem=\"" + mensagem + "\"";
    }
}
