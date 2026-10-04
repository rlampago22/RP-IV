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
  simularAnomalia,
  reconhecerAlarme,
  resolverAlarme,
  dispararCenario,
} from '../api/estado.js'

const POLL_MS = 3000

const EstadoContext = createContext(null)

export function EstadoProvider({ children }) {
  const [estado, setEstado] = useState(ESTADO_MOCK)
  const [origemMock, setOrigemMock] = useState(true)
  const [erroApi, setErroApi] = useState('')
  const emVooRef = useRef(false)

  const atualizar = useCallback(async () => {
    if (emVooRef.current) return
    emVooRef.current = true
    try {
      const dados = await buscarEstado()
      setEstado(dados)
      setOrigemMock(false)
      setErroApi('')
    } catch (erro) {
      // API Java (mvp/4-EXECUTAR-API-ESTADO.bat) indisponível — mantém T01/T02 usável com o mock documentado.
      setOrigemMock(true)
      setErroApi(erro?.message || 'API indisponivel')
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
    if (emVooRef.current) return false
    emVooRef.current = true
    try {
      const dados = await acao()
      setEstado(dados)
      setOrigemMock(false)
      setErroApi('')
      return true
    } catch (erro) {
      if (fallbackMock) {
        setEstado(fallbackMock())
        setOrigemMock(true)
        setErroApi('')
        return true
      }
      setErroApi(erro?.message || 'Falha na acao')
      return false
    } finally {
      emVooRef.current = false
    }
  }, [])

  const valor = {
    estado,
    origemMock,
    erroApi,
    iniciarTempoReal: () => executarAcao(iniciarTempoReal),
    pausarTempoReal: () => executarAcao(pausarTempoReal),
    aplicarCenarioNormal: () => executarAcao(aplicarCenarioNormal, estadoMockNormal),
    aplicarCenarioObservacao: () => executarAcao(aplicarCenarioObservacao, estadoMockObservacao),
    simularAnomalia: () => executarAcao(simularAnomalia),
    reconhecerAlarme: (alarmeId) => executarAcao(() => reconhecerAlarme(alarmeId)),
    resolverAlarme: (alarmeId) => executarAcao(() => resolverAlarme(alarmeId)),
    dispararCenario: (cenario) => executarAcao(() => dispararCenario(cenario)),
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
