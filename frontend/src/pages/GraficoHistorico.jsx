/**
 * Quatro curvas empilhadas do histórico — uma por parâmetro RF-1, cada uma com escala própria.
 *
 * Escala compartilhada não serve aqui: temperatura (~310), pressão (~155), radiação (~2,4) e fluxo
 * (~1100) estão em ordens de grandeza diferentes, e num eixo único a radiação viraria uma reta
 * colada no zero.
 *
 * O eixo X é compartilhado, então o crosshair é único: passar o mouse em qualquer gráfico mostra o
 * valor dos quatro parâmetros no mesmo instante.
 *
 * Decisões de honestidade, porque gráfico engana fácil:
 *  - o eixo X é **tempo real**, não índice do registro. Os registros não são equidistantes (há
 *    linhas de ciclo a cada ~3 s e linhas de transição a milissegundos de distância); plotar por
 *    índice esticaria e comprimiria o tempo em silêncio;
 *  - o eixo Y é ajustado **aos dados**, não à faixa total do sensor. Isso amplia variações
 *    pequenas, então os valores mínimo e máximo reais ficam visíveis ao lado de cada curva;
 *  - acima de {@link LIMITE_PONTOS} registros os pontos são amostrados, e o gráfico diz isso;
 *  - se a consulta foi truncada, o gráfico avisa que mostra apenas o recorte retornado.
 */

import { useState } from 'react'
import { SENSORES_RF1 } from '../data/sensores.js'

const LIMITE_PONTOS = 400

const LARGURA = 1000
const ALTURA = 110
const PAD_Y = 10

const PARAMETROS = [
  { campo: 'temperatura', rotulo: 'Temperatura', unidade: '°C', tipo: 'TEMPERATURA', cor: 'var(--crit)', hex: '#ef4444' },
  { campo: 'pressao', rotulo: 'Pressão', unidade: 'bar', tipo: 'PRESSAO', cor: 'var(--warn)', hex: '#f59e0b' },
  { campo: 'radiacao', rotulo: 'Radiação', unidade: 'mSv/h', tipo: 'RADIACAO', cor: 'var(--purple)', hex: '#a855f7' },
  { campo: 'fluxoResfriamento', rotulo: 'Fluxo resfriamento', unidade: 'm³/h', tipo: 'FLUXO_RESFRIAMENTO', cor: 'var(--cyan)', hex: '#06b6d4' },
]

const META_POR_TIPO = Object.fromEntries(SENSORES_RF1.map((s) => [s.tipo, s]))

/** Amostragem uniforme preservando o primeiro e o último ponto. */
function amostrar(registros) {
  if (registros.length <= LIMITE_PONTOS) return registros
  const passo = (registros.length - 1) / (LIMITE_PONTOS - 1)
  return Array.from({ length: LIMITE_PONTOS }, (_, i) => registros[Math.round(i * passo)])
}

/**
 * Monta um SVG autônomo com as quatro curvas, para download.
 *
 * Construído a partir dos dados, não copiado do DOM: os gráficos da tela usam `var(--cor)` do
 * tema, e um arquivo solto não tem acesso a essas variáveis — as cores sairiam preto. Aqui vão
 * valores literais, fundo branco e texto escuro, para o arquivo servir para impressão.
 */
