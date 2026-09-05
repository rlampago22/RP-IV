package br.edu.unipampa.usina.alarmes;

import java.util.List;

public class Alarme {
    private String tipo;
    private String status;

    /** Feedback APS — notifica destinatários (Operador, Supervisão Central). */
    public void emitirAlerta(List<String> destinatarios) {
        throw new UnsupportedOperationException("Marco 2");
    }

    /** Feedback APS — persiste ocorrência do alarme. */
    public void registrarEvento() {
        throw new UnsupportedOperationException("Marco 2");
    }

    public String getTipo() { return tipo; }
    public String getStatus() { return status; }
}
