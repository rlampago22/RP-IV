import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, NavLink, Route, Routes } from 'react-router-dom'
import './styles/app.css'
import OverviewPage from './pages/OverviewPage.jsx'
import SensoresPage from './pages/SensoresPage.jsx'
import AlarmesPage from './pages/AlarmesPage.jsx'
import AuditoriaPage from './pages/AuditoriaPage.jsx'
import DemoPage from './pages/DemoPage.jsx'

function Shell() {
  return (
    <div className="app-shell">
      <header className="topbar">
        <strong>Usina Nuclear — Supervisão</strong>
        <span className="pill ok">EDA · MVP</span>
      </header>
      <div className="body">
        <nav className="sidenav">
          <NavLink to="/">Visão geral</NavLink>
          <NavLink to="/sensores">Sensores</NavLink>
          <NavLink to="/alarmes">Alarmes</NavLink>
          <NavLink to="/auditoria">Auditoria</NavLink>
          <NavLink to="/demo">Demo</NavLink>
        </nav>
        <main className="content">
          <Routes>
            <Route path="/" element={<OverviewPage />} />
            <Route path="/sensores" element={<SensoresPage />} />
            <Route path="/alarmes" element={<AlarmesPage />} />
            <Route path="/auditoria" element={<AuditoriaPage />} />
            <Route path="/demo" element={<DemoPage />} />
          </Routes>
        </main>
      </div>
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
