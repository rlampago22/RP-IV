import React, { useEffect, useState } from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, NavLink, Route, Routes, useLocation } from 'react-router-dom'
import './styles/app.css'
import OverviewPage from './pages/OverviewPage.jsx'
import SensoresPage from './pages/SensoresPage.jsx'
import SensorDetailPage from './pages/sensores/SensorDetailPage.jsx'
import AlarmesPage from './pages/AlarmesPage.jsx'
import AuditoriaPage from './pages/AuditoriaPage.jsx'
import DemoPage from './pages/DemoPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import HistoricoPage from './pages/HistoricoPage.jsx'
import MonitoramentoPage from './pages/MonitoramentoPage.jsx'
import SegurancaPage from './pages/SegurancaPage.jsx'
import CelularSimuladoPage from './pages/CelularSimuladoPage.jsx'
import EmergenciaPage from './pages/EmergenciaPage.jsx'
import { EstadoProvider, useEstado } from './state/EstadoContext.jsx'
import { HistoricoProvider } from './state/HistoricoContext.jsx'
import { buscaDoReator } from './state/reatorSelecionado.js'
import { authApi } from './services/authApi.js'
import ConfirmacaoAlarme from './components/ConfirmacaoAlarme.jsx'
import {
  AtomLogo,
  IconGrid,
  IconActivity,
  IconThermometer,
  IconBell,
  IconShield,
  IconClock,
  IconPlay,
  IconPower,
  IconLock,
  IconAlertTriangle,
} from './components/icons.jsx'

const TABS = [
  { to: '/', end: true, text: 'Visão Geral', Icon: IconGrid },
  { to: '/monitoramento', text: 'Monitoramento', Icon: IconActivity },
  { to: '/sensores', text: 'Sensores', Icon: IconThermometer },
  { to: '/alarmes', text: 'Alarmes', Icon: IconBell },
  { to: '/auditoria', text: 'Auditoria', Icon: IconShield },
  { to: '/historico', text: 'Histórico', Icon: IconClock },
  { to: '/demo', text: 'Demo / Cenários', Icon: IconPlay },
  { to: '/emergencia', text: 'Emergência', Icon: IconAlertTriangle },
  { to: '/seguranca', text: 'Segurança', Icon: IconLock },
]

/** Fallback do selo quando a API não manda `reatores` (API antiga ou mock): status do R-01. */
const BADGE_TEXTO = {
  ESTAVEL: 'R-01 ● ESTÁVEL',
  ATENCAO: 'R-01 ● ATENÇÃO',
  CRITICO: 'R-01 ● CRÍTICO',
}

/**
 * Selo da usina: o pior nível entre os reatores e quantos estão nele. O topo não segue o filtro de
 * reator das abas — é o lugar que sempre mostra a usina inteira.
 */
function seloDaUsina(estado) {
  const reatores = estado.reatores ?? []
  if (reatores.length === 0) {
    return { texto: BADGE_TEXTO[estado.status] ?? estado.status, classe: BADGE_CLASSE[estado.status] ?? '' }
  }
  const conta = (nivel) => reatores.filter((r) => r.nivel === nivel).length
  const criticos = conta('CRITICO')
  const atencao = conta('ATENCAO')
  if (criticos > 0) {
    return {
      texto: `● ${criticos} CRÍTICO${atencao ? ` · ${atencao} ATENÇÃO` : ''}`,
      classe: ' crit',
    }
  }
  if (atencao > 0) return { texto: `● ${atencao} EM ATENÇÃO`, classe: ' warn' }
  return { texto: `● ${reatores.length} ESTÁVEIS`, classe: '' }
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
    return (
      <main className="auth-screen">
        <p className="auth-loading">Verificando sessão segura…</p>
      </main>
    )
  }

  if (usuario === null) {
    return (
      <LoginPage onAutenticado={setUsuario} />
    )
  }

  const podeAcessarCentral = usuario.papeis.some((papel) => PERFIS_DA_CENTRAL.has(papel))
  if (!podeAcessarCentral) {
    return (
      <main className="auth-screen">
        <section className="auth-card">
          <p className="scada-kicker">Acesso autenticado</p>
          <h1>Perfil sem acesso à Central</h1>
          <p>
            O perfil Guarda / Controle de Acesso é destinado ao UC04 e não possui acesso à
            supervisão operacional. Use a API de acesso ou autentique-se como Operador.
          </p>
          <button
            type="button"
            className="scada-btn scada-btn-ghost"
            onClick={async () => {
              await authApi.logout()
              setUsuario(null)
            }}
          >
            Sair
          </button>
        </section>
      </main>
    )
  }

  return (
    <EstadoProvider operadorNome={usuario.nome}>
      <HistoricoProvider>
        <BrowserRouter>
          <Shell
            usuario={usuario}
            onLogout={async () => {
              await authApi.logout()
              setUsuario(null)
            }}
          />
        </BrowserRouter>
      </HistoricoProvider>
    </EstadoProvider>
  )
}

