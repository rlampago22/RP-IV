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
 *   sensores: { id, tipo, unidade, valor, limiteMinimo, limiteMaximo, atualizadoEm, historico }[],
 *   contadores: { medicoes, alarmes, auditoria },
 *   alarmes: { id, sensorId, severidade, mensagem, status, criadoEm, operador, solucao }[],
 *   eventos: { ocorridoEm, tipo, resumo }[],
 * }
 */

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const agora = () => new Date().toISOString()

/** Mesmo contrato do backend — usado quando a API Java não está rodando (demo offline). */
export const ESTADO_MOCK = {
  status: 'ESTAVEL',
  tempoReal: false,
  atualizadoEm: agora(),
  sensores: [
    {
      id: 1,
      tipo: 'TEMPERATURA',
      unidade: 'Celsius',
      valor: 310.5,
      limiteMinimo: 0,
      limiteMaximo: 350,
      atualizadoEm: agora(),
      historico: [308.0, 309.0, 309.5, 310.0, 310.2, 310.5],
    },
    {
      id: 2,
      tipo: 'PRESSAO',
      unidade: 'bar',
      valor: 155.0,
      limiteMinimo: 0,
      limiteMaximo: 160,
      atualizadoEm: agora(),
      historico: [152.0, 153.0, 153.5, 154.0, 154.5, 155.0],
    },
    {
      id: 3,
      tipo: 'RADIACAO',
      unidade: 'mSv/h',
      valor: 2.4,
      limiteMinimo: 0,
      limiteMaximo: 5,
      atualizadoEm: agora(),
      historico: [2.0, 2.1, 2.2, 2.3, 2.35, 2.4],
    },
    {
      id: 4,
      tipo: 'FLUXO_RESFRIAMENTO',
      unidade: 'm3/h',
      valor: 1100.0,
      limiteMinimo: 500,
      limiteMaximo: 1500,
      atualizadoEm: agora(),
      historico: [1080.0, 1090.0, 1095.0, 1100.0, 1100.0, 1100.0],
    },
  ],
  contadores: { medicoes: 24, alarmes: 0, auditoria: 24 },
  alarmes: [],
  eventos: [
    {
      ocorridoEm: agora(),
      tipo: 'MEDICAO_REGISTRADA',
      resumo: 'sensor=4 tipo=FLUXO_RESFRIAMENTO valor=1100.00 m3/h (mock local)',
    },
  ],
}

/** Seeds offline — mesmos valores de CenariosMedicao.java (demo sem API). */
const MOCK_NORMAL = {
  status: 'ESTAVEL',
  sensores: [
    { id: 1, tipo: 'TEMPERATURA', unidade: 'Celsius', valor: 310.5, limiteMinimo: 0, limiteMaximo: 350, historico: [308.0, 309.0, 309.5, 310.0, 310.2, 310.5] },
    { id: 2, tipo: 'PRESSAO', unidade: 'bar', valor: 155.0, limiteMinimo: 0, limiteMaximo: 160, historico: [152.0, 153.0, 153.5, 154.0, 154.5, 155.0] },
    { id: 3, tipo: 'RADIACAO', unidade: 'mSv/h', valor: 2.4, limiteMinimo: 0, limiteMaximo: 5, historico: [2.0, 2.1, 2.2, 2.3, 2.35, 2.4] },
    { id: 4, tipo: 'FLUXO_RESFRIAMENTO', unidade: 'm3/h', valor: 1100.0, limiteMinimo: 500, limiteMaximo: 1500, historico: [1080.0, 1090.0, 1095.0, 1100.0, 1100.0, 1100.0] },
  ],
}

const MOCK_OBSERVACAO = {
  status: 'ATENCAO',
  sensores: [
    { id: 1, tipo: 'TEMPERATURA', unidade: 'Celsius', valor: 328.0, limiteMinimo: 0, limiteMaximo: 350, historico: [309.0, 309.5, 310.0, 310.2, 310.5, 328.0] },
    { id: 2, tipo: 'PRESSAO', unidade: 'bar', valor: 155.0, limiteMinimo: 0, limiteMaximo: 160, historico: [153.0, 153.5, 154.0, 154.5, 155.0, 155.0] },
    { id: 3, tipo: 'RADIACAO', unidade: 'mSv/h', valor: 2.4, limiteMinimo: 0, limiteMaximo: 5, historico: [2.1, 2.2, 2.3, 2.35, 2.4, 2.4] },
    { id: 4, tipo: 'FLUXO_RESFRIAMENTO', unidade: 'm3/h', valor: 1100.0, limiteMinimo: 500, limiteMaximo: 1500, historico: [1090.0, 1095.0, 1100.0, 1100.0, 1100.0, 1100.0] },
  ],
}

function aplicarSeedMock(base) {
  const ts = agora()
  return {
    ...ESTADO_MOCK,
    status: base.status,
    tempoReal: false,
    atualizadoEm: ts,
    sensores: base.sensores.map((s) => ({ ...s, atualizadoEm: ts })),
    eventos: [
      {
        ocorridoEm: ts,
        tipo: base.status === 'ATENCAO' ? 'OBSERVACAO_REGISTRADA' : 'MEDICAO_REGISTRADA',
        resumo: `seed mock ${base.status === 'ATENCAO' ? 'observacao' : 'normal'}`,
      },
      ...ESTADO_MOCK.eventos,
    ],
  }
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

/** Seed Normal (S4 #50) — valores fixos via ReatorFacade.receberLeitura. */
export function aplicarCenarioNormal() {
  return chamar('/api/cenarios/normal', { method: 'POST' })
}

/** Seed Observação (Alt. 1 UC01) — temperatura 328 °C → ObservacaoRegistrada. */
export function aplicarCenarioObservacao() {
  return chamar('/api/cenarios/observacao', { method: 'POST' })
}

/** Fallback offline para botões de seed na T02. */
export function estadoMockNormal() {
  return aplicarSeedMock(MOCK_NORMAL)
}

export function estadoMockObservacao() {
  return aplicarSeedMock(MOCK_OBSERVACAO)
}

export function reconhecerAlarme(alarmeId) {
  return chamar(`/api/alarmes/${encodeURIComponent(alarmeId)}/reconhecer`, { method: 'POST' })
}

export function resolverAlarme(alarmeId) {
  return chamar(`/api/alarmes/${encodeURIComponent(alarmeId)}/resolver`, { method: 'POST' })
}
