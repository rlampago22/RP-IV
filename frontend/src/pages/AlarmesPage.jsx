export default function AlarmesPage() {
  return (
    <section>
      <p className="scada-kicker">T03 · UC01 / RF-2</p>
      <h1 className="scada-heading">Alarmes</h1>
      <p className="scada-lead">Emitido → Reconhecido → Resolvido</p>

      <div className="scada-card" style={{ marginTop: 16 }}>
        <h2>Gestão de alarmes & notificações</h2>
        <div className="scada-alarm-box">
{`[ALTO] Temperatura acima do limiar (T-CORE-01) · ATIVO
[MANUT] Falha comunicação R-CONT-01 · RECONHECIDO`}
        </div>
        <div className="scada-actions">
          <button type="button" className="scada-btn scada-btn-amber">Validar / Reconhecer</button>
          <button type="button" className="scada-btn scada-btn-green">Normalizar / Encerrar</button>
        </div>
      </div>
    </section>
  )
}
