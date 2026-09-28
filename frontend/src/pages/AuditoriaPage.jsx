import { useEstado } from '../state/EstadoContext.jsx'

export default function AuditoriaPage() {
  const { estado } = useEstado()
  const eventos = estado.eventos ?? []

  return (
    <section>
      <p className="scada-kicker">T04 · RNF-03/05</p>
      <h1 className="scada-heading">Auditoria</h1>
      <p className="scada-lead">
        Trilha de eventos EDA via <code>/api/estado</code> (hash SHA-256 no MVP Java).
      </p>

      <div className="scada-card" style={{ marginTop: 16 }}>
        <div className="scada-table-wrap" style={{ maxHeight: 'none' }}>
          <table className="scada-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Timestamp</th>
                <th>Evento</th>
                <th>Detalhe</th>
              </tr>
            </thead>
            <tbody>
              {eventos.length === 0 && (
                <tr>
                  <td colSpan={4}>Sem eventos de auditoria nesta sessão.</td>
                </tr>
              )}
              {eventos.map((row, i) => (
                <tr key={`${row.ocorridoEm}-${row.tipo}-${i}`}>
                  <td>{eventos.length - i}</td>
                  <td>{row.ocorridoEm}</td>
                  <td className="ev">{row.tipo}</td>
                  <td>{row.resumo}</td>
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
