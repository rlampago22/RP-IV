import { urlExportacao } from '../api/historico.js'
import { CODIGO_POR_ID, CODIGO_POR_TIPO } from '../data/sensores.js'
import { useHistorico } from '../state/HistoricoContext.jsx'
import GraficoHistorico from './GraficoHistorico.jsx'
import RelatorioDiaMock from './RelatorioDiaMock.jsx'

const CLASSE_STATUS = { ESTAVEL: 'ok', ATENCAO: 'warn', CRITICO: 'crit' }
const TEXTO_STATUS = { ESTAVEL: 'ESTÁVEL', ATENCAO: 'ATENÇÃO', CRITICO: 'CRÍTICO' }

const ORIGEM_ROTULO = {
  NAO_IDENTIFICADO: 'não identificado',
  DECLARADO_PELA_UI: 'declarado',
  SESSAO_VALIDADA: 'verificado',
}

const FAIXAS = [
  { campo: 'temperatura', rotulo: 'Temperatura', unidade: '°C', passo: '0.1' },
  { campo: 'pressao', rotulo: 'Pressão', unidade: 'bar', passo: '0.1' },
  { campo: 'radiacao', rotulo: 'Radiação', unidade: 'mSv/h', passo: '0.01' },
  { campo: 'fluxo', rotulo: 'Fluxo resfriamento', unidade: 'm³/h', passo: '1' },
]

const hora = (iso) =>
  new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit', hour12: false })

const dataHora = (iso) => new Date(iso).toLocaleString('pt-BR', { hour12: false })

const diaBr = (dia) => dia.split('-').reverse().join('/')

/**
 * Rótulo vem da API como `id:TIPO` (ex.: `2:PRESSAO`) e é traduzido para o código operacional
 * (`P-PRIM-01`) — o mesmo que o T02 exibe, para o sensor não ter dois nomes no sistema.
 */
function nomeSensor(rotulo) {
  const [id, tipo] = rotulo.split(':')
  return CODIGO_POR_ID[Number(id)] ?? CODIGO_POR_TIPO[tipo] ?? `${tipo} #${id}`
}

function ListaSensores({ rotulos, classe }) {
  if (!rotulos || rotulos.length === 0) return <span style={{ color: 'var(--dim)' }}>—</span>
  return (
    <>
      {rotulos.map((rotulo) => (
        <span key={rotulo} className={`scada-pill ${classe}`} style={{ marginRight: 4 }}>
          {nomeSensor(rotulo)}
        </span>
      ))}
    </>
  )
}

