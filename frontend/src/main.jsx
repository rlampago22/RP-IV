import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, NavLink, Route, Routes } from 'react-router-dom'
import './styles/app.css'
import OverviewPage from './pages/OverviewPage.jsx'
import SensoresPage from './pages/SensoresPage.jsx'
import AlarmesPage from './pages/AlarmesPage.jsx'
import AuditoriaPage from './pages/AuditoriaPage.jsx'
import DemoPage from './pages/DemoPage.jsx'
import { PlantProvider, usePlant } from './state/PlantContext.jsx'

const TABS = [
  { to: '/', end: true, text: 'Visão Geral' },
  { to: '/sensores', text: 'Sensores' },
  { to: '/alarmes', text: 'Alarmes' },
  { to: '/auditoria', text: 'Auditoria' },
  { to: '/demo', text: 'Demo / Cenários' },
]

function Shell() {
  const { status, banner, reconhecer } = usePlant()

  return (
    <div className="scada-shell">
      <header className="scada-top">
        <div className="scada-brand">
          <div className="scada-dot">RN</div>
          <div>
            <h1>Central de Supervisão · Reator-01</h1>
            <small>Opção A — SCADA escuro · EventBus ONLINE</small>
          </div>
        </div>
        <div className="scada-top-right">
          <span className={`scada-badge${status.cls ? ` ${status.cls}` : ''}`}>
            ● {status.label}
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

      {banner && (
        <div className="scada-banner">
          <span>{banner}</span>
          <button type="button" className="scada-btn scada-btn-amber" onClick={() => reconhecer()}>
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
    <BrowserRouter>
      <PlantProvider>
        <Shell />
      </PlantProvider>
    </BrowserRouter>
  </React.StrictMode>
)
