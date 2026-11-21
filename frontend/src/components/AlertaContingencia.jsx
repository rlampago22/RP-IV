import { useEffect, useRef, useState } from 'react'
import { useEstado } from '../state/EstadoContext.jsx'
import { IconAlertTriangle } from './icons.jsx'

/** Quanto tempo o alerta fica na tela. */
const DURACAO_MS = 5000

/**
 * RF10 / #99 — alerta de tela cheia por 5 s quando um protocolo de contingência é ativado.
 *
 * Aparece em todas as sessões abertas (a ativação chega pelo push do núcleo), em qualquer aba. Só
 * dispara para protocolo ativado DEPOIS que a tela abriu: recarregar a página com um protocolo já
 * em vigor não repete o alerta (o painel da Visão Geral e a faixa continuam mostrando).
 * Fecha sozinho, com clique ou com Esc — numa emergência o operador precisa voltar aos sensores.
 */
export default function AlertaContingencia() {
  const { estado } = useEstado()
  const conhecidosRef = useRef(null)
  const [alerta, setAlerta] = useState(null)

  const protocolos = estado.contingencia?.protocolos
  useEffect(() => {
    if (!protocolos) return // estado ainda não veio do núcleo
    const ids = protocolos.map((p) => p.emergenciaId)
    if (conhecidosRef.current === null) {
      conhecidosRef.current = new Set(ids) // linha de base: o que já estava ativo ao abrir
      return
    }
    const novo = protocolos.find((p) => !conhecidosRef.current.has(p.emergenciaId))
    conhecidosRef.current = new Set(ids)
    if (novo) setAlerta({ ...novo, chave: `${novo.emergenciaId}-${Date.now()}` })
  }, [protocolos])

  useEffect(() => {
    if (!alerta) return undefined
    const fechar = () => setAlerta(null)
    const temporizador = setTimeout(fechar, DURACAO_MS)
    const aoTeclar = (evento) => evento.key === 'Escape' && fechar()
    window.addEventListener('keydown', aoTeclar)
    return () => {
      clearTimeout(temporizador)
      window.removeEventListener('keydown', aoTeclar)
    }
  }, [alerta])

  if (!alerta) return null

  return (
    <div
      key={alerta.chave}
      className="alerta-contingencia"
      role="alertdialog"
      aria-live="assertive"
      aria-label={`Protocolo de contingência ativado: ${alerta.nome}. Evacuar.`}
      onClick={() => setAlerta(null)}
    >
      <div className="alerta-contingencia-caixa">
        <span className="alerta-contingencia-icone" aria-hidden="true">
          <IconAlertTriangle size={64} />
        </span>
        <p className="alerta-contingencia-kicker">PROTOCOLO DE CONTINGÊNCIA ATIVADO</p>
        <p className="alerta-contingencia-protocolo">
          {alerta.nome} · emergência #{alerta.emergenciaId} · por {alerta.ativadoPor}
        </p>
        <p className="alerta-contingencia-ordem">
          <span>EVACUAR</span>
          <span>EVACUAR</span>
        </p>
        <div className="alerta-contingencia-barra" aria-hidden="true">
          <span style={{ animationDuration: `${DURACAO_MS}ms` }} />
        </div>
        <p className="alerta-contingencia-dica">Clique ou Esc para fechar</p>
      </div>
    </div>
  )
}
