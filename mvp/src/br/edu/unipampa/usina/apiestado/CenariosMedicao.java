package br.edu.unipampa.usina.apiestado;

import br.edu.unipampa.usina.controlereator.ReatorFacade;

import java.util.Map;

/**
 * Seeds reproduzíveis de medição (S4 · Bernardo #50) para demo T02.
 * Valores fixos → {@link ReatorFacade#receberLeitura} → EventBus → histórico no ReatorRepository.
 */
public final class CenariosMedicao {
    /** Ponto de operação normal por sensor (último passo de {@link #aplicarNormal}). */
    private static final Map<Long, Double> PONTO_NORMAL = Map.of(
        1L, 310.5,
        2L, 155.0,
        3L, 2.4,
        4L, 1100.0
    );

    private CenariosMedicao() {}

    /**
     * Devolve o sensor ao ponto de operação normal. Usado quando um alarme é resolvido
     * ("parâmetros normalizados"), para que as leituras exibidas acompanhem o status do reator.
     */
    public static void normalizarSensor(ReatorFacade reator, long sensorId) {
        Double valor = PONTO_NORMAL.get(sensorId);
        if (valor != null && reator.obterSensor(sensorId) != null) {
            reator.receberLeitura(sensorId, valor);
        }
    }

    /** Operação normal estável (dentro da faixa ideal). */
    public static void aplicarNormal(ReatorFacade reator) {
        // Sequência curta previsível para preencher sparkline/histórico (antigo → atual).
        double[][] passos = {
            {308.0, 152.0, 2.0, 1080.0},
            {309.0, 153.0, 2.1, 1090.0},
            {309.5, 153.5, 2.2, 1095.0},
            {310.0, 154.0, 2.3, 1100.0},
            {310.2, 154.5, 2.35, 1100.0},
            {310.5, 155.0, 2.4, 1100.0},
        };
        for (double[] passo : passos) {
            reator.receberLeitura(1L, passo[0]);
            reator.receberLeitura(2L, passo[1]);
            reator.receberLeitura(3L, passo[2]);
            reator.receberLeitura(4L, passo[3]);
        }
    }

    /**
     * Observação preventiva (Alt. 1 UC01): temperatura na faixa de atenção
     * (fora do ideal, dentro do limite seguro) — publica ObservacaoRegistrada.
     */
    public static void aplicarObservacao(ReatorFacade reator) {
        aplicarNormal(reator);
        reator.receberLeitura(1L, 328.0); // atencaoMax cadastrado = 325
        reator.receberLeitura(2L, 155.0);
        reator.receberLeitura(3L, 2.4);
        reator.receberLeitura(4L, 1100.0);
    }
}
