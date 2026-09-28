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
 * T05 — botões do roteiro Marcus (Issue #52).
 * Dispara cenários via /api/cenarios/{key} atualizando o EventBus e refletindo em T01–T04.
 */
export default function DemoPage() {
  const { dispararCenario, origemMock } = useEstado()
  const [scenario, setScenario] = useState('NORMAL')
  const [disparando, setDisparando] = useState(false)
  const [mensagemFeedback, setMensagemFeedback] = useState(null)

  const handleDisparar = async (item) => {
    setScenario(item.key)
    setDisparando(true)
    setMensagemFeedback(null)
    const ok = await dispararCenario(item.key)
    if (ok) {
      setMensagemFeedback({
        tipo: 'sucesso',
        texto: `Cenário "${item.title}" disparado com sucesso no EventBus! Verifique T01 (Overview), T03 (Alarmes) e T04 (Auditoria).`,
      })
    } else {
      setMensagemFeedback({
        tipo: 'erro',
        texto: 'Não foi possível disparar o cenário. Confira se a API Java está rodando (mvp/4-EXECUTAR-API-ESTADO.bat).',
      })
    }
    setDisparando(false)
  }

  return (
    <section>
      <p className="scada-kicker">T05 · Roteiro Marcus (Issue #52)</p>
      <h1 className="scada-heading">Demo / Cenários</h1>
      <p className="scada-lead">
        Dispara os fluxos da apresentação: Normal, Observação, Falha Sensor e Anomalia Crítica diretamente no barramento EDA.
      </p>

      <div className="scada-scenarios">
        {SCENARIOS.map((item) => (
          <button
            key={item.key}
            type="button"
            className={`scada-scenario ${scenario === item.key ? 'scada-scenario-active' : ''}`}
            disabled={disparando}
            onClick={() => handleDisparar(item)}
          >
            <strong>{item.title}</strong>
            <span>{item.detail}</span>
          </button>
        ))}
      </div>

      {mensagemFeedback && (
        <div
          className={`scada-note ${mensagemFeedback.tipo === 'sucesso' ? 'scada-note-ok' : ''}`}
          role="status"
          style={{ marginTop: '1rem' }}
        >
          {mensagemFeedback.texto}
        </div>
      )}

      <div className="scada-note" role="status" style={{ marginTop: '1rem' }}>
        Status da conexão:{' '}
        <strong>
          {origemMock ? 'Mock Offline (API desligada)' : 'Conectado à API Java (EventBus Ativo)'}
        </strong>
        . Para demonstração integrada completa, use <code>mvp/4-EXECUTAR-API-ESTADO.bat</code>.
      </div>
    </section>
  )
}
