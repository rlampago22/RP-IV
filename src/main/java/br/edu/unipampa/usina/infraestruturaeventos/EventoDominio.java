package br.edu.unipampa.usina.infraestruturaeventos;

import java.time.Instant;

public final class EventoDominio {
    private final String tipo;
    private final Instant timestamp;
    private final Object payload;

    public EventoDominio(String tipo, Instant timestamp, Object payload) {
        this.tipo = tipo;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public String getTipo() { return tipo; }
    public Instant getTimestamp() { return timestamp; }
    public Object getPayload() { return payload; }
}
