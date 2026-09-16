export default function DemoPage() {
  return (
    <section>
      <h1>Demo / Cenários</h1>
      <p className="muted">T05 — dispara cenários no backend (normal, anomalia, falha).</p>
      <div className="grid">
        <button className="card" type="button">Normal</button>
        <button className="card" type="button">Anomalia</button>
        <button className="card" type="button">Falha sensor</button>
        <button className="card" type="button">Reconhecer alarmes</button>
      </div>
    </section>
  )
}
