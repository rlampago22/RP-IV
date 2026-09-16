/**
 * Contrato compartilhado T01/T02 — sensores RF-1 + histórico curto (mock).
 * Espelha o que `ReatorFacade` / `ReatorRepository` expõem no MVP Java.
 * Quando a API do Bruno (#44) existir, trocar a origem destes dados.
 */

export const SENSORES_RF1 = [
  {
    id: 'T-CORE-01',
    sensorId: 1,
    tipo: 'TEMPERATURA',
    unidade: '°C',
    valor: 312.0,
    min: 0,
    max: 350,
    atencaoMin: 20,
    atencaoMax: 325,
    historico: [308.2, 309.5, 310.1, 311.0, 311.4, 312.0],
  },
  {
    id: 'P-PRIM-01',
    sensorId: 2,
    tipo: 'PRESSAO',
    unidade: 'bar',
    valor: 155.0,
    min: 0,
    max: 160,
    atencaoMin: 10,
    atencaoMax: 155,
    historico: [152.0, 153.2, 154.0, 154.5, 154.8, 155.0],
  },
  {
    id: 'R-CONT-01',
    sensorId: 3,
    tipo: 'RADIACAO',
    unidade: 'mSv/h',
    valor: 0.12,
    min: 0,
    max: 5,
    atencaoMin: 0.1,
    atencaoMax: 4.2,
    historico: [0.08, 0.09, 0.1, 0.11, 0.115, 0.12],
  },
  {
    id: 'F-COOL-01',
    sensorId: 4,
    tipo: 'FLUXO',
    unidade: 'm³/h',
    valor: 980,
    min: 500,
    max: 1500,
    atencaoMin: 600,
    atencaoMax: 1400,
    historico: [960, 970, 975, 978, 982, 980],
  },
]

export function statusSensor(s) {
  if (s.valor < s.min || s.valor > s.max) return { label: 'CRÍTICO', cls: 'crit' }
  if (s.valor < s.atencaoMin || s.valor > s.atencaoMax) return { label: 'Atenção', cls: 'warn' }
  return { label: 'OK', cls: 'ok' }
}

export function pctBarra(s) {
  const span = s.max - s.min || 1
  return Math.max(0, Math.min(100, ((s.valor - s.min) / span) * 100))
}

export function corTipo(tipo) {
  switch (tipo) {
    case 'TEMPERATURA':
      return 'var(--crit)'
    case 'PRESSAO':
      return 'var(--warn)'
    case 'RADIACAO':
      return 'var(--purple)'
    default:
      return 'var(--cyan)'
  }
}
