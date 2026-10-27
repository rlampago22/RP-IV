import { useState } from 'react'
import { startAuthentication, startRegistration } from '@simplewebauthn/browser'
import { authApi } from '../services/authApi.js'
import { AtomLogo, IconAlertTriangle, IconLock } from '../components/icons.jsx'

/**
 * Login em dois fatores (RNF08): senha → confirmar no celular. Quem ainda não tem celular cadastrado
 * passa pelo primeiro acesso, com o código de ativação entregue pelo Administrador do Sistema (UC02).
 */
export default function LoginPage({ onLogin, onConfirmarBiometria, onPrimeiroAcesso }) {
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)
  // null = pedir senha · { etapa: 'BIOMETRIA', opcoes } · { etapa: 'PRIMEIRO_ACESSO' }
  const [etapa, setEtapa] = useState(null)

  function voltarParaSenha(mensagem = '') {
    setEtapa(null)
    setErro(mensagem)
  }

  async function enviar(event) {
    event.preventDefault()
    setErro('')
    setEnviando(true)
    try {
      const proxima = await onLogin(email, senha)
      if (proxima) {
        setEtapa(proxima)
        setSenha('')
      }
    } catch (error) {
      setErro(error.message)
    } finally {
      setEnviando(false)
    }
  }

  const mensagemErro = erro ? (
    <p className="auth-error" role="alert" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <IconAlertTriangle size={14} />
      {erro}
    </p>
  ) : null

  if (etapa?.etapa === 'BIOMETRIA') {
    return (
      <ConfirmarNoCelular
        opcoes={etapa.opcoes}
        onConfirmar={onConfirmarBiometria}
        onVoltar={voltarParaSenha}
      />
    )
  }
  if (etapa?.etapa === 'PRIMEIRO_ACESSO') {
    return <PrimeiroAcesso onConcluir={onPrimeiroAcesso} onVoltar={voltarParaSenha} />
  }

  return (
    <main className="auth-screen">
      <section className="auth-card" aria-labelledby="login-title">
        <div className="brand-mark">
          <AtomLogo size={26} />
        </div>
        <p className="scada-kicker">Acesso restrito</p>
        <h1 id="login-title">Central de Supervisão</h1>
        <p>Identifique-se para acessar os dados operacionais do Reator-01.</p>
        <form onSubmit={enviar} className="auth-form">
          <label>
            E-mail institucional
            <input type="email" autoComplete="username" value={email} onChange={(event) => setEmail(event.target.value)} required />
          </label>
          <label>
            Senha
            <input type="password" autoComplete="current-password" value={senha} onChange={(event) => setSenha(event.target.value)} required />
          </label>
          {mensagemErro}
          <button className="scada-btn" type="submit" disabled={enviando}>{enviando ? 'Autenticando…' : 'Entrar no sistema'}</button>
        </form>
        <p className="auth-passos">Passo 1 de 2 · senha. Depois, confirmação no celular.</p>
      </section>
    </main>
  )
}

function CartaoSegundoFator({ kicker, titulo, children }) {
  return (
    <main className="auth-screen">
      <section className="auth-card" aria-labelledby="segundo-fator-title">
        <div className="brand-mark">
          <IconLock size={24} />
        </div>
        <p className="scada-kicker">{kicker}</p>
        <h1 id="segundo-fator-title">{titulo}</h1>
        {children}
      </section>
    </main>
  )
}

function Erro({ texto }) {
  if (!texto) return null
  return (
    <p className="auth-error" role="alert" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <IconAlertTriangle size={14} />
      {texto}
    </p>
  )
}

function ConfirmarNoCelular({ opcoes, onConfirmar, onVoltar }) {
  const [erro, setErro] = useState('')
  const [aguardando, setAguardando] = useState(false)

  async function confirmar() {
    setErro('')
    setAguardando(true)
    let credencial
    try {
      credencial = await startAuthentication({ optionsJSON: opcoes })
    } catch {
      // Cancelou, demorou ou o celular não respondeu: o desafio continua valendo, pode tentar de novo.
      setErro('Confirmação cancelada ou não concluída. Tente de novo ou volte para a senha.')
      setAguardando(false)
      return
    }
    try {
      await onConfirmar(credencial)
    } catch (error) {
      onVoltar(error.message)
    }
  }

  return (
    <CartaoSegundoFator kicker="Passo 2 de 2 · segundo fator" titulo="Confirme no celular">
      <p>
        Senha conferida. Para abrir a sessão, confirme com a biometria do celular cadastrado:
        escaneie o QR code que o navegador vai mostrar e use a digital ou o rosto.
      </p>
      <div className="auth-form">
        <Erro texto={erro} />
        <button className="scada-btn" type="button" onClick={confirmar} disabled={aguardando}>
          {aguardando ? 'Aguardando o celular…' : 'Confirmar com biometria'}
        </button>
        <button className="scada-btn scada-btn-ghost" type="button" disabled={aguardando} onClick={() => onVoltar()}>
          Voltar para a senha
        </button>
      </div>
    </CartaoSegundoFator>
  )
}

function PrimeiroAcesso({ onConcluir, onVoltar }) {
  const [codigo, setCodigo] = useState('')
  const [apelido, setApelido] = useState('Meu celular')
  const [erro, setErro] = useState('')
  const [aguardando, setAguardando] = useState(false)

  async function cadastrar(event) {
    event.preventDefault()
    setErro('')
    setAguardando(true)
    try {
      const opcoes = await authApi.primeiroAcessoOpcoes(codigo)
      let credencial
      try {
        credencial = await startRegistration({ optionsJSON: opcoes })
      } catch {
        throw new Error('Cadastro cancelado ou não concluído no celular. Tente de novo.')
      }
      await onConcluir(apelido, credencial)
    } catch (error) {
      // 401 = acabou o primeiro acesso (muitas tentativas ou tempo esgotado): volta para a senha.
      if (error.status === 401) {
        onVoltar(error.message)
        return
      }
      setErro(error.message)
      setAguardando(false)
    }
  }

  return (
    <CartaoSegundoFator kicker="Primeiro acesso · cadastro da biometria" titulo="Cadastre seu celular">
      <p>
        Senha conferida, mas você ainda não tem um celular cadastrado. Digite o <strong>código de
        ativação</strong> que o Administrador do Sistema te entregou e confirme com a digital ou o rosto.
        Depois disso, todo login pede a senha e a confirmação no celular.
      </p>
      <form className="auth-form" onSubmit={cadastrar}>
        <label>
          Código de ativação
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
          Nome do aparelho
          <input value={apelido} maxLength={80} onChange={(event) => setApelido(event.target.value)} />
        </label>
        <Erro texto={erro} />
        <button className="scada-btn" type="submit" disabled={aguardando}>
          {aguardando ? 'Aguardando o celular…' : 'Cadastrar celular e entrar'}
        </button>
        <button className="scada-btn scada-btn-ghost" type="button" disabled={aguardando} onClick={() => onVoltar()}>
          Voltar para a senha
        </button>
      </form>
      <p className="auth-passos">Não tem o código? Peça ao Administrador do Sistema (aba Segurança).</p>
    </CartaoSegundoFator>
  )
}
