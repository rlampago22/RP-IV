package br.edu.unipampa.usina.infraestruturaeventos;

import java.time.Instant;
import java.util.Locale;

public record MedicaoRegistrada(
    Instant ocorridoEm,
    long sensorId,
    String tipoSensor,
    String unidade,
    double valor,
    double limiteMinimo,
    double limiteMaximo
) implements EventoDominio {

    @Override
    public String tipo() {
        return "MEDICAO_REGISTRADA";
    }

    @Override
    public String resumo() {
        return String.format(
            Locale.ROOT,
            "sensor=%d tipo=%s valor=%.2f %s faixa=[%.2f, %.2f]",
            sensorId,
            tipoSensor,
            valor,
            unidade,
            limiteMinimo,
            limiteMaximo
        );
    }
}
