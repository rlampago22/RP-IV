export default function AlarmesPage({ alarms, onAck, onResolve }) {
  return (
    <section>
      <p className="scada-kicker">T03 · UC01 / RF-2</p>
      <h1 className="scada-heading">Alarmes</h1>
      <p className="scada-lead">Emitido → Reconhecido → Resolvido</p>

      <div className="scada-card" style={{ marginTop: 16 }}>
        <h2>Gestão de alarmes & notificações</h2>
        <div className="scada-alarm-box">
          {alarms.length === 0
            ? 'Nenhum alarme emitido no cenário atual.'
            : alarms.map((alarm) => `[ALTO] ${alarm.id} · ${alarm.mensagem} · ${alarm.status}`).join('\n')}
        </div>
        <div className="scada-actions">
          <button type="button" className="scada-btn scada-btn-amber" onClick={onAck}>Validar / Reconhecer</button>
          <button type="button" className="scada-btn scada-btn-green" onClick={onResolve}>Normalizar / Encerrar</button>
        </div>
      </div>
    </section>
  )
}
