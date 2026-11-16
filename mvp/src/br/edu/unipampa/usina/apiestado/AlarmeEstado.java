package br.edu.unipampa.usina.apiestado;

import java.time.Instant;

/**
 * Alarme observado via {@code AlarmeEmitido}/{@code AlarmeReconhecido}/{@code AlarmeResolvido},
 * com ciclo de vida ATIVO → RECONHECIDO → RESOLVIDO.
 */
public record AlarmeEstado(
    String id,
    String reatorId,
    long sensorId,
    String severidade,
    String mensagem,
    String status,
    Instant criadoEm,
    String operador,
    String solucao
) {
    public AlarmeEstado comStatus(String novoStatus, String operador, String solucao) {
        return new AlarmeEstado(id, reatorId, sensorId, severidade, mensagem, novoStatus, criadoEm, operador, solucao);
    }
}