export default function HistoricoPage() {
  const {
    periodo,
    saidas,
    selecionada,
    faixas,
    detalhe,
    faixasAplicadas,
    relatorioAberto,
    graficosAbertos,
    anexarGraficos,
    erro,
    carregando,
    alterarPeriodo,
    alterarFaixa,
    pesquisarSaidas,
    limparPeriodo,
    abrirSaida,
    pesquisarDetalhe,
    limparFaixas,
    voltarParaSaidas,
    alternarRelatorio,
    fecharRelatorio,
    alternarGraficos,
    definirAnexarGraficos,
  } = useHistorico()

  return (
    <section>
      <p className="scada-kicker scada-nao-imprimir">T06 · RF16 / RF17</p>
      <h1 className="scada-heading scada-nao-imprimir">Histórico Operacional</h1>

      {!selecionada ? (
        <>
          <p className="scada-lead">
            Busque as saídas de operação por período e operador. Cada linha é um dia de serviço —
            um dia com troca de turno aparece uma vez por operador.
          </p>

          <form
            className="scada-card scada-filtros"
            onSubmit={(e) => {
              e.preventDefault()
              pesquisarSaidas()
            }}
          >
            <h2>Período e operador</h2>
            <div className="scada-filtro-linha">
              <label className="scada-campo">
                <span>Data inicial</span>
                <input
                  type="date"
                  value={periodo.de}
                  onChange={(e) => alterarPeriodo('de', e.target.value)}
                />
              </label>
              <label className="scada-campo">
                <span>Data final</span>
                <input
                  type="date"
                  value={periodo.ate}
                  onChange={(e) => alterarPeriodo('ate', e.target.value)}
                />
              </label>
              <label className="scada-campo">
                <span>Operador</span>
                <input
                  type="text"
                  placeholder="trecho do nome"
                  value={periodo.operador}
                  onChange={(e) => alterarPeriodo('operador', e.target.value)}
                />
              </label>
            </div>
            <div className="scada-actions">
              <button type="submit" className="scada-btn" disabled={carregando}>
                {carregando ? 'Buscando…' : 'Buscar saídas'}
              </button>
              <button
                type="button"
                className="scada-btn"
                disabled={carregando}
                onClick={limparPeriodo}
              >
                Limpar
              </button>
            </div>
          </form>

          {erro && <div className="scada-api-error" style={{ marginTop: 14 }}>{erro}</div>}

          {saidas && (
            <div className="scada-card" style={{ marginTop: 14 }}>
              <h2>
                {saidas.total === 0
                  ? 'Nenhuma saída de operação no período'
                  : `${saidas.total} saída(s) · dia no fuso ${saidas.fusoHorario}`}
              </h2>

              {saidas.total > 0 && (
                <div className="scada-table-wrap" style={{ maxHeight: 420 }}>
                  <table className="scada-table">
                    <thead>
                      <tr>
                        <th>Dia</th>
                        <th>Operador</th>
                        <th>Período</th>
                        <th>Ciclos</th>
                        <th>Em alarme</th>
                        <th>Em falha</th>
                        <th>Sensores envolvidos</th>
                        <th>Pior status</th>
                        <th />
                      </tr>
                    </thead>
                    <tbody>
                      {saidas.dias.map((saida) => (
                        <tr
                          key={`${saida.dia}-${saida.operador}-${saida.primeiroRegistro}`}
                          className="scada-linha-clicavel"
                          onClick={() => abrirSaida(saida)}
                        >
                          <td className="ev">{diaBr(saida.dia)}</td>
                          <td>
                            {saida.operador || '—'}{' '}
                            <small style={{ color: 'var(--dim)' }}>
                              ({ORIGEM_ROTULO[saida.origemOperador] ?? saida.origemOperador})
                            </small>
                          </td>
                          <td>
                            {hora(saida.primeiroRegistro)} – {hora(saida.ultimoRegistro)}
                          </td>
                          <td>{saida.ciclos}</td>
                          <td>{saida.ciclosComAlarme}</td>
                          <td>{saida.ciclosComFalha}</td>
                          <td>
                            <ListaSensores rotulos={saida.sensoresEnvolvidos} classe="purple" />
                          </td>
                          <td>
                            <span className={`scada-pill ${CLASSE_STATUS[saida.piorStatus] ?? ''}`}>
                              {TEXTO_STATUS[saida.piorStatus] ?? saida.piorStatus}
                            </span>
                          </td>
                          <td style={{ color: 'var(--cyan)' }}>abrir →</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}

              <p className="scada-event-note">
                "Em alarme" conta ciclos com alarme ativo, não alarmes distintos — o histórico
                guarda a quantidade por ciclo, não os identificadores.
              </p>
            </div>
          )}
        </>
      ) : (
        <>
          <p className="scada-lead scada-nao-imprimir">
            Saída de <strong>{diaBr(selecionada.dia)}</strong> ·{' '}
            {selecionada.operador || 'operador não identificado'} ·{' '}
            {hora(selecionada.primeiroRegistro)} – {hora(selecionada.ultimoRegistro)}
          </p>

          <div className="scada-actions scada-nao-imprimir" style={{ marginBottom: 14 }}>
            <button type="button" className="scada-btn" onClick={voltarParaSaidas}>
              ← Voltar às saídas
            </button>
            <button
              type="button"
              className="scada-btn scada-btn-amber"
              onClick={alternarRelatorio}
            >
              {relatorioAberto ? 'Ocultar relatório do dia' : 'Relatório do dia'}
            </button>
            <button type="button" className="scada-btn" onClick={alternarGraficos}>
              {graficosAbertos ? 'Ocultar gráficos' : 'Mostrar gráficos'}
            </button>
          </div>

          <div className="scada-impressao">
            {relatorioAberto && (
              <RelatorioDiaMock
                saida={selecionada}
                onFechar={fecharRelatorio}
                anexarGraficos={anexarGraficos}
                onAnexarGraficos={definirAnexarGraficos}
              />
            )}

            {graficosAbertos && detalhe && detalhe.total > 0 && (
              <GraficoHistorico resultado={detalhe} imprimirComRelatorio={anexarGraficos} />
            )}
          </div>

          <form
            className="scada-card scada-filtros"
            onSubmit={(e) => {
              e.preventDefault()
              pesquisarDetalhe()
            }}
          >
            <h2>Faixas dos parâmetros RF-1</h2>
            <div className="scada-filtro-grade">
              {FAIXAS.map(({ campo, rotulo, unidade, passo }) => (
                <div className="scada-faixa" key={campo}>
                  <span className="scada-faixa-rotulo">
                    {rotulo} <small>{unidade}</small>
                  </span>
                  <div className="scada-faixa-campos">
                    <input
                      type="number"
                      step={passo}
                      placeholder="mín"
                      value={faixas[`${campo}Min`]}
                      onChange={(e) => alterarFaixa(`${campo}Min`, e.target.value)}
                    />
                    <input
                      type="number"
                      step={passo}
                      placeholder="máx"
                      value={faixas[`${campo}Max`]}
                      onChange={(e) => alterarFaixa(`${campo}Max`, e.target.value)}
                    />
                  </div>
                </div>
              ))}
            </div>
            <div className="scada-actions">
              <button type="submit" className="scada-btn" disabled={carregando}>
                {carregando ? 'Buscando…' : 'Buscar'}
              </button>
              <button
                type="button"
                className="scada-btn"
                onClick={limparFaixas}
                disabled={carregando}
              >
                Limpar
              </button>
              {faixasAplicadas ? (
                <a
                  className="scada-btn scada-btn-green"
                  href={urlExportacao(selecionada, faixasAplicadas)}
                >
                  Exportar CSV
                </a>
              ) : (
                <button type="button" className="scada-btn" disabled>
                  Exportar CSV
                </button>
              )}
            </div>
          </form>

          {erro && <div className="scada-api-error" style={{ marginTop: 14 }}>{erro}</div>}

          {detalhe && (
            <div className="scada-card" style={{ marginTop: 14 }}>
              <h2>
                {detalhe.total === 0
                  ? 'Nenhum registro nas faixas informadas'
                  : `${detalhe.total} registro(s) · exibindo ${detalhe.retornados}`}
              </h2>

              {detalhe.truncado && (
                <p className="scada-note">
                  A consulta foi limitada a {detalhe.retornados} linhas. O arquivo exportado traz
                  o conjunto completo dos {detalhe.total} ciclos.
                </p>
              )}

              {detalhe.total > 0 && (
                <div className="scada-table-wrap" style={{ maxHeight: 420 }}>
                  <table className="scada-table">
                    <thead>
                      <tr>
                        <th>Instante</th>
                        <th>Temp (°C)</th>
                        <th>Pressão (bar)</th>
                        <th>Radiação (mSv/h)</th>
                        <th>Fluxo (m³/h)</th>
                        <th>Status</th>
                        <th>Em alarme</th>
                        <th>Observação</th>
                        <th>Em falha</th>
                      </tr>
                    </thead>
                    <tbody>
                      {detalhe.registros.map((registro) => (
                        <tr key={registro.instante}>
                          <td className="ev">{dataHora(registro.instante)}</td>
                          <td>{registro.temperatura.toFixed(2)}</td>
                          <td>{registro.pressao.toFixed(2)}</td>
                          <td>{registro.radiacao.toFixed(2)}</td>
                          <td>{registro.fluxoResfriamento.toFixed(2)}</td>
                          <td>
                            <span className={`scada-pill ${CLASSE_STATUS[registro.status] ?? ''}`}>
                              {TEXTO_STATUS[registro.status] ?? registro.status}
                            </span>
                          </td>
                          <td>
                            <ListaSensores rotulos={registro.sensoresEmAlarme} classe="crit" />
                          </td>
                          <td>
                            <ListaSensores rotulos={registro.sensoresEmObservacao} classe="warn" />
                          </td>
                          <td>
                            <ListaSensores rotulos={registro.sensoresEmFalha} classe="purple" />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}

              <p className="scada-event-note">
                Status é derivado dos alarmes e observações de cada ciclo, não armazenado. Falha de
                sensor é eixo próprio e não altera o status — quando um sensor está em falha, sua
                coluna de valor mostra a última leitura conhecida, não uma medição atual.
              </p>
            </div>
          )}
        </>
      )}
    </section>
  )
}
