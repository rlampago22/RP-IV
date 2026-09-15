package br.edu.unipampa.usina.controlereator;

import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import br.edu.unipampa.usina.infraestruturaeventos.FalhaSensorDetectada;
import br.edu.unipampa.usina.infraestruturaeventos.MedicaoRegistrada;
import br.edu.unipampa.usina.infraestruturaeventos.ObservacaoRegistrada;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Ponto de entrada do módulo ControleReator (padrão Facade). */
public final class ReatorFacade {
    private static final List<String> DESTINATARIOS_FALHA =
        List.of("Equipe Tecnica", "Engenheiro de Seguranca");

    private final EventBus eventBus;
    private final Map<Long, Sensor> sensores = new LinkedHashMap<>();
    private final List<MedicaoReator> historico = new ArrayList<>();

    public ReatorFacade() {
        this(new EventBus());
    }

    public ReatorFacade(EventBus eventBus) {
        this.eventBus = eventBus != null ? eventBus : new EventBus();
    }

    public void registrarSensor(Sensor sensor) {
        sensores.put(sensor.id(), sensor);
    }

    public Sensor registrarSensor(long id, String tipo) {
        Sensor sensor = new Sensor(id, tipo, "OPERACIONAL");
        sensores.put(id, sensor);
        return sensor;
    }

    public Sensor obterSensor(long sensorId) {
        return sensores.get(sensorId);
    }

    public MedicaoReator receberLeitura(long sensorId, double valor) {
        Sensor sensor = sensores.get(sensorId);
        if (sensor == null) {
            sensor = registrarSensor(sensorId, "DESCONHECIDO");
        }

        MedicaoReator medicao = new MedicaoReator();
        medicao.registrarMedicao(valor, sensor, Instant.now());
        historico.add(medicao);

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
        return List.copyOf(historico);
    }

    public List<MedicaoReator> consultarHistorico(long reatorId) {
        return List.copyOf(historico);
    }

    public int quantidadeMedicoes() {
        return historico.size();
    }
}
