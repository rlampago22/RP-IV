import { NavLink } from 'react-router-dom'
import { EVENTO_EMERGENCIAS, useEmergencias } from '../state/useEmergencias.js'

export { EVENTO_EMERGENCIAS }

/**
 * RF09/RF10 — faixa visível em todas as abas enquanto houver emergência aberta ou protocolo de
 * contingência ativo. Protocolo ativo é o caso mais grave: faixa vermelha. Emergência aberta sem
 * protocolo: faixa amarela.
 */
export default function FaixaEmergencia() {
  const emergencias = useEmergencias()

  const emAndamento = emergencias.filter((e) => e.status !== 'ENCERRADA')
  if (emAndamento.length === 0) return null

  const principal = emAndamento.find((e) => e.status === 'PROTOCOLO_ATIVO') ?? emAndamento[0]
  const protocoloAtivo = principal.status === 'PROTOCOLO_ATIVO'
  const outras = emAndamento.length - 1
  const texto = protocoloAtivo
    ? `PROTOCOLO DE CONTINGÊNCIA ATIVO — ${principal.protocolo.nome} (emergência #${principal.id}, ativado por ${principal.protocoloAtivadoPor}). Cenários, tempo real e novos acessos travados.`
    : `EMERGÊNCIA ABERTA #${principal.id} — ${principal.descricao} (sem protocolo ativo)`

  return (
    <div className={`scada-banner ${protocoloAtivo ? 'contingencia-banner' : 'warn-banner'}`} role="alert">
      <span>
        {texto}
        {outras > 0 ? ` · mais ${outras} em andamento` : ''}
      </span>
      <NavLink to="/emergencia" className="scada-btn scada-btn-ghost">
        Ver emergência
      </NavLink>
    </div>
  )
}
