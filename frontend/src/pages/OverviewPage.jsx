import { useEstado } from '../state/EstadoContext.jsx'
import { interpretarEvento } from '../data/eventos.js'
import {
  IconThermometer,
  IconGauge,
  IconRadiation,
  IconDroplet,
  IconCore,
  IconActivity,
  IconBell,
  IconShield,
  IconClock,
  IconPlay,
  IconCheck,
} from '../components/icons.jsx'

const NOME_SENSOR = {
  TEMPERATURA: 'Temperatura',
  PRESSAO: 'Pressão',
  RADIACAO: 'Radiação',
  FLUXO_RESFRIAMENTO: 'Fluxo resfriamento',
}

const ICONE_SENSOR = {
  TEMPERATURA: IconThermometer,
  PRESSAO: IconGauge,
  RADIACAO: IconRadiation,
  FLUXO_RESFRIAMENTO: IconDroplet,
}

const UNIDADE_EXIBICAO = {
  Celsius: '°C',
  bar: 'bar',
  'mSv/h': 'mSv/h',
  'm3/h': 'm³/h',
}

const STATUS_TEXTO = { ESTAVEL: 'ESTÁVEL', ATENCAO: 'ATENÇÃO', CRITICO: 'CRÍTICO' }
const STATUS_CLASSE = { ESTAVEL: '', ATENCAO: 'warn', CRITICO: 'crit' }

function percentualNaFaixa(sensor) {
  const faixa = sensor.limiteMaximo - sensor.limiteMinimo
  if (faixa <= 0) return 0
  const percentual = ((sensor.valor - sensor.limiteMinimo) / faixa) * 100
  return Math.max(0, Math.min(100, percentual))
}

function corSensor(sensor, alarmes) {
  const temAlarmeAtivo = alarmes.some((a) => a.sensorId === sensor.id && a.status === 'ATIVO')
  if (temAlarmeAtivo) return 'var(--crit)'
  const temAlarmeReconhecido = alarmes.some(
    (a) => a.sensorId === sensor.id && a.status === 'RECONHECIDO'
  )
  if (temAlarmeReconhecido) return 'var(--warn)'
  return 'var(--cyan)'
}

function formatarHora(iso) {
  return new Date(iso).toLocaleTimeString('pt-BR', { hour12: false })
}

