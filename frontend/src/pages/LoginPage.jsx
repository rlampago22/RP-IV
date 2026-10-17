import { useState } from 'react'

export default function LoginPage({ onLogin }) {
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  async function enviar(event) {
    event.preventDefault()
    setErro('')
    setEnviando(true)
    try {
      await onLogin(email, senha)
    } catch (error) {
      setErro(error.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <main className="auth-screen">
      <section className="auth-card" aria-labelledby="login-title">
        <div className="scada-dot">RN</div>
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
          {erro ? <p className="auth-error" role="alert">{erro}</p> : null}
          <button className="scada-btn" type="submit" disabled={enviando}>{enviando ? 'Autenticando…' : 'Entrar no sistema'}</button>
        </form>
      </section>
    </main>
  )
}
