package br.edu.unipampa.usina.apiestado;

import java.time.Instant;
import java.util.List;

/**
 * Última leitura e série recente de um sensor de um reator, para o painel de monitoramento.
 *
 * <p>{@code emAtencao} é verdadeiro quando a última leitura gerou {@code ObservacaoRegistrada}
 * (fora da faixa ideal, dentro do limite seguro). {@code serie} vai da mais antiga à mais recente.
 */
public record SensorReatorEstado(
    long id,
    String tipo,
    String unidade,
    double valor,
    double limiteMinimo,
    double limiteMaximo,
    boolean emAtencao,
    Instant atualizadoEm,
    List<Amostra> serie
) {
    public SensorReatorEstado {
        serie = serie == null ? List.of() : List.copyOf(serie);
    }

    public boolean foraDoLimite() {
        return valor < limiteMinimo || valor > limiteMaximo;
    }
}
