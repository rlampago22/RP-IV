import { useEffect, useState } from 'react'
import { authApi } from '../services/authApi.js'

// RF18 — "celular" da demonstração. Faz o papel do app no aparelho do usuário: recebe a
// solicitação de login, mostra o código e deixa aprovar ou recusar. Em produção seria push/SMS.
export default function CelularSimuladoPage() {
  const [notificacoes, setNotificacoes] = useState([])
  const [erro, setErro] = useState('')

  async function carregar() {
    try {
      setNotificacoes(await authApi.celularNotificacoes())
      setErro('')
    } catch (error) {
      setErro(error.message)
    }
  }

  useEffect(() => {
    carregar()
    // Simula a chegada da notificação no aparelho (a Central em si não faz polling).
    const id = setInterval(carregar, 2000)
    return () => clearInterval(id)
  }, [])

  async function responder(id, aprovar) {
    try {
      await (aprovar ? authApi.celularAprovar(id) : authApi.celularRecusar(id))
    } catch (error) {
      setErro(error.message)
    }
    carregar()
  }

  return (
    <main className="auth-screen">
      <section className="celular-sim" aria-label="Celular simulado">
        <div className="celular-notch" />
        <p className="scada-kicker">Celular simulado · RF18</p>
        <h1 style={{ fontSize: 18, margin: '4px 0 12px' }}>Usina · Autenticador</h1>
        {erro ? <p className="auth-error">{erro}</p> : null}
        {notificacoes.length === 0 ? <p className="auth-loading">Nenhuma solicitação de acesso.</p> : null}
        {notificacoes.map((n) => (
          <article key={n.id} className="celular-notificacao">
            <strong>Tentativa de acesso à Central</strong>
            <span>{n.usuario} · {n.destino}</span>
            <span className="celular-codigo">{n.codigo}</span>
            <span>Situação: {n.status}</span>
            {n.status === 'PENDENTE' ? (
              <div style={{ display: 'flex', gap: 8 }}>
                <button type="button" className="scada-btn" onClick={() => responder(n.id, true)}>Aprovar</button>
                <button type="button" className="scada-btn scada-btn-ghost" onClick={() => responder(n.id, false)}>Recusar</button>
              </div>
            ) : null}
          </article>
        ))}
      </section>
    </main>
  )
}
