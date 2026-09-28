export default function AlarmesPage({ alarmState, onAcknowledge, onResolve }) {
  const message = alarmState === 'active' ? '[ALTO] Temperatura acima do limiar (T-CORE-01) · ATIVO' : alarmState === 'acknowledged' ? '[ALTO] Temperatura acima do limiar (T-CORE-01) · RECONHECIDO' : 'Nenhum alarme ativo.'
  return (
    <section>
      <p className="scada-kicker">T03 · UC01 / RF-2</p>
      <h1 className="scada-heading">Alarmes</h1>
      <p className="scada-lead">Emitido → Reconhecido → Resolvido</p>

      <div className="scada-card" style={{ marginTop: 16 }}>
        <h2>Gestão de alarmes & notificações</h2>
          <div className="scada-alarm-box">{message}</div>
        <div className="scada-actions">
          <button type="button" className="scada-btn scada-btn-amber" onClick={onAcknowledge}>Validar / Reconhecer</button>
          <button type="button" className="scada-btn scada-btn-green" onClick={onResolve}>Normalizar / Encerrar</button>
        </div>
      </div>
    </section>
  )
}
