export default function AuditoriaPage({ events }) {
  const openLog = () => {
    window.alert('O log persistente do domínio Java fica em: mvp/dados/auditoria.log')
  }

  return (
    <section>
      <p className="scada-kicker">T04 · RNF-03/05</p>
      <h1 className="scada-heading">Auditoria</h1>
      <p className="scada-lead">Trilha append-only · cadeia SHA-256 (estado compartilhado da demo)</p>

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
              {events.map((entry) => (
                <tr key={entry.sequence}>
                  <td>{entry.sequence}</td>
                  <td>{entry.timestamp}</td>
                  <td className="ev">{entry.tipo}</td>
                  <td title="Hash demonstrativo no front; SHA-256 real no Java">{entry.hash}…</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="scada-actions">
          <button type="button" className="scada-btn scada-btn-ghost" onClick={openLog}>Abrir Log</button>
          <code className="scada-log-path">mvp/dados/auditoria.log</code>
        </div>
      </div>
    </section>
  )
}
