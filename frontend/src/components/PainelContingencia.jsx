import { NavLink } from 'react-router-dom'
import { useEstado } from '../state/EstadoContext.jsx'
import { useEmergencias } from '../state/useEmergencias.js'
import { IconAlertTriangle } from './icons.jsx'

const formatarHora = (iso) =>
  iso ? new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '—'

/**
 * RF10 / #99 — topo da Visão Geral em modo de contingência: qual protocolo está em vigor, quem
 * ativou, o roteiro de passos e o que está travado. Some sozinho quando a emergência é encerrada.
 *
 * O "está ativo" vem do núcleo (8080, mesmo dado que trava as rotas); os passos vêm da Central.
 */
export default function PainelContingencia() {
  const { estado } = useEstado()
  const emergencias = useEmergencias()
  const ativos = estado.contingencia?.protocolos ?? []
  if (!estado.contingencia?.ativa || ativos.length === 0) return null

  return (
    <div className="contingencia-painel" role="alert" aria-live="assertive">
      <div className="contingencia-painel-topo">
        <span className="contingencia-painel-icone" aria-hidden="true">
          <IconAlertTriangle size={26} />
        </span>
        <div>
          <p className="contingencia-painel-kicker">MODO DE CONTINGÊNCIA · USINA EM EMERGÊNCIA</p>
          <h2 className="contingencia-painel-titulo">
            {ativos.length === 1 ? ativos[0].nome : `${ativos.length} protocolos de contingência ativos`}
          </h2>
        </div>
        <NavLink to="/emergencia" className="scada-btn contingencia-painel-btn">
          Ir para Emergência
        </NavLink>
      </div>

      {ativos.map((ativo) => {
        const emergencia = emergencias.find((e) => e.id === ativo.emergenciaId)
        const passos = emergencia?.protocolo?.passos ?? []
        return (
          <div key={ativo.emergenciaId} className="contingencia-painel-protocolo">
            <p className="contingencia-painel-meta">
              Emergência #{ativo.emergenciaId}
              {emergencia ? ` · ${emergencia.descricao}` : ''} · ativado por <strong>{ativo.ativadoPor}</strong> às{' '}
              {formatarHora(emergencia?.protocoloAtivadoEm ?? ativo.ativadoEm)}
            </p>
            {passos.length > 0 && (
              <ol className="contingencia-painel-passos">
                {passos.map((passo) => (
                  <li key={passo}>{passo}</li>
                ))}
              </ol>
            )}
          </div>
        )
      })}

      <div className="contingencia-painel-regras">
        <div>
          <strong>Travado até o encerramento</strong>
          <span>Cenários de simulação · religar o tempo real · novos acessos às áreas</span>
        </div>
        <div>
          <strong>Continua liberado</strong>
          <span>Monitoramento dos 5 reatores · tratamento de alarmes · auditoria</span>
        </div>
      </div>
    </div>
  )
}
