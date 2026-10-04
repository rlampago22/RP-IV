import { SENSORES_RF1, statusSensor, pctBarra, corTipo } from '../data/sensores.js'
import { useEstado } from '../state/EstadoContext.jsx'

const UNIDADE_EXIBICAO = {
  Celsius: '°C',
  bar: 'bar',
  'mSv/h': 'mSv/h',
  'm3/h': 'm³/h',
}

const CODIGO_POR_TIPO = {
  TEMPERATURA: 'T-CORE-01',
  PRESSAO: 'P-PRIM-01',
  RADIACAO: 'R-CONT-01',
  FLUXO_RESFRIAMENTO: 'F-COOL-01',
}

function Sparkline({ values, color }) {
  if (!values?.length) return null
  const w = 120
  const h = 28
  const min = Math.min(...values)
  const max = Math.max(...values)
  const span = max - min || 1
  const pts = values
    .map((v, i) => {
      const x = (i / (values.length - 1 || 1)) * w
      const y = h - ((v - min) / span) * (h - 4) - 2
      return `${x},${y}`
    })
    .join(' ')
  return (
    <svg width={w} height={h} viewBox={`0 0 ${w} ${h}`} aria-hidden="true">
      <polyline
        fill="none"
        stroke={color}
        strokeWidth="2"
        strokeLinejoin="round"
        strokeLinecap="round"
        points={pts}
      />
    </svg>
  )
}

function mapearDaApi(sensoresApi) {
  return sensoresApi.map((s) => {
    const meta = SENSORES_RF1.find((m) => m.sensorId === s.id) || SENSORES_RF1[0]
    const unidade = UNIDADE_EXIBICAO[s.unidade] ?? s.unidade
    const historico =
      Array.isArray(s.historico) && s.historico.length > 0
        ? s.historico
        : [s.valor]
    return {
      id: CODIGO_POR_TIPO[s.tipo] ?? `S-${s.id}`,
      sensorId: s.id,
      tipo: s.tipo,
      unidade,
      valor: s.valor,
      min: s.limiteMinimo,
      max: s.limiteMaximo,
      atencaoMin: meta.atencaoMin ?? s.limiteMinimo,
      atencaoMax: meta.atencaoMax ?? s.limiteMaximo,
      historico,
    }
  })
}

function celulasHistorico(historico) {
  const valores = historico ?? []
  const preenchido = [...Array(Math.max(0, 6 - valores.length)).fill(null), ...valores].slice(-6)
  return preenchido
}

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
      <p className="scada-kicker">T02 · Telemetria RF-1</p>
      <h1 className="scada-heading">Sensores</h1>
      <p className="scada-lead">
        Lista RF-1 com barra, status e histórico curto alinhados à T01 via{' '}
        <code>/api/estado</code>. Fonte:{' '}
        {origemMock ? 'mock local (API offline)' : '/api/estado'} · núcleo{' '}
        <span className={`scada-pill ${statusNucleo.cls}`}>{statusNucleo.label}</span>
      </p>

      <div className="scada-scenarios" aria-label="Seeds de medição">
        <button type="button" className="scada-scenario" onClick={aplicarCenarioNormal}>
          <strong>Seed · Normal</strong>
          <span>POST /api/cenarios/normal — 6 leituras estáveis (sparkline previsível)</span>
        </button>
        <button type="button" className="scada-scenario" onClick={aplicarCenarioObservacao}>
          <strong>Seed · Observação</strong>
          <span>POST /api/cenarios/observacao — temp 328 °C (atenção UC01 Alt. 1)</span>
        </button>
      </div>

      <div className="scada-sensors" style={{ marginTop: 16 }}>
        {sensores.map((s) => {
          const st = statusSensor(s)
          const cor = corTipo(s.tipo === 'FLUXO_RESFRIAMENTO' ? 'FLUXO' : s.tipo)
          const pct = pctBarra(s)
          return (
            <div
              className="scada-sensor"
              key={s.id}
              style={{ gridTemplateColumns: '1fr auto auto' }}
            >
              <div>
                <div className="name">
                  {s.id} · {s.tipo}
                </div>
                <div className="val" style={{ color: cor, marginTop: 4 }}>
                  {s.valor.toFixed(2)}{' '}
                  <small style={{ color: 'var(--dim)', fontWeight: 600 }}>{s.unidade}</small>
                </div>
              </div>
              <Sparkline values={s.historico} color={cor} />
              <span className={`scada-pill ${st.cls}`}>{st.label}</span>
              <div className="scada-bar">
                <i style={{ width: `${pct}%`, background: cor }} />
              </div>
            </div>
          )
        })}
      </div>

      <div className="scada-card" style={{ marginTop: 16 }}>
        <h2>Histórico recente (últimas 6 · MedicaoRegistrada)</h2>
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
          Histórico vem do campo <code>sensores[].historico</code> no snapshot (buffer do{' '}
          <code>EstadoAgregador</code>, espelhando leituras via EventBus). Seeds aplicam{' '}
          <code>ReatorFacade.receberLeitura</code> — ver <code>CenariosMedicao</code> e{' '}
          <code>docs/api-estado-contrato.md</code>.
        </div>
      </div>
    </section>
  )
}
