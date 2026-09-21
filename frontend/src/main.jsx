import React, { useState } from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, NavLink, Route, Routes } from 'react-router-dom'
import './styles/app.css'
import OverviewPage from './pages/OverviewPage.jsx'
import SensoresPage from './pages/SensoresPage.jsx'
import AlarmesPage from './pages/AlarmesPage.jsx'
import AuditoriaPage from './pages/AuditoriaPage.jsx'
import DemoPage from './pages/DemoPage.jsx'

const TABS = [
  { to: '/', end: true, text: 'Visão Geral' },
  { to: '/sensores', text: 'Sensores' },
  { to: '/alarmes', text: 'Alarmes' },
  { to: '/auditoria', text: 'Auditoria' },
  { to: '/demo', text: 'Demo / Cenários' },
]

const INITIAL_ALARMS = [
  {
    id: 'AL-0001',
    severity: 'ALTO',
    status: 'ATIVO',
    message: 'Temperatura acima do limiar',
    sensor: 'T-CORE-01',
  },
  {
    id: 'AL-0002',
    severity: 'MANUT',
    status: 'ATIVO',
    message: 'Falha de comunicação',
    sensor: 'R-CONT-01',
  },
]

function Shell() {
  const [alarms, setAlarms] = useState(INITIAL_ALARMS)
  const [events, setEvents] = useState([])

  function publishAlarmEvent(type, alarmId) {
    setEvents((current) => [...current, { type, alarmId }])
  }

  function acknowledgeAlarm(alarmId) {
    setAlarms((current) => current.map((alarm) => (
      alarm.id === alarmId ? { ...alarm, status: 'RECONHECIDO' } : alarm
    )))
    publishAlarmEvent('ALARME_RECONHECIDO', alarmId)
  }

  function resolveAlarm(alarmId) {
    setAlarms((current) => current.map((alarm) => (
      alarm.id === alarmId ? { ...alarm, status: 'RESOLVIDO' } : alarm
    )))
    publishAlarmEvent('ALARME_RESOLVIDO', alarmId)
  }

  const activeAlarm = alarms.find((alarm) => alarm.status === 'ATIVO')
  const acknowledgedAlarm = alarms.find((alarm) => alarm.status === 'RECONHECIDO')
  const badgeClass = activeAlarm ? 'crit' : acknowledgedAlarm ? 'warn' : ''
  const badgeLabel = activeAlarm ? '● CRÍTICO' : acknowledgedAlarm ? '● EM TRATAMENTO' : '● ESTÁVEL'

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
          <span className={`scada-badge ${badgeClass}`}>
            {badgeLabel}
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
          <span>ALARME ATIVO — {activeAlarm.message} ({activeAlarm.sensor}).</span>
          <button
            type="button"
            className="scada-btn scada-btn-amber"
            onClick={() => acknowledgeAlarm(activeAlarm.id)}
          >
            Validar / Reconhecer
          </button>
        </div>
      )}

      {!activeAlarm && acknowledgedAlarm && (
        <div className="scada-banner warn-banner">
          <span>ALARME RECONHECIDO — ocorrência em tratamento ({acknowledgedAlarm.id}).</span>
          <button
            type="button"
            className="scada-btn scada-btn-green"
            onClick={() => resolveAlarm(acknowledgedAlarm.id)}
          >
            Normalizar / Encerrar
          </button>
        </div>
      )}

      <main className="scada-main">
        <Routes>
          <Route path="/" element={<OverviewPage />} />
          <Route path="/sensores" element={<SensoresPage />} />
          <Route path="/alarmes" element={<AlarmesPage alarms={alarms} eventCount={events.length} onAcknowledge={acknowledgeAlarm} onResolve={resolveAlarm} />} />
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
      <Shell />
    </BrowserRouter>
  </React.StrictMode>
)
