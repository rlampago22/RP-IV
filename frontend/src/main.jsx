import React, { useReducer } from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, NavLink, Route, Routes } from 'react-router-dom'
import './styles/app.css'
import OverviewPage from './pages/OverviewPage.jsx'
import SensoresPage from './pages/SensoresPage.jsx'
import AlarmesPage from './pages/AlarmesPage.jsx'
import AuditoriaPage from './pages/AuditoriaPage.jsx'
import DemoPage from './pages/DemoPage.jsx'
import { initialMvpState, mvpDemoReducer, scenarioActions } from './mvpDemoState.js'

const TABS = [
  { to: '/', end: true, text: 'Visão Geral' },
  { to: '/sensores', text: 'Sensores' },
  { to: '/alarmes', text: 'Alarmes' },
  { to: '/auditoria', text: 'Auditoria' },
  { to: '/demo', text: 'Demo / Cenários' },
]

function Shell() {
  const [mvp, dispatch] = useReducer(mvpDemoReducer, initialMvpState)
  const activeAlarm = mvp.alarms.find((alarm) => alarm.status === 'ATIVO')
  const statusClass = mvp.coreStatus === 'ESTÁVEL' ? '' : ' crit'
  const runScenario = (scenario) => dispatch({ type: scenarioActions[scenario] })

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
          <span className={`scada-badge${statusClass}`}>
            ● {mvp.coreStatus}
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

      {activeAlarm && (
        <div className="scada-banner">
          <span>ALARME ATIVO — {activeAlarm.mensagem}.</span>
          <button
            type="button"
            className="scada-btn scada-btn-amber"
            onClick={() => dispatch({ type: 'ACK_ALARM' })}
          >
            Validar / Reconhecer
          </button>
        </div>
      )}

      <main className="scada-main">
        <Routes>
          <Route path="/" element={<OverviewPage state={mvp} onAck={() => dispatch({ type: 'ACK_ALARM' })} onResolve={() => dispatch({ type: 'RESOLVE_ALARM' })} />} />
          <Route path="/sensores" element={<SensoresPage sensors={mvp.sensors} />} />
          <Route path="/alarmes" element={<AlarmesPage alarms={mvp.alarms} onAck={() => dispatch({ type: 'ACK_ALARM' })} onResolve={() => dispatch({ type: 'RESOLVE_ALARM' })} />} />
          <Route path="/auditoria" element={<AuditoriaPage events={mvp.events} />} />
          <Route path="/demo" element={<DemoPage state={mvp} onScenario={runScenario} />} />
        </Routes>
      </main>
    </div>
  )
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <Shell />
    </BrowserRouter>
  </React.StrictMode>
)
