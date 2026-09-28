package br.edu.unipampa.usina.app;

import br.edu.unipampa.usina.alarmes.AlarmeFacade;
import br.edu.unipampa.usina.alarmes.AlarmeFactory;
import br.edu.unipampa.usina.alarmes.AvaliadorFaixaSegura;
import br.edu.unipampa.usina.apiestado.ApiEstadoHttpServer;
import br.edu.unipampa.usina.apiestado.EstadoAgregador;
import br.edu.unipampa.usina.apiestado.SensorEstado;
import br.edu.unipampa.usina.auditorialogs.AuditoriaSubscriber;
import br.edu.unipampa.usina.auditorialogs.RegistroAuditoria;
import br.edu.unipampa.usina.controlereator.ReatorFacade;
import br.edu.unipampa.usina.controlereator.Sensor;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Random;

/**
 * Sobe o backend EDA (EventBus + facades) e expõe {@code /api/estado} para o frontend React
 * (T01 Overview, Opção A). Contrato: docs/api-estado-contrato.md.
 */
public final class ApiEstadoMain {
    private static final int PORTA = 8080;
    private static final long[] SENSOR_IDS = {1L, 2L, 3L, 4L};

    /** Ponto de operação normal de cada sensor — a simulação oscila em torno deles, sem deriva permanente. */
    private static final Map<Long, Double> SETPOINTS = Map.of(
        1L, 310.5,
        2L, 155.0,
        3L, 2.4,
        4L, 1100.0
    );

    private ApiEstadoMain() {}

    public static void main(String[] args) throws IOException {
        EventBus eventBus = new EventBus();
        RegistroAuditoria registro = new RegistroAuditoria(Path.of("dados", "auditoria.log"));
        new AuditoriaSubscriber(eventBus, registro);

        AlarmeFacade alarmeFacade = new AlarmeFacade(eventBus, new AvaliadorFaixaSegura(), new AlarmeFactory());
        ReatorFacade reator = new ReatorFacade(eventBus);
        EstadoAgregador estado = new EstadoAgregador(eventBus);

        reator.registrarSensor(new Sensor(1L, "TEMPERATURA", "Celsius", 0.0, 350.0, 20.0, 325.0));
        reator.registrarSensor(new Sensor(2L, "PRESSAO", "bar", 0.0, 160.0, 10.0, 155.0));
        reator.registrarSensor(new Sensor(3L, "RADIACAO", "mSv/h", 0.0, 5.0, 0.1, 4.2));
        reator.registrarSensor(new Sensor(4L, "FLUXO_RESFRIAMENTO", "m3/h", 500.0, 1500.0, 600.0, 1400.0));

        // Operação normal inicial: o T01 já mostra os 4 parâmetros RF-1 assim que o painel React abre.
        reator.receberLeitura(1L, 310.5);
        reator.receberLeitura(2L, 155.0);
        reator.receberLeitura(3L, 2.4);
        reator.receberLeitura(4L, 1100.0);

        ApiEstadoHttpServer servidor = new ApiEstadoHttpServer(estado, alarmeFacade, PORTA);
        servidor.iniciar();

        iniciarTempoRealSimulado(reator, estado);

        System.out.println("[API ESTADO] Pressione CTRL+C para encerrar.");
    }

    /**
     * Enquanto {@code tempoReal} estiver ligado (POST /api/tempo-real/iniciar), varia levemente
     * as leituras dos 4 sensores para simular telemetria contínua no T01.
     */
    private static void iniciarTempoRealSimulado(ReatorFacade reator, EstadoAgregador estado) {
        Random aleatorio = new Random();
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException interrompida) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (!estado.isTempoRealAtivo()) {
                    continue;
                }
                var sensoresAtuais = estado.snapshot().sensores();
                for (long sensorId : SENSOR_IDS) {
                    sensoresAtuais.stream()
                        .filter(sensor -> sensor.id() == sensorId)
                        .findFirst()
                        .ifPresent(sensor -> reator.receberLeitura(sensorId, proximoValor(sensor, aleatorio)));
                }
            }
        }, "tempo-real-simulado");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Passeio aleatório com reversão à média (Ornstein-Uhlenbeck simplificado): puxa o valor de
     * volta ao setpoint operacional a cada tick, evitando deriva permanente para fora da faixa segura
     * que geraria alarmes contínuos na demo (observado em teste manual: passeio aleatório puro "grudou"
     * acima do limite de pressão e disparou dezenas de alarmes em poucos minutos).
     */
    private static double proximoValor(SensorEstado sensor, Random aleatorio) {
        double faixa = sensor.limiteMaximo() - sensor.limiteMinimo();
        double setpoint = SETPOINTS.getOrDefault(sensor.id(), (sensor.limiteMinimo() + sensor.limiteMaximo()) / 2);
        double atracaoAoSetpoint = 0.12 * (setpoint - sensor.valor());
        double ruido = (aleatorio.nextDouble() - 0.5) * faixa * 0.025;
        double novoValor = sensor.valor() + atracaoAoSetpoint + ruido;
        double minimo = sensor.limiteMinimo() - faixa * 0.1;
        double maximo = sensor.limiteMaximo() + faixa * 0.1;
        return Math.max(minimo, Math.min(maximo, novoValor));
    }
}
