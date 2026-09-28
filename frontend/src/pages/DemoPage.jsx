import { useState } from 'react'
import { useEstado } from '../state/EstadoContext.jsx'

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
  const [mensagem, setMensagem] = useState('')
  const { simularAnomalia } = useEstado()

  async function selecionarCenario(chave) {
    setScenario(chave)
    if (chave === 'CRITICO') {
      const sucesso = await simularAnomalia()
      setMensagem(sucesso
        ? 'Anomalia enviada à API. O banner e a fila de alarmes foram atualizados.'
        : 'Não foi possível simular a anomalia. Confira se a API Java está rodando.')
      return
    }
    setMensagem('')
  }

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
            onClick={() => selecionarCenario(item.key)}
          >
            <strong>{item.title}</strong>
            <span>{item.detail}</span>
          </button>
        ))}
      </div>

      <div className="scada-note" role="status">
        Cenário selecionado: <strong>{scenario}</strong>. {mensagem || 'T01–T04 consomem o snapshot live de /api/estado.'}
      </div>
    </section>
  )
}
