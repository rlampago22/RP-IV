import { useEffect, useState } from 'react'
import { startRegistration } from '@simplewebauthn/browser'
import { authApi } from '../services/authApi.js'
import { executarAcaoCritica } from '../services/acaoCritica.js'
import { IconLock, IconShield } from '../components/icons.jsx'

function formatarData(iso) {
  return iso ? new Date(iso).toLocaleString('pt-BR', { hour12: false }) : '—'
}

function Aviso({ aviso }) {
  if (!aviso) return null
  return (
    <p className={`scada-note ${aviso.tipo === 'ok' ? 'scada-note-ok' : 'scada-note-erro'}`} role="status" style={{ marginTop: 14 }}>
      {aviso.texto}
    </p>
  )
}

/** RNF08 — digital como segundo fator das ações críticas; cadastro prévio pelo Administrador (UC02). */
export default function SegurancaPage({ usuario }) {
  const administrador = usuario.papeis.includes('ADMINISTRADOR_SISTEMA')

  return (
    <section>
      <p className="scada-kicker">RNF08 · AUTENTICAÇÃO EM DOIS FATORES</p>
      <h1 className="scada-heading">Segurança do acesso</h1>
      <p className="scada-lead">
        O login da Central usa a senha e, em seguida, a <strong>confirmação no celular</strong> (RF18: aprovar
        a solicitação ou digitar o código de 6 dígitos). As <strong>ações críticas</strong> pedem também a sua digital
        (leitor do computador ou celular): administrar acessos, remover uma digital e, quando existir,
        ativar o protocolo de contingência. Depois de confirmar, você tem 5 minutos para outras ações
        críticas sem confirmar de novo. A digital nunca sai do aparelho: a Central guarda só uma chave
        pública. O cadastro exige um código de ativação entregue pelo Administrador do Sistema.
      </p>
      <MinhaDigital administrador={administrador} />
      {administrador && <AdministracaoAcessos usuarioAtual={usuario} />}
      {administrador && <TrilhaAutenticacao />}
    </section>
  )
}