export default function OverviewPage() {
  const { estado, iniciarTempoReal, pausarTempoReal, reconhecerAlarme, resolverAlarme } =
    useEstado()
  const { status, tempoReal, sensores, contadores, alarmes, eventos } = estado

  const alarmePendenteReconhecer = alarmes.find((a) => a.status === 'ATIVO')
  const alarmePendenteResolver = alarmes.find((a) => a.status === 'RECONHECIDO')
  const alarmesNaoResolvidos = alarmes.filter((a) => a.status !== 'RESOLVIDO')

  return (
    <section>
      <p className="scada-kicker">PAINEL DE SUPERVISÃO CENTRAL • PACOTE R-01</p>
      <h1 className="scada-heading">Visão Geral Operacional</h1>
      <p className="scada-lead">
        Supervisão contínua do reator, telemetria de resfriamento e linha do tempo de eventos em tempo real.
      </p>

      <div className="scada-split">
        <div
          className="scada-card"
          style={{ background: 'radial-gradient(circle at 50% 20%, var(--card2) 0%, var(--card) 60%)' }}
        >
          <h2 className="panel-heading">
            <IconCore size={13} />
            Status do Núcleo do Reator
          </h2>

          <div style={{ display: 'flex', justifyContent: 'center', padding: '10px 0 18px' }}>
            {(() => {
              const pico = sensores.length
                ? Math.max(...sensores.map((s) => percentualNaFaixa(s)))
                : 0
              const cor =
                status === 'CRITICO' ? 'var(--crit)' : status === 'ATENCAO' ? 'var(--warn)' : 'var(--ok)'
              return (
                <div
                  className="ring ring-lg"
                  style={{ background: `conic-gradient(${cor} ${pico}%, var(--border) 0)` }}
                >
                  <div className="ring-value">
                    <div className="mono" style={{ fontSize: 30, fontWeight: 700, color: cor }}>
                      {sensores.length ? `${pico.toFixed(0)}%` : '—'}
                    </div>
                    <div style={{ fontSize: 10, fontWeight: 700, letterSpacing: '0.14em', color: cor, marginTop: 4 }}>
                      {STATUS_TEXTO[status] ?? status}
                    </div>
                  </div>
                </div>
              )
            })()}
          </div>

          {sensores.length === 0 ? (
            <p className="scada-lead">Aguardando a primeira leitura dos sensores…</p>
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
              {sensores.map((sensor) => {
                const cor = corSensor(sensor, alarmes)
                const Icone = ICONE_SENSOR[sensor.tipo] ?? IconActivity
                const pct = percentualNaFaixa(sensor)
                return (
                  <div className="sensor-tile2" key={sensor.id}>
                    <div className="ring ring-sm" style={{ background: `conic-gradient(${cor} ${pct}%, var(--border) 0)` }}>
                      <Icone size={18} className="ring-icon" style={{ color: 'var(--text)' }} />
                    </div>
                    <div className="meta">
                      <div className="name">{(NOME_SENSOR[sensor.tipo] ?? sensor.tipo).toUpperCase()}</div>
                      <div className="value mono">
                        {sensor.valor.toFixed(2)}
                        <small>{UNIDADE_EXIBICAO[sensor.unidade] ?? sensor.unidade}</small>
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
            <div className="stat-tile">
              <IconActivity size={20} style={{ color: 'var(--sky)' }} />
              <div>
                <div className="num">{contadores.medicoes}</div>
                <div className="lbl">MEDIÇÕES</div>
              </div>
            </div>
            <div className="stat-tile">
              <IconBell size={20} style={{ color: contadores.alarmes > 0 ? 'var(--crit)' : 'var(--muted)' }} />
              <div>
                <div className="num" style={{ color: contadores.alarmes > 0 ? 'var(--crit)' : undefined }}>
                  {contadores.alarmes}
                </div>
                <div className="lbl">ALARMES</div>
              </div>
            </div>
            <div className="stat-tile">
              <IconShield size={20} style={{ color: 'var(--sky)' }} />
              <div>
                <div className="num">{contadores.auditoria}</div>
                <div className="lbl">AUDITORIA</div>
              </div>
            </div>
          </div>

          <div className="scada-card" style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0 }}>
            <h2 className="panel-heading">
              <IconClock size={13} />
              Histórico Operacional em Tempo Real
            </h2>
            <div className="scada-table-wrap" style={{ maxHeight: 220 }}>
              {eventos.length === 0 ? (
                <p className="scada-lead">Sem eventos ainda.</p>
              ) : (
                <ul className="timeline2">
                  {eventos.slice(0, 8).map((evento, indice) => {
                    const info = interpretarEvento(evento.tipo, evento.resumo)
                    return (
                      <li key={`${evento.ocorridoEm}-${indice}`}>
                        <span className="dot" style={{ background: info.cor }} />
                        <div className="time">{formatarHora(evento.ocorridoEm)}</div>
                        <div className="label">{info.titulo}</div>
                        <div className="detail">{info.descricao}</div>
                      </li>
                    )
                  })}
                </ul>
              )}
            </div>

            {alarmesNaoResolvidos.length > 0 && (
              <div className="scada-alarm-box" style={{ marginTop: 12 }}>
                {alarmesNaoResolvidos
                  .map((a) => `[${a.severidade}] ${a.mensagem} · ${a.status}`)
                  .join('\n')}
              </div>
            )}

            <div style={{ marginTop: 'auto', paddingTop: 14, display: 'flex', gap: 10, flexWrap: 'wrap' }}>
              <button
                type="button"
                className="action-btn2"
                style={{ background: '#0f766e' }}
                onClick={() => (tempoReal ? pausarTempoReal() : iniciarTempoReal())}
              >
                <IconPlay size={14} />
                {tempoReal ? 'Pausar Tempo Real' : 'Iniciar Tempo Real'}
              </button>
              <button
                type="button"
                className="action-btn2"
                style={{ background: '#b45309' }}
                disabled={!alarmePendenteReconhecer}
                onClick={() =>
                  alarmePendenteReconhecer && reconhecerAlarme(alarmePendenteReconhecer.id)
                }
              >
                <IconCheck size={14} />
                Validar / Reconhecer
              </button>
              <button
                type="button"
                className="action-btn2"
                style={{ background: '#059669' }}
                disabled={!alarmePendenteResolver}
                onClick={() => alarmePendenteResolver && resolverAlarme(alarmePendenteResolver.id)}
              >
                <IconCheck size={14} />
                Normalizar / Encerrar
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
