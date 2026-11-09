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
import { EstadoObservavel } from './EstadoObservavel.js'

/**
 * Sujeito observável único da aplicação (Observer V1 — front-end, pedido do
 * professor em 04/11/2026). Vive fora do componente de propósito: é "o
 * objeto observado" da UI, não um estado de uma tela específica — cada
 * <EstadoProvider> apenas se assina nele.
 *
 * V2: quem publica aqui é o push do back-end (`produzirPorPush`), via SSE da
 * Central. Saiu a leitura periódica da V1 — o estado chega quando o evento
 * acontece, não de 3 em 3 segundos. Como previsto na V1, só o produtor mudou:
 * o contrato assinar()/update() que as telas usam permanece o mesmo.
 */
const estadoObservavel = new EstadoObservavel()

const EstadoContext = createContext(null)

let produtorIniciado = false
let assinantesAtivos = 0

/**
 * Produtor V2: abre o fluxo SSE da Central e publica cada mensagem no sujeito.
 *
 * A primeira carga ainda é um fetch, para a tela não ficar vazia esperando o
 * próximo evento. Daí em diante não há temporizador nenhum — o EventSource
 * reconecta sozinho se a conexão cair.
 *
 * Devolve a função que encerra o fluxo.
 */
function produzirPorPush(aoFalhar) {
  buscarEstado()
    .then((dados) => {
      estadoObservavel.publicar(dados)
      aoFalhar(null)
    })
    .catch((erro) => aoFalhar(erro?.message || 'API indisponivel'))

  return conectarEventos(
    (dados) => {
      estadoObservavel.publicar(dados)
      aoFalhar(null)
    },
    (erro) => aoFalhar(erro?.message || 'Canal de eventos indisponivel'),
  )
}

export function EstadoProvider({ children, operadorNome = 'Operador de Reator (UI Web)' }) {
  const [estado, setEstado] = useState(ESTADO_MOCK)
  const [origemMock, setOrigemMock] = useState(true)
  const [erroApi, setErroApi] = useState('')
  const emVooRef = useRef(false)
  const operadorRef = useRef(operadorNome)
  operadorRef.current = operadorNome

  useEffect(() => {
    // Esta tela se registra como observadora do sujeito — update() só roda
    // quando o dado publicado muda, nunca em timer próprio desta tela.
    const observador = {
      update(novoEstado) {
        setEstado(novoEstado)
        setOrigemMock(false)
        setErroApi('')
      },
    }
    const cancelarAssinatura = estadoObservavel.assinar(observador)

    assinantesAtivos += 1
    let pararProdutor = null
    if (!produtorIniciado) {
      produtorIniciado = true
      pararProdutor = produzirPorPush((mensagemErro) => {
        if (mensagemErro) {
          setOrigemMock(true)
          setErroApi(mensagemErro)
        }
      })
    }

    return () => {
      cancelarAssinatura()
      assinantesAtivos -= 1
      if (assinantesAtivos <= 0 && pararProdutor) {
        pararProdutor()
        produtorIniciado = false
      }
    }
  }, [])

  const executarAcao = useCallback(async (acao, fallbackMock) => {
    if (emVooRef.current) return false
    emVooRef.current = true
    try {
      const dados = await acao()
      // Ação do operador também passa pelo sujeito único — mesma garantia de
      // "só notifica quem mudou" vale para resultado de ação quanto para leitura.
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
