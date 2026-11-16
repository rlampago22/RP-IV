package br.edu.unipampa.usina.apiestado;

import java.time.Instant;

/**
 * Alarme observado via {@code AlarmeEmitido}/{@code AlarmeReconhecido}/{@code AlarmeResolvido},
 * com ciclo de vida ATIVO → RECONHECIDO → RESOLVIDO.
 */
public record AlarmeEstado(
    String id,
    long sensorId,
    String severidade,
    String mensagem,
    String status,
    Instant criadoEm,
    String operador,
    String solucao,
    Instant resolvidoEm,
    String resolvidoPor
) {
    public AlarmeEstado comReconhecimento(String operador) {
        return new AlarmeEstado(
            id, sensorId, severidade, mensagem, "RECONHECIDO", criadoEm, operador, solucao, resolvidoEm, resolvidoPor
        );
    }

    public AlarmeEstado comResolucao(Instant momento, String responsavel, String solucao) {
        return new AlarmeEstado(
            id, sensorId, severidade, mensagem, "RESOLVIDO", criadoEm, operador, solucao, momento, responsavel
        );
    }
}
