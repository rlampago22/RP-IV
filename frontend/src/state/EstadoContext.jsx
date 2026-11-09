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
  solicitarAcesso,
} from '../api/estado.js'
import { conectarEventos } from '../api/eventos.js'
import { estadoObservavel } from './estadoObservavel.js'

const EstadoContext = createContext(null)

export function EstadoProvider({ children, operadorNome = 'Operador de Reator (UI Web)' }) {
  const [estado, setEstado] = useState(ESTADO_MOCK)
  const [origemMock, setOrigemMock] = useState(true)
  const [erroApi, setErroApi] = useState('')
  const emVooRef = useRef(false)
  const operadorRef = useRef(operadorNome)
  operadorRef.current = operadorNome

  const atualizar = useCallback(async () => {
    if (emVooRef.current) return
    emVooRef.current = true
    try {
      const dados = await buscarEstado()
      estadoObservavel.publicar(dados)
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

  // Observador: o provider só escuta o sujeito, não sabe de onde o estado vem.
  useEffect(() => estadoObservavel.assinar(setEstado), [])

  // Produtor: push por SSE no lugar do polling. O EventSource reconecta sozinho,
  // então aqui não há temporizador nenhum — o estado chega quando o evento acontece.
  useEffect(() => {
    atualizar() // primeira carga: deixa a tela usável mesmo antes do primeiro evento
    return conectarEventos(
      (dados) => {
        estadoObservavel.publicar(dados)
        setOrigemMock(false)
        setErroApi('')
      },
      (erro) => {
        setOrigemMock(true)
        setErroApi(erro?.message || 'Fluxo de eventos indisponivel')
      },
    )
  }, [atualizar])

  const executarAcao = useCallback(async (acao, fallbackMock) => {
    if (emVooRef.current) return false
    emVooRef.current = true
    try {
      const dados = await acao()
      estadoObservavel.publicar(dados)
      setOrigemMock(false)
      setErroApi('')
      return true
    } catch (erro) {
      if (fallbackMock) {
        estadoObservavel.publicar(fallbackMock())
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
    reconhecerAlarme: (alarmeId) =>
      executarAcao(() =>
        reconhecerAlarme(alarmeId, {
          operador: operadorRef.current,
          justificativa: 'Reconhecido via Central autenticada',
        }),
      ),
    resolverAlarme: (alarmeId) =>
      executarAcao(() =>
        resolverAlarme(alarmeId, {
          operador: operadorRef.current,
          solucao: 'Parametros normalizados via Central autenticada',
        }),
      ),
    dispararCenario: (cenario) => executarAcao(() => dispararCenario(cenario)),
    solicitarAcesso: async (credencial, area) => {
      try {
        const resultado = await solicitarAcesso(credencial, area)
        setErroApi('')
        return resultado
      } catch (erro) {
        setErroApi(erro?.message || 'Falha no registro de acesso')
        return null
      }
    },
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
