package br.edu.unipampa.usina.controlereator;

public class Limiar {
    private final String parametro;
    private final Double valorMin;
    private final Double valorMax;

    public Limiar(String parametro, Double valorMin, Double valorMax) {
        if (parametro == null || parametro.isBlank()) {
            throw new IllegalArgumentException("Parametro do limiar e obrigatorio");
        }
        if (valorMin == null && valorMax == null) {
            throw new IllegalArgumentException("Informe ao menos um limite");
        }
        if (valorMin != null && valorMax != null && valorMin > valorMax) {
            throw new IllegalArgumentException("Limite minimo nao pode superar o maximo");
        }
        this.parametro = parametro;
        this.valorMin = valorMin;
        this.valorMax = valorMax;
    }

    public String getParametro() { return parametro; }
    public Double getValorMin() { return valorMin; }
    public Double getValorMax() { return valorMax; }
}
