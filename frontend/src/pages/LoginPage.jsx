import { useState } from 'react'
import { AtomLogo, IconAlertTriangle } from '../components/icons.jsx'
import { authApi } from '../services/authApi.js'

// RF18 (2FA por celular): depois da senha, o login fica pendente até o celular aprovar
// ou até o código de 6 dígitos recebido no celular ser digitado aqui.
export default function LoginPage({ onAutenticado }) {
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [pendente, setPendente] = useState(null)
  const [codigo, setCodigo] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function executar(acao) {
    setErro('')
    setEnviando(true)
    try {
      await acao()
    } catch (error) {
      setErro(error.message)
    } finally {
      setEnviando(false)
    }
  }

  function enviarSenha(event) {
    event.preventDefault()
    executar(async () => {
      const resposta = await authApi.login(email, senha)
      if (resposta?.segundoFator) setPendente(resposta)
      else onAutenticado(resposta)
    })
  }

  function enviarCodigo(event) {
    event.preventDefault()
    executar(async () => onAutenticado(await authApi.segundoFatorCodigo(codigo)))
  }

  function jaAprovei() {
    executar(async () => onAutenticado(await authApi.segundoFatorAprovacao()))
  }

  function voltar() {
    setPendente(null)
    setCodigo('')
    setSenha('')
    setErro('')
  }

  const mensagemErro = erro ? (
    <p className="auth-error" role="alert" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <IconAlertTriangle size={14} />
      {erro}
    </p>
  ) : null

  return (
    <main className="auth-screen">
      <section className="auth-card" aria-labelledby="login-title">
        <div className="brand-mark">
          <AtomLogo size={26} />
        </div>
        <p className="scada-kicker">{pendente ? 'Segundo fator · celular' : 'Acesso restrito'}</p>
        <h1 id="login-title">{pendente ? 'Confirme no seu celular' : 'Central de Supervisão'}</h1>

        {!pendente ? (
          <>
            <p>Identifique-se para acessar os dados operacionais do Reator-01.</p>
            <form onSubmit={enviarSenha} className="auth-form">
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
          </>
        ) : (
          <>
            <p>
              Senha conferida. Enviamos uma solicitação ao celular vinculado a <strong>{pendente.destino}</strong>.
              Aprove no celular ou digite o código de 6 dígitos que chegou nele.
            </p>
            <form onSubmit={enviarCodigo} className="auth-form">
              <label>
                Código recebido no celular
                <input
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  maxLength={7}
                  value={codigo}
                  onChange={(event) => setCodigo(event.target.value)}
                  placeholder="000000"
                  required
                />
              </label>
              {mensagemErro}
              <button className="scada-btn" type="submit" disabled={enviando}>Confirmar código</button>
              <button className="scada-btn scada-btn-ghost" type="button" disabled={enviando} onClick={jaAprovei}>
                Já aprovei no celular
              </button>
              <button className="scada-btn scada-btn-ghost" type="button" onClick={voltar}>Voltar</button>
            </form>
            <p style={{ fontSize: 12 }}>
              Demonstração: o celular é simulado em{' '}
              <a href="/celular" target="_blank" rel="noreferrer">/celular</a>.
            </p>
          </>
        )}
      </section>
    </main>
  )
}
