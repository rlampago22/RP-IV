package br.edu.unipampa.usina.infraestruturaeventos;

import java.time.Instant;

/** Contrato comum dos fatos publicados no barramento de eventos. */
public interface EventoDominio {
    String tipo();
    Instant ocorridoEm();
    String resumo();
}
