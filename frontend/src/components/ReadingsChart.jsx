import { useRef, useState } from 'react'

const W = 640
const H = 200
const PAD_LEFT = 46
const PAD_RIGHT = 12
const PAD_TOP = 14
const PAD_BOTTOM = 24
const PLOT_W = W - PAD_LEFT - PAD_RIGHT
const PLOT_H = H - PAD_TOP - PAD_BOTTOM

function rotulo(i, n) {
  return i === n - 1 ? 'Atual' : `-${n - 1 - i}`
}

/**
 * Gráfico de tendência de 1 sensor — pedido do professor/usuário: mostrar
 * melhor as últimas leituras do que a sparkline pequena + tabela crua.
 * Mostra os valores no contexto da faixa operacional (min–max), com a zona
 * de atenção destacada, e um tooltip com crosshair ao passar o mouse.
 */
export default function ReadingsChart({ sensor }) {
  const svgRef = useRef(null)
  const [hover, setHover] = useState(null)

  const valores = sensor.historico?.length ? sensor.historico : [sensor.valor]
  const n = valores.length
  const cor = sensor.cor
  const unidade = sensor.unidade

  const domMin = Math.min(sensor.min, ...valores)
  const domMax = Math.max(sensor.max, ...valores)
  const span = domMax - domMin || 1

  const x = (i) => PAD_LEFT + (n === 1 ? PLOT_W / 2 : (i / (n - 1)) * PLOT_W)
  const y = (v) => PAD_TOP + PLOT_H - ((v - domMin) / span) * PLOT_H

  const pontos = valores.map((v, i) => [x(i), y(v)])
  const linha = pontos.map(([px, py]) => `${px},${py}`).join(' ')
  const area = `${PAD_LEFT},${PAD_TOP + PLOT_H} ${linha} ${PAD_LEFT + PLOT_W},${PAD_TOP + PLOT_H}`

  const atencaoMinY = y(Math.max(sensor.atencaoMin, domMin))
  const atencaoMaxY = y(Math.min(sensor.atencaoMax, domMax))

  const yTicks = [domMax, (domMax + domMin) / 2, domMin]

  function aoMoverPonteiro(evento) {
    const retangulo = svgRef.current.getBoundingClientRect()
    const relX = ((evento.clientX - retangulo.left) / retangulo.width) * W
    let maisProximo = 0
    let menorDist = Infinity
    for (let i = 0; i < n; i++) {
      const dist = Math.abs(x(i) - relX)
      if (dist < menorDist) {
        menorDist = dist
        maisProximo = i
      }
    }
    setHover(maisProximo)
  }

  const hoverValor = hover != null ? valores[hover] : null
  const tooltipEsquerda = hover != null && x(hover) > W / 2

  return (
    <div className="readings-chart">
      <svg
        ref={svgRef}
        viewBox={`0 0 ${W} ${H}`}
        width="100%"
        height={H}
        role="img"
        aria-label={`Tendência de ${unidade ? 'leitura' : 'valor'} — últimas ${n} amostras, atual ${sensor.valor.toFixed(2)} ${unidade}`}
        onPointerMove={aoMoverPonteiro}
        onPointerLeave={() => setHover(null)}
      >
        {/* zona de atenção (fora do intervalo confortável, dentro da faixa segura) */}
        <rect x={PAD_LEFT} y={PAD_TOP} width={PLOT_W} height={Math.max(0, atencaoMaxY - PAD_TOP)} fill="var(--warn)" opacity="0.08" />
        <rect x={PAD_LEFT} y={atencaoMinY} width={PLOT_W} height={Math.max(0, PAD_TOP + PLOT_H - atencaoMinY)} fill="var(--warn)" opacity="0.08" />

        {/* gridlines */}
        {yTicks.map((v, i) => (
          <g key={i}>
            <line x1={PAD_LEFT} x2={W - PAD_RIGHT} y1={y(v)} y2={y(v)} stroke="var(--border)" strokeWidth="1" />
            <text x={PAD_LEFT - 8} y={y(v)} dy="3" textAnchor="end" fontSize="10" fill="var(--muted)">
              {v.toFixed(v < 10 ? 1 : 0)}
            </text>
          </g>
        ))}

        {/* área sob a linha */}
        <polygon points={area} fill={cor} opacity="0.1" />

        {/* linha de tendência */}
        <polyline points={linha} fill="none" stroke={cor} strokeWidth="2" strokeLinejoin="round" strokeLinecap="round" />

        {/* pontos */}
        {pontos.map(([px, py], i) => (
          <circle
            key={i}
            cx={px}
            cy={py}
            r={i === hover ? 5 : 4}
            fill={cor}
            stroke="var(--card)"
            strokeWidth="2"
          />
        ))}

        {/* crosshair */}
        {hover != null && (
          <line x1={x(hover)} x2={x(hover)} y1={PAD_TOP} y2={PAD_TOP + PLOT_H} stroke="var(--muted)" strokeWidth="1" strokeDasharray="2 3" />
        )}

        {/* rótulo direto do valor atual */}
        <text x={x(n - 1)} y={y(valores[n - 1]) - 10} textAnchor="middle" fontSize="12" fontWeight="600" fill="var(--text)">
          {valores[n - 1].toFixed(2)}
        </text>

        {/* eixo x */}
        {valores.map((_, i) => (
          <text key={i} x={x(i)} y={H - 6} textAnchor="middle" fontSize="10" fill="var(--muted)">
            {rotulo(i, n)}
          </text>
        ))}
      </svg>

      {hover != null && (
        <div
          className="readings-chart-tip"
          style={{
            left: `${(x(hover) / W) * 100}%`,
            transform: tooltipEsquerda ? 'translate(-100%, -8px)' : 'translate(8px, -8px)',
          }}
        >
          <strong>{hoverValor.toFixed(2)} {unidade}</strong>
          <span>{rotulo(hover, n) === 'Atual' ? 'Leitura atual' : `Amostra ${rotulo(hover, n)}`}</span>
        </div>
      )}
    </div>
  )
}
