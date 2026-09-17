import { SENSORES_RF1, statusSensor, pctBarra, corTipo } from '../data/sensores.js'
import { usePlant } from '../state/PlantContext.jsx'

export default function OverviewPage() {
  const { alarmes, status, reconhecer, resolver, auditExtra } = usePlant()
  const alarmesAbertos = alarmes.filter((a) => a.estado !== 'RESOLVIDO')
  const coreCls = status.cls || ''
  const coreLabel = status.label === 'ESTÁVEL' ? 'ESTÁVEL' : status.label

  const timeline = [
    ...auditExtra.map((e) => ({ hora: e.hora, tipo: e.evento, detalhe: e.detalhe })),
    { hora: '22:45:01', tipo: 'MEDICAO_REGISTRADA', detalhe: 'T=312.0 P=155.0' },
    { hora: '22:44:58', tipo: 'ALARME_EMITIDO', detalhe: 'Temp acima do limiar' },
    { hora: '22:44:50', tipo: 'MEDICAO_REGISTRADA', detalhe: 'ciclo tempo real' },
  ]

  return (
    <section>
      <p className="scada-kicker">T01 · Overview</p>
      <h1 className="scada-heading">Visão Geral</h1>
      <p className="scada-lead">
        Núcleo + telemetria RF-1 + timeline EDA. Contrato sensores = T02; alarmes = T03.
        Evidência EventBus: <code>mvp/.../infraestruturaeventos/EventBus.java</code> (#41).
      </p>

      <div className="scada-split">
        <div className="scada-card">
          <h2>Núcleo do reator</h2>
          <div className="scada-reactor">
            <div className={`scada-core${coreCls ? ` ${coreCls}` : ''}`}>{coreLabel}</div>
          </div>
          <div className="scada-sensors">
            {SENSORES_RF1.map((s) => {
              const st = statusSensor(s)
              const cor = corTipo(s.tipo)
              return (
                <div className="scada-sensor" key={s.id}>
                  <div className="name">{s.tipo}</div>
                  <div className="val" style={{ color: cor }}>
                    {s.valor.toFixed(2)} {s.unidade}
                  </div>
                  <div className="scada-bar">
                    <i style={{ width: `${pctBarra(s)}%`, background: cor }} />
                  </div>
                  <span
                    className={`scada-pill ${st.cls}`}
                    style={{ gridColumn: '1 / -1', justifySelf: 'start' }}
                  >
                    {st.label}
                  </span>
                </div>
              )
            })}
          </div>
        </div>

        <div className="scada-card">
          <h2>Contadores EDA</h2>
          <div className="scada-metrics">
            <div className="scada-metric">
              <span>MEDIÇÕES</span>
              <strong>12</strong>
            </div>
            <div className="scada-metric">
              <span>ALARMES</span>
              <strong style={{ color: alarmesAbertos.length ? 'var(--crit)' : 'var(--ok)' }}>
                {alarmesAbertos.length}
              </strong>
            </div>
            <div className="scada-metric">
              <span>AUDIT</span>
              <strong style={{ color: 'var(--cyan)' }}>{18 + auditExtra.length}</strong>
            </div>
            <div className="scada-metric">
              <span>STATUS</span>
              <strong
                style={{
                  color: status.cls === 'crit' ? 'var(--crit)' : status.cls === 'warn' ? 'var(--warn)' : 'var(--ok)',
                  fontSize: 14,
                }}
              >
                {status.label}
              </strong>
            </div>
          </div>
          <h2>Linha do tempo de eventos</h2>
          <div className="scada-table-wrap">
            <table className="scada-table">
              <thead>
                <tr>
                  <th>Hora</th>
                  <th>Evento</th>
                  <th>Detalhe</th>
                </tr>
              </thead>
              <tbody>
                {timeline.slice(0, 8).map((e, i) => (
                  <tr key={`${e.hora}-${e.tipo}-${i}`}>
                    <td>{e.hora}</td>
                    <td className="ev">{e.tipo}</td>
                    <td>{e.detalhe}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="scada-alarm-box">
            {alarmesAbertos.length === 0
              ? 'Nenhum alarme pendente nesta sessão.'
              : alarmesAbertos
                  .map((a) => `[${a.severidade}] ${a.mensagem} (${a.codigo}) · ${a.estado}`)
                  .join('\n')}
          </div>
          <div className="scada-actions">
            <button type="button" className="scada-btn">
              Iniciar Tempo Real
            </button>
            <button type="button" className="scada-btn scada-btn-amber" onClick={() => reconhecer()}>
              Validar / Reconhecer
            </button>
            <button type="button" className="scada-btn scada-btn-green" onClick={() => resolver()}>
              Normalizar / Encerrar
            </button>
          </div>
        </div>
      </div>
    </section>
  )
}
