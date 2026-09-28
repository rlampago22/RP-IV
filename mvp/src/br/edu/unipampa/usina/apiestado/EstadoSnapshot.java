package br.edu.unipampa.usina.apiestado;

import java.time.Instant;
import java.util.List;

/** Fotografia imutável do estado derivado do EventBus — corpo de {@code GET /api/estado}. */
public record EstadoSnapshot(
    StatusReator status,
    boolean tempoReal,
    Instant atualizadoEm,
    List<SensorEstado> sensores,
    Contadores contadores,
    List<AlarmeEstado> alarmes,
    List<EventoEstado> eventos
) {}
