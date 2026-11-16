import { useEffect, useState } from 'react'
import { NavLink } from 'react-router-dom'
import { authApi } from '../services/authApi.js'

/** Nome do evento que a tela de Emergência dispara quando algo muda (sem polling). */
export const EVENTO_EMERGENCIAS = 'emergencias-alteradas'

/**
 * RF09/RF10 — faixa visível em todas as abas enquanto houver emergência aberta ou protocolo de
 * contingência ativo. Carrega ao entrar e quando a tela de Emergência avisa que algo mudou.
 */
export default function FaixaEmergencia() {
  const [emergencias, setEmergencias] = useState([])

  useEffect(() => {
    const carregar = () => authApi.listarEmergencias().then(setEmergencias).catch(() => setEmergencias([]))
    carregar()
    window.addEventListener(EVENTO_EMERGENCIAS, carregar)
    return () => window.removeEventListener(EVENTO_EMERGENCIAS, carregar)
  }, [])

  const emAndamento = emergencias.filter((e) => e.status !== 'ENCERRADA')
  if (emAndamento.length === 0) return null

  const principal = emAndamento.find((e) => e.status === 'PROTOCOLO_ATIVO') ?? emAndamento[0]
  const outras = emAndamento.length - 1
  const texto = principal.status === 'PROTOCOLO_ATIVO'
    ? `PROTOCOLO DE CONTINGÊNCIA ATIVO — ${principal.protocolo.nome} (emergência #${principal.id}, ativado por ${principal.protocoloAtivadoPor})`
    : `EMERGÊNCIA ABERTA #${principal.id} — ${principal.descricao} (sem protocolo ativo)`

  return (
    <div className={`scada-banner${principal.status === 'PROTOCOLO_ATIVO' ? ' warn-banner' : ''}`} role="alert">
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
