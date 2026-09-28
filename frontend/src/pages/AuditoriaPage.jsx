export default function AuditoriaPage({ events }) {
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
              {events.map((event, index) => (
                <tr key={`${event.type}-${index}`}>
                  <td>{events.length - index}</td>
                  <td>sessão atual</td>
                  <td className="ev">{event.type}</td>
                  <td>mock-{String(events.length - index).padStart(4, '0')}…</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="scada-actions">
          <button type="button" className="scada-btn scada-btn-ghost">Abrir Log</button>
        </div>
      </div>
    </section>
  )
}
