import { useParams, Navigate } from 'react-router-dom'
import { SENSORES_RF1, corTipo } from '../../data/sensores.js'
import { useEstado } from '../../state/EstadoContext.jsx'
import { IconClock } from '../../components/icons.jsx'
import SensorCard, { TIPOS_SENSOR, mapearDaApi, celulasHistorico } from '../../components/SensorCard.jsx'
import SensorSubNav from '../../components/SensorSubNav.jsx'
import ReadingsChart from '../../components/ReadingsChart.jsx'

/**
 * Uma aba por sensor (pedido do professor, 13/11): rota genérica `/sensores/:slug`
 * parametrizada pelo tipo, em vez de 4 páginas quase idênticas.
 */
export default function SensorDetailPage() {
  const { slug } = useParams()
  const { estado, origemMock } = useEstado()
  const info = TIPOS_SENSOR.find((s) => s.slug === slug)

  if (!info) {
    return <Navigate to="/sensores" replace />
  }

  const sensores =
    estado.sensores?.length > 0 ? mapearDaApi(estado.sensores) : SENSORES_RF1
  const sensor = sensores.find((s) => s.tipo === info.tipo)

  if (!sensor) {
    return (
      <section>
        <p className="scada-kicker">TELEMETRIA INSTRUMENTAL</p>
        <h1 className="scada-heading">{info.nome}</h1>
        <SensorSubNav />
        <div className="scada-card" style={{ marginTop: 16 }}>
          Sensor ainda não reportou leitura nesta sessão.
        </div>
      </section>
    )
  }

  const cor = corTipo(sensor.tipo === 'FLUXO_RESFRIAMENTO' ? 'FLUXO' : sensor.tipo)
  const historico = celulasHistorico(sensor.historico)

  return (
    <section>
      <p className="scada-kicker">TELEMETRIA INSTRUMENTAL • SENSOR {sensor.id}</p>
      <h1 className="scada-heading">{info.nome}</h1>
      <p className="scada-lead">
        Fonte: {origemMock ? 'simulado local (offline)' : 'telemetria ao vivo'} · faixa segura{' '}
        {sensor.min}–{sensor.max} {sensor.unidade}
      </p>

      <SensorSubNav />

      <div style={{ display: 'grid', gap: 10, marginTop: 16 }}>
        <SensorCard sensor={sensor} />
      </div>

      <div className="scada-card" style={{ marginTop: 16 }}>
        <h2 className="panel-heading">
          <IconClock size={13} />
          Tendência — últimas {sensor.historico?.length || 1} leituras
        </h2>
        <ReadingsChart sensor={{ ...sensor, cor }} />
        <div className="scada-note">
          Zona sombreada = faixa de atenção ({sensor.atencaoMax}–{sensor.max} /{' '}
          {sensor.min}–{sensor.atencaoMin} {sensor.unidade}). Passe o mouse sobre o
          gráfico para ver cada amostra.
        </div>
        <div className="scada-table-wrap" style={{ maxHeight: 'none', marginTop: 10 }}>
          <table className="scada-table">
            <thead>
              <tr>
                <th>Amostra -5</th>
                <th>-4</th>
                <th>-3</th>
                <th>-2</th>
                <th>-1</th>
                <th>Atual</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                {historico.map((v, i) => (
                  <td key={i}>{v == null ? '—' : Number(v).toFixed(2)}</td>
                ))}
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>
  )
}
