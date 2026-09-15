package br.edu.unipampa.usina.infraestruturaeventos;

import java.time.Instant;
import java.util.Locale;

/**
 * Representa o Fluxo Alternativo 1 do UC01:
 * "Valores fora da faixa ideal de operação, mas dentro dos limites normais.
 * Registra observação sem gerar alarme crítico."
 */
public record ObservacaoRegistrada(
    Instant ocorridoEm,
    long sensorId,
    String tipoSensor,
    String unidade,
    double valor,
    String observacao
) implements EventoDominio {

    @Override
    public String tipo() {
        return "OBSERVACAO_REGISTRADA";
    }

    @Override
    public String resumo() {
        return String.format(
            Locale.ROOT,
            "sensor=%d tipo=%s valor=%.2f %s obs=\"%s\"",
            sensorId,
            tipoSensor,
            valor,
            unidade,
            observacao
        );
    }
}
