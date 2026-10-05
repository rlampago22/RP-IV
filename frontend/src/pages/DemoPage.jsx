import { useState } from 'react'
import { useEstado } from '../state/EstadoContext.jsx'

const SCENARIOS = [
  { key: 'NORMAL', title: 'Operação Normal', detail: 'Leituras na faixa ideal · núcleo ESTÁVEL' },
  {
    key: 'OBSERVACAO',
    title: 'Simular Observação',
    detail: 'Temperatura em faixa de atenção · sem alarme',
  },
  {
    key: 'FALHA',
    title: 'Falha de Sensor',
    detail: 'Perda de comunicação · alerta à manutenção · ATENÇÃO',
  },
  {
    key: 'CRITICO',
    title: 'Simular Anomalia (Crítico)',
    detail: 'Temperatura 372 °C · fluxo baixo · alarmes emitidos',
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
        texto: `Cenário "${item.title}" aplicado. Confira os efeitos em Visão Geral, Alarmes e Auditoria.`,
      })
    } else {
      setMensagemFeedback({
        tipo: 'erro',
        texto: 'Não foi possível aplicar o cenário: sem resposta da API. Verifique se o serviço está em execução.',
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
        Telemetria:{' '}
        <strong>
          {origemMock ? 'modo simulado local (sem conexão com a API)' : 'conectada à usina em tempo real'}
        </strong>
        . Alarmes abertos permanecem até serem reconhecidos e encerrados em Alarmes.
      </div>
    </section>
  )
}
