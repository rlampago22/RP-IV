import { useState } from 'react'
import { startAuthentication } from '@simplewebauthn/browser'
import { AtomLogo, IconAlertTriangle, IconLock } from '../components/icons.jsx'

export default function LoginPage({ onLogin, onConfirmarBiometria }) {
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)
  // Opções do WebAuthn enviadas pelo backend quando a senha confere e o usuário tem biometria cadastrada.
  const [desafio, setDesafio] = useState(null)

  async function enviar(event) {
    event.preventDefault()
    setErro('')
    setEnviando(true)
    try {
      const etapa = await onLogin(email, senha)
      if (etapa) {
        setDesafio(etapa.opcoes)
        setSenha('')
      }
    } catch (error) {
      setErro(error.message)
    } finally {
      setEnviando(false)
    }
  }

  async function confirmarBiometria() {
    setErro('')
    setEnviando(true)
    let credencial
    try {
      credencial = await startAuthentication({ optionsJSON: desafio })
    } catch {
      // Cancelou, demorou ou o celular não respondeu: o desafio continua valendo, pode tentar de novo.
      setErro('Confirmação cancelada ou não concluída. Tente de novo ou volte para a senha.')
      setEnviando(false)
      return
    }
    try {
      await onConfirmarBiometria(credencial)
    } catch (error) {
      setDesafio(null)
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

  if (desafio) {
    return (
      <main className="auth-screen">
        <section className="auth-card" aria-labelledby="biometria-title">
          <div className="brand-mark">
            <IconLock size={24} />
          </div>
          <p className="scada-kicker">Segundo fator</p>
          <h1 id="biometria-title">Confirme no celular</h1>
          <p>
            Senha conferida. Para abrir a sessão, confirme com a biometria do celular cadastrado:
            escaneie o QR code que o navegador vai mostrar e use a digital ou o rosto.
          </p>
          <div className="auth-form">
            {mensagemErro}
            <button className="scada-btn" type="button" onClick={confirmarBiometria} disabled={enviando}>
              {enviando ? 'Aguardando o celular…' : 'Confirmar com biometria'}
            </button>
            <button
              className="scada-btn scada-btn-ghost"
              type="button"
              disabled={enviando}
              onClick={() => {
                setDesafio(null)
                setErro('')
              }}
            >
              Voltar para a senha
            </button>
          </div>
        </section>
      </main>
    )
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
      </section>
    </main>
  )
}
