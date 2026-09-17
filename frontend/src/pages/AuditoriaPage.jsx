import { usePlant } from '../state/PlantContext.jsx'

const AUDIT_BASE = [
  { n: 18, ts: '2026-09-15T22:45:01Z', evento: 'MEDICAO_REGISTRADA', hash: 'a2f91c0e…' },
  { n: 17, ts: '2026-09-15T22:44:58Z', evento: 'ALARME_EMITIDO', hash: 'b7e12a44…' },
  { n: 16, ts: '2026-09-15T22:44:50Z', evento: 'OBSERVACAO_REGISTRADA', hash: 'c91d03ab…' },
  { n: 15, ts: '2026-09-15T22:40:00Z', evento: 'FALHA_SENSOR_DETECTADA', hash: 'd4aa8812…' },
]

export default function AuditoriaPage() {
  const { auditExtra } = usePlant()
  const extras = auditExtra.map((e, i) => ({
    n: 18 + auditExtra.length - i,
    ts: e.hora,
    evento: e.evento,
    hash: 'mock…',
  }))

  return (
    <section>
      <p className="scada-kicker">T04 · RNF-03/05</p>
      <h1 className="scada-heading">Auditoria</h1>
      <p className="scada-lead">Trilha append-only · cadeia SHA-256 (mock visual)</p>

      <div className="scada-card" style={{ marginTop: 16 }}>
        <div className="scada-table-wrap" style={{ maxHeight: 'none' }}>
          <table className="scada-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Timestamp</th>
                <th>Evento</th>
                <th>Hash</th>
              </tr>
            </thead>
            <tbody>
              {[...extras, ...AUDIT_BASE].map((row) => (
                <tr key={`${row.n}-${row.evento}-${row.ts}`}>
                  <td>{row.n}</td>
                  <td>{row.ts}</td>
                  <td className="ev">{row.evento}</td>
                  <td>{row.hash}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="scada-actions">
          <button type="button" className="scada-btn scada-btn-ghost">
            Abrir Log
          </button>
        </div>
      </div>
    </section>
  )
}
