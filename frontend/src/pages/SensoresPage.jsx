import { SENSORES_RF1, statusSensor, pctBarra, corTipo } from '../data/sensores.js'

function Sparkline({ values, color }) {
  if (!values?.length) return null
  const w = 120
  const h = 28
  const min = Math.min(...values)
  const max = Math.max(...values)
  const span = max - min || 1
  const pts = values
    .map((v, i) => {
      const x = (i / (values.length - 1 || 1)) * w
      const y = h - ((v - min) / span) * (h - 4) - 2
      return `${x},${y}`
    })
    .join(' ')
  return (
    <svg width={w} height={h} viewBox={`0 0 ${w} ${h}`} aria-hidden="true">
      <polyline
        fill="none"
        stroke={color}
        strokeWidth="2"
        strokeLinejoin="round"
        strokeLinecap="round"
        points={pts}
      />
    </svg>
  )
}

export default function SensoresPage() {
  return (
    <section>
      <p className="scada-kicker">T02 · Telemetria RF-1</p>
      <h1 className="scada-heading">Sensores</h1>
      <p className="scada-lead">
        Lista RF-1 com barra, status e histórico curto (MedicaoRegistrada).
        Contrato compartilhado com T01 — mock até API Java.
      </p>

      <div className="scada-sensors" style={{ marginTop: 16 }}>
        {SENSORES_RF1.map((s) => {
          const st = statusSensor(s)
          const cor = corTipo(s.tipo)
          const pct = pctBarra(s)
          return (
            <div className="scada-sensor" key={s.id} style={{ gridTemplateColumns: '1fr auto auto' }}>
              <div>
                <div className="name">
                  {s.id} · {s.tipo}
                </div>
                <div className="val" style={{ color: cor, marginTop: 4 }}>
                  {s.valor.toFixed(2)}{' '}
                  <small style={{ color: 'var(--dim)', fontWeight: 600 }}>{s.unidade}</small>
                </div>
              </div>
              <Sparkline values={s.historico} color={cor} />
              <span className={`scada-pill ${st.cls}`}>{st.label}</span>
              <div className="scada-bar">
                <i style={{ width: `${pct}%`, background: cor }} />
              </div>
            </div>
          )
        })}
      </div>

      <div className="scada-card" style={{ marginTop: 16 }}>
        <h2>Histórico recente (mock · ReatorRepository)</h2>
        <div className="scada-table-wrap" style={{ maxHeight: 'none' }}>
          <table className="scada-table">
            <thead>
              <tr>
                <th>Sensor</th>
                <th>Amostra -5</th>
                <th>-4</th>
                <th>-3</th>
                <th>-2</th>
                <th>-1</th>
                <th>Atual</th>
              </tr>
            </thead>
            <tbody>
              {SENSORES_RF1.map((s) => (
                <tr key={`h-${s.id}`}>
                  <td className="ev">{s.id}</td>
                  {s.historico.map((v, i) => (
                    <td key={i}>{Number(v).toFixed(2)}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="scada-note">
          Backend: <code>ReatorFacade.consultarHistorico(sensorId)</code> /
          <code> consultarHistoricoRecente(n)</code> · SEQ-UC01 em{' '}
          <code>docs/marco1/07-sequencias-mvp.md</code>
        </div>
      </div>
    </section>
  )
}
