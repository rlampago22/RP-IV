export default function OverviewPage({ alarmState, events, onAcknowledge, onResolve }) {
  const isCritical = alarmState === 'active'
  const isAcknowledged = alarmState === 'acknowledged'
  const coreLabel = isCritical ? 'CRÍTICO' : isAcknowledged ? 'TRATAMENTO' : 'ESTÁVEL'
  return (
    <section>
      <p className="scada-kicker">T01 · Overview</p>
      <h1 className="scada-heading">Visão Geral</h1>
      <p className="scada-lead">
        Núcleo + telemetria RF-1 + timeline EDA. Mock — ligar à API Java.
      </p>

      <div className="scada-split">
        <div className="scada-card">
          <h2>Núcleo do reator</h2>
          <div className="scada-reactor">
            <div className={`scada-core${isCritical ? ' crit' : isAcknowledged ? ' warn' : ''}`}>{coreLabel}</div>
          </div>
          <div className="scada-sensors">
            <div className="scada-sensor">
              <div className="name">Temperatura</div>
              <div className="val" style={{ color: isCritical ? 'var(--crit)' : 'var(--text)' }}>{isCritical ? '372.00' : '312.00'} °C</div>
              <div className="scada-bar"><i style={{ width: isCritical ? '93%' : '78%', background: isCritical ? 'var(--crit)' : 'var(--cyan)' }} /></div>
            </div>
            <div className="scada-sensor">
              <div className="name">Pressão</div>
              <div className="val">155.00 bar</div>
              <div className="scada-bar"><i style={{ width: '86%', background: 'var(--cyan)' }} /></div>
            </div>
            <div className="scada-sensor">
              <div className="name">Radiação</div>
              <div className="val">0.12 mSv/h</div>
              <div className="scada-bar"><i style={{ width: '12%', background: 'var(--cyan)' }} /></div>
            </div>
            <div className="scada-sensor">
              <div className="name">Fluxo resfriamento</div>
              <div className="val" style={{ color: isCritical ? 'var(--crit)' : 'var(--text)' }}>{isCritical ? '420.00' : '980.00'} m³/h</div>
              <div className="scada-bar"><i style={{ width: isCritical ? '35%' : '82%', background: isCritical ? 'var(--crit)' : 'var(--cyan)' }} /></div>
            </div>
          </div>
        </div>

        <div className="scada-card">
          <h2>Contadores EDA</h2>
          <div className="scada-metrics">
            <div className="scada-metric"><span>MEDIÇÕES</span><strong>12</strong></div>
            <div className="scada-metric"><span>ALARMES</span><strong style={{ color: isCritical ? 'var(--crit)' : 'var(--ok)' }}>{isCritical ? 1 : 0}</strong></div>
            <div className="scada-metric"><span>AUDIT</span><strong style={{ color: 'var(--cyan)' }}>{events.length}</strong></div>
            <div className="scada-metric"><span>STATUS</span><strong style={{ color: isCritical ? 'var(--crit)' : isAcknowledged ? 'var(--warn)' : 'var(--ok)', fontSize: 14 }}>{coreLabel}</strong></div>
          </div>
          <h2>Linha do tempo de eventos</h2>
          <div className="scada-table-wrap">
            <table className="scada-table">
              <thead>
                <tr><th>Hora</th><th>Evento</th><th>Detalhe</th></tr>
              </thead>
              <tbody>
                {events.slice(0, 3).map((event, index) => <tr key={`${event.type}-${index}`}><td>agora</td><td className="ev">{event.type}</td><td>{event.detail}</td></tr>)}
              </tbody>
            </table>
          </div>
          <div className="scada-alarm-box">{isCritical ? '[ALTO] Temperatura acima do limiar (T-CORE-01) · ATIVO' : isAcknowledged ? '[ALTO] Temperatura acima do limiar (T-CORE-01) · RECONHECIDO' : 'Sem alarmes ativos.'}</div>
          <div className="scada-actions">
            <button type="button" className="scada-btn">Iniciar Tempo Real</button>
            <button type="button" className="scada-btn scada-btn-amber" onClick={onAcknowledge}>Validar / Reconhecer</button>
            <button type="button" className="scada-btn scada-btn-green" onClick={onResolve}>Normalizar / Encerrar</button>
          </div>
        </div>
      </div>
    </section>
  )
}
