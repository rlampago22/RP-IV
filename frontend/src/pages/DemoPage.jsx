import { useState } from 'react'
import { useEstado } from '../state/EstadoContext.jsx'
import { IconPlay, IconLock, IconCheck } from '../components/icons.jsx'

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
  const { dispararCenario, origemMock, solicitarAcesso } = useEstado()
  const [scenario, setScenario] = useState('NORMAL')
  const [disparando, setDisparando] = useState(false)
  const [mensagemFeedback, setMensagemFeedback] = useState(null)
  const [acessoFeedback, setAcessoFeedback] = useState(null)

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

  const handleAcesso = async (credencial, area, rotulo) => {
    setAcessoFeedback(null)
    const resultado = await solicitarAcesso(credencial, area)
    if (!resultado) {
      setAcessoFeedback({
        tipo: 'erro',
        texto: 'Falha ao registrar acesso. Confira a API Java (mvp/4-EXECUTAR-API-ESTADO.bat).',
      })
      return
    }
    setAcessoFeedback({
      tipo: resultado.resultado === 'SUCESSO' ? 'sucesso' : 'erro',
      texto: `${rotulo}: ${resultado.resultado} · área ${resultado.area} · crachá ${resultado.credencial}`,
    })
  }

  return (
    <section>
      <p className="scada-kicker" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <IconPlay size={11} />
        SIMULADOR OPERACIONAL • INJEÇÃO DE CENÁRIOS
      </p>
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

      <p className="scada-kicker" style={{ marginTop: '1.5rem', display: 'flex', alignItems: 'center', gap: 6 }}>
        <IconLock size={11} />
        UC04 · RF07/RF08 — CONTROLE DE ACESSO
      </p>
      <h2 className="scada-heading" style={{ fontSize: '1.25rem' }}>Registro de acesso a área restrita</h2>
      <p className="scada-lead">
        Credenciais demo: OPER-01 (nível 2), SUP-01 (nível 3), GUARDA-01 (nível 1). Áreas: PERIMETRO, SALA-CONTROLE, CORE-REATOR.
      </p>
      <div className="scada-scenarios">
        <button
          type="button"
          className="scada-scenario"
          onClick={() => handleAcesso('OPER-01', 'SALA-CONTROLE', 'Operador → Sala de Controle')}
        >
          <strong style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <IconCheck size={13} style={{ color: 'var(--ok)' }} />
            Acesso autorizado
          </strong>
          <span>OPER-01 em SALA-CONTROLE → SUCESSO</span>
        </button>
        <button
          type="button"
          className="scada-scenario"
          onClick={() => handleAcesso('GUARDA-01', 'CORE-REATOR', 'Guarda → Core')}
        >
          <strong>Acesso negado</strong>
          <span>GUARDA-01 em CORE-REATOR → FALHA</span>
        </button>
      </div>
      {acessoFeedback && (
        <div
          className={`scada-note ${acessoFeedback.tipo === 'sucesso' ? 'scada-note-ok' : ''}`}
          role="status"
          style={{ marginTop: '1rem' }}
        >
          {acessoFeedback.texto}
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
