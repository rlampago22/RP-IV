package br.edu.unipampa.usina.controlereator;

import br.edu.unipampa.usina.alarmes.AlarmeFactory;
import br.edu.unipampa.usina.alarmes.AlarmeFacade;
import br.edu.unipampa.usina.auditorialogs.AuditoriaSubscriber;
import br.edu.unipampa.usina.infraestruturaeventos.EventBus;
import br.edu.unipampa.usina.infraestruturaeventos.EventoDominio;
import br.edu.unipampa.usina.persistenciareator.ReatorRepository;
import java.time.Instant;

/**
 * Fachada do módulo ControleReator (padrão Facade).
 * Semana 2: leituras ficam em memória.
 */
public class ReatorFacade {

    private final java.util.List<MedicaoReator> historico = new java.util.ArrayList<>();
    private final java.util.Map<Long, Sensor> sensores = new java.util.HashMap<>();
    private final EventBus eventBus;
    private final ReatorRepository repository;
    private final AvaliadorLimiar avaliador;
    private final AlarmeFacade alarmeFacade;
    private final AuditoriaSubscriber auditoriaSubscriber;

    public ReatorFacade() {
        this(new EventBus(), new ReatorRepository(), new AvaliadorLimiarPadrao());
    }

    public ReatorFacade(EventBus eventBus, ReatorRepository repository, AvaliadorLimiar avaliador) {
        this.eventBus = eventBus;
        this.repository = repository;
        this.avaliador = avaliador;
        this.alarmeFacade = new AlarmeFacade(new AlarmeFactory(), repository, eventBus);
        this.auditoriaSubscriber = new AuditoriaSubscriber();
        eventBus.assinar("MedicaoRegistrada", auditoriaSubscriber);
        eventBus.assinar("AlarmeEmitido", auditoriaSubscriber);
    }

    /** Cadastra um sensor simples para a demo (em memória). */
    public Sensor registrarSensor(long id, String tipo) {
        Sensor sensor = new Sensor(id, tipo, "OPERACIONAL");
        sensores.put(id, sensor);
        return sensor;
    }

    /**
     * Recebe uma leitura de sensor, cria a medição via entidade
     * {@link MedicaoReator#registrarMedicao} e guarda em memória.
     */
    public MedicaoReator receberLeitura(long sensorId, double valor) {
        Sensor sensor = sensores.get(sensorId);
        if (sensor == null) {
            sensor = registrarSensor(sensorId, "DESCONHECIDO");
        }

        MedicaoReator medicao = new MedicaoReator();
        medicao.registrarMedicao(valor, sensor, java.time.Instant.now());
        historico.add(medicao);
        repository.salvarMedicao(medicao);
        eventBus.publicar(new EventoDominio("MedicaoRegistrada", Instant.now(), medicao));
        return medicao;
    }

    public MedicaoReator receberLeitura(long sensorId, double valor, Limiar limiar) {
        MedicaoReator medicao = receberLeitura(sensorId, valor);
        ResultadoAvaliacao resultado = avaliador.avaliar(medicao, limiar);
        alarmeFacade.processarAvaliacao(resultado);
        return medicao;
    }

    /** Retorna cópia do histórico em memória (todas as medições). */
    public java.util.List<MedicaoReator> consultarHistorico(long reatorId) {
        return java.util.List.copyOf(historico);
    }

    public int quantidadeMedicoes() {
        return historico.size();
    }

    public EventBus getEventBus() { return eventBus; }
    public ReatorRepository getRepository() { return repository; }
    public AuditoriaSubscriber getAuditoriaSubscriber() { return auditoriaSubscriber; }
}