function construirSvgAutonomo({ registros, xs, inicio, fim, truncado, total, retornados }) {
  const L = 900
  const H_PAINEL = 120
  const MARGEM = 46
  const TOPO = 64
  const altura = TOPO + PARAMETROS.length * (H_PAINEL + 26) + 46
  const largura = L + MARGEM * 2

  const escalaX = (i) => MARGEM + (xs[i] / LARGURA) * L
  const partes = []

  partes.push(
    `<rect width="${largura}" height="${altura}" fill="#ffffff"/>`,
    `<text x="${MARGEM}" y="26" font-family="sans-serif" font-size="15" font-weight="700" fill="#111111">`
      + `Histórico operacional — curvas dos parâmetros RF-1</text>`,
    `<text x="${MARGEM}" y="46" font-family="sans-serif" font-size="11" fill="#555555">`
      + `${inicio} até ${fim} · ${retornados} registro(s)`
      + (truncado ? ` · recorte limitado de ${total}` : '')
      + ` · escala vertical ajustada por parâmetro</text>`,
  )

  PARAMETROS.forEach((parametro, indice) => {
    const base = TOPO + indice * (H_PAINEL + 26)
    const valores = registros.map((r) => r[parametro.campo])
    const minimo = Math.min(...valores)
    const maximo = Math.max(...valores)
    const amplitude = maximo - minimo
    const paraY = (valor) =>
      amplitude <= 0
        ? base + H_PAINEL / 2
        : base + H_PAINEL - ((valor - minimo) / amplitude) * H_PAINEL

    partes.push(
      `<text x="${MARGEM}" y="${base - 8}" font-family="sans-serif" font-size="12" font-weight="700" fill="#111111">`
        + `${parametro.rotulo} (${parametro.unidade})</text>`,
      `<text x="${MARGEM + L}" y="${base - 8}" text-anchor="end" font-family="monospace" font-size="11" fill="#555555">`
        + `${minimo.toFixed(2)} — ${maximo.toFixed(2)}</text>`,
      `<rect x="${MARGEM}" y="${base}" width="${L}" height="${H_PAINEL}" fill="none" stroke="#cccccc"/>`,
    )

    registros.forEach((r, i) => {
      if (r.sensoresEmAlarme?.length || r.sensoresEmFalha?.length) {
        const cor = r.sensoresEmAlarme?.length ? '#ef4444' : '#a855f7'
        const x = escalaX(i).toFixed(1)
        partes.push(
          `<line x1="${x}" x2="${x}" y1="${base}" y2="${base + H_PAINEL}" stroke="${cor}" stroke-width="2" opacity="0.3"/>`,
        )
      }
    })

    const meta = META_POR_TIPO[parametro.tipo]
    if (meta) {
      ;[
        { valor: meta.atencaoMax, texto: `atenção ${meta.atencaoMax}`, cor: '#b45309' },
        { valor: meta.max, texto: `limite ${meta.max}`, cor: '#b91c1c' },
        { valor: meta.atencaoMin, texto: `atenção ${meta.atencaoMin}`, cor: '#b45309' },
        { valor: meta.min, texto: `limite ${meta.min}`, cor: '#b91c1c' },
      ]
        .filter((r) => r.valor > minimo && r.valor < maximo)
        .forEach((r) => {
          const y = paraY(r.valor).toFixed(1)
          partes.push(
            `<line x1="${MARGEM}" x2="${MARGEM + L}" y1="${y}" y2="${y}" stroke="${r.cor}" stroke-width="1" stroke-dasharray="6 5"/>`,
            `<text x="${MARGEM + 6}" y="${Number(y) - 4}" font-family="sans-serif" font-size="10" fill="${r.cor}">${r.texto}</text>`,
          )
        })
    }

    const pontos = registros
      .map((r, i) => `${escalaX(i).toFixed(1)},${paraY(r[parametro.campo]).toFixed(1)}`)
      .join(' ')
    partes.push(
      `<polyline points="${pontos}" fill="none" stroke="${parametro.hex}" stroke-width="1.6"/>`,
    )
  })

  partes.push(
    `<text x="${MARGEM}" y="${altura - 18}" font-family="sans-serif" font-size="10" fill="#555555">`
      + `Marcas verticais: vermelho = alarme ativo, roxo = falha de sensor. `
      + `Eixo horizontal em tempo real. Central de Supervisão — Reator-01</text>`,
  )

  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${largura} ${altura}" `
    + `width="${largura}" height="${altura}">${partes.join('')}</svg>`
}

