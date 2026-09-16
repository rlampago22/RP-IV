export default function SensoresPage() {
  return (
    <section>
      <h1>Sensores</h1>
      <p className="muted">T02 — cards/lista alimentados por MedicaoRegistrada.</p>
      <table>
        <thead><tr><th>ID</th><th>Tipo</th><th>Valor</th><th>Status</th></tr></thead>
        <tbody>
          <tr><td>1</td><td>TEMPERATURA</td><td>312.0</td><td>OK</td></tr>
          <tr><td>2</td><td>PRESSAO</td><td>155.0</td><td>OK</td></tr>
        </tbody>
      </table>
    </section>
  )
}
