import { useEffect, useState } from 'react'
import { authApi } from '../services/authApi.js'
import { useEstado } from './EstadoContext.jsx'

/** Nome do evento que a tela de Emergência dispara quando algo muda na própria aba. */
export const EVENTO_EMERGENCIAS = 'emergencias-alteradas'

/**
 * RF09/RF10 — emergências da Central (8081), com os passos de cada protocolo.
 *
 * Recarrega ao montar, quando a aba Emergência avisa que algo mudou e quando o núcleo (8080)
 * informa pelo push que os protocolos em vigor mudaram — assim uma ativação feita em outra
 * sessão também chega aqui, sem polling.
 */
export function useEmergencias() {
  const [emergencias, setEmergencias] = useState([])
  const { estado } = useEstado()
  const protocolosNoNucleo = (estado?.contingencia?.protocolos ?? []).map((p) => p.emergenciaId).join(',')

  useEffect(() => {
    let ativo = true
    const carregar = () =>
      authApi
        .listarEmergencias()
        .then((lista) => ativo && setEmergencias(lista))
        .catch(() => ativo && setEmergencias([]))
    carregar()
    window.addEventListener(EVENTO_EMERGENCIAS, carregar)
    return () => {
      ativo = false
      window.removeEventListener(EVENTO_EMERGENCIAS, carregar)
    }
  }, [protocolosNoNucleo])

  return emergencias
}
