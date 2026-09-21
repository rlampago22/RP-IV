export default function OverviewPage({ state, onAck, onResolve }) {
  const measurements = state.events.filter((entry) => entry.tipo === 'MEDICAO_REGISTRADA').length
  const alarms = state.events.filter((entry) => entry.tipo === 'ALARME_EMITIDO').length
  const activeAlarm = state.alarms.find((alarm) => alarm.status !== 'RESOLVIDO')

  return (
    <section>
      <p className="scada-kicker">T01 · Overview</p>
      <h1 className="scada-heading">Visão Geral</h1>
      <p className="scada-lead">
        Núcleo + telemetria RF-1 + timeline EDA. Estado compartilhado entre T01–T05.
      </p>

      <div className="scada-split">
        <div className="scada-card">
          <h2>Núcleo do reator</h2>
          <div className="scada-reactor">
            <div className={`scada-core ${state.coreStatus === 'ESTÁVEL' ? '' : 'critical'}`}>{state.coreStatus}</div>
          </div>
          <div className="scada-sensors">
            {state.sensors.map((sensor) => (
              <div className="scada-sensor" key={sensor.id}>
                <div className="name">{sensor.tipo}</div>
                <div className="val" style={{ color: sensor.color }}>
                  {sensor.valor === null ? 'SEM LEITURA' : `${sensor.valor.toFixed(sensor.valor < 10 ? 2 : 0)} ${sensor.unidade}`}
                </div>
                <div className="scada-bar"><i style={{ width: `${sensor.percent}%`, background: sensor.color }} /></div>
              </div>
            ))}
          </div>
        </div>

        <div className="scada-card">
          <h2>Contadores EDA</h2>
          <div className="scada-metrics">
            <div className="scada-metric"><span>MEDIÇÕES</span><strong>{measurements}</strong></div>
            <div className="scada-metric"><span>ALARMES</span><strong style={{ color: 'var(--crit)' }}>{alarms}</strong></div>
            <div className="scada-metric"><span>AUDIT</span><strong style={{ color: 'var(--cyan)' }}>{state.events.length}</strong></div>
            <div className="scada-metric"><span>STATUS</span><strong style={{ color: state.coreStatus === 'ESTÁVEL' ? 'var(--ok)' : 'var(--crit)', fontSize: 14 }}>{state.coreStatus}</strong></div>
          </div>
          <h2>Linha do tempo de eventos</h2>
          <div className="scada-table-wrap">
            <table className="scada-table">
              <thead>
                <tr><th>Hora</th><th>Evento</th><th>Detalhe</th></tr>
              </thead>
              <tbody>
                {state.events.slice(0, 5).map((entry) => (
                  <tr key={entry.sequence}>
                    <td>{entry.timestamp.slice(11, 19)}</td>
                    <td className="ev">{entry.tipo}</td>
                    <td>{entry.detalhe}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="scada-alarm-box">
            {activeAlarm ? `[ALTO] ${activeAlarm.mensagem} · ${activeAlarm.status}` : 'Sem alarmes pendentes.'}
          </div>
          <div className="scada-actions">
            <button type="button" className="scada-btn" disabled>Tempo Real: usar T05</button>
            <button type="button" className="scada-btn scada-btn-amber" onClick={onAck}>Validar / Reconhecer</button>
            <button type="button" className="scada-btn scada-btn-green" onClick={onResolve}>Normalizar / Encerrar</button>
          </div>
        </div>
      </div>
    </section>
  )
}
