/**
 * Gráfico completo de um sensor de um reator, atualizado por push.
 *
 * "Completo" no sentido pedido pelo professor: eixo de valores com escala, eixo de tempo, linhas
 * de referência (faixa de atenção e limite seguro), valor atual, mínimo e máximo da janela, e o
 * valor de qualquer ponto ao passar o mouse.
 *
 * Mesmas decisões de honestidade do `GraficoHistorico`:
 *  - o eixo X é **tempo real**: cada reator atualiza em intervalo variável, e plotar por índice
 *    esticaria e comprimiria o tempo em silêncio;
 *  - a escala Y é **fixa por parâmetro** (ver `data/reatores.js`), para os 5 reatores serem
 *    comparáveis lado a lado.
 */

import { useState } from 'react'
import { PARAMETROS, dominioY, faixaDoSensor, formatarHora, formatarValor } from '../data/reatores.js'

const PAD = { esq: 46, dir: 10, topo: 8, base: 20 }

/**
 * `largura` e `altura` são o sistema de coordenadas do SVG, não pixels: o SVG escala para a
 * largura do bloco. Um bloco estreito usa largura menor para o texto dos eixos continuar legível.
 */
export default function GraficoAoVivo({ sensor, tipo, largura = 480, altura = 150 }) {
  const [foco, setFoco] = useState(null)
  const meta = PARAMETROS[tipo]
  const serie = sensor?.serie ?? []

  if (!sensor || !meta) {
    return <div className="mon-grafico mon-vazio">Sem leitura de {meta?.rotulo ?? tipo}.</div>
  }

  const L = largura
  const H = altura
  const larguraUtil = L - PAD.esq - PAD.dir
  const alturaUtil = H - PAD.topo - PAD.base
  const [yMin, yMax] = dominioY(tipo, serie)
  const t0 = serie.length ? serie[0][0] : 0
  const t1 = serie.length ? serie[serie.length - 1][0] : 0
  const dt = t1 - t0 || 1

  const x = (t) => PAD.esq + (serie.length > 1 ? ((t - t0) / dt) * larguraUtil : larguraUtil)
  const y = (v) => PAD.topo + (1 - (v - yMin) / (yMax - yMin)) * alturaUtil

  const pontos = serie.map(([t, v]) => `${x(t).toFixed(1)},${y(v).toFixed(1)}`).join(' ')
  const area = serie.length > 1
    ? `${PAD.esq},${PAD.topo + alturaUtil} ${pontos} ${x(t1).toFixed(1)},${PAD.topo + alturaUtil}`
    : ''

  const marcasY = [0, 1, 2, 3, 4].map((i) => yMin + ((yMax - yMin) * i) / 4)
  const margemRef = (yMax - yMin) * 0.02
  const dentro = (v) => v != null && v > yMin + margemRef && v < yMax - margemRef
  const referencias = [
    { valor: meta.atencao?.max, classe: 'warn', rotulo: 'atenção' },
    { valor: sensor.limiteMaximo, classe: 'crit', rotulo: 'limite' },
    { valor: meta.atencao?.min, classe: 'warn', rotulo: 'atenção' },
    { valor: sensor.limiteMinimo, classe: 'crit', rotulo: 'limite' },
  ].filter((r) => dentro(r.valor))

  const valores = serie.map(([, v]) => v)
  const minimo = valores.length ? Math.min(...valores) : sensor.valor
  const maximo = valores.length ? Math.max(...valores) : sensor.valor
  const faixa = faixaDoSensor(sensor)

  function aoMover(evento) {
    if (serie.length < 2) return
    const caixa = evento.currentTarget.getBoundingClientRect()
    const xSvg = ((evento.clientX - caixa.left) / caixa.width) * L
    const tAlvo = t0 + ((xSvg - PAD.esq) / larguraUtil) * dt
    let melhor = 0
    for (let i = 1; i < serie.length; i += 1) {
      if (Math.abs(serie[i][0] - tAlvo) < Math.abs(serie[melhor][0] - tAlvo)) melhor = i
    }
    setFoco(melhor)
  }

  const pontoFoco = foco != null && serie[foco] ? serie[foco] : null

  return (
    <div className="mon-grafico">
      <div className="mon-grafico-topo">
        <span className="mon-grafico-rotulo">{meta.rotulo}</span>
        <span className={`mon-valor ${faixa}`}>
          {formatarValor(sensor.valor, tipo)}
          <small>{meta.unidade}</small>
        </span>
      </div>

      <svg
        viewBox={`0 0 ${L} ${H}`}
        className="mon-svg"
        role="img"
        aria-label={`${meta.rotulo}: ${formatarValor(sensor.valor, tipo)} ${meta.unidade}, ${serie.length} leituras`}
        onMouseMove={aoMover}
        onMouseLeave={() => setFoco(null)}
      >
        {marcasY.map((v) => (
          <g key={v}>
            <line x1={PAD.esq} x2={L - PAD.dir} y1={y(v)} y2={y(v)} className="mon-grade-linha" />
            <text x={PAD.esq - 6} y={y(v) + 3} textAnchor="end" className="mon-eixo">
              {formatarValor(v, tipo)}
            </text>
          </g>
        ))}

        {referencias.map((r) => (
          <g key={`${r.classe}-${r.valor}`}>
            <line
              x1={PAD.esq}
              x2={L - PAD.dir}
              y1={y(r.valor)}
              y2={y(r.valor)}
              className={`mon-ref ${r.classe}`}
            />
            <text x={L - PAD.dir - 2} y={y(r.valor) - 3} textAnchor="end" className={`mon-ref-texto ${r.classe}`}>
              {r.rotulo} {formatarValor(r.valor, tipo)}
            </text>
          </g>
        ))}

        {area && <polygon points={area} style={{ fill: meta.cor }} className="mon-area" />}
        {serie.length > 1 && (
          <polyline points={pontos} fill="none" style={{ stroke: meta.cor }} className="mon-linha" />
        )}
        {serie.length > 0 && (
          <circle cx={x(t1)} cy={y(serie[serie.length - 1][1])} r="3.5" style={{ fill: meta.cor }} />
        )}

        {pontoFoco && (
          <g>
            <line
              x1={x(pontoFoco[0])}
              x2={x(pontoFoco[0])}
              y1={PAD.topo}
              y2={PAD.topo + alturaUtil}
              className="mon-cursor"
            />
            <circle cx={x(pontoFoco[0])} cy={y(pontoFoco[1])} r="4" className="mon-cursor-ponto" />
            <text
              x={Math.min(Math.max(x(pontoFoco[0]), PAD.esq + 60), L - PAD.dir - 60)}
              y={PAD.topo + 12}
              textAnchor="middle"
              className="mon-cursor-texto"
            >
              {formatarValor(pontoFoco[1], tipo)} {meta.unidade} · {formatarHora(pontoFoco[0])}
            </text>
          </g>
        )}

        {serie.length > 0 && (
          <>
            <text x={PAD.esq} y={H - 5} className="mon-eixo">{formatarHora(t0)}</text>
            <text x={L - PAD.dir} y={H - 5} textAnchor="end" className="mon-eixo">{formatarHora(t1)}</text>
          </>
        )}
      </svg>

      <div className="mon-grafico-rodape">
        mín {formatarValor(minimo, tipo)} · máx {formatarValor(maximo, tipo)} · {serie.length} leitura(s)
      </div>
    </div>
  )
}

