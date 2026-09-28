package br.edu.unipampa.usina.alarmes;

import br.edu.unipampa.usina.controlereator.MedicaoReator;
import java.time.Instant;
import java.util.List;

public class Alarme {
    private final String tipo;
    private final MedicaoReator medicao;
    private final Instant timestamp;
    private String status;

    public Alarme(String tipo, MedicaoReator medicao) {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("Tipo do alarme e obrigatorio");
        }
        if (medicao == null) {
            throw new IllegalArgumentException("Medicao do alarme e obrigatoria");
        }
        this.tipo = tipo;
        this.medicao = medicao;
        this.timestamp = Instant.now();
        this.status = "CRIADO";
    }

    /** Feedback APS — notifica destinatários (Operador, Supervisão Central). */
    public void emitirAlerta(List<String> destinatarios) {
        if (destinatarios == null || destinatarios.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um destinatario");
        }
        for (String destinatario : destinatarios) {
            System.out.println("[ALARME] " + tipo + " -> " + destinatario);
        }
        status = "EMITIDO";
    }

    /** Feedback APS — persiste ocorrência do alarme. */
    public void registrarEvento() {
        status = "REGISTRADO";
    }

    public String getTipo() { return tipo; }
    public String getStatus() { return status; }
    public MedicaoReator getMedicao() { return medicao; }
    public Instant getTimestamp() { return timestamp; }
}