function baixarSvg(conteudo, nome) {
  const blob = new Blob([conteudo], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const ancora = document.createElement('a')
  ancora.href = url
  ancora.download = nome
  ancora.click()
  URL.revokeObjectURL(url)
}

function Curva({ parametro, registros, xs, indiceAtivo, onMover, onSair }) {
  const valores = registros.map((r) => r[parametro.campo])
  const minimo = Math.min(...valores)
  const maximo = Math.max(...valores)
  const amplitude = maximo - minimo

  const paraY = (valor) => {
    if (amplitude <= 0) return ALTURA / 2
    return ALTURA - PAD_Y - ((valor - minimo) / amplitude) * (ALTURA - 2 * PAD_Y)
  }

  const pontos = registros
    .map((r, i) => `${xs[i].toFixed(1)},${paraY(r[parametro.campo]).toFixed(1)}`)
    .join(' ')

  const meta = META_POR_TIPO[parametro.tipo]
  // Linhas de referência só aparecem se caírem dentro da janela visível — desenhar fora do
  // alcance daria a impressão de que o valor está perto de um limite que nem está no gráfico.
  const referencias = meta
    ? [
        { valor: meta.atencaoMax, texto: `atenção ${meta.atencaoMax}`, cor: 'var(--warn)' },
        { valor: meta.max, texto: `limite ${meta.max}`, cor: 'var(--crit)' },
        { valor: meta.atencaoMin, texto: `atenção ${meta.atencaoMin}`, cor: 'var(--warn)' },
        { valor: meta.min, texto: `limite ${meta.min}`, cor: 'var(--crit)' },
      ].filter((r) => r.valor > minimo && r.valor < maximo)
    : []

  const marcadores = registros.flatMap((r, i) => {
    if (r.sensoresEmAlarme?.length) return [{ x: xs[i], cor: 'var(--crit)' }]
    if (r.sensoresEmFalha?.length) return [{ x: xs[i], cor: 'var(--purple)' }]
    return []
  })

  const ativo = indiceAtivo != null ? registros[indiceAtivo] : null
  const valorAtivo = ativo ? ativo[parametro.campo] : null
  const xAtivo = indiceAtivo != null ? xs[indiceAtivo] : null
  const yAtivo = valorAtivo != null ? paraY(valorAtivo) : null

  return (
    <div className="scada-grafico">
      <div className="scada-grafico-topo">
        <span className="scada-faixa-rotulo">
          {parametro.rotulo} <small>{parametro.unidade}</small>
        </span>
        {valorAtivo != null ? (
          <span className="scada-grafico-valor" style={{ color: parametro.cor }}>
            {valorAtivo.toFixed(2)} {parametro.unidade}
          </span>
        ) : (
          <span className="scada-grafico-escala">
            {minimo.toFixed(2)} — {maximo.toFixed(2)}
          </span>
        )}
      </div>

      <svg
        viewBox={`0 0 ${LARGURA} ${ALTURA}`}
        preserveAspectRatio="none"
        className="scada-grafico-svg"
        role="img"
        aria-label={`${parametro.rotulo}: ${minimo.toFixed(2)} a ${maximo.toFixed(2)} ${parametro.unidade}`}
        onMouseMove={onMover}
        onMouseLeave={onSair}
      >
        {marcadores.map((m, i) => (
          <line
            key={`m-${i}`}
            x1={m.x}
            x2={m.x}
            y1={0}
            y2={ALTURA}
            stroke={m.cor}
            strokeWidth="2"
            opacity="0.35"
          />
        ))}

        {referencias.map((r) => (
          <g key={r.texto}>
            <line
              x1={0}
              x2={LARGURA}
              y1={paraY(r.valor)}
              y2={paraY(r.valor)}
              stroke={r.cor}
              strokeWidth="1"
              strokeDasharray="6 5"
              opacity="0.6"
            />
            <text x={6} y={paraY(r.valor) - 4} fill={r.cor} fontSize="11" opacity="0.8">
              {r.texto}
            </text>
          </g>
        ))}

        <polyline
          points={pontos}
          fill="none"
          stroke={parametro.cor}
          strokeWidth="2"
          vectorEffect="non-scaling-stroke"
        />

        {xAtivo != null && (
          <>
            <line
              x1={xAtivo}
              x2={xAtivo}
              y1={0}
              y2={ALTURA}
              stroke="var(--text)"
              strokeWidth="1"
              opacity="0.45"
              vectorEffect="non-scaling-stroke"
            />
            {/* Marcador em cruz: linhas não distorcem com preserveAspectRatio="none", círculos sim. */}
            <line
              x1={xAtivo - 10}
              x2={xAtivo + 10}
              y1={yAtivo}
              y2={yAtivo}
              stroke={parametro.cor}
              strokeWidth="3"
              vectorEffect="non-scaling-stroke"
            />
          </>
        )}
      </svg>
    </div>
  )
}

export default function GraficoHistorico({ resultado, imprimirComRelatorio = false }) {
  const [indiceAtivo, setIndiceAtivo] = useState(null)

  const registros = [...(resultado.registros ?? [])].sort(
    (a, b) => new Date(a.instante) - new Date(b.instante),
  )

  if (registros.length < 2) {
    return (
      <div className="scada-card" style={{ marginTop: 14 }}>
        <h2>Curvas dos parâmetros</h2>
        <p className="scada-lead">
          São necessários pelo menos dois registros para desenhar uma curva.
        </p>
      </div>
    )
  }

  const plotados = amostrar(registros)
  const tempos = plotados.map((r) => new Date(r.instante).getTime())
  const inicio = tempos[0]
  const fim = tempos[tempos.length - 1]
  const intervalo = fim - inicio
  const xs = tempos.map((t) => (intervalo <= 0 ? LARGURA / 2 : ((t - inicio) / intervalo) * LARGURA))

  /** Converte a posição do mouse para coordenada do viewBox e acha o ponto mais próximo. */
  function mover(evento) {
    const caixa = evento.currentTarget.getBoundingClientRect()
    if (caixa.width === 0) return
    const alvo = ((evento.clientX - caixa.left) / caixa.width) * LARGURA
    let maisProximo = 0
    let menorDistancia = Infinity
    for (let i = 0; i < xs.length; i += 1) {
      const distancia = Math.abs(xs[i] - alvo)
      if (distancia < menorDistancia) {
        menorDistancia = distancia
        maisProximo = i
      }
    }
    setIndiceAtivo(maisProximo)
  }

  const ativo = indiceAtivo != null ? plotados[indiceAtivo] : null
  const rotuloInicio = new Date(inicio).toLocaleString('pt-BR', { hour12: false })
  const rotuloFim = new Date(fim).toLocaleString('pt-BR', { hour12: false })

  function exportarSvg() {
    const svg = construirSvgAutonomo({
      registros: plotados,
      xs,
      inicio: rotuloInicio,
      fim: rotuloFim,
      truncado: resultado.truncado,
      total: resultado.total,
      retornados: plotados.length,
    })
    baixarSvg(svg, `curvas-historico-${new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-')}.svg`)
  }

  return (
    <div
      className={`scada-card${imprimirComRelatorio ? '' : ' scada-nao-imprimir'}`}
      style={{ marginTop: 14 }}
    >
      <div className="scada-grafico-cabecalho">
        <h2 style={{ margin: 0 }}>Curvas dos parâmetros</h2>
        {ativo && (
          <span className="scada-grafico-instante">
            {new Date(ativo.instante).toLocaleString('pt-BR', { hour12: false })} ·{' '}
            <strong>{ativo.status}</strong>
          </span>
        )}
      </div>

      <p className="scada-note">
        {rotuloInicio} → {rotuloFim}. Passe o mouse sobre qualquer curva para ver os quatro valores
        no mesmo instante. Eixo horizontal em tempo real, então intervalos sem registro aparecem
        como trechos esticados. Cada curva tem escala própria, ajustada aos seus dados.
        {plotados.length < registros.length && (
          <>
            {' '}
            Mostrando {plotados.length} de {registros.length} registros (amostragem uniforme para o
            desenho não ficar pesado).
          </>
        )}
        {resultado.truncado && (
          <>
            {' '}
            <strong>
              A consulta foi limitada a {resultado.retornados} de {resultado.total} registros — o
              gráfico cobre apenas esse recorte.
            </strong>
          </>
        )}
      </p>

      {PARAMETROS.map((parametro) => (
        <Curva
          key={parametro.campo}
          parametro={parametro}
          registros={plotados}
          xs={xs}
          indiceAtivo={indiceAtivo}
          onMover={mover}
          onSair={() => setIndiceAtivo(null)}
        />
      ))}

      <div className="scada-actions scada-nao-imprimir">
        <button type="button" className="scada-btn" onClick={exportarSvg}>
          Baixar gráfico (SVG)
        </button>
      </div>

      <p className="scada-event-note">
        Marcas verticais: <span style={{ color: 'var(--crit)' }}>vermelho</span> onde havia alarme
        ativo, <span style={{ color: 'var(--purple)' }}>roxo</span> onde houve falha de sensor.
        Linhas tracejadas são os limites do sensor, desenhadas só quando caem dentro da escala
        visível. O SVG baixado sai em tema claro, pronto para imprimir.
      </p>
    </div>
  )
}
