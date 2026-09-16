export default function AlarmesPage() {
  return (
    <section>
      <h1>Alarmes</h1>
      <p className="muted">T03 — fila priorizada (AlarmeEmitido / Reconhecido / Resolvido).</p>
      <table>
        <thead><tr><th>Severidade</th><th>Mensagem</th><th>Estado</th></tr></thead>
        <tbody>
          <tr><td>ALTO</td><td>Temperatura acima do limiar</td><td>ATIVO</td></tr>
        </tbody>
      </table>
    </section>
  )
}
