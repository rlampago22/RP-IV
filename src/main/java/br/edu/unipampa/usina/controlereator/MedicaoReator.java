package br.edu.unipampa.usina.controlereator;

import java.time.Instant;

public class MedicaoReator {
    private double valor;
    private Instant timestamp;
    private Sensor sensor;

    /** Método exigido pelo feedback APS — lógica na entidade. */
    public void registrarMedicao(double valor, Sensor sensor, Instant timestamp) {
        this.valor = valor;
        this.sensor = sensor;
        this.timestamp = timestamp;
    }

    public double getValor() { return valor; }
    public Instant getTimestamp() { return timestamp; }
    public Sensor getSensor() { return sensor; }
}
