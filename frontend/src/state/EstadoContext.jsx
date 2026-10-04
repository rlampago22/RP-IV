import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import {
  ESTADO_MOCK,
  buscarEstado,
  iniciarTempoReal,
  pausarTempoReal,
  aplicarCenarioNormal,
  aplicarCenarioObservacao,
  estadoMockNormal,
  estadoMockObservacao,
  reconhecerAlarme,
  resolverAlarme,
} from '../api/estado.js'

const POLL_MS = 3000

const EstadoContext = createContext(null)

export function EstadoProvider({ children }) {
  const [estado, setEstado] = useState(ESTADO_MOCK)
  const [origemMock, setOrigemMock] = useState(true)
  const emVooRef = useRef(false)

  const atualizar = useCallback(async () => {
    if (emVooRef.current) return
    emVooRef.current = true
    try {
      const dados = await buscarEstado()
      setEstado(dados)
      setOrigemMock(false)
    } catch {
      // API Java (mvp/4-EXECUTAR-API-ESTADO.bat) indisponível — mantém T01/T02 usável com o mock documentado.
      setOrigemMock(true)
    } finally {
      emVooRef.current = false
    }
  }, [])

  useEffect(() => {
    atualizar()
    const intervalo = setInterval(atualizar, POLL_MS)
    return () => clearInterval(intervalo)
  }, [atualizar])

  const executarAcao = useCallback(async (acao, fallbackMock) => {
    try {
      const dados = await acao()
      setEstado(dados)
      setOrigemMock(false)
    } catch {
      if (fallbackMock) {
        setEstado(fallbackMock())
        setOrigemMock(true)
      }
    }
  }, [])

  const valor = {
    estado,
    origemMock,
    iniciarTempoReal: () => executarAcao(iniciarTempoReal),
    pausarTempoReal: () => executarAcao(pausarTempoReal),
    aplicarCenarioNormal: () => executarAcao(aplicarCenarioNormal, estadoMockNormal),
    aplicarCenarioObservacao: () => executarAcao(aplicarCenarioObservacao, estadoMockObservacao),
    reconhecerAlarme: (alarmeId) => executarAcao(() => reconhecerAlarme(alarmeId)),
    resolverAlarme: (alarmeId) => executarAcao(() => resolverAlarme(alarmeId)),
  }

  return <EstadoContext.Provider value={valor}>{children}</EstadoContext.Provider>
}

export function useEstado() {
  const contexto = useContext(EstadoContext)
  if (!contexto) {
    throw new Error('useEstado precisa estar dentro de <EstadoProvider>')
  }
  return contexto
}
