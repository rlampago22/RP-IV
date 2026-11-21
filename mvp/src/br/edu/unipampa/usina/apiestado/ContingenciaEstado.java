package br.edu.unipampa.usina.apiestado;

import java.time.Instant;

/** Protocolo de contingência em vigor, como aparece em {@code GET /api/estado} (campo {@code contingencia}). */
public record ContingenciaEstado(
    long emergenciaId,
    String protocolo,
    String nome,
    String ativadoPor,
    Instant ativadoEm
) {}