/** Versão compacta para os parâmetros secundários: valor atual e tendência, sem eixos. */
export function TendenciaCompacta({ sensor, tipo }) {
  const meta = PARAMETROS[tipo]
  if (!sensor || !meta) return null
  const valores = (sensor.serie ?? []).slice(-20).map(([, v]) => v)
  const w = 84
  const h = 22
  const min = Math.min(...valores)
  const max = Math.max(...valores)
  const span = max - min || 1
  const pontos = valores
    .map((v, i) => `${((i / (valores.length - 1 || 1)) * w).toFixed(1)},${(h - 2 - ((v - min) / span) * (h - 4)).toFixed(1)}`)
    .join(' ')

  return (
    <div className="mon-compacto">
      <span className="mon-compacto-rotulo">{meta.rotulo}</span>
      {valores.length > 1 && (
        <svg width={w} height={h} viewBox={`0 0 ${w} ${h}`} aria-hidden="true">
          <polyline points={pontos} fill="none" style={{ stroke: meta.cor }} strokeWidth="1.5" />
        </svg>
      )}
      <span className={`mon-compacto-valor ${faixaDoSensor(sensor)}`}>
        {formatarValor(sensor.valor, tipo)} <small>{meta.unidade}</small>
      </span>
    </div>
  )
}
