import { CODIGO_POR_TIPO, SENSORES_RF1, statusSensor, pctBarra, corTipo } from '../data/sensores.js'
import { useEstado } from '../state/EstadoContext.jsx'
import SeletorReator, { AvisoOutrosReatores } from '../components/SeletorReator.jsx'
import { TODOS, useReatorSelecionado } from '../state/reatorSelecionado.js'
import {
  NIVEL,
  PARAMETROS,
  TODOS_PARAMETROS,
  faixaDoSensor,
  formatarHora,
  formatarValor,
  nomeReator,
  sensorDoTipo,
} from '../data/reatores.js'
import {
  IconThermometer,
  IconGauge,
  IconRadiation,
  IconDroplet,
  IconActivity,
  IconClock,
} from '../components/icons.jsx'

const ICONE_TIPO = {
  TEMPERATURA: IconThermometer,
  PRESSAO: IconGauge,
  RADIACAO: IconRadiation,
  FLUXO: IconDroplet,
  FLUXO_RESFRIAMENTO: IconDroplet,
}

const NOME_TIPO = {
  TEMPERATURA: 'Temperatura',
  PRESSAO: 'Pressão',
  RADIACAO: 'Radiação',
  FLUXO: 'Fluxo de resfriamento',
  FLUXO_RESFRIAMENTO: 'Fluxo de resfriamento',
}

