/**
 * Cliente da consulta e exportação do histórico operacional (RF16/RF17).
 * Contrato: docs/api-estado-contrato.md, seção 5.
 *
 * Diferente de `estado.js`, aqui não há polling nem mock de fallback: é consulta sob demanda
 * sobre dado persistido. Sem a API no ar não existe histórico para mostrar, e inventar um
 * faria a tela afirmar registros de operação que nunca aconteceram.
 */

import { API_BASE_URL } from './estado.js'

/**
 * Converte `<input type="date">` (AAAA-MM-DD) para instante ISO-8601.
 *
 * O construtor `new Date('2026-10-05')` interpreta como meia-noite **UTC**, o que em São Paulo
 * é 21h do dia anterior — jogaria o filtro para o dia errado. Por isso montamos a data no fuso
 * local explicitamente.
 */
function instanteDoDiaLocal(valorData, fimDoDia) {
  if (!valorData) return null
  const [ano, mes, dia] = valorData.split('-').map(Number)
  if (!ano || !mes || !dia) return null
  const data = fimDoDia
    ? new Date(ano, mes - 1, dia, 23, 59, 59, 999)
    : new Date(ano, mes - 1, dia, 0, 0, 0, 0)
  return data.toISOString()
}

function queryPeriodo({ de, ate, operador }) {
  const parametros = new URLSearchParams()
  const inicio = instanteDoDiaLocal(de, false)
  const fim = instanteDoDiaLocal(ate, true)
  if (inicio) parametros.set('de', inicio)
  if (fim) parametros.set('ate', fim)
  if (operador) parametros.set('operador', operador)
  return parametros
}

/** Primeiro nível: saídas de operação (dia local + operador). */
export async function buscarDias(filtroPeriodo) {
  const resposta = await fetch(`${API_BASE_URL}/api/historico/dias?${queryPeriodo(filtroPeriodo)}`)
  const corpo = await resposta.json().catch(() => ({}))
  if (!resposta.ok) {
    throw new Error(corpo.erro ?? `A API respondeu ${resposta.status}.`)
  }
  return corpo
}

/**
 * Query do segundo nível: a saída escolhida (recorte exato de instantes) mais as faixas dos
 * parâmetros. Usa `primeiroRegistro`/`ultimoRegistro` da saída em vez do dia inteiro, para o
 * detalhe não invadir o turno do operador seguinte.
 */
function queryDetalhe(saida, faixas) {
  const parametros = new URLSearchParams()
  parametros.set('de', saida.primeiroRegistro)
  parametros.set('ate', saida.ultimoRegistro)
  if (saida.operador) parametros.set('operador', saida.operador)

  for (const campo of ['temperatura', 'pressao', 'radiacao', 'fluxo']) {
    const min = faixas[`${campo}Min`]
    const max = faixas[`${campo}Max`]
    if (min !== '' && min != null) parametros.set(`${campo}Min`, min)
    if (max !== '' && max != null) parametros.set(`${campo}Max`, max)
  }
  return parametros
}

export async function buscarDetalhe(saida, faixas) {
  const resposta = await fetch(`${API_BASE_URL}/api/historico?${queryDetalhe(saida, faixas)}`)
  const corpo = await resposta.json().catch(() => ({}))
  if (!resposta.ok) {
    throw new Error(corpo.erro ?? `A API respondeu ${resposta.status}.`)
  }
  return corpo
}

/** URL de download — o navegador baixa pelo Content-Disposition da resposta. */
export function urlExportacao(saida, faixas) {
  return `${API_BASE_URL}/api/historico/exportar?${queryDetalhe(saida, faixas)}`
}

export const PERIODO_VAZIO = { de: '', ate: '', operador: '' }

export const FAIXAS_VAZIAS = {
  temperaturaMin: '',
  temperaturaMax: '',
  pressaoMin: '',
  pressaoMax: '',
  radiacaoMin: '',
  radiacaoMax: '',
  fluxoMin: '',
  fluxoMax: '',
}
