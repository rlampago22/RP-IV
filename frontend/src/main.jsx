import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, NavLink, Route, Routes } from 'react-router-dom'
import './styles/app.css'
import OverviewPage from './pages/OverviewPage.jsx'
import SensoresPage from './pages/SensoresPage.jsx'
import AlarmesPage from './pages/AlarmesPage.jsx'
import AuditoriaPage from './pages/AuditoriaPage.jsx'
import DemoPage from './pages/DemoPage.jsx'
import { EstadoProvider, useEstado } from './state/EstadoContext.jsx'

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

function Shell() {
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
          {origemMock && (
            <span className="scada-pill warn">MOCK · rode mvp/4-EXECUTAR-API-ESTADO.bat</span>
          )}
          <span className={`scada-badge${BADGE_CLASSE[estado.status] ?? ''}`}>
            {BADGE_TEXTO[estado.status] ?? estado.status}
          </span>
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
    <EstadoProvider>
      <BrowserRouter>
        <Shell />
      </BrowserRouter>
    </EstadoProvider>
  </React.StrictMode>
)