const STATUS_FRASE = {
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

/** Sensores de um reator do painel no formato desta tela (histórico curto = últimas 6 da série). */
function sensoresDoReator(reator) {
  return mapearDaApi(
    (reator?.sensores ?? []).map((s) => ({ ...s, historico: (s.serie ?? []).slice(-6).map(([, v]) => v) })),
  )
}

/** "Todos": reatores nas linhas, os 4 parâmetros nas colunas, cada valor colorido pela faixa. */
function MatrizReatores({ reatores, onSelecionar }) {
  return (
    <div className="scada-card" style={{ marginTop: 16 }}>
      <h2 className="panel-heading">
        <IconActivity size={13} />
        Matriz de telemetria · {reatores.length} reatores × 4 parâmetros
      </h2>
      <div className="scada-table-wrap" style={{ maxHeight: 'none' }}>
        <table className="scada-table matriz-sensores">
          <thead>
            <tr>
              <th>Reator</th>
              {TODOS_PARAMETROS.map((tipo) => (
                <th key={tipo}>
                  {PARAMETROS[tipo].rotulo} <small>({PARAMETROS[tipo].unidade})</small>
                </th>
              ))}
              <th>Atualizado</th>
            </tr>
          </thead>
          <tbody>
            {reatores.map((reator) => {
              const info = NIVEL[reator.nivel] ?? NIVEL.ESTAVEL
              return (
                <tr key={reator.id}>
                  <td>
                    <button type="button" className="matriz-reator" onClick={() => onSelecionar(reator.id)}>
                      <span className={`mon-lampada pequena ${info.classe}`} aria-hidden="true" />
                      {nomeReator(reator.id)}
                    </button>
                  </td>
                  {TODOS_PARAMETROS.map((tipo) => {
                    const sensor = sensorDoTipo(reator, tipo)
                    const faixa = faixaDoSensor(sensor)
                    return (
                      <td key={tipo} className={`matriz-valor ${faixa}`}>
                        {sensor ? formatarValor(sensor.valor, tipo) : '—'}
                      </td>
                    )
                  })}
                  <td className="ev">{formatarHora(reator.atualizadoEm)}</td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
      <div className="scada-note">
        Só o que sai do normal é destacado: amarelo na faixa de atenção, vermelho fora do limite
        seguro. Clique num reator para ver os sensores dele em detalhe.
      </div>
    </div>
  )
}

export default function SensoresPage() {
  const { estado, origemMock, aplicarCenarioNormal, aplicarCenarioObservacao } = useEstado()
  const [reator, selecionar] = useReatorSelecionado()
  const reatores = estado.reatores ?? []
  const comReatores = reatores.length > 0
  const reatorAtual = comReatores && reator !== TODOS ? reatores.find((r) => r.id === reator) : null
  const mostrarMatriz = comReatores && reator === TODOS

  // Sem `reatores` (API antiga ou mock) a tela volta ao comportamento de um reator só.
  const sensores = reatorAtual
    ? sensoresDoReator(reatorAtual)
    : estado.sensores?.length > 0
      ? mapearDaApi(estado.sensores)
      : SENSORES_RF1

  const statusNucleo = reatorAtual
    ? { label: NIVEL[reatorAtual.nivel]?.texto ?? reatorAtual.nivel, cls: NIVEL[reatorAtual.nivel]?.classe ?? 'ok' }
    : estado.status === 'CRITICO'
      ? { label: 'CRÍTICO', cls: 'crit' }
      : estado.status === 'ATENCAO'
        ? { label: 'ATENÇÃO', cls: 'warn' }
        : { label: 'ESTÁVEL', cls: 'ok' }

  // Cenário precisa de um reator: com "Todos" os botões ficam desligados.
  const alvoCenario = comReatores ? reatorAtual?.id : undefined
  const cenarioDesligado = comReatores && !reatorAtual

  return (
    <section>
      <p className="scada-kicker">TELEMETRIA INSTRUMENTAL • MALHA PRIMÁRIA</p>
      <h1 className="scada-heading">Sensores Operacionais</h1>
      <p className="scada-lead">
        Monitoramento contínuo de grandezas físicas com limites operacionais, margens de segurança e
        tendência recente. Fonte: {origemMock ? 'simulado local (offline)' : 'telemetria ao vivo'}
        {!mostrarMatriz && (
          <>
            {' '}· {reatorAtual ? nomeReator(reatorAtual.id) : 'núcleo'}{' '}
            <span className={`scada-pill ${statusNucleo.cls}`}>{statusNucleo.label}</span>
          </>
        )}
      </p>

      <SeletorReator />
      <AvisoOutrosReatores />

      <div className="scada-scenarios" aria-label="Condições de medição">
        <button
          type="button"
          className="scada-scenario"
          disabled={cenarioDesligado}
          onClick={() => aplicarCenarioNormal(alvoCenario)}
        >
          <strong>Condição · Normal{reatorAtual ? ` · ${nomeReator(reatorAtual.id)}` : ''}</strong>
          <span>{cenarioDesligado ? 'Escolha um reator acima' : '6 leituras estáveis — tendência previsível no histórico'}</span>
        </button>
        <button
          type="button"
          className="scada-scenario"
          disabled={cenarioDesligado}
          onClick={() => aplicarCenarioObservacao(alvoCenario)}
        >
          <strong>Condição · Observação{reatorAtual ? ` · ${nomeReator(reatorAtual.id)}` : ''}</strong>
          <span>{cenarioDesligado ? 'Escolha um reator acima' : 'Temperatura 328 °C — faixa de atenção preventiva'}</span>
        </button>
      </div>

      {mostrarMatriz ? (
        <MatrizReatores reatores={reatores} onSelecionar={selecionar} />
      ) : (
      <>
      <div style={{ display: 'grid', gap: 10, marginTop: 16 }}>
        {sensores.map((s) => {
          const st = statusSensor(s)
          const cor = corTipo(s.tipo === 'FLUXO_RESFRIAMENTO' ? 'FLUXO' : s.tipo)
          const pct = pctBarra(s)
          const Icone = ICONE_TIPO[s.tipo] ?? IconActivity
          const nome = NOME_TIPO[s.tipo] ?? s.tipo
          return (
            <div className="sensor-row" key={s.id}>
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
        })}
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
          {origemMock
            ? 'Sem conexão com a API: exibindo histórico simulado localmente.'
            : 'Últimas 6 leituras de cada sensor, atualizadas automaticamente. As condições Normal e Observação aplicam leituras de referência.'}
        </div>
      </div>
      </>
      )}
    </section>
  )
}
