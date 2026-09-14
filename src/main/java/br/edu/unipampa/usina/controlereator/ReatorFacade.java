package br.edu.unipampa.usina.controlereator;

/**
 * Fachada do módulo ControleReator (padrão Facade).
 * Semana 2: leituras ficam em memória.
 */
public class ReatorFacade {

    private final java.util.List<MedicaoReator> historico = new java.util.ArrayList<>();
    private final java.util.Map<Long, Sensor> sensores = new java.util.HashMap<>();

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
        return medicao;
    }

    /** Retorna cópia do histórico em memória (todas as medições). */
    public java.util.List<MedicaoReator> consultarHistorico(long reatorId) {
        return java.util.List.copyOf(historico);
    }

    public int quantidadeMedicoes() {
        return historico.size();
    }
}
