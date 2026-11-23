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
 * Produtores (issue #96, pedido do professor em 16/11): dado rotineiro por
 * polling, dado crítico por push. `produzirEstado` busca `/api/estado` a cada
 * INTERVALO_POLLING_MS e, em paralelo, mantém aberto o SSE da Central, por onde
 * o núcleo empurra na hora só os eventos críticos (leitura fora da faixa
 * segura, falha de sensor, contingência). Os dois publicam no mesmo sujeito,
 * que descarta estado repetido — então as telas continuam com o mesmo contrato
 * assinar()/update() e não sabem por qual caminho o dado chegou.
 */
const estadoObservavel = new EstadoObservavel()

const EstadoContext = createContext(null)

let produtorIniciado = false
let assinantesAtivos = 0

/** Intervalo do polling do dado rotineiro. Os reatores leem a cada 2 a 8 s. */
export const INTERVALO_POLLING_MS = 2000

/**
 * Conta as ações do operador. Uma consulta do polling que saiu antes de uma
 * ação e voltou depois dela traz estado velho; com o contador, ela é descartada
 * em vez de desfazer na tela o resultado da ação por até um ciclo.
 */
let versaoAcoes = 0

/**
 * Produtor do sujeito: polling de `/api/estado` (rotina) + SSE da Central
 * (crítico, imediato).
 *
 * `aoResultado(null)` = API respondeu; `aoResultado(mensagem)` = API fora.
 * Falha só do SSE não derruba a tela para o mock: os dados continuam chegando
 * pelo polling, e um alarme chega no máximo um ciclo depois. Fica um aviso no
 * console (uma vez) para quem estiver depurando.
 *
 * Devolve a função que encerra os dois.
 */
function produzirEstado(aoResultado) {
  let consultaEmAndamento = false
  let avisouSse = false

  const consultar = async () => {
    if (consultaEmAndamento) return
    consultaEmAndamento = true
    const versao = versaoAcoes
    try {
      const dados = await buscarEstado()
      if (versao === versaoAcoes) {
        estadoObservavel.publicar(dados)
      }
      aoResultado(null)
    } catch (erro) {
      aoResultado(erro?.message || 'API indisponivel')
    } finally {
      consultaEmAndamento = false
    }
  }

  consultar()
  const intervalo = setInterval(consultar, INTERVALO_POLLING_MS)

  const fecharSse = conectarEventos(
    (dados) => {
      estadoObservavel.publicar(dados)
      aoResultado(null)
    },
    (erro) => {
      if (!avisouSse) {
        avisouSse = true
        console.warn(
          `[push] ${erro?.message || 'Canal de eventos indisponivel'} — alarmes chegam pelo polling (${INTERVALO_POLLING_MS} ms).`,
        )
      }
    },
  )

  return () => {
    clearInterval(intervalo)
    fecharSse()
  }
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
      pararProdutor = produzirEstado((mensagemErro) => {
        if (mensagemErro) {
          setOrigemMock(true)
          setErroApi(mensagemErro)
        } else {
          // API voltou: sai do mock mesmo que o estado não tenha mudado
          // (o sujeito não notifica estado repetido).
          setOrigemMock(false)
          setErroApi('')
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
    // Se outra ação estiver em andamento, espera terminar em vez de descartar o clique
    // (antes o botão falhava com "confira se a API está rodando" mesmo com a API no ar).
    for (let tentativa = 0; emVooRef.current && tentativa < 60; tentativa++) {
      await new Promise((resolve) => setTimeout(resolve, 50))
    }
    if (emVooRef.current) return false
    emVooRef.current = true
    versaoAcoes += 1
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
        setErroApi(erro?.message || 'API indisponivel')
        return true
      }
      setErroApi(erro?.message || 'Falha na acao')
      if (erro?.travadoPorContingencia) {
        buscarEstado().then((dados) => estadoObservavel.publicar(dados)).catch(() => {})
      }
      return false
    } finally {
      // De novo no fim: uma consulta que saiu DURANTE a ação também pode trazer estado de antes dela.
      versaoAcoes += 1
      emVooRef.current = false
    }
  }, [])

  const valor = {
    estado,
    origemMock,
    erroApi,
    iniciarTempoReal: () => executarAcao(iniciarTempoReal),
    pausarTempoReal: () => executarAcao(pausarTempoReal),
    aplicarCenarioNormal: (reatorId) =>
      executarAcao(() => aplicarCenarioNormal(reatorId), estadoMockNormal),
    aplicarCenarioObservacao: (reatorId) =>
      executarAcao(() => aplicarCenarioObservacao(reatorId), estadoMockObservacao),
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
    dispararCenario: (cenario, reatorId) => executarAcao(() => dispararCenario(cenario, reatorId)),
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
