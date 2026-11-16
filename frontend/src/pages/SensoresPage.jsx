import { SENSORES_RF1 } from '../data/sensores.js'
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
import { IconActivity, IconClock } from '../components/icons.jsx'
import SensorCard, { mapearDaApi, celulasHistorico, sensoresDoReator } from '../components/SensorCard.jsx'
import SensorSubNav from '../components/SensorSubNav.jsx'

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

      {/* Uma aba por tipo de sensor (independente do reator escolhido acima) — ver SensorSubNav. */}
      <SensorSubNav />

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