function MinhaDigital({ administrador }) {
  // null = carregando
  const [digitais, setDigitais] = useState(null)
  const [codigo, setCodigo] = useState('')
  const [apelido, setApelido] = useState('Leitor do notebook')
  const [ocupado, setOcupado] = useState(false)
  const [aviso, setAviso] = useState(null)

  async function carregar() {
    try {
      setDigitais(await authApi.listarBiometrias())
    } catch (error) {
      setDigitais([])
      setAviso({ tipo: 'erro', texto: error.message })
    }
  }

  useEffect(() => {
    carregar()
  }, [])

  async function cadastrar(event) {
    event.preventDefault()
    setOcupado(true)
    setAviso(null)
    try {
      const opcoes = await authApi.opcoesCadastroBiometria(codigo)
      let credencial
      try {
        credencial = await startRegistration({ optionsJSON: opcoes })
      } catch {
        throw new Error('Cadastro cancelado ou não concluído no leitor. O código continua valendo: tente de novo.')
      }
      await authApi.cadastrarBiometria(apelido, credencial)
      setCodigo('')
      setAviso({ tipo: 'ok', texto: 'Digital cadastrada. Ela será pedida nas ações críticas.' })
      await carregar()
    } catch (error) {
      setAviso({ tipo: 'erro', texto: error.message })
    } finally {
      setOcupado(false)
    }
  }

  async function remover(digital) {
    if (!window.confirm(`Remover "${digital.apelido}"? Ele deixa de servir para confirmar ações críticas.`)) return
    setOcupado(true)
    setAviso(null)
    try {
      await executarAcaoCritica(() => authApi.removerBiometria(digital.id))
      await carregar()
    } catch (error) {
      setAviso({ tipo: 'erro', texto: error.message })
    } finally {
      setOcupado(false)
    }
  }

  return (
    <div className="scada-card" style={{ marginTop: 16 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap', marginBottom: 14 }}>
        <h2 className="panel-heading" style={{ margin: 0 }}>
          <IconLock size={13} />
          Minha digital
        </h2>
        {digitais !== null && (
          <span className={`scada-pill ${digitais.length > 0 ? 'ok' : 'warn'}`}>
            {digitais.length > 0 ? 'CADASTRADA' : 'NÃO CADASTRADA'}
          </span>
        )}
      </div>

      {digitais === null && <p className="scada-lead">Carregando…</p>}
      {digitais !== null && digitais.length === 0 && (
        <p className="scada-lead" style={{ marginTop: 0 }}>
          Você ainda não tem digital cadastrada, então não consegue fazer ações críticas. Peça o código de
          ativação ao Administrador do Sistema e cadastre abaixo.
          {administrador && (
            <>
              {' '}<strong>Primeiro Administrador do sistema?</strong> O código está no terminal do servidor
              (janela "Login (backend 8081)"), na linha <code>[PRIMEIRO ACESSO]</code>.
            </>
          )}
        </p>
      )}
      {digitais !== null && digitais.length > 0 && (
        <div className="scada-table-wrap" style={{ maxHeight: 'none' }}>
          <table className="scada-table">
            <thead>
              <tr>
                <th>Leitor</th>
                <th>Cadastrado em</th>
                <th>Último uso</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {digitais.map((digital) => (
                <tr key={digital.id}>
                  <td className="ev">{digital.apelido}</td>
                  <td>{formatarData(digital.criadoEm)}</td>
                  <td>{formatarData(digital.ultimoUsoEm)}</td>
                  <td style={{ textAlign: 'right' }}>
                    <button type="button" className="scada-btn scada-btn-ghost" disabled={ocupado} onClick={() => remover(digital)}>
                      Remover
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <form className="auth-form" style={{ marginTop: 16, maxWidth: 420 }} onSubmit={cadastrar}>
        <label>
          Código de ativação (entregue pelo Administrador)
          <input
            value={codigo}
            onChange={(event) => setCodigo(event.target.value.toUpperCase())}
            placeholder="XXXX-XXXX"
            autoComplete="one-time-code"
            maxLength={9}
            required
          />
        </label>
        <label>
          Nome do leitor
          <input value={apelido} maxLength={80} onChange={(event) => setApelido(event.target.value)} />
        </label>
        <button type="submit" className="scada-btn" disabled={ocupado}>
          {ocupado ? 'Aguardando o leitor…' : digitais?.length ? 'Cadastrar outro leitor' : 'Cadastrar digital'}
        </button>
      </form>
      <Aviso aviso={aviso} />
    </div>
  )
}

function AdministracaoAcessos({ usuarioAtual }) {
  const [usuarios, setUsuarios] = useState(null)
  const [ocupado, setOcupado] = useState(false)
  const [aviso, setAviso] = useState(null)
  // Código recém-gerado: só aparece agora, o servidor guarda apenas o hash.
  const [codigoGerado, setCodigoGerado] = useState(null)

  async function carregar() {
    try {
      setUsuarios(await authApi.listarUsuarios())
    } catch (error) {
      setUsuarios([])
      setAviso({ tipo: 'erro', texto: error.message })
    }
  }

  useEffect(() => {
    carregar()
  }, [])

  async function gerarCodigo(alvo) {
    setOcupado(true)
    setAviso(null)
    try {
      const emitido = await executarAcaoCritica(() => authApi.emitirCodigoAtivacao(alvo.id))
      setCodigoGerado({ ...emitido, nome: alvo.nome })
      await carregar()
    } catch (error) {
      setAviso({ tipo: 'erro', texto: error.message })
    } finally {
      setOcupado(false)
    }
  }

  async function revogar(alvo) {
    const texto = alvo.email === usuarioAtual.email
      ? 'Revogar a sua própria digital? Para cadastrar de novo você vai precisar de um código (gerado na próxima inicialização do servidor ou por outro Administrador).'
      : `Revogar a digital de ${alvo.nome}? Use quando o leitor ou o celular foi perdido ou trocado.`
    if (!window.confirm(texto)) return
    setOcupado(true)
    setAviso(null)
    try {
      await executarAcaoCritica(() => authApi.revogarBiometriaDe(alvo.id))
      setAviso({ tipo: 'ok', texto: `Digital de ${alvo.nome} revogada. Gere um código novo para ele cadastrar outra.` })
      await carregar()
    } catch (error) {
      setAviso({ tipo: 'erro', texto: error.message })
    } finally {
      setOcupado(false)
    }
  }

  return (
    <div className="scada-card" style={{ marginTop: 16 }}>
      <h2 className="panel-heading">
        <IconShield size={13} />
        Administração de acessos (Administrador do Sistema)
      </h2>
      <p className="scada-lead" style={{ marginTop: 0 }}>
        Cadastro prévio (UC02): gere o código de ativação e entregue pessoalmente ao funcionário. Ele vale por
        24 horas e uma única vez; gerar outro invalida o anterior. Gerar código e revogar são ações críticas e
        pedem a sua digital.
      </p>

      {codigoGerado && (
        <div className="scada-note scada-note-ok" style={{ marginBottom: 14 }} role="status">
          Código de ativação de <strong>{codigoGerado.nome}</strong>:{' '}
          <span className="codigo-ativacao">{codigoGerado.codigo}</span>
          <br />
          Vale até {formatarData(codigoGerado.expiraEm)}. Anote agora: ele não será mostrado de novo.
        </div>
      )}

      {usuarios === null && <p className="scada-lead">Carregando…</p>}
      {usuarios !== null && usuarios.length > 0 && (
        <div className="scada-table-wrap" style={{ maxHeight: 'none' }}>
          <table className="scada-table">
            <thead>
              <tr>
                <th>Funcionário</th>
                <th>Perfil</th>
                <th>Digital</th>
                <th>Código de ativação</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {usuarios.map((alvo) => (
                <tr key={alvo.id}>
                  <td className="ev">
                    {alvo.nome}
                    <br />
                    <small>{alvo.email}</small>
                  </td>
                  <td>{alvo.papeis.map((papel) => papel.replaceAll('_', ' ')).join(' · ')}</td>
                  <td>
                    <span className={`scada-pill ${alvo.aparelhos > 0 ? 'ok' : 'warn'}`}>
                      {alvo.aparelhos > 0 ? 'CADASTRADA' : 'NÃO CADASTRADA'}
                    </span>
                  </td>
                  <td>{alvo.codigoValidoAte ? `válido até ${formatarData(alvo.codigoValidoAte)}` : '—'}</td>
                  <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                    <button type="button" className="scada-btn" disabled={ocupado} onClick={() => gerarCodigo(alvo)}>
                      Gerar código
                    </button>{' '}
                    <button
                      type="button"
                      className="scada-btn scada-btn-ghost"
                      disabled={ocupado || alvo.aparelhos === 0}
                      onClick={() => revogar(alvo)}
                    >
                      Revogar digital
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <Aviso aviso={aviso} />
    </div>
  )
}

/** Issue #81 (RNF03/RNF05) — trilha de autenticação encadeada por hash, inclusive cada etapa do 2FA. */
function TrilhaAutenticacao() {
  const [trilha, setTrilha] = useState(null)
  const [erro, setErro] = useState('')

  async function carregar() {
    try {
      setTrilha(await authApi.auditoriaAutenticacao())
      setErro('')
    } catch (error) {
      setErro(error.message)
    }
  }

  useEffect(() => {
    carregar()
  }, [])

  return (
    <div className="scada-card" style={{ marginTop: 16 }}>
      <h2 className="panel-heading">
        <IconShield size={13} />
        Trilha de autenticação (auditoria do 2FA)
      </h2>
      <p className="scada-lead" style={{ marginTop: 0 }}>
        Cada tentativa de login e cada etapa do segundo fator ficam registradas (sucesso e falha), sem o código
        e sem o telefone. Cada linha guarda o hash da anterior: alterar ou apagar uma linha quebra a cadeia.
      </p>
      {erro ? <p className="scada-note scada-note-erro">{erro}</p> : null}
      {trilha && (
        <>
          <p style={{ margin: '0 0 10px' }}>
            <span className={`scada-pill ${trilha.integra ? 'ok' : 'crit'}`}>
              {trilha.integra ? 'CADEIA ÍNTEGRA' : `ADULTERADA NO REGISTRO ${trilha.primeiroAdulterado}`}
            </span>{' '}
            {trilha.total} registros{' '}
            <button type="button" className="scada-btn scada-btn-ghost" onClick={carregar}>Atualizar</button>
          </p>
          <div style={{ overflowX: 'auto' }}>
            <table className="scada-table">
              <thead>
                <tr><th>Quando</th><th>E-mail</th><th>Método</th><th>Resultado</th><th>Hash</th></tr>
              </thead>
              <tbody>
                {[...trilha.registros].reverse().map((r) => (
                  <tr key={r.id}>
                    <td>{formatarData(r.registradoEm)}</td>
                    <td>{r.email}</td>
                    <td>{r.metodo}</td>
                    <td>{r.resultado}</td>
                    <td><code>{r.hash}…</code></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  )
}
