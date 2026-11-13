import { SENSORES_RF1 } from '../data/sensores.js'
import { useEstado } from '../state/EstadoContext.jsx'
import { IconClock } from '../components/icons.jsx'
import SensorCard, { mapearDaApi, celulasHistorico } from '../components/SensorCard.jsx'
import SensorSubNav from '../components/SensorSubNav.jsx'

export default function SensoresPage() {
  const { estado, origemMock, aplicarCenarioNormal, aplicarCenarioObservacao } = useEstado()
  const sensores =
    estado.sensores?.length > 0 ? mapearDaApi(estado.sensores) : SENSORES_RF1

  const statusNucleo =
    estado.status === 'CRITICO'
      ? { label: 'CRÍTICO', cls: 'crit' }
      : estado.status === 'ATENCAO'
        ? { label: 'ATENÇÃO', cls: 'warn' }
        : { label: 'ESTÁVEL', cls: 'ok' }

  return (
    <section>
      <p className="scada-kicker">TELEMETRIA INSTRUMENTAL • MALHA PRIMÁRIA</p>
      <h1 className="scada-heading">Sensores Operacionais</h1>
      <p className="scada-lead">
        Monitoramento contínuo de grandezas físicas com limites operacionais, margens de segurança e
        tendência recente. Fonte: {origemMock ? 'simulado local (offline)' : 'telemetria ao vivo'} ·
        núcleo <span className={`scada-pill ${statusNucleo.cls}`}>{statusNucleo.label}</span>
      </p>

      <SensorSubNav />

      <div className="scada-scenarios" aria-label="Condições de medição">
        <button type="button" className="scada-scenario" onClick={aplicarCenarioNormal}>
          <strong>Condição · Normal</strong>
          <span>6 leituras estáveis — tendência previsível no histórico</span>
        </button>
        <button type="button" className="scada-scenario" onClick={aplicarCenarioObservacao}>
          <strong>Condição · Observação</strong>
          <span>Temperatura 328 °C — faixa de atenção preventiva</span>
        </button>
      </div>

      <div style={{ display: 'grid', gap: 10, marginTop: 16 }}>
        {sensores.map((s) => (
          <SensorCard sensor={s} key={s.id} />
        ))}
      </div>

      <div className="scada-card" style={{ marginTop: 16 }}>
        <h2 className="panel-heading">
          <IconClock size={13} />
          Histórico recente (últimas 6 · MedicaoRegistrada)
        </h2>
        <div className="scada-table-wrap" style={{ maxHeight: 'none' }}>
          <table className="scada-table">
            <thead>
              <tr>
                <th>Sensor</th>
                <th>Amostra -5</th>
                <th>-4</th>
                <th>-3</th>
                <th>-2</th>
                <th>-1</th>
                <th>Atual</th>
              </tr>
            </thead>
            <tbody>
              {sensores.map((s) => (
                <tr key={`h-${s.id}`}>
                  <td className="ev">{s.id}</td>
                  {celulasHistorico(s.historico).map((v, i) => (
                    <td key={i}>{v == null ? '—' : Number(v).toFixed(2)}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="scada-note">
          {origemMock ? (
            <>Modo offline: histórico do mock local (mesmos valores dos seeds). Suba a API para telemetria real.</>
          ) : (
            <>
              Online: últimas 6 leituras de <code>sensores[].historico</code> no snapshot (
              <code>MedicaoRegistrada</code> → EventBus). Condições disparam{' '}
              <code>ReatorFacade.receberLeitura</code> via <code>/api/cenarios/*</code>.
            </>
          )}
        </div>
      </div>
    </section>
  )
}
