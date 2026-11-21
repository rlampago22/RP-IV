import { useEffect, useRef, useState } from 'react'
import { useEstado } from '../state/EstadoContext.jsx'
import { avisoDoProtocolo } from '../data/contingencia.js'
import { silenciar, tocarAnuncio, tocarSirene } from '../services/alertaSonoro.js'
import { IconAlertTriangle } from './icons.jsx'

/** Tempo mínimo na tela; com som, fica até o anúncio terminar (com teto). */
const MINIMO_MS = 6000
const MAXIMO_MS = 22000
const FOLGA_APOS_FALA_MS = 1200

const formatarHora = (iso) =>
  iso ? new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : ''

/**
 * RF10 / #99 — alerta de tela cheia quando um protocolo de contingência é ativado: sirene curta e
 * anúncio por voz do protocolo em vigor.
 *
 * Aparece em todas as sessões abertas (a ativação chega pelo push do núcleo), em qualquer aba. Só
 * dispara para protocolo ativado DEPOIS que a tela abriu: recarregar a página com um protocolo já
 * em vigor não repete o alerta (o painel da Visão Geral e a faixa continuam mostrando).
 * Fecha sozinho, com clique ou com Esc — e fechar corta o som; o operador precisa voltar aos sensores.
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
    let encerrado = false
    const abertoEm = Date.now()
    const fechar = () => {
      if (encerrado) return
      encerrado = true
      silenciar()
      setAlerta(null)
    }

    const teto = setTimeout(fechar, MAXIMO_MS)
    let aposFala = null
    tocarSirene()
      .then(() => !encerrado && tocarAnuncio(avisoDoProtocolo(alerta.protocolo, alerta.nome).audio))
      .then(() => {
        if (encerrado) return
        const restante = Math.max(MINIMO_MS - (Date.now() - abertoEm), FOLGA_APOS_FALA_MS)
        aposFala = setTimeout(fechar, restante)
      })

    const aoTeclar = (evento) => evento.key === 'Escape' && fechar()
    window.addEventListener('keydown', aoTeclar)
    return () => {
      clearTimeout(teto)
      clearTimeout(aposFala)
      window.removeEventListener('keydown', aoTeclar)
      if (!encerrado) silenciar()
      encerrado = true
    }
  }, [alerta])

  if (!alerta) return null
  const aviso = avisoDoProtocolo(alerta.protocolo, alerta.nome)

  return (
    <div
      key={alerta.chave}
      className="alerta-contingencia"
      role="alertdialog"
      aria-live="assertive"
      aria-label={`Protocolo de contingência ativado: ${aviso.titulo}. ${aviso.diretriz}`}
      onClick={() => {
        silenciar()
        setAlerta(null)
      }}
    >
      <div className="alerta-contingencia-caixa">
        <span className="alerta-contingencia-icone" aria-hidden="true">
          <IconAlertTriangle size={56} />
        </span>
        <p className="alerta-contingencia-kicker">CENTRAL DE SUPERVISÃO · ALERTA DE CONTINGÊNCIA</p>
        <p className="alerta-contingencia-ativado">PROTOCOLO DE CONTINGÊNCIA ATIVADO</p>
        <p className="alerta-contingencia-ordem">{aviso.titulo}</p>
        <p className="alerta-contingencia-diretriz">{aviso.diretriz}</p>
        <p className="alerta-contingencia-protocolo">
          Emergência #{alerta.emergenciaId} · autorizado por {alerta.ativadoPor}
          {alerta.ativadoEm ? ` · ${formatarHora(alerta.ativadoEm)}` : ''}
        </p>
        <div className="alerta-contingencia-barra" aria-hidden="true">
          <span />
        </div>
        <p className="alerta-contingencia-dica">Aviso sonoro em transmissão · clique ou Esc para fechar</p>
      </div>
    </div>
  )
}
