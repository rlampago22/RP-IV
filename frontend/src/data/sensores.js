/**
 * Meta RF-1 compartilhada T01/T02 — faixas de atenção + fallback offline.
 * Valores live/histórico vêm de `/api/estado` (EstadoContext). Seeds: CenariosMedicao.
 */

export const SENSORES_RF1 = [
  {
    id: 'T-CORE-01',
    sensorId: 1,
    tipo: 'TEMPERATURA',
    unidade: '°C',
    valor: 310.5,
    min: 0,
    max: 350,
    atencaoMin: 20,
    atencaoMax: 325,
    historico: [308.0, 309.0, 309.5, 310.0, 310.2, 310.5],
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
    historico: [152.0, 153.0, 153.5, 154.0, 154.5, 155.0],
  },
  {
    id: 'R-CONT-01',
    sensorId: 3,
    tipo: 'RADIACAO',
    unidade: 'mSv/h',
    valor: 2.4,
    min: 0,
    max: 5,
    atencaoMin: 0.1,
    atencaoMax: 4.2,
    historico: [2.0, 2.1, 2.2, 2.3, 2.35, 2.4],
  },
  {
    id: 'F-COOL-01',
    sensorId: 4,
    tipo: 'FLUXO',
    unidade: 'm³/h',
    valor: 1100,
    min: 500,
    max: 1500,
    atencaoMin: 600,
    atencaoMax: 1400,
    historico: [1080, 1090, 1095, 1100, 1100, 1100],
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
