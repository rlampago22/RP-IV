const SCENARIOS = [
  { key: 'normal', title: 'Normal / Tempo Real', detail: 'Medições na faixa · núcleo ESTÁVEL' },
  { key: 'observation', title: 'Simular Observação (Alt. 1)', detail: 'OBSERVACAO_REGISTRADA · sem alarme alto' },
  { key: 'failure', title: 'Falha Sensor (Exceção)', detail: 'FALHA_SENSOR_DETECTADA · manutenção' },
  { key: 'critical', title: 'Simular Anomalia (Crítico)', detail: 'Temp 372 · fluxo baixo · ALARME_EMITIDO' },
]

export default function DemoPage({ state, onScenario }) {
  return (
    <section>
      <p className="scada-kicker">T05 · Roteiro Marcus</p>
      <h1 className="scada-heading">Demo / Cenários</h1>
      <p className="scada-lead">
        Dispara os fluxos da apresentação: Normal, Observação, Falha Sensor, Anomalia Crítica.
      </p>

      <div className="scada-scenarios">
        {SCENARIOS.map((scenario) => (
          <button
            key={scenario.key}
            type="button"
            className="scada-scenario"
            onClick={() => onScenario(scenario.key)}
          >
            <strong>{scenario.title}</strong>
            <span>{scenario.detail}</span>
          </button>
        ))}
      </div>

      <div className="scada-note" role="status">
        Cenário aplicado: <strong>{state.scenario}</strong>. As telas Visão Geral, Sensores,
        Alarmes e Auditoria compartilham o mesmo estado da demonstração. A ligação HTTP
        com o EventBus Java permanece como próximo passo da arquitetura.
      </div>
    </section>
  )
}
