import { useEffect, useRef, useState } from 'react'
import { authApi } from '../services/authApi.js'
import { executarAcaoCritica } from '../services/acaoCritica.js'
import { useEstado } from '../state/EstadoContext.jsx'
import { PARAMETROS } from '../data/reatores.js'
import { IconCore } from '../components/icons.jsx'

/**
 * Issue #96 — cadastro de reatores (Administrador do Sistema).
 *
 * A Central grava em `nucleo.reator` / `nucleo.sensor_reator`; o núcleo relê o cadastro a cada 2 s e
 * inclui, ajusta ou retira o reator sem reiniciar. A coluna "Painel" mostra quando a mudança já
 * chegou ao monitoramento (o reator aparece ou some de `/api/estado`).
 */

const CAMPOS = [
  ['limiteMinimo', 'Limite mín.'],
  ['atencaoMinima', 'Atenção mín.'],
  ['setpoint', 'Setpoint'],
  ['atencaoMaxima', 'Atenção máx.'],
  ['limiteMaximo', 'Limite máx.'],
]

function rotulo(tipo) {
  return PARAMETROS[tipo]?.rotulo ?? tipo
}

function numero(valor) {
  return Number.isFinite(valor) ? valor.toLocaleString('pt-BR', { maximumFractionDigits: 3 }) : '—'
}

/** Mesma regra do backend: limite mín. < atenção mín. ≤ setpoint ≤ atenção máx. < limite máx. */
function erroDoSensor(s) {
  const valores = CAMPOS.map(([campo]) => s[campo])
  if (valores.some((v) => !Number.isFinite(v))) return `${rotulo(s.tipo)}: preencha todos os valores.`
  if (!(s.limiteMinimo < s.atencaoMinima && s.atencaoMinima <= s.atencaoMaxima && s.atencaoMaxima < s.limiteMaximo)) {
    return `${rotulo(s.tipo)}: a faixa precisa seguir limite mín. < atenção mín. ≤ atenção máx. < limite máx.`
  }
  if (s.setpoint < s.atencaoMinima || s.setpoint > s.atencaoMaxima) {
    return `${rotulo(s.tipo)}: o setpoint precisa ficar dentro da faixa de atenção.`
  }
  return null
}

function Aviso({ aviso }) {
  if (!aviso) return null
  return (
    <p className={`scada-note ${aviso.tipo === 'ok' ? 'scada-note-ok' : 'scada-note-erro'}`} role="status" style={{ marginTop: 14 }}>
      {aviso.texto}
    </p>
  )
}

