package br.edu.unipampa.usina.controlereator;

import java.time.Instant;

/** Entidade responsável por registrar uma medição válida. */
public final class MedicaoReator {
    private double valor;
    private Instant timestamp;
    private Sensor sensor;

    public void registrarMedicao(double valor, Sensor sensor, Instant timestamp) {
        if (!Double.isFinite(valor)) {
            throw new IllegalArgumentException("O valor da medição deve ser numérico e finito.");
        }
        if (sensor == null || timestamp == null) {
            throw new IllegalArgumentException("Sensor e instante são obrigatórios.");
        }
        this.valor = valor;
        this.sensor = sensor;
        this.timestamp = timestamp;
    }

    public double getValor() {
        return valor;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Sensor getSensor() {
        return sensor;
    }
}