function Shell({ usuario, onLogout }) {
  const { estado, origemMock, erroApi, reconhecerAlarme } = useEstado()
  const [acaoPendente, setAcaoPendente] = useState(null)
  const { search } = useLocation()
  const buscaMenu = buscaDoReator(search)
  const selo = seloDaUsina(estado)
  const alarmeAtivo = estado.alarmes.find((a) => a.status === 'ATIVO')

  const alarmeAtivoAgora = estado.alarmes.some((a) => a.status === 'ATIVO')
  const auditoriaOk = (estado.contadores?.auditoria ?? 0) >= 0 && !erroApi

  return (
    <div className="scada-shell">
      <header className="scada-top">
        <div className="scada-brand">
          <div className="brand-mark">
            <AtomLogo size={26} />
          </div>
          <div>
            <h1>Central de Supervisão · {estado.reatores?.length || 5} reatores</h1>
            <small style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
              <span
                className={`status-dot ${origemMock ? 'warn' : 'ok'}`}
                style={{ width: 6, height: 6 }}
              />
              EventBus {origemMock ? 'MOCK (API offline)' : 'ONLINE'}
            </small>
          </div>
        </div>
        <div className="scada-top-right">
          <div className="status-dots">
            <span
              className={`status-dot ${selo.classe.trim() || 'ok'}`}
              title="Reatores"
            />
            <span className={`status-dot ${alarmeAtivoAgora ? 'crit' : 'ok'}`} title="Alarmes" />
            <span className={`status-dot ${auditoriaOk ? 'ok' : 'warn'}`} title="Auditoria" />
            <span className="status-dot ok" title="Acesso" />
          </div>
          <div className="scada-user">
            <strong>{usuario.nome}</strong>
            <span>{usuario.papeis.map((papel) => papel.replaceAll('_', ' ')).join(' · ')}</span>
          </div>
          {origemMock && (
            <span className="scada-pill warn">MOCK · rode mvp/4-EXECUTAR-API-ESTADO.bat</span>
          )}
          <span className={`scada-badge${selo.classe}`} title="Nível dos reatores da usina">
            {selo.texto}
          </span>
          <button type="button" className="scada-btn scada-btn-ghost scada-logout" onClick={onLogout}>
            <IconPower size={13} />
            Sair
          </button>
        </div>
      </header>

      <nav className="scada-tabs">
        {TABS.map((tab) => (
          <NavLink
            key={tab.to}
            to={{ pathname: tab.to, search: buscaMenu }}
            end={tab.end}
            className={({ isActive }) => `tab2${isActive ? ' active' : ''}`}
          >
            <tab.Icon size={15} />
            {tab.text}
          </NavLink>
        ))}
      </nav>

      {erroApi && (
        <div className="scada-api-error" role="alert">
          Falha na API: {erroApi}
        </div>
      )}

      {alarmeAtivo && (
        <div className="scada-banner">
          <span>ALARME ATIVO — {alarmeAtivo.mensagem}</span>
          <button
            type="button"
            className="scada-btn scada-btn-amber"
            onClick={() => setAcaoPendente({ tipo: 'reconhecer', alarme: alarmeAtivo })}
          >
            Validar / Reconhecer
          </button>
        </div>
      )}

      <main className="scada-main">
        <Routes>
          <Route path="/" element={<OverviewPage />} />
          <Route path="/monitoramento" element={<MonitoramentoPage />} />
          <Route path="/sensores" element={<SensoresPage />} />
          <Route path="/sensores/:slug" element={<SensorDetailPage />} />
          <Route path="/alarmes" element={<AlarmesPage />} />
          <Route path="/auditoria" element={<AuditoriaPage />} />
          <Route path="/historico" element={<HistoricoPage />} />
          <Route path="/demo" element={<DemoPage />} />
          <Route path="/emergencia" element={<EmergenciaPage />} />
          <Route path="/seguranca" element={<SegurancaPage usuario={usuario} />} />
        </Routes>
      </main>
      <ConfirmacaoAlarme
        acao={acaoPendente}
        onCancelar={() => setAcaoPendente(null)}
        onConfirmar={() => {
          const alarme = acaoPendente.alarme
          setAcaoPendente(null)
          reconhecerAlarme(alarme.id)
        }}
      />
    </div>
  )
}

// RF18: /celular é o celular simulado da demonstração (fora da sessão da Central).
const ehCelularSimulado = window.location.pathname === '/celular'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    {ehCelularSimulado ? <CelularSimuladoPage /> : <App />}
  </React.StrictMode>,
)
