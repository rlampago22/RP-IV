export default function AuditoriaPage() {
  return (
    <section>
      <h1>Auditoria</h1>
      <p className="muted">T04 — trilha append-only do consumidor AuditoriaLogs.</p>
      <table>
        <thead><tr><th>Timestamp</th><th>Evento</th><th>Detalhe</th></tr></thead>
        <tbody>
          <tr><td>—</td><td>MedicaoRegistrada</td><td>sensor=1 valor=312.0</td></tr>
        </tbody>
      </table>
    </section>
  )
}
