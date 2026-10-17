package br.edu.unipampa.usina.infraestruturaeventos;

import java.time.Instant;

/** Evento de domínio UC04 / RF08 — tentativa de acesso registrada. */
public record AcessoRegistrado(
    Instant ocorridoEm,
    String registroId,
    String credencial,
    String area,
    String resultado
) implements EventoDominio {
    @Override
    public String tipo() {
        return "ACESSO_REGISTRADO";
    }

    @Override
    public String resumo() {
        return "acesso registro=" + registroId
            + " credencial=\"" + credencial + "\""
            + " area=\"" + area + "\""
            + " resultado=" + resultado;
    }
}