export default function ReatoresPage({ usuario }) {
  const administrador = usuario.papeis.includes('ADMINISTRADOR_SISTEMA')
  const { estado } = useEstado()
  const [reatores, setReatores] = useState(null)
  const [edicao, setEdicao] = useState(null) // { id: null (novo) | 'R-0X', sensores: [...] }
  const [ocupado, setOcupado] = useState(false)
  const [aviso, setAviso] = useState(null)

  const noPainel = new Set((estado.reatores ?? []).map((r) => r.id))

  async function carregar() {
    try {
      setReatores(await authApi.listarReatores())
    } catch (error) {
      setReatores([])
      setAviso({ tipo: 'erro', texto: error.message })
    }
  }

  useEffect(() => {
    if (administrador) carregar()
  }, [administrador])

  // O formulário abre acima da tabela. Quem clica em "Ajustar" numa linha de baixo pode estar com a
  // página rolada: ao abrir (novo ou ajuste), rola até ele e põe o foco no primeiro campo.
  const formulario = useRef(null)
  const formularioAberto = edicao === null ? null : edicao.id ?? 'novo'
  useEffect(() => {
    const form = formulario.current
    if (!formularioAberto || !form) return
    const semAnimacao = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    form.scrollIntoView({ behavior: semAnimacao ? 'auto' : 'smooth', block: 'start' })
    form.querySelector('input')?.focus({ preventScroll: true })
  }, [formularioAberto])

  if (!administrador) {
    return (
      <section>
        <p className="scada-kicker">ISSUE #96 · CADASTRO DE REATORES</p>
        <h1 className="scada-heading">Reatores</h1>
        <p className="scada-note scada-note-erro">Acesso restrito ao Administrador do Sistema.</p>
      </section>
    )
  }

  async function executar(acao, mensagemOk) {
    setOcupado(true)
    setAviso(null)
    try {
      const resultado = await executarAcaoCritica(acao)
      setAviso({ tipo: 'ok', texto: mensagemOk(resultado) })
      setEdicao(null)
      await carregar()
    } catch (error) {
      setAviso({ tipo: 'erro', texto: error.message })
    } finally {
      setOcupado(false)
    }
  }

  async function novoReator() {
    setAviso(null)
    try {
      setEdicao({ id: null, sensores: await authApi.sensoresPadrao() })
    } catch (error) {
      setAviso({ tipo: 'erro', texto: error.message })
    }
  }

  function salvar(event) {
    event.preventDefault()
    const erro = edicao.sensores.map(erroDoSensor).find(Boolean)
    if (erro) {
      setAviso({ tipo: 'erro', texto: erro })
      return
    }
    const sensores = edicao.sensores.map(({ sensorId, limiteMinimo, limiteMaximo, atencaoMinima, atencaoMaxima, setpoint }) => (
      { sensorId, limiteMinimo, limiteMaximo, atencaoMinima, atencaoMaxima, setpoint }
    ))
    if (edicao.id) {
      executar(
        () => authApi.ajustarReator(edicao.id, sensores),
        (r) => `${r.nome}: faixas e setpoints salvos. O núcleo aplica em até 2 s, a partir da próxima leitura.`,
      )
    } else {
      executar(
        () => authApi.incluirReator(sensores),
        (r) => `${r.nome} (${r.id}) cadastrado. Ele entra no Monitoramento em até 2 s.`,
      )
    }
  }

  function retirar(reator) {
    if (!window.confirm(`Retirar ${reator.nome} de operação? Ele sai do Monitoramento e da simulação; alarmes, histórico e auditoria dele continuam guardados.`)) return
    executar(() => authApi.retirarReator(reator.id), (r) => `${r.nome} retirado de operação.`)
  }

  function reativar(reator) {
    executar(() => authApi.reativarReator(reator.id), (r) => `${r.nome} devolvido à operação. Volta ao Monitoramento em até 2 s.`)
  }

  function alterarCampo(indice, campo, texto) {
    const valor = texto === '' ? NaN : Number(texto.replace(',', '.'))
    setEdicao((atual) => ({
      ...atual,
      sensores: atual.sensores.map((s, i) => (i === indice ? { ...s, [campo]: valor } : s)),
    }))
  }

  return (
    <section>
      <p className="scada-kicker">ISSUE #96 · CADASTRO DE REATORES</p>
      <h1 className="scada-heading">Reatores</h1>
      <p className="scada-lead">
        Reatores da usina, com a faixa segura, a faixa de atenção e o ponto de operação (setpoint) de cada sensor.
        A simulação parte do setpoint; leitura fora da faixa de atenção acende o amarelo e fora do limite gera alarme.
        As mudanças valem <strong>sem reiniciar o núcleo</strong> (em até 2 s). Reator não é excluído, só sai de
        operação: alarmes, histórico e auditoria continuam apontando para ele. Incluir, ajustar, retirar e reativar
        são ações críticas e pedem a sua digital.
      </p>

      {/* Formulário acima da tabela: abre à vista, e a tabela fica embaixo para consulta. */}
      {edicao && (
        <form ref={formulario} className="scada-card" style={{ marginTop: 16, scrollMarginTop: 16 }} onSubmit={salvar}>
          <h2 className="panel-heading">
            <IconCore size={13} />
            {edicao.id ? `Ajustar ${edicao.nome}` : 'Novo reator'}
          </h2>
          <p className="scada-lead" style={{ marginTop: 0 }}>
            {edicao.id
              ? 'As faixas novas valem a partir da próxima leitura; a simulação vai se movendo até o novo setpoint.'
              : 'O reator entra com os 4 sensores RF-1. Os valores abaixo são os padrões da usina; ajuste se precisar.'}
          </p>
          <div className="scada-table-wrap" style={{ maxHeight: 'none' }}>
            <table className="scada-table reatores-edicao">
              <thead>
                <tr>
                  <th>Sensor</th>
                  {CAMPOS.map(([campo, texto]) => (
                    <th key={campo}>{texto}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {edicao.sensores.map((s, indice) => (
                  <tr key={s.sensorId}>
                    <td className="ev">
                      {rotulo(s.tipo)}
                      <br />
                      <span className="reatores-sub">{s.unidade}</span>
                    </td>
                    {CAMPOS.map(([campo, texto]) => (
                      <td key={campo}>
                        <input
                          type="number"
                          step="any"
                          inputMode="decimal"
                          aria-label={`${rotulo(s.tipo)} — ${texto}`}
                          value={Number.isFinite(s[campo]) ? s[campo] : ''}
                          onChange={(event) => alterarCampo(indice, campo, event.target.value)}
                          required
                        />
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="reatores-botoes">
            <button type="submit" className="scada-btn" disabled={ocupado}>
              {ocupado ? 'Aguardando a digital…' : edicao.id ? 'Salvar ajustes' : 'Cadastrar reator'}
            </button>
            <button type="button" className="scada-btn scada-btn-ghost" disabled={ocupado} onClick={() => { setEdicao(null); setAviso(null) }}>
              Cancelar
            </button>
          </div>
          <Aviso aviso={aviso} />
        </form>
      )}

      <div className="scada-card" style={{ marginTop: 16 }}>
        <div className="reatores-topo">
          <h2 className="panel-heading" style={{ margin: 0 }}>
            <IconCore size={13} />
            Reatores cadastrados
          </h2>
          <button type="button" className="scada-btn" disabled={ocupado || edicao !== null} onClick={novoReator}>
            + Novo reator
          </button>
        </div>

        {reatores === null && <p className="scada-lead">Carregando…</p>}
        {reatores !== null && reatores.length > 0 && (
          <div className="scada-table-wrap" style={{ maxHeight: 'none' }}>
            <table className="scada-table">
              <thead>
                <tr>
                  <th>Reator</th>
                  <th>Situação</th>
                  <th>Painel</th>
                  {reatores[0].sensores.map((s) => (
                    <th key={s.sensorId}>{rotulo(s.tipo)}</th>
                  ))}
                  <th />
                </tr>
              </thead>
              <tbody>
                {reatores.map((reator) => {
                  const visivel = noPainel.has(reator.id)
                  const sincronizado = visivel === reator.ativo
                  return (
                    <tr key={reator.id}>
                      <td className="ev">
                        {reator.nome}
                        <br />
                        <span className="reatores-sub">{reator.id}{reator.id === 'R-01' ? ' · principal' : ''}</span>
                      </td>
                      <td>
                        <span className={`scada-pill ${reator.ativo ? 'ok' : 'warn'}`}>
                          {reator.ativo ? 'EM OPERAÇÃO' : 'FORA DE OPERAÇÃO'}
                        </span>
                      </td>
                      <td>
                        <span className={`scada-pill ${sincronizado ? 'ok' : 'warn'}`} title="Situação no /api/estado do núcleo">
                          {sincronizado ? (visivel ? 'NO PAINEL' : 'FORA DO PAINEL') : 'APLICANDO…'}
                        </span>
                      </td>
                      {reator.sensores.map((s) => (
                        <td key={s.sensorId} className="reatores-faixa">
                          <strong>{numero(s.setpoint)}</strong> {s.unidade}
                          <br />
                          <span className="reatores-sub">
                            limite {numero(s.limiteMinimo)}–{numero(s.limiteMaximo)} · atenção {numero(s.atencaoMinima)}–{numero(s.atencaoMaxima)}
                          </span>
                        </td>
                      ))}
                      <td className="reatores-acoes">
                        <button
                          type="button"
                          className="scada-btn scada-btn-ghost"
                          disabled={ocupado || edicao !== null || !reator.ativo}
                          onClick={() => {
                            setAviso(null)
                            setEdicao({ id: reator.id, nome: reator.nome, sensores: reator.sensores })
                          }}
                        >
                          Ajustar
                        </button>
                        {reator.ativo ? (
                          <button
                            type="button"
                            className="scada-btn scada-btn-ghost"
                            disabled={ocupado || edicao !== null || reator.id === 'R-01'}
                            title={reator.id === 'R-01' ? 'O reator principal não sai de operação' : undefined}
                            onClick={() => retirar(reator)}
                          >
                            Retirar
                          </button>
                        ) : (
                          <button
                            type="button"
                            className="scada-btn scada-btn-green"
                            disabled={ocupado || edicao !== null}
                            onClick={() => reativar(reator)}
                          >
                            Devolver à operação
                          </button>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
        {edicao === null && <Aviso aviso={aviso} />}
      </div>
    </section>
  )
}
