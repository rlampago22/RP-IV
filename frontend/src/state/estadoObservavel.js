/**
 * Sujeito observável do estado da usina (padrão Observer, lado do frontend).
 *
 * O sujeito não sabe quem o observa nem de onde o estado veio — quem alimenta é um
 * produtor externo, hoje o fluxo SSE de api/eventos.js. Trocar o produtor não afeta
 * nenhum observador, que é a propriedade que o padrão existe para dar.
 *
 * Contrato:
 *   assinar(observador)  → registra e devolve a função que cancela a inscrição
 *   publicar(estado)     → notifica todos os observadores
 *
 * O observador pode ser uma função ou um objeto com método update(estado).
 */
export function criarEstadoObservavel() {
  const observadores = new Set()
  let ultimoEstado = null

  function notificar(observador, estado) {
    try {
      if (typeof observador === 'function') {
        observador(estado)
      } else if (observador && typeof observador.update === 'function') {
        observador.update(estado)
      }
    } catch (erro) {
      // Mesma regra do EventBus do mvp: um observador que falha não derruba os outros.
      console.error('[estadoObservavel] observador falhou:', erro)
    }
  }

  return {
    assinar(observador) {
      observadores.add(observador)
      // Quem chega depois do primeiro evento não fica com a tela vazia esperando o próximo.
      if (ultimoEstado !== null) {
        notificar(observador, ultimoEstado)
      }
      return () => observadores.delete(observador)
    },

    publicar(estado) {
      ultimoEstado = estado
      for (const observador of observadores) {
        notificar(observador, estado)
      }
    },

    ultimo() {
      return ultimoEstado
    },

    get quantidade() {
      return observadores.size
    },
  }
}

/** Instância única consumida pelo EstadoProvider. */
export const estadoObservavel = criarEstadoObservavel()
