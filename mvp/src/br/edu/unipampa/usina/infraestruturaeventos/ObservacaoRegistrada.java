package br.edu.unipampa.usina.infraestruturaeventos;

import java.time.Instant;
import java.util.Locale;

/**
 * Representa o Fluxo Alternativo 1 do UC01:
 * "Valores fora da faixa ideal de operação, mas dentro dos limites normais.
 * Registra observação sem gerar alarme crítico."
 */
public record ObservacaoRegistrada(
    String reatorId,
    Instant ocorridoEm,
    long sensorId,
    String tipoSensor,
    String unidade,
    double valor,
    String observacao
) implements EventoDominio {

    public ObservacaoRegistrada {
        reatorId = EventoDominio.reatorOuPrincipal(reatorId);
    }

    /** Observação do reator principal. */
    public ObservacaoRegistrada(
        Instant ocorridoEm,
        long sensorId,
        String tipoSensor,
        String unidade,
        double valor,
        String observacao
    ) {
        this(REATOR_PRINCIPAL, ocorridoEm, sensorId, tipoSensor, unidade, valor, observacao);
    }

    @Override
    public String tipo() {
        return "OBSERVACAO_REGISTRADA";
    }

    @Override
    public String resumo() {
        return String.format(
            Locale.ROOT,
            "reator=%s sensor=%d tipo=%s valor=%.2f %s obs=\"%s\"",
            reatorId,
            sensorId,
            tipoSensor,
            valor,
            unidade,
            observacao
        );
    }
}
