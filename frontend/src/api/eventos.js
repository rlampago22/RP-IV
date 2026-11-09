/**
 * Produtor de estado por push (SSE) — substitui o polling do EstadoContext.
 *
 * O fluxo vem da Central, em `GET /api/eventos` no Spring (8081), e não da API do núcleo
 * (8080): o canal exige sessão autenticada, então só quem fez login recebe o push. O
 * processo do mvp alimenta esse canal encaminhando cada evento do EventBus.
 *
 * Cada mensagem traz o mesmo JSON de `GET /api/estado`, então o formato consumido pelas
 * telas não muda — muda só quem empurra e quando.
 *
 * O EventSource reconecta sozinho quando a conexão cai, por isso aqui não há laço de
 * retry; só o aviso para a UI poder sinalizar que o canal está fora.
 */
import { API_BASE_URL } from '../services/authApi.js'

/** Tipos publicados por mvp/infraestruturaeventos, mais o nome usado na abertura. */
export const TIPOS_CONHECIDOS = [
  'snapshot',
  'MEDICAO_REGISTRADA',
  'OBSERVACAO_REGISTRADA',
  'FALHA_SENSOR_DETECTADA',
  'ALARME_EMITIDO',
  'ALARME_RECONHECIDO',
  'ALARME_RESOLVIDO',
  'ACESSO_REGISTRADO',
]

/**
 * Abre o fluxo de eventos da Central.
 *
 * @param {(estado: object, tipoEvento: string) => void} aoReceber  chamado a cada mensagem
 * @param {(erro: Error) => void} [aoFalhar]                        conexão caiu ou não subiu
 * @returns {() => void} encerra o fluxo
 */
export function conectarEventos(aoReceber, aoFalhar) {
  if (typeof EventSource === 'undefined') {
    aoFalhar?.(new Error('Navegador sem suporte a EventSource (SSE)'))
    return () => {}
  }

  // withCredentials manda o cookie de sessão: sem ele o Spring responde 401.
  const fonte = new EventSource(`${API_BASE_URL}/api/eventos`, { withCredentials: true })

  const tratar = (mensagem) => {
    try {
      aoReceber(JSON.parse(mensagem.data), mensagem.type)
    } catch (erro) {
      aoFalhar?.(erro instanceof Error ? erro : new Error(String(erro)))
    }
  }

  // onmessage só pega mensagem sem nome; os blocos vêm nomeados com o tipo do evento,
  // então cada tipo conhecido precisa do seu listener.
  fonte.onmessage = tratar
  for (const tipo of TIPOS_CONHECIDOS) {
    fonte.addEventListener(tipo, tratar)
  }

  fonte.onerror = () => {
    aoFalhar?.(new Error('Canal de eventos da Central indisponivel'))
  }

  return () => fonte.close()
}
