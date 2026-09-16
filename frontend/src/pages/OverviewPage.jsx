export default function OverviewPage() {
  return (
    <section>
      <div className="banner">Idealização T01 — substituir dados mock por API Java (EDA).</div>
      <h1>Visão geral</h1>
      <p className="muted">Estado do reator e KPIs. Referência visual em docs/ui/exports.</p>
      <div className="grid" style={{ marginTop: '1rem' }}>
        <div className="card"><h3>Temperatura</h3><div className="value">312.0 °C</div></div>
        <div className="card"><h3>Pressão</h3><div className="value">155.0 bar</div></div>
        <div className="card"><h3>Radiação</h3><div className="value">0.12 mSv/h</div></div>
        <div className="card"><h3>Fluxo resfriamento</h3><div className="value">98 %</div></div>
      </div>
      <div className="card" style={{ marginTop: '1rem' }}>
        <h3>Mimic simplificado</h3>
        <p className="muted">Área para diagrama de processo leve (sem animação decorativa).</p>
      </div>
    </section>
  )
}
