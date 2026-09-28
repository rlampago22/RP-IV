/**
 * Cliente da API de estado (mvp/src/.../apiestado) — contrato documentado em
 * docs/api-estado-contrato.md. Único ponto do frontend que conhece a forma do
 * estado derivado do EventBus; páginas consomem via EstadoContext (não direto).
 *
 * Formato de EstadoSnapshot (igual ao stub Java):
 * {
 *   status: 'ESTAVEL' | 'ATENCAO' | 'CRITICO',
 *   tempoReal: boolean,
 *   atualizadoEm: string (ISO-8601),
 *   sensores: { id, tipo, unidade, valor, limiteMinimo, limiteMaximo, atualizadoEm }[],
 *   contadores: { medicoes, alarmes, auditoria },
 *   alarmes: { id, sensorId, severidade, mensagem, status, criadoEm, operador, solucao }[],
 *   eventos: { ocorridoEm, tipo, resumo }[],
 * }
 */

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

/** Mesmo contrato do backend — usado quando a API Java não está rodando (demo offline). */
export const ESTADO_MOCK = {
  status: 'ESTAVEL',
  tempoReal: false,
  atualizadoEm: new Date().toISOString(),
  sensores: [
    { id: 1, tipo: 'TEMPERATURA', unidade: 'Celsius', valor: 310.5, limiteMinimo: 0, limiteMaximo: 350, atualizadoEm: new Date().toISOString() },
    { id: 2, tipo: 'PRESSAO', unidade: 'bar', valor: 155.0, limiteMinimo: 0, limiteMaximo: 160, atualizadoEm: new Date().toISOString() },
    { id: 3, tipo: 'RADIACAO', unidade: 'mSv/h', valor: 2.4, limiteMinimo: 0, limiteMaximo: 5, atualizadoEm: new Date().toISOString() },
    { id: 4, tipo: 'FLUXO_RESFRIAMENTO', unidade: 'm3/h', valor: 1100.0, limiteMinimo: 500, limiteMaximo: 1500, atualizadoEm: new Date().toISOString() },
  ],
  contadores: { medicoes: 4, alarmes: 0, auditoria: 4 },
  alarmes: [],
  eventos: [
    { ocorridoEm: new Date().toISOString(), tipo: 'MEDICAO_REGISTRADA', resumo: 'sensor=4 tipo=FLUXO_RESFRIAMENTO valor=1100.00 m3/h (mock local)' },
  ],
}

async function chamar(caminho, opcoes) {
  const resposta = await fetch(`${API_BASE_URL}${caminho}`, opcoes)
  if (!resposta.ok) {
    throw new Error(`API estado respondeu ${resposta.status} em ${caminho}`)
  }
  return resposta.json()
}

export function buscarEstado() {
  return chamar('/api/estado')
}

export function iniciarTempoReal() {
  return chamar('/api/tempo-real/iniciar', { method: 'POST' })
}

export function pausarTempoReal() {
  return chamar('/api/tempo-real/pausar', { method: 'POST' })
}

export function reconhecerAlarme(alarmeId) {
  return chamar(`/api/alarmes/${encodeURIComponent(alarmeId)}/reconhecer`, { method: 'POST' })
}

export function resolverAlarme(alarmeId) {
  return chamar(`/api/alarmes/${encodeURIComponent(alarmeId)}/resolver`, { method: 'POST' })
}
