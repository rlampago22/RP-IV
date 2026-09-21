export default function AuditoriaPage() {
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
              <tr>
                <td>18</td>
                <td>2026-09-15T22:45:01Z</td>
                <td className="ev">MEDICAO_REGISTRADA</td>
                <td>a2f91c0e…</td>
              </tr>
              <tr>
                <td>17</td>
                <td>2026-09-15T22:44:58Z</td>
                <td className="ev">ALARME_EMITIDO</td>
                <td>b7e12a44…</td>
              </tr>
              <tr>
                <td>16</td>
                <td>2026-09-15T22:44:50Z</td>
                <td className="ev">OBSERVACAO_REGISTRADA</td>
                <td>c91d03ab…</td>
              </tr>
              <tr>
                <td>15</td>
                <td>2026-09-15T22:40:00Z</td>
                <td className="ev">FALHA_SENSOR_DETECTADA</td>
                <td>d4aa8812…</td>
              </tr>
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
