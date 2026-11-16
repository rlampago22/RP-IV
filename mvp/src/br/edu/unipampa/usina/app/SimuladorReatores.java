package br.edu.unipampa.usina.app;

import br.edu.unipampa.usina.controlereator.ReatorFacade;
import br.edu.unipampa.usina.controlereator.Sensor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Telemetria simulada dos 5 reatores, cada um no seu próprio ritmo.
 *
 * <p><b>Sem sincronia:</b> cada reator agenda a própria próxima leitura com um intervalo sorteado
 * entre {@link #INTERVALO_MIN_MS} e {@link #INTERVALO_MAX_MS}. No painel, um reator atualiza
 * enquanto o vizinho continua parado — foi o pedido do professor ("esse aqui atualiza, esse aqui
 * permanece não atualizado").
 *
 * <p><b>Uma thread só:</b> os reatores dividem um agendador de thread única. Os ritmos continuam
 * independentes (cada um tem a sua agenda), mas as publicações no EventBus nunca acontecem ao
 * mesmo tempo, porque os assinantes (alarmes, histórico) não foram escritos para concorrência.
 *
 * <p><b>Picos de radiação:</b> de vez em quando um reator entra num pico com forma fixa — sobe,
 * fica no topo um tick e desce — para a lâmpada passar por amarelo (faixa de atenção) ou vermelho
 * (acima do limite seguro). O pico vermelho fica acima do limite em <b>um único tick</b>: o
 * {@code AlarmeFacade} emite um alarme por leitura violada, e um pico longo encheria a lista de
 * alarmes repetidos.
 */
public final class SimuladorReatores {
    static final long INTERVALO_MIN_MS = 2_000;
    static final long INTERVALO_MAX_MS = 8_000;
    /** Quanto tempo um cenário manual (aba Demo) fica visível antes de a simulação retomar. */
    public static final long SEGURAR_CENARIO_MS = 20_000;

    /** Chance, a cada leitura, de um reator começar um pico de radiação. */
    private static final double CHANCE_PICO = 0.05;
    /** Entre os picos, a fração que passa do limite seguro (vermelho); o resto fica no amarelo. */
    private static final double FRACAO_PICO_CRITICO = 0.3;

    private static final long SENSOR_TEMPERATURA = 1L;
    private static final long SENSOR_PRESSAO = 2L;
    private static final long SENSOR_RADIACAO = 3L;
    private static final long SENSOR_FLUXO = 4L;

    private final List<ReatorSimulado> reatores;
    private final BooleanSupplier ligado;
    private final ScheduledExecutorService agenda = Executors.newSingleThreadScheduledExecutor(tarefa -> {
        Thread thread = new Thread(tarefa, "simulador-reatores");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * @param reatores facades com os 4 sensores RF-1 já registrados
     * @param ligado   consultado a cada leitura — com o tempo real pausado, as agendas seguem
     *                 rodando mas não publicam nada
     */
    public SimuladorReatores(List<ReatorFacade> reatores, BooleanSupplier ligado) {
        this.ligado = ligado;
        this.reatores = new ArrayList<>();
        for (int i = 0; i < reatores.size(); i++) {
            this.reatores.add(new ReatorSimulado(reatores.get(i), i));
        }
    }

    /** Publica uma leitura inicial de cada sensor, para o painel abrir com dados. */
    public void semear() {
        for (ReatorSimulado reator : reatores) {
            reator.publicarLeituraAtual();
        }
    }

    public void iniciar() {
        for (ReatorSimulado reator : reatores) {
            // Atraso inicial diferente para cada um: sem isso, os 5 começariam juntos.
            agendar(reator, 500 + reator.aleatorio.nextInt(4_000));
        }
    }

    public void parar() {
        agenda.shutdownNow();
    }

    /**
     * Suspende a simulação de um reator por {@code duracaoMs}. Usado quando o operador dispara um
     * cenário naquele reator: sem isso, a próxima leitura simulada (em 2 a 8 s) apagaria o
     * resultado do cenário. Os outros reatores seguem normalmente.
     *
     * <p>Ao retomar, a simulação continua dos valores dela, não dos do cenário — equivale a uma
     * manobra corretiva que normaliza o reator.
     */
    public void segurar(String reatorId, long duracaoMs) {
        long ate = System.currentTimeMillis() + duracaoMs;
        for (ReatorSimulado reator : reatores) {
            if (reator.facade.getReatorId().equals(reatorId)) {
                reator.seguradoAte = ate;
            }
        }
    }

    private void agendar(ReatorSimulado reator, long atrasoMs) {
        agenda.schedule(() -> {
            try {
                if (ligado.getAsBoolean() && System.currentTimeMillis() >= reator.seguradoAte) {
                    reator.avancar();
                }
            } catch (RuntimeException falha) {
                System.err.println("[SIMULADOR] " + reator.facade.getReatorId() + ": " + falha.getMessage());
            } finally {
                agendar(reator, reator.proximoIntervaloMs());
            }
        }, atrasoMs, TimeUnit.MILLISECONDS);
    }

    /** Estado da simulação de um reator: valores atuais, ponto de operação e pico em andamento. */
    private static final class ReatorSimulado {
        final ReatorFacade facade;
        final Random aleatorio;
        final Map<Long, Double> setpoints = new LinkedHashMap<>();
        final Map<Long, Double> atuais = new LinkedHashMap<>();
        final Deque<Double> picoRadiacao = new ArrayDeque<>();
        /** Lido pela thread do agendador, escrito pela thread HTTP. */
        volatile long seguradoAte;

        ReatorSimulado(ReatorFacade facade, int indice) {
            this.facade = facade;
            this.aleatorio = new Random();
            // Cada reator opera num ponto um pouco diferente, para os gráficos não saírem iguais.
            double deslocamento = (indice - 2) * 0.5;
            setpoints.put(SENSOR_TEMPERATURA, 310.0 + deslocamento * 3);
            setpoints.put(SENSOR_PRESSAO, 150.0 + deslocamento);
            setpoints.put(SENSOR_RADIACAO, 2.4 + deslocamento * 0.15);
            setpoints.put(SENSOR_FLUXO, 1100.0 + deslocamento * 20);
            atuais.putAll(setpoints);
        }

        long proximoIntervaloMs() {
            return INTERVALO_MIN_MS + (long) (aleatorio.nextDouble() * (INTERVALO_MAX_MS - INTERVALO_MIN_MS));
        }

        void publicarLeituraAtual() {
            atuais.forEach(facade::receberLeitura);
        }

        void avancar() {
            if (picoRadiacao.isEmpty() && aleatorio.nextDouble() < CHANCE_PICO) {
                iniciarPico();
            }
            for (Long sensorId : setpoints.keySet()) {
                Sensor sensor = facade.obterSensor(sensorId);
                if (sensor == null) {
                    continue;
                }
                double valor = sensorId == SENSOR_RADIACAO && !picoRadiacao.isEmpty()
                    ? picoRadiacao.removeFirst()
                    : proximoValor(sensor, atuais.get(sensorId), setpoints.get(sensorId));
                atuais.put(sensorId, valor);
                facade.receberLeitura(sensorId, valor);
            }
        }

        /**
         * Forma do pico: subida, topo, descida. O topo amarelo fica entre a faixa de atenção
         * (4,2) e o limite (5,0); o vermelho passa do limite só no ponto do topo.
         */
        private void iniciarPico() {
            boolean critico = aleatorio.nextDouble() < FRACAO_PICO_CRITICO;
            double topo = critico ? 5.15 + aleatorio.nextDouble() * 0.35 : 4.45 + aleatorio.nextDouble() * 0.4;
            double base = atuais.get(SENSOR_RADIACAO);
            picoRadiacao.add(base + (topo - base) * 0.45);
            picoRadiacao.add(critico ? 4.6 + aleatorio.nextDouble() * 0.25 : topo - 0.15);
            picoRadiacao.add(topo);
            picoRadiacao.add(critico ? 4.5 + aleatorio.nextDouble() * 0.3 : topo - 0.2);
            picoRadiacao.add(base + (topo - base) * 0.35);
        }

        /**
         * Passeio aleatório com reversão à média (Ornstein-Uhlenbeck simplificado): puxa o valor
         * de volta ao ponto de operação a cada leitura, evitando deriva permanente para fora da
         * faixa segura (passeio aleatório puro "grudou" acima do limite de pressão em teste manual
         * e disparou dezenas de alarmes em poucos minutos).
         */
        private double proximoValor(Sensor sensor, double atual, double setpoint) {
            double faixa = sensor.limiteMaximo() - sensor.limiteMinimo();
            double atracao = 0.12 * (setpoint - atual);
            double ruido = (aleatorio.nextDouble() - 0.5) * faixa * 0.025;
            double novoValor = atual + atracao + ruido;
            double minimo = sensor.limiteMinimo() - faixa * 0.1;
            double maximo = sensor.limiteMaximo() + faixa * 0.1;
            return Math.max(minimo, Math.min(maximo, novoValor));
        }
    }
}
