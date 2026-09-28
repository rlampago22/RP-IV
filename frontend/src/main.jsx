import React, { useEffect, useState } from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, NavLink, Route, Routes } from 'react-router-dom'
import './styles/app.css'
import OverviewPage from './pages/OverviewPage.jsx'
import SensoresPage from './pages/SensoresPage.jsx'
import AlarmesPage from './pages/AlarmesPage.jsx'
import AuditoriaPage from './pages/AuditoriaPage.jsx'
import DemoPage from './pages/DemoPage.jsx'
import { EstadoProvider, useEstado } from './state/EstadoContext.jsx'
import LoginPage from './pages/LoginPage.jsx'
import { authApi } from './services/authApi.js'

const TABS = [
  { to: '/', end: true, text: 'Visão Geral' },
  { to: '/sensores', text: 'Sensores' },
  { to: '/alarmes', text: 'Alarmes' },
  { to: '/auditoria', text: 'Auditoria' },
  { to: '/demo', text: 'Demo / Cenários' },
]

const BADGE_TEXTO = {
  ESTAVEL: '● ESTÁVEL',
  ATENCAO: '● ATENÇÃO',
  CRITICO: '● CRÍTICO',
}

const BADGE_CLASSE = {
  ESTAVEL: '',
  ATENCAO: ' warn',
  CRITICO: ' crit',
}

const PERFIS_DA_CENTRAL = new Set([
  'OPERADOR_REATOR',
  'SUPERVISAO_CENTRAL',
  'ADMINISTRADOR_SISTEMA',
])

function App() {
  const [usuario, setUsuario] = useState(undefined)

  useEffect(() => {
    authApi.me().then(setUsuario).catch(() => setUsuario(null))
  }, [])

  if (usuario === undefined) {
    return <main className="auth-screen"><p className="auth-loading">Verificando sessão segura…</p></main>
  }

  if (usuario === null) {
    return <LoginPage onLogin={async (email, senha) => setUsuario(await authApi.login(email, senha))} />
  }

  const podeAcessarCentral = usuario.papeis.some((papel) => PERFIS_DA_CENTRAL.has(papel))
  if (!podeAcessarCentral) {
    return (
      <main className="auth-screen">
        <section className="auth-card">
          <p className="scada-kicker">Acesso autenticado</p>
          <h1>Perfil sem acesso à Central</h1>
          <p>O perfil Guarda / Controle de Acesso é destinado ao UC04 e não possui acesso à supervisão operacional.</p>
          <button type="button" className="scada-btn scada-btn-ghost" onClick={async () => { await authApi.logout(); setUsuario(null) }}>Sair</button>
        </section>
      </main>
    )
  }

  return (
    <EstadoProvider>
      <BrowserRouter>
        <Shell usuario={usuario} onLogout={async () => { await authApi.logout(); setUsuario(null) }} />
      </BrowserRouter>
    </EstadoProvider>
  )
}

function Shell({ usuario, onLogout }) {
  const { estado, origemMock, reconhecerAlarme } = useEstado()
  const alarmeAtivo = estado.alarmes.find((a) => a.status === 'ATIVO')

  return (
    <div className="scada-shell">
      <header className="scada-top">
        <div className="scada-brand">
          <div className="scada-dot">RN</div>
          <div>
            <h1>Central de Supervisão · Reator-01</h1>
            <small>
              Opção A — SCADA escuro · EventBus {origemMock ? 'MOCK (API offline)' : 'ONLINE'}
            </small>
          </div>
        </div>
        <div className="scada-top-right">
          <div className="scada-user">
            <strong>{usuario.nome}</strong>
            <span>{usuario.papeis.map((papel) => papel.replaceAll('_', ' ')).join(' · ')}</span>
          </div>
          {origemMock && (
            <span className="scada-pill warn">MOCK · rode mvp/4-EXECUTAR-API-ESTADO.bat</span>
          )}
          <span className={`scada-badge${BADGE_CLASSE[estado.status] ?? ''}`}>
            {BADGE_TEXTO[estado.status] ?? estado.status}
          </span>
          <button type="button" className="scada-btn scada-btn-ghost scada-logout" onClick={onLogout}>Sair</button>
        </div>
      </header>

      <nav className="scada-tabs">
        {TABS.map((tab) => (
          <NavLink
            key={tab.to}
            to={tab.to}
            end={tab.end}
            className={({ isActive }) => `scada-tab${isActive ? ' active' : ''}`}
          >
            {tab.text}
          </NavLink>
        ))}
      </nav>

      {alarmeAtivo && (
        <div className="scada-banner">
          <span>ALARME ATIVO — {alarmeAtivo.mensagem}</span>
          <button
            type="button"
            className="scada-btn scada-btn-amber"
            onClick={() => reconhecerAlarme(alarmeAtivo.id)}
          >
            Validar / Reconhecer
          </button>
        </div>
      )}

      <main className="scada-main">
        <Routes>
          <Route path="/" element={<OverviewPage />} />
          <Route path="/sensores" element={<SensoresPage />} />
          <Route path="/alarmes" element={<AlarmesPage />} />
          <Route path="/auditoria" element={<AuditoriaPage />} />
          <Route path="/demo" element={<DemoPage />} />
        </Routes>
      </main>
    </div>
  )
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)
