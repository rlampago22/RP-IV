import React, { useEffect, useState } from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, NavLink, Route, Routes } from 'react-router-dom'
import './styles/app.css'
import OverviewPage from './pages/OverviewPage.jsx'
import SensoresPage from './pages/SensoresPage.jsx'
import AlarmesPage from './pages/AlarmesPage.jsx'
import AuditoriaPage from './pages/AuditoriaPage.jsx'
import DemoPage from './pages/DemoPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import { authApi } from './services/authApi.js'

const TABS = [
  { to: '/', end: true, text: 'Visão Geral' },
  { to: '/sensores', text: 'Sensores' },
  { to: '/alarmes', text: 'Alarmes' },
  { to: '/auditoria', text: 'Auditoria' },
  { to: '/demo', text: 'Demo / Cenários' },
]

const PERFIS_DA_CENTRAL = new Set(['OPERADOR_REATOR', 'SUPERVISAO_CENTRAL', 'ADMINISTRADOR_SISTEMA'])

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

  return <Shell usuario={usuario} onLogout={async () => { await authApi.logout(); setUsuario(null) }} />
}

function Shell({ usuario, onLogout }) {
  const [alarmState, setAlarmState] = useState('stable')
  const [apiOnline, setApiOnline] = useState(false)
  const [events, setEvents] = useState([
    { type: 'MEDICAO_REGISTRADA', detail: 'ciclo tempo real' },
  ])

  const addEvent = (type, detail) => {
    setEvents((current) => [{ type, detail }, ...current])
  }
  const simulateCritical = () => {
    setAlarmState('active')
    addEvent('ALARME_EMITIDO', 'Temp 372 °C · fluxo 420 m³/h')
  }
  const acknowledgeAlarm = () => {
    if (alarmState !== 'active') return
    setAlarmState('acknowledged')
    addEvent('ALARME_RECONHECIDO', 'Operador de Reator confirmou a ocorrência')
  }
  const resolveAlarm = () => {
    if (alarmState === 'stable') return
    setAlarmState('stable')
    addEvent('ALARME_RESOLVIDO', 'Parâmetros normalizados pelo operador')
  }

  const isCritical = alarmState === 'active'
  const isAcknowledged = alarmState === 'acknowledged'
  const podeAcessarCentral = usuario.papeis.some((papel) => PERFIS_DA_CENTRAL.has(papel))

  useEffect(() => {
    if (!podeAcessarCentral) return undefined
    let ativo = true
    authApi.centralStatus().then(() => ativo && setApiOnline(true)).catch(() => ativo && setApiOnline(false))
    return () => { ativo = false }
  }, [podeAcessarCentral])

  if (!podeAcessarCentral) {
    return (
      <main className="auth-screen">
        <section className="auth-card">
          <p className="scada-kicker">Acesso autenticado</p>
          <h1>Perfil sem acesso à Central</h1>
          <p>O perfil Guarda / Controle de Acesso é destinado ao UC04 de áreas restritas e não possui acesso à supervisão operacional.</p>
          <button type="button" className="scada-btn scada-btn-ghost" onClick={onLogout}>Sair</button>
        </section>
      </main>
    )
  }

  return (
    <div className="scada-shell">
      <header className="scada-top">
        <div className="scada-brand">
          <div className="scada-dot">RN</div>
          <div>
            <h1>Central de Supervisão · Reator-01</h1>
            <small>Opção A — SCADA escuro · API {apiOnline ? 'ONLINE' : 'OFFLINE'} · EventBus ONLINE</small>
          </div>
        </div>
        <div className="scada-top-right">
          <div className="scada-user">
            <strong>{usuario.nome}</strong>
            <span>{usuario.papeis.map((papel) => papel.replaceAll('_', ' ')).join(' · ')}</span>
          </div>
          <span className={`scada-badge${isCritical ? ' crit' : isAcknowledged ? ' warn' : ''}`}>
            {isCritical ? '● CRÍTICO' : isAcknowledged ? '● EM TRATAMENTO' : '● ESTÁVEL'}
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

      {isCritical && (
        <div className="scada-banner">
          <span>ALARME ATIVO — Temperatura acima do limiar (T-CORE-01).</span>
          <button
            type="button"
            className="scada-btn scada-btn-amber"
            onClick={acknowledgeAlarm}
          >
            Validar / Reconhecer
          </button>
        </div>
      )}

      <main className="scada-main">
        <Routes>
          <Route path="/" element={<OverviewPage alarmState={alarmState} events={events} onAcknowledge={acknowledgeAlarm} onResolve={resolveAlarm} />} />
          <Route path="/sensores" element={<SensoresPage />} />
          <Route path="/alarmes" element={<AlarmesPage alarmState={alarmState} onAcknowledge={acknowledgeAlarm} onResolve={resolveAlarm} />} />
          <Route path="/auditoria" element={<AuditoriaPage events={events} />} />
          <Route path="/demo" element={<DemoPage onCritical={simulateCritical} onNormal={resolveAlarm} />} />
        </Routes>
      </main>
    </div>
  )
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </React.StrictMode>
)
