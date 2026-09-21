package br.edu.unipampa.usina.infraestruturaeventos;

import java.time.Instant;
import java.util.List;

/**
 * Representa o Fluxo de Exceções do UC01:
 * "Detecta falha de comunicação com um sensor.
 * Emite alerta de manutenção para a equipe técnica."
 */
public record FalhaSensorDetectada(
    Instant ocorridoEm,
    long sensorId,
    String tipoSensor,
    String motivo,
    List<String> destinatarios
) implements EventoDominio {

    public FalhaSensorDetectada {
        destinatarios = List.copyOf(destinatarios);
    }

    @Override
    public String tipo() {
        return "FALHA_SENSOR_DETECTADA";
    }

    @Override
    public String resumo() {
        return "sensor=" + sensorId
            + " tipo=" + tipoSensor
            + " motivo=\"" + motivo + "\""
            + " destinatarios=" + String.join(", ", destinatarios);
    }
}
