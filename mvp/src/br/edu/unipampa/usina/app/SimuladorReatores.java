package br.edu.unipampa.usina.app;

import br.edu.unipampa.usina.controlereator.ReatorFacade;
import br.edu.unipampa.usina.controlereator.Sensor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Telemetria simulada dos reatores do cadastro, cada um no seu próprio ritmo.
 *
 * <p><b>Sem sincronia:</b> cada reator agenda a própria próxima leitura com um intervalo sorteado
 * entre {@link #INTERVALO_MIN_MS} e {@link #INTERVALO_MAX_MS}. No painel, um reator atualiza
 * enquanto o vizinho continua parado — foi o pedido do professor ("esse aqui atualiza, esse aqui
 * permanece não atualizado").
 *
 * <p><b>Uma thread só:</b> os reatores dividem um agendador de thread única. Os ritmos continuam
 * independentes (cada um tem a sua agenda), mas as publicações no EventBus nunca acontecem ao
 * mesmo tempo, porque os assinantes (alarmes, histórico) não foram escritos para concorrência.
 * Pelo mesmo motivo, ajuste e retirada de reator (#96) rodam nessa thread.
 *
 * <p><b>Cadastro vivo (#96):</b> {@link #adicionar}, {@link #atualizar} e {@link #remover} mudam
 * o conjunto de reatores com o núcleo no ar — quem chama é o {@code SincronizadorCadastroReatores}.
 *
 * <p><b>Picos de radiação:</b> de vez em quando um reator entra num pico com forma fixa — sobe,
 * fica no topo um tick e desce — para a lâmpada passar por amarelo (faixa de atenção) ou vermelho
 * (acima do limite seguro). O pico vermelho fica acima do limite em <b>um único tick</b>: o
 * {@code AlarmeFacade} emite um alarme por leitura violada, e um pico longo encheria a lista de
 * alarmes repetidos. A forma é calculada a partir da faixa cadastrada do sensor (atenção máxima e
 * limite máximo), então continua amarela/vermelha se o Administrador mudar a faixa.
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

    /** Único sensor com comportamento próprio (os picos); os demais seguem o passeio aleatório. */
    static final long SENSOR_RADIACAO = 3L;

    private final Map<String, ReatorSimulado> reatores = new ConcurrentHashMap<>();
    private final BooleanSupplier ligado;
    private final ScheduledExecutorService agenda = Executors.newSingleThreadScheduledExecutor(tarefa -> {
        Thread thread = new Thread(tarefa, "simulador-reatores");
        thread.setDaemon(true);
        return thread;
    });
    private boolean iniciado;

    /**
     * @param reatores  facades com os sensores do cadastro já registrados
     * @param setpoints ponto de operação de cada sensor, por id de reator e id de sensor — vem do
     *                  cadastro no banco ({@code nucleo.sensor_reator}, issue #96); só os sensores
     *                  com setpoint são simulados
     * @param ligado    consultado a cada leitura — com o tempo real pausado, as agendas seguem
     *                  rodando mas não publicam nada
     */
    public SimuladorReatores(List<ReatorFacade> reatores, Map<String, Map<Long, Double>> setpoints, BooleanSupplier ligado) {
        this.ligado = ligado;
        for (ReatorFacade reator : reatores) {
            this.reatores.put(reator.getReatorId(), new ReatorSimulado(reator, exigirSetpoints(reator, setpoints.get(reator.getReatorId()))));
        }
    }

    /** Publica uma leitura inicial de cada sensor, para o painel abrir com dados. */
    public void semear() {
        for (ReatorSimulado reator : reatores.values()) {
            reator.publicarLeituraAtual();
        }
    }

    public synchronized void iniciar() {
        iniciado = true;
        for (ReatorSimulado reator : reatores.values()) {
            agendarPrimeira(reator);
        }
    }

    public void parar() {
        agenda.shutdownNow();
    }

    /** Ids dos reatores sendo simulados agora. */
    public List<String> reatoresSimulados() {
        return List.copyOf(reatores.keySet());
    }

    /**
     * Reator novo no cadastro (#96): publica a leitura inicial na hora (o painel já o mostra) e, se a
     * simulação estiver rodando, começa a agenda dele.
     */
    public synchronized void adicionar(ReatorFacade facade, Map<Long, Double> setpoints) {
        ReatorSimulado novo = new ReatorSimulado(facade, exigirSetpoints(facade, setpoints));
        ReatorSimulado anterior = reatores.put(facade.getReatorId(), novo);
        if (anterior != null) {
            anterior.ativo = false;
        }
        naAgenda(novo::publicarLeituraAtual);
        if (iniciado) {
            agendarPrimeira(novo);
        }
    }

    /** Setpoints ajustados no cadastro: a simulação passa a puxar os valores para o novo ponto. */
    public void atualizar(String reatorId, Map<Long, Double> setpoints) {
        ReatorSimulado reator = reatores.get(reatorId);
        if (reator == null) {
            return;
        }
        Map<Long, Double> novos = exigirSetpoints(reator.facade, setpoints);
        naAgenda(() -> reator.trocarSetpoints(novos));
    }

    /**
     * Reator retirado do cadastro. Roda na thread do simulador e só volta depois: quando o chamador
     * publica a retirada, nenhuma leitura daquele reator está a caminho para "ressuscitá-lo" no painel.
     */
    public void remover(String reatorId) {
        ReatorSimulado reator = reatores.remove(reatorId);
        if (reator != null) {
            naAgenda(() -> reator.ativo = false);
        }
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
        ReatorSimulado reator = reatores.get(reatorId);
        if (reator != null) {
            reator.seguradoAte = System.currentTimeMillis() + duracaoMs;
        }
    }

    /**
     * Forma do pico de radiação para a faixa do sensor: subida, topo, descida. Com a faixa padrão
     * (atenção 4,2, limite 5,0) reproduz os valores de antes do #96: topo amarelo entre 4,45 e 4,85,
     * topo vermelho entre 5,15 e 5,5 — só o ponto do topo passa do limite.
     */
    static List<Double> formaDoPico(double base, double atencaoMaxima, double limiteMaximo, boolean critico, Random aleatorio) {
        double faixa = limiteMaximo - atencaoMaxima;
        double topo = critico
            ? limiteMaximo + faixa * (0.1875 + aleatorio.nextDouble() * 0.4375)
            : atencaoMaxima + faixa * (0.3125 + aleatorio.nextDouble() * 0.5);
        return List.of(
            base + (topo - base) * 0.45,
            critico ? atencaoMaxima + faixa * (0.5 + aleatorio.nextDouble() * 0.3125) : topo - faixa * 0.1875,
            topo,
            critico ? atencaoMaxima + faixa * (0.375 + aleatorio.nextDouble() * 0.375) : topo - faixa * 0.25,
            base + (topo - base) * 0.35
        );
    }

    private static Map<Long, Double> exigirSetpoints(ReatorFacade reator, Map<Long, Double> setpoints) {
        if (setpoints == null || setpoints.isEmpty()) {
            throw new IllegalArgumentException("Reator " + reator.getReatorId() + " sem setpoints cadastrados.");
        }
        return setpoints;
    }

    /**
     * Executa na thread do simulador e espera terminar (mesma thread das leituras, então não há
     * corrida com elas). Com o simulador parado (testes), executa direto.
     */
    private void naAgenda(Runnable tarefa) {
        try {
            agenda.submit(tarefa).get();
        } catch (RejectedExecutionException parado) {
            tarefa.run();
        } catch (InterruptedException interrompido) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException falha) {
            throw new IllegalStateException(falha.getCause().getMessage(), falha.getCause());
        }
    }

    private void agendarPrimeira(ReatorSimulado reator) {
        // Atraso inicial diferente para cada um: sem isso, todos começariam juntos.
        agendar(reator, 500 + reator.aleatorio.nextInt(4_000));
    }

    private void agendar(ReatorSimulado reator, long atrasoMs) {
        try {
            agenda.schedule(() -> {
                if (!reator.ativo) {
                    return; // retirado do cadastro: a agenda dele termina aqui
                }
                try {
                    if (ligado.getAsBoolean() && System.currentTimeMillis() >= reator.seguradoAte) {
                        reator.avancar();
                    }
                } catch (RuntimeException falha) {
                    System.err.println("[SIMULADOR] " + reator.facade.getReatorId() + ": " + falha.getMessage());
                } finally {
                    if (reator.ativo) {
                        agendar(reator, reator.proximoIntervaloMs());
                    }
                }
            }, atrasoMs, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException parado) {
            // simulador encerrado
        }
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
        /** Falso depois de retirado do cadastro: a agenda para no próximo tick. */
        volatile boolean ativo = true;

        ReatorSimulado(ReatorFacade facade, Map<Long, Double> pontosDeOperacao) {
            this.facade = facade;
            this.aleatorio = new Random();
            // Cada reator opera num ponto um pouco diferente (cadastrado no banco), para os
            // gráficos não saírem iguais.
            setpoints.putAll(pontosDeOperacao);
            atuais.putAll(setpoints);
        }

        long proximoIntervaloMs() {
            return INTERVALO_MIN_MS + (long) (aleatorio.nextDouble() * (INTERVALO_MAX_MS - INTERVALO_MIN_MS));
        }

        void publicarLeituraAtual() {
            atuais.forEach(facade::receberLeitura);
        }

        /** Sensor que continua mantém o valor atual (sem salto no gráfico); sensor novo parte do setpoint. */
        void trocarSetpoints(Map<Long, Double> novos) {
            setpoints.clear();
            setpoints.putAll(novos);
            atuais.keySet().retainAll(novos.keySet());
            novos.forEach(atuais::putIfAbsent);
            if (!setpoints.containsKey(SENSOR_RADIACAO)) {
                picoRadiacao.clear();
            }
        }

        void avancar() {
            if (setpoints.containsKey(SENSOR_RADIACAO) && picoRadiacao.isEmpty() && aleatorio.nextDouble() < CHANCE_PICO) {
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

        private void iniciarPico() {
            Sensor radiacao = facade.obterSensor(SENSOR_RADIACAO);
            if (radiacao == null) {
                return;
            }
            boolean critico = aleatorio.nextDouble() < FRACAO_PICO_CRITICO;
            picoRadiacao.addAll(formaDoPico(
                atuais.get(SENSOR_RADIACAO), radiacao.atencaoMaxima(), radiacao.limiteMaximo(), critico, aleatorio));
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
