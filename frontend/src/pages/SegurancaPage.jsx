import { useEffect, useState } from 'react'
import { startRegistration } from '@simplewebauthn/browser'
import { authApi } from '../services/authApi.js'
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

/** RNF08 — login em dois fatores obrigatório: senha + biometria do celular (passkey / WebAuthn). */
export default function SegurancaPage({ usuario }) {
  const administrador = usuario.papeis.includes('ADMINISTRADOR_SISTEMA')

  return (
    <section>
      <p className="scada-kicker">RNF08 · AUTENTICAÇÃO EM DOIS FATORES</p>
      <h1 className="scada-heading">Segurança do acesso</h1>
      <p className="scada-lead">
        Para entrar na Central são exigidos dois fatores: a senha e a confirmação no celular com
        digital ou rosto (passkey / WebAuthn). A biometria nunca sai do aparelho: a Central guarda só
        uma chave pública e confere a assinatura que o celular devolve. O cadastro do celular só é
        feito com um código de ativação entregue pelo Administrador do Sistema.
      </p>
      <MeusAparelhos />
      {administrador && <AdministracaoAcessos usuarioAtual={usuario} />}
    </section>
  )
}

function MeusAparelhos() {
  // null = carregando
  const [aparelhos, setAparelhos] = useState(null)
  const [apelido, setApelido] = useState('Celular reserva')
  const [ocupado, setOcupado] = useState(false)
  const [aviso, setAviso] = useState(null)

  async function carregar() {
    try {
      setAparelhos(await authApi.listarBiometrias())
    } catch (error) {
      setAparelhos([])
      setAviso({ tipo: 'erro', texto: error.message })
    }
  }

  useEffect(() => {
    carregar()
  }, [])

  async function adicionar() {
    setOcupado(true)
    setAviso(null)
    try {
      const opcoes = await authApi.opcoesCadastroBiometria()
      let credencial
      try {
        credencial = await startRegistration({ optionsJSON: opcoes })
      } catch {
        throw new Error('Cadastro cancelado ou não concluído no aparelho.')
      }
      await authApi.cadastrarBiometria(apelido, credencial)
      setAviso({ tipo: 'ok', texto: 'Aparelho adicionado. Ele também pode confirmar o seu login.' })
      await carregar()
    } catch (error) {
      setAviso({ tipo: 'erro', texto: error.message })
    } finally {
      setOcupado(false)
    }
  }

  async function remover(aparelho) {
    if (!window.confirm(`Remover "${aparelho.apelido}"? Ele deixa de servir para confirmar o login.`)) return
    setOcupado(true)
    setAviso(null)
    try {
      await authApi.removerBiometria(aparelho.id)
      await carregar()
    } catch (error) {
      setAviso({ tipo: 'erro', texto: error.message })
    } finally {
      setOcupado(false)
    }
  }

  const unico = aparelhos?.length === 1

  return (
    <div className="scada-card" style={{ marginTop: 16 }}>
      <h2 className="panel-heading">
        <IconLock size={13} />
        Meus aparelhos
      </h2>

      {aparelhos === null && <p className="scada-lead">Carregando…</p>}
      {aparelhos !== null && aparelhos.length > 0 && (
        <div className="scada-table-wrap" style={{ maxHeight: 'none' }}>
          <table className="scada-table">
            <thead>
              <tr>
                <th>Aparelho</th>
                <th>Cadastrado em</th>
                <th>Último uso</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {aparelhos.map((aparelho) => (
                <tr key={aparelho.id}>
                  <td className="ev">{aparelho.apelido}</td>
                  <td>{formatarData(aparelho.criadoEm)}</td>
                  <td>{formatarData(aparelho.ultimoUsoEm)}</td>
                  <td style={{ textAlign: 'right' }}>
                    <button
                      type="button"
                      className="scada-btn scada-btn-ghost"
                      disabled={ocupado || unico}
                      title={unico ? 'Único aparelho: se perdeu o celular, peça ao Administrador para revogar.' : undefined}
                      onClick={() => remover(aparelho)}
                    >
                      Remover
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="auth-form" style={{ marginTop: 16, maxWidth: 420 }}>
        <label>
          Adicionar outro aparelho (reserva)
          <input value={apelido} maxLength={80} onChange={(event) => setApelido(event.target.value)} />
        </label>
        <button type="button" className="scada-btn scada-btn-ghost" disabled={ocupado} onClick={adicionar}>
          {ocupado ? 'Aguardando o aparelho…' : 'Adicionar aparelho'}
        </button>
      </div>
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
      setCodigoGerado({ ...(await authApi.emitirCodigoAtivacao(alvo.id)), nome: alvo.nome })
      await carregar()
    } catch (error) {
      setAviso({ tipo: 'erro', texto: error.message })
    } finally {
      setOcupado(false)
    }
  }

  async function revogar(alvo) {
    const proprio = alvo.email === usuarioAtual.email
    const texto = proprio
      ? 'Revogar a sua própria biometria? Você só voltará a entrar com um código novo (gerado na próxima inicialização do servidor ou por outro Administrador).'
      : `Revogar a biometria de ${alvo.nome}? Use quando o celular foi perdido ou trocado. No próximo login será preciso um código novo.`
    if (!window.confirm(texto)) return
    setOcupado(true)
    setAviso(null)
    try {
      await authApi.revogarBiometriaDe(alvo.id)
      setAviso({ tipo: 'ok', texto: `Biometria de ${alvo.nome} revogada. Gere um código novo para ele cadastrar outro celular.` })
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
        Cadastro prévio (UC02): gere o código de ativação e entregue pessoalmente ao funcionário. Ele vale
        por 24 horas e uma única vez; gerar outro invalida o anterior.
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
                <th>Biometria</th>
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
                      {alvo.aparelhos > 0 ? `ATIVA · ${alvo.aparelhos} aparelho${alvo.aparelhos > 1 ? 's' : ''}` : 'NÃO CADASTRADA'}
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
                      Revogar biometria
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
