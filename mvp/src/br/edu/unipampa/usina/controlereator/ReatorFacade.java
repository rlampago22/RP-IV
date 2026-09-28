package br.edu.unipampa.usina.controlereator;

import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import br.edu.unipampa.usina.infraestruturaeventos.FalhaSensorDetectada;
import br.edu.unipampa.usina.infraestruturaeventos.MedicaoRegistrada;
import br.edu.unipampa.usina.infraestruturaeventos.ObservacaoRegistrada;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ponto de entrada do módulo ControleReator (padrão Facade).
 *
 * <p>Alinhado ao SEQ-UC01 (fluxo principal):
 * Sensor → receberLeitura → MedicaoReator.registrarMedicao →
 * ReatorRepository.salvarMedicao → EventBus.publicar(MedicaoRegistrada).
 * Avaliação de limiar / alarme ocorre via assinante {@code AlarmeFacade}
 * no barramento (desacoplamento EDA).
 */
public final class ReatorFacade {
    private static final List<String> DESTINATARIOS_FALHA =
        List.of("Equipe Tecnica", "Engenheiro de Seguranca");

    private final EventBus eventBus;
    private final ReatorRepository repository;
    private final Map<Long, Sensor> sensores = new LinkedHashMap<>();

    public ReatorFacade(EventBus eventBus) {
        this(eventBus, new ReatorRepository());
    }

    public ReatorFacade(EventBus eventBus, ReatorRepository repository) {
        this.eventBus = eventBus;
        this.repository = repository;
    }

    public void registrarSensor(Sensor sensor) {
        sensores.put(sensor.id(), sensor);
    }

    public Sensor obterSensor(long sensorId) {
        return sensores.get(sensorId);
    }

    public List<Sensor> listarSensores() {
        return List.copyOf(sensores.values());
    }

    /**
     * SEQ-UC01 — Registrar medição (fluxo principal até publicação no EventBus).
     */
    public MedicaoReator receberLeitura(long sensorId, double valor) {
        Sensor sensor = sensores.get(sensorId);
        if (sensor == null) {
            throw new IllegalArgumentException("Sensor não cadastrado: " + sensorId);
        }

        MedicaoReator medicao = new MedicaoReator();
        medicao.registrarMedicao(valor, sensor, Instant.now());
        repository.salvarMedicao(medicao);

        System.out.printf(
            Locale.ROOT,
            "[MEDICAO] Sensor %d (%s): %.2f %s%n",
            sensor.id(),
            sensor.tipo(),
            valor,
            sensor.unidade()
        );

        eventBus.publicar(new MedicaoRegistrada(
            medicao.getTimestamp(),
            sensor.id(),
            sensor.tipo(),
            sensor.unidade(),
            valor,
            sensor.limiteMinimo(),
            sensor.limiteMaximo()
        ));

        // Fluxo Alternativo 1 (UC01): Registro de observação preventiva
        if (sensor.isNaFaixaObservacao(valor)) {
            String motivo = String.format(
                Locale.ROOT,
                "Valor %.2f %s fora da faixa ideal de atencao [%.2f, %.2f], mas dentro dos limites normais",
                valor,
                sensor.unidade(),
                sensor.atencaoMinima(),
                sensor.atencaoMaxima()
            );
            eventBus.publicar(new ObservacaoRegistrada(
                Instant.now(),
                sensor.id(),
                sensor.tipo(),
                sensor.unidade(),
                valor,
                motivo
            ));
        }

        return medicao;
    }

    /**
     * Fluxo de Exceções do UC01: Falha na comunicação com sensor.
     * Emite alerta de manutenção para a equipe técnica.
     */
    public void simularFalhaSensor(long sensorId, String motivo) {
        Sensor sensor = sensores.get(sensorId);
        String tipoSensor = sensor != null ? sensor.tipo() : "DESCONHECIDO";
        System.out.printf(
            Locale.ROOT,
            "[FALHA DE SENSOR] Sensor %d (%s): %s%n",
            sensorId,
            tipoSensor,
            motivo
        );
        eventBus.publicar(new FalhaSensorDetectada(
            Instant.now(),
            sensorId,
            tipoSensor,
            motivo,
            DESTINATARIOS_FALHA
        ));
    }

    public List<MedicaoReator> consultarHistorico() {
        return repository.listarTodas();
    }

    /** Histórico filtrado por sensor (T02 / telemetria). */
    public List<MedicaoReator> consultarHistorico(long sensorId) {
        return repository.listarPorSensor(sensorId);
    }

    /** Últimas N medições do repositório (histórico curto). */
    public List<MedicaoReator> consultarHistoricoRecente(int limite) {
        return repository.listarRecentes(limite);
    }

    public ReatorRepository getRepository() {
        return repository;
    }
}
