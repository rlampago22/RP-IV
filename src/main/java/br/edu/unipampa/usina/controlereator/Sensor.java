package br.edu.unipampa.usina.controlereator;

public class Sensor {
    private long id;
    private String tipo;
    private String status;

    public long getId() { return id; }
    public String getTipo() { return tipo; }
    public String getStatus() { return status; }
    public String obterLocalizacao() { return ""; }
}
