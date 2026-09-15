package br.edu.unipampa.usina.infraestruturaeventos;

import java.time.Instant;

/**
 * Representa o evento em que o alarme é solucionado e o reator/parâmetro retorna ao estado estável.
 */
public record AlarmeResolvido(
    Instant ocorridoEm,
    String alarmeId,
    String responsavel,
    String solucao
) implements EventoDominio {

    @Override
    public String tipo() {
        return "ALARME_RESOLVIDO";
    }

    @Override
    public String resumo() {
        return "alarme=" + alarmeId
            + " responsavel=\"" + responsavel + "\""
            + " solucao=\"" + solucao + "\"";
    }
}
