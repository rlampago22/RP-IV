import { CODIGO_POR_TIPO, SENSORES_RF1, statusSensor, pctBarra, corTipo } from '../data/sensores.js'
import {
  IconThermometer,
  IconGauge,
  IconRadiation,
  IconDroplet,
  IconActivity,
} from './icons.jsx'

export const ICONE_TIPO = {
  TEMPERATURA: IconThermometer,
  PRESSAO: IconGauge,
  RADIACAO: IconRadiation,
  FLUXO: IconDroplet,
  FLUXO_RESFRIAMENTO: IconDroplet,
}

export const NOME_TIPO = {
  TEMPERATURA: 'Temperatura',
  PRESSAO: 'Pressão',
  RADIACAO: 'Radiação',
  FLUXO: 'Fluxo de resfriamento',
  FLUXO_RESFRIAMENTO: 'Fluxo de resfriamento',
}

export const STATUS_FRASE = {
  crit: 'fora da faixa segura',
  warn: 'em faixa de atenção',
  ok: 'dentro da faixa segura',
}

const UNIDADE_EXIBICAO = {
  Celsius: '°C',
  bar: 'bar',
  'mSv/h': 'mSv/h',
  'm3/h': 'm³/h',
}

/**
 * Uma aba por sensor (pedido do professor, 13/11): cada entrada vira uma rota
 * `/sensores/:slug` — ver `SensorDetailPage.jsx` e `main.jsx`.
 */
export const TIPOS_SENSOR = [
  { tipo: 'TEMPERATURA', slug: 'temperatura' },
  { tipo: 'PRESSAO', slug: 'pressao' },
  { tipo: 'RADIACAO', slug: 'radiacao' },
  { tipo: 'FLUXO_RESFRIAMENTO', slug: 'fluxo-resfriamento' },
].map((item) => ({ ...item, nome: NOME_TIPO[item.tipo] }))

export function mapearDaApi(sensoresApi) {
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

export function celulasHistorico(historico) {
  const valores = historico ?? []
  const preenchido = [...Array(Math.max(0, 6 - valores.length)).fill(null), ...valores].slice(-6)
  return preenchido
}

/** Sensores de um reator do painel (ver `data/reatores.js`), no formato desta tela —
 * histórico curto = últimas 6 amostras da série `[epochMillis, valor]` do reator. */
export function sensoresDoReator(reator) {
  return mapearDaApi(
    (reator?.sensores ?? []).map((s) => ({ ...s, historico: (s.serie ?? []).slice(-6).map(([, v]) => v) })),
  )
}

export function Sparkline({ values, color }) {
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

/** Card de 1 sensor — bloco extraído de SensoresPage (T02), reaproveitado na visão
 * geral e em cada aba individual (`SensorDetailPage`). */
export default function SensorCard({ sensor: s }) {
  const st = statusSensor(s)
  const cor = corTipo(s.tipo === 'FLUXO_RESFRIAMENTO' ? 'FLUXO' : s.tipo)
  const pct = pctBarra(s)
  const Icone = ICONE_TIPO[s.tipo] ?? IconActivity
  const nome = NOME_TIPO[s.tipo] ?? s.tipo

  return (
    <div className="sensor-row">
      <div className="ring ring-sm" style={{ background: `conic-gradient(${cor} ${pct}%, var(--border) 0)` }}>
        <Icone size={18} className="ring-icon" style={{ color: 'var(--text)' }} />
      </div>

      <div>
        <div className="sensor-row-top">
          <span className="sensor-row-name">
            {nome}
            <span className="code">{s.id}</span>
          </span>
          <span className={`scada-pill ${st.cls}`}>{st.label}</span>
        </div>
        <div className="sensor-row-value">
          <strong style={{ color: cor }}>{s.valor.toFixed(2)}</strong>
          <span className="unit">{s.unidade}</span>
        </div>
        <div className="scada-bar" style={{ marginTop: 10 }}>
          <i style={{ width: `${pct}%`, background: cor }} />
        </div>
        <div className="sensor-row-caption">
          Leitura {STATUS_FRASE[st.cls] ?? ''} · faixa segura {s.min}–{s.max} {s.unidade}
        </div>
      </div>

      <div className="sensor-row-trend">
        <Sparkline values={s.historico} color={cor} />
        <span>últimas leituras</span>
      </div>
    </div>
  )
}
