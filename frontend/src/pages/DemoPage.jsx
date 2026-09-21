import { useState } from 'react'

const SCENARIOS = [
  { key: 'NORMAL', title: 'Normal / Tempo Real', detail: 'Medições na faixa · núcleo ESTÁVEL' },
  {
    key: 'OBSERVACAO',
    title: 'Simular Observação (Alt. 1)',
    detail: 'OBSERVACAO_REGISTRADA · sem alarme alto',
  },
  {
    key: 'FALHA',
    title: 'Falha Sensor (Exceção)',
    detail: 'FALHA_SENSOR_DETECTADA · manutenção',
  },
  {
    key: 'CRITICO',
    title: 'Simular Anomalia (Crítico)',
    detail: 'Temp 372 · fluxo baixo · ALARME_EMITIDO',
  },
]

/**
 * T05 — botões do roteiro Marcus.
 * Estado live T01–T04 vem de EstadoContext (/api/estado).
 * Cenários locais ficam registrados aqui; ligação completa = S4 #52.
 * Reducer de referência: mvpDemoState.js
 */
export default function DemoPage() {
  const [scenario, setScenario] = useState('NORMAL')

  return (
    <section>
      <p className="scada-kicker">T05 · Roteiro Marcus</p>
      <h1 className="scada-heading">Demo / Cenários</h1>
      <p className="scada-lead">
        Dispara os fluxos da apresentação: Normal, Observação, Falha Sensor, Anomalia Crítica.
      </p>

      <div className="scada-scenarios">
        {SCENARIOS.map((item) => (
          <button
            key={item.key}
            type="button"
            className="scada-scenario"
            onClick={() => setScenario(item.key)}
          >
            <strong>{item.title}</strong>
            <span>{item.detail}</span>
          </button>
        ))}
      </div>

      <div className="scada-note" role="status">
        Cenário selecionado: <strong>{scenario}</strong>. T01–T04 usam{' '}
        <code>EstadoContext</code> (<code>/api/estado</code>). Para anomalia ao vivo, use{' '}
        <strong>Iniciar Tempo Real</strong> na Visão Geral com a API Java rodando (
        <code>mvp/4-EXECUTAR-API-ESTADO.bat</code>). Integração T05→EventBus: issue #52.
      </div>
    </section>
  )
}
