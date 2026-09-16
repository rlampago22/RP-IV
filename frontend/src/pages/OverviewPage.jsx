import { SENSORES_RF1, statusSensor, pctBarra, corTipo } from '../data/sensores.js'

export default function OverviewPage() {
  return (
    <section>
      <p className="scada-kicker">T01 · Overview</p>
      <h1 className="scada-heading">Visão Geral</h1>
      <p className="scada-lead">
        Núcleo + telemetria RF-1 + timeline EDA. KPIs alinhados ao contrato T02.
      </p>

      <div className="scada-split">
        <div className="scada-card">
          <h2>Núcleo do reator</h2>
          <div className="scada-reactor">
            <div className="scada-core">ESTÁVEL</div>
          </div>
          <div className="scada-sensors">
            {SENSORES_RF1.map((s) => {
              const st = statusSensor(s)
              const cor = corTipo(s.tipo)
              return (
                <div className="scada-sensor" key={s.id}>
                  <div className="name">{s.tipo}</div>
                  <div className="val" style={{ color: cor }}>
                    {s.valor.toFixed(2)} {s.unidade}
                  </div>
                  <div className="scada-bar">
                    <i style={{ width: `${pctBarra(s)}%`, background: cor }} />
                  </div>
                  <span className={`scada-pill ${st.cls}`} style={{ gridColumn: '1 / -1', justifySelf: 'start' }}>
                    {st.label}
                  </span>
                </div>
              )
            })}
          </div>
        </div>

        <div className="scada-card">
          <h2>Contadores EDA</h2>
          <div className="scada-metrics">
            <div className="scada-metric"><span>MEDIÇÕES</span><strong>12</strong></div>
            <div className="scada-metric"><span>ALARMES</span><strong style={{ color: 'var(--crit)' }}>1</strong></div>
            <div className="scada-metric"><span>AUDIT</span><strong style={{ color: 'var(--cyan)' }}>18</strong></div>
            <div className="scada-metric"><span>STATUS</span><strong style={{ color: 'var(--ok)', fontSize: 14 }}>OK</strong></div>
          </div>
          <h2>Linha do tempo de eventos</h2>
          <div className="scada-table-wrap">
            <table className="scada-table">
              <thead>
                <tr><th>Hora</th><th>Evento</th><th>Detalhe</th></tr>
              </thead>
              <tbody>
                <tr><td>22:45:01</td><td className="ev">MEDICAO_REGISTRADA</td><td>T=312.0 P=155.0</td></tr>
                <tr><td>22:44:58</td><td className="ev">ALARME_EMITIDO</td><td>Temp acima do limiar</td></tr>
                <tr><td>22:44:50</td><td className="ev">MEDICAO_REGISTRADA</td><td>ciclo tempo real</td></tr>
              </tbody>
            </table>
          </div>
          <div className="scada-alarm-box">[ALTO] Temperatura acima do limiar (T-CORE-01) · ATIVO</div>
          <div className="scada-actions">
            <button type="button" className="scada-btn">Iniciar Tempo Real</button>
            <button type="button" className="scada-btn scada-btn-amber">Validar / Reconhecer</button>
            <button type="button" className="scada-btn scada-btn-green">Normalizar / Encerrar</button>
          </div>
        </div>
      </div>
    </section>
  )
}
