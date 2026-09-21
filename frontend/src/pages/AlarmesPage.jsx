const STATUS_LABELS = {
  ATIVO: 'ATIVO',
  RECONHECIDO: 'EM TRATAMENTO',
  RESOLVIDO: 'RESOLVIDO',
}

export default function AlarmesPage({ alarms, eventCount, onAcknowledge, onResolve }) {
  return (
    <section>
      <p className="scada-kicker">T03 · UC01 / RF-2</p>
      <h1 className="scada-heading">Alarmes</h1>
      <p className="scada-lead">Emitido → Reconhecido → Resolvido</p>

      <div className="scada-card" style={{ marginTop: 16 }}>
        <h2>Gestão de alarmes & notificações</h2>
        <div className="scada-alarm-list">
          {alarms.map((alarm) => (
            <article className={`scada-alarm-row ${alarm.severity.toLowerCase()}`} key={alarm.id}>
              <div className="scada-alarm-main">
                <div className="scada-alarm-heading">
                  <span className={`scada-pill ${alarm.severity === 'ALTO' ? 'crit' : 'purple'}`}>
                    {alarm.severity}
                  </span>
                  <strong>{alarm.message}</strong>
                </div>
                <span className="scada-alarm-meta">{alarm.sensor} · {alarm.id}</span>
              </div>
              <span className={`scada-pill ${alarm.status === 'RESOLVIDO' ? 'ok' : alarm.status === 'ATIVO' ? 'crit' : 'warn'}`}>
                {STATUS_LABELS[alarm.status]}
              </span>
              <div className="scada-actions">
                {alarm.status === 'ATIVO' && (
                  <button type="button" className="scada-btn scada-btn-amber" onClick={() => onAcknowledge(alarm.id)}>
                    Validar / Reconhecer
                  </button>
                )}
                {alarm.status === 'RECONHECIDO' && (
                  <button type="button" className="scada-btn scada-btn-green" onClick={() => onResolve(alarm.id)}>
                    Normalizar / Encerrar
                  </button>
                )}
              </div>
            </article>
          ))}
        </div>
        <p className="scada-event-note">
          Eventos emitidos nesta sessão: {eventCount} · ALARME_RECONHECIDO / ALARME_RESOLVIDO
        </p>
      </div>
    </section>
  )
}
