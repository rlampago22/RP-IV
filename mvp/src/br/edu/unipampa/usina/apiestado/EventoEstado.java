package br.edu.unipampa.usina.apiestado;

import java.time.Instant;

/** Linha da timeline de eventos EDA exibida no T01 (mais recente primeiro). */
public record EventoEstado(Instant ocorridoEm, String tipo, String resumo) {}
