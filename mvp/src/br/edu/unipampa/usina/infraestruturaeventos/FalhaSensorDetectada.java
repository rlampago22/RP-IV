package br.edu.unipampa.usina.infraestruturaeventos;

import java.time.Instant;
import java.util.List;

/**
 * Representa o Fluxo de Exceções do UC01:
 * "Detecta falha de comunicação com um sensor.
 * Emite alerta de manutenção para a equipe técnica."
 */
public record FalhaSensorDetectada(
    String reatorId,
    Instant ocorridoEm,
    long sensorId,
    String tipoSensor,
    String motivo,
    List<String> destinatarios
) implements EventoDominio {

    public FalhaSensorDetectada {
        reatorId = EventoDominio.reatorOuPrincipal(reatorId);
        destinatarios = List.copyOf(destinatarios);
    }

    /** Falha num sensor do reator principal. */
    public FalhaSensorDetectada(
        Instant ocorridoEm,
        long sensorId,
        String tipoSensor,
        String motivo,
        List<String> destinatarios
    ) {
        this(REATOR_PRINCIPAL, ocorridoEm, sensorId, tipoSensor, motivo, destinatarios);
    }

    @Override
    public String tipo() {
        return "FALHA_SENSOR_DETECTADA";
    }

    @Override
    public String resumo() {
        return "reator=" + reatorId
            + " sensor=" + sensorId
            + " tipo=" + tipoSensor
            + " motivo=\"" + motivo + "\""
            + " destinatarios=" + String.join(", ", destinatarios);
    }
}
