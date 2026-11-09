import { useState } from 'react'
import { useEstado } from '../state/EstadoContext.jsx'
import { IconPlay, IconLock, IconCheck } from '../components/icons.jsx'

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

/** Mensagem após aplicar o cenário; o tom segue as cores de status (verde, amarelo/atenção, vermelho). */
const FEEDBACK = {
  NORMAL: { tom: 'ok', texto: 'Leituras normais aplicadas. Veja o resultado em Visão Geral.' },
  OBSERVACAO: {
    tom: 'warn',
    texto: 'Observação preventiva registrada: temperatura em faixa de atenção, sem alarme. Veja em Visão Geral e Auditoria.',
  },
  FALHA: {
    tom: 'warn',
    texto: 'Falha de comunicação registrada no sensor de pressão e equipe técnica avisada. Veja o evento em Visão Geral e Auditoria; a falha não abre alarme.',
  },
  CRITICO: {
    tom: 'crit',
    texto: 'Anomalia crítica aplicada: alarmes abertos. Veja em Alarmes, Visão Geral e Auditoria.',
  },
}

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
      setMensagemFeedback({ tipo: FEEDBACK[item.key].tom, texto: FEEDBACK[item.key].texto })
    } else {
      setMensagemFeedback({
        tipo: 'erro',
        texto: 'Não foi possível aplicar o cenário: sem resposta da API. Verifique se o serviço está em execução.',
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
          className={`scada-note scada-note-${mensagemFeedback.tipo}`}
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
        Telemetria:{' '}
        <strong>
          {origemMock ? 'modo simulado local (sem conexão com a API)' : 'conectada à usina em tempo real'}
        </strong>
        . Alarmes abertos permanecem até serem reconhecidos e encerrados em Alarmes.
      </div>
    </section>
  )
}
