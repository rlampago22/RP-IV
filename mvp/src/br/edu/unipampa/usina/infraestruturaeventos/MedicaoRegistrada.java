package br.edu.unipampa.usina.infraestruturaeventos;

import java.time.Instant;
import java.util.Locale;

public record MedicaoRegistrada(
    String reatorId,
    Instant ocorridoEm,
    long sensorId,
    String tipoSensor,
    String unidade,
    double valor,
    double limiteMinimo,
    double limiteMaximo
) implements EventoDominio {

    public MedicaoRegistrada {
        reatorId = EventoDominio.reatorOuPrincipal(reatorId);
    }

    /** Medição do reator principal — forma usada antes de o sistema ter 5 reatores. */
    public MedicaoRegistrada(
        Instant ocorridoEm,
        long sensorId,
        String tipoSensor,
        String unidade,
        double valor,
        double limiteMinimo,
        double limiteMaximo
    ) {
        this(REATOR_PRINCIPAL, ocorridoEm, sensorId, tipoSensor, unidade, valor, limiteMinimo, limiteMaximo);
    }

    @Override
    public String tipo() {
        return "MEDICAO_REGISTRADA";
    }

    @Override
    public String resumo() {
        return String.format(
            Locale.ROOT,
            "reator=%s sensor=%d tipo=%s valor=%.2f %s faixa=[%.2f, %.2f]",
            reatorId,
            sensorId,
            tipoSensor,
            valor,
            unidade,
            limiteMinimo,
            limiteMaximo
        );
    }
}
