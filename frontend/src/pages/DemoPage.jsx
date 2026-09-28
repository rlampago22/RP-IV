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
      <p className="scada-kicker">SIMULADOR OPERACIONAL • INJEÇÃO DE CENÁRIOS</p>
      <h1 className="scada-heading">Simulação de Cenários</h1>
      <p className="scada-lead">
        Injeção controlada de condições operacionais para validação de resposta do sistema: Normal, Atenção Preventiva, Falha Instrumental e Anomalia Crítica.
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
        Barramento de telemetria:{' '}
        <strong>
          {origemMock
            ? 'Modo Simulado Local (Offline)'
            : 'Conectado à Usina em Tempo Real (EventBus Online)'}
        </strong>
        .
      </div>
    </section>
  )
}
