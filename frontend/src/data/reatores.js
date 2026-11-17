/**
 * Metadados do painel de monitoramento (5 reatores).
 *
 * A API manda os valores e a faixa segura; a faixa de atenção e a escala de exibição ficam aqui,
 * junto da meta de `sensores.js`, porque são decisões de tela.
 */

import { SENSORES_RF1 } from './sensores.js'

const ATENCAO_POR_TIPO = Object.fromEntries(
  SENSORES_RF1.map((s) => [s.tipo, { min: s.atencaoMin, max: s.atencaoMax }]),
)

/**
 * Escala vertical **fixa** por parâmetro, igual nos 5 reatores.
 *
 * Escala ajustada aos dados ampliaria ruído em um reator e achataria o pico de outro; com a mesma
 * escala, dá para comparar os blocos lado a lado. Cada escala inclui a faixa de atenção e o limite
 * seguro, para o pico aparecer contra as linhas de referência. Se um valor sair da escala, ela se
 * estende para mostrá-lo (ver `dominioY`).
 */
export const PARAMETROS = {
  RADIACAO: {
    rotulo: 'Radiação',
    unidade: 'mSv/h',
    cor: 'var(--purple)',
    casas: 2,
    escala: [0, 6],
    atencao: ATENCAO_POR_TIPO.RADIACAO,
  },
  TEMPERATURA: {
    rotulo: 'Temperatura do núcleo',
    unidade: '°C',
    cor: 'var(--crit)',
    casas: 1,
    escala: [280, 360],
    atencao: ATENCAO_POR_TIPO.TEMPERATURA,
  },
  PRESSAO: {
    rotulo: 'Pressão',
    unidade: 'bar',
    cor: 'var(--warn)',
    casas: 1,
    escala: [130, 170],
    atencao: ATENCAO_POR_TIPO.PRESSAO,
  },
  FLUXO_RESFRIAMENTO: {
    rotulo: 'Fluxo de resfriamento',
    unidade: 'm³/h',
    cor: 'var(--cyan)',
    casas: 0,
    escala: [400, 1600],
    atencao: ATENCAO_POR_TIPO.FLUXO_RESFRIAMENTO,
  },
}

/** Gráficos completos na visão geral — a radiação é a grandeza da lâmpada de nível crítico. */
export const PRINCIPAIS = ['RADIACAO', 'TEMPERATURA']
/** Na visão geral aparecem só com valor atual e tendência; completos ao maximizar. */
export const SECUNDARIOS = ['PRESSAO', 'FLUXO_RESFRIAMENTO']
export const TODOS_PARAMETROS = [...PRINCIPAIS, ...SECUNDARIOS]

export const NIVEL = {
  ESTAVEL: { texto: 'ESTÁVEL', classe: 'ok' },
  ATENCAO: { texto: 'ATENÇÃO', classe: 'warn' },
  CRITICO: { texto: 'CRÍTICO', classe: 'crit' },
}

/** "R-03" → "Reator-03". */
export function nomeReator(id) {
  return id?.startsWith('R-') ? `Reator-${id.slice(2)}` : id ?? 'Reator'
}

export function sensorDoTipo(reator, tipo) {
  return reator?.sensores?.find((s) => s.tipo === tipo)
}

/** Faixa do valor atual: fora do limite seguro, na faixa de atenção ou ideal. */
export function faixaDoSensor(sensor) {
  if (!sensor) return 'ok'
  if (sensor.valor < sensor.limiteMinimo || sensor.valor > sensor.limiteMaximo) return 'crit'
  if (sensor.emAtencao) return 'warn'
  return 'ok'
}

export function formatarValor(valor, tipo) {
  const casas = PARAMETROS[tipo]?.casas ?? 2
  return Number(valor).toLocaleString('pt-BR', {
    minimumFractionDigits: casas,
    maximumFractionDigits: casas,
  })
}

export function formatarHora(instante) {
  if (instante == null) return '—'
  return new Date(instante).toLocaleTimeString('pt-BR', { hour12: false })
}

/** Escala fixa do parâmetro, estendida se algum valor da série sair dela. */
export function dominioY(tipo, serie) {
  const [baseMin, baseMax] = PARAMETROS[tipo]?.escala ?? [0, 1]
  if (!serie?.length) return [baseMin, baseMax]
  const valores = serie.map(([, v]) => v)
  const min = Math.min(baseMin, ...valores)
  const max = Math.max(baseMax, ...valores)
  const folga = min < baseMin || max > baseMax ? (max - min) * 0.05 : 0
  return [min - (min < baseMin ? folga : 0), max + (max > baseMax ? folga : 0)]
}
