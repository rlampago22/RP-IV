/**
 * Contrato mock T03 — alarmes RF-2 / UC01.
 * Espelha AlarmeFacade + Strategy (AvaliadorLimiar) do mvp/.
 * Severidades: ALTO (crit) · MANUT (warn) · OBS (purple).
 */

export const ALARMES_INICIAIS = [
  {
    id: 'alm-1',
    severidade: 'ALTO',
    codigo: 'T-CORE-01',
    mensagem: 'Temperatura acima do limiar',
    estado: 'ATIVO',
    emitidoEm: '22:44:58',
  },
  {
    id: 'alm-2',
    severidade: 'MANUT',
    codigo: 'R-CONT-01',
    mensagem: 'Falha comunicação sensor',
    estado: 'RECONHECIDO',
    emitidoEm: '22:40:00',
  },
  {
    id: 'alm-3',
    severidade: 'OBS',
    codigo: 'P-PRIM-01',
    mensagem: 'Pressão na faixa de atenção',
    estado: 'ATIVO',
    emitidoEm: '22:43:12',
  },
]

export function clsSeveridade(sev) {
  switch (sev) {
    case 'ALTO':
      return 'crit'
    case 'MANUT':
      return 'warn'
    case 'OBS':
      return 'purple'
    default:
      return 'ok'
  }
}

export function plantStatusFromAlarmes(alarmes) {
  const abertos = alarmes.filter((a) => a.estado !== 'RESOLVIDO')
  if (abertos.some((a) => a.severidade === 'ALTO' && a.estado === 'ATIVO')) {
    return { label: 'CRÍTICO', cls: 'crit' }
  }
  if (abertos.some((a) => a.estado === 'ATIVO' || a.estado === 'RECONHECIDO')) {
    return { label: 'ATENÇÃO', cls: 'warn' }
  }
  return { label: 'ESTÁVEL', cls: '' }
}

export function alarmeBanner(alarmes) {
  const ativo = alarmes.find((a) => a.severidade === 'ALTO' && a.estado === 'ATIVO')
  if (!ativo) return null
  return `ALARME ATIVO — ${ativo.mensagem} (${ativo.codigo}).`
}
