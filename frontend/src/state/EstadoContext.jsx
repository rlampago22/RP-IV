import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import {
  ESTADO_MOCK,
  buscarEstado,
  iniciarTempoReal,
  pausarTempoReal,
  simularAnomalia,
  reconhecerAlarme,
  resolverAlarme,
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
      // API Java (mvp/4-EXECUTAR-API-ESTADO.bat) indisponível — mantém T01 usável com o mock documentado.
      setOrigemMock(true)
      setErroApi(erro.message)
    } finally {
      emVooRef.current = false
    }
  }, [])

  useEffect(() => {
    atualizar()
    const intervalo = setInterval(atualizar, POLL_MS)
    return () => clearInterval(intervalo)
  }, [atualizar])

  const executarAcao = useCallback(async (acao) => {
    if (emVooRef.current) return false
    emVooRef.current = true
    try {
      const dados = await acao()
      setEstado(dados)
      setOrigemMock(false)
      setErroApi('')
      return true
    } catch (erro) {
      setErroApi(erro.message)
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
    simularAnomalia: () => executarAcao(simularAnomalia),
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
