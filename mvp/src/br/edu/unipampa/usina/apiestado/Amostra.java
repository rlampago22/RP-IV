package br.edu.unipampa.usina.apiestado;

import java.time.Instant;

/**
 * Um ponto da série de um sensor: valor e instante da medição.
 *
 * <p>O instante vai junto porque cada reator atualiza num ritmo diferente (intervalo variável).
 * Um gráfico que posicionasse os pontos pelo índice esticaria e comprimiria o tempo em silêncio.
 */
public record Amostra(Instant instante, double valor) {}
