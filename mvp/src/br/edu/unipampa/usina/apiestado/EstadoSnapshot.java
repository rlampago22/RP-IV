package br.edu.unipampa.usina.apiestado;

import java.time.Instant;
import java.util.List;

/**
 * Fotografia imutável do estado derivado do EventBus — corpo de {@code GET /api/estado}.
 *
 * <p>{@code status} e {@code sensores} descrevem o reator principal (R-01), como antes de o sistema
 * ter 5 reatores — T01, T02 e o histórico continuam lendo esses campos. {@code alarmes} e
 * {@code eventos} são da usina inteira. {@code reatores} traz os 5 reatores para o painel de
 * monitoramento.
 */
public record EstadoSnapshot(
    StatusReator status,
    boolean tempoReal,
    Instant atualizadoEm,
    List<SensorEstado> sensores,
    Contadores contadores,
    List<AlarmeEstado> alarmes,
    List<EventoEstado> eventos,
    List<ReatorEstado> reatores
) {}
