package br.edu.unipampa.usina.infraestruturaeventos;

import java.time.Instant;
import java.util.List;

public record AlarmeEmitido(
    Instant ocorridoEm,
    String alarmeId,
    long sensorId,
    String severidade,
    String mensagem,
    List<String> destinatarios
) implements EventoDominio {

    public AlarmeEmitido {
        destinatarios = List.copyOf(destinatarios);
    }

    @Override
    public String tipo() {
        return "ALARME_EMITIDO";
    }

    @Override
    public String resumo() {
        return "alarme=" + alarmeId
            + " sensor=" + sensorId
            + " severidade=" + severidade
            + " destinatarios=" + String.join(", ", destinatarios)
            + " mensagem=\"" + mensagem + "\"";
    }
}
