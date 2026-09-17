import { createContext, useContext, useMemo, useState } from 'react'
import {
  ALARMES_INICIAIS,
  alarmeBanner,
  plantStatusFromAlarmes,
} from '../data/alarmes.js'

const PlantContext = createContext(null)

export function PlantProvider({ children }) {
  const [alarmes, setAlarmes] = useState(ALARMES_INICIAIS)
  const [auditExtra, setAuditExtra] = useState([])

  const status = useMemo(() => plantStatusFromAlarmes(alarmes), [alarmes])
  const banner = useMemo(() => alarmeBanner(alarmes), [alarmes])

  function reconhecer(alarmeId) {
    setAlarmes((prev) =>
      prev.map((a) => {
        if (alarmeId && a.id !== alarmeId) return a
        if (a.estado !== 'ATIVO') return a
        return { ...a, estado: 'RECONHECIDO' }
      })
    )
    setAuditExtra((prev) => [
      {
        id: `x-${Date.now()}`,
        evento: 'ALARME_RECONHECIDO',
        detalhe: alarmeId || 'lote',
        hora: new Date().toLocaleTimeString('pt-BR', { hour12: false }),
      },
      ...prev,
    ])
  }

  function resolver(alarmeId) {
    setAlarmes((prev) =>
      prev.map((a) => {
        if (alarmeId && a.id !== alarmeId) return a
        if (a.estado === 'RESOLVIDO') return a
        return { ...a, estado: 'RESOLVIDO' }
      })
    )
    setAuditExtra((prev) => [
      {
        id: `x-${Date.now()}`,
        evento: 'ALARME_RESOLVIDO',
        detalhe: alarmeId || 'lote',
        hora: new Date().toLocaleTimeString('pt-BR', { hour12: false }),
      },
      ...prev,
    ])
  }

  const value = {
    alarmes,
    status,
    banner,
    auditExtra,
    reconhecer,
    resolver,
  }

  return <PlantContext.Provider value={value}>{children}</PlantContext.Provider>
}

export function usePlant() {
  const ctx = useContext(PlantContext)
  if (!ctx) throw new Error('usePlant deve ser usado dentro de PlantProvider')
  return ctx
}
