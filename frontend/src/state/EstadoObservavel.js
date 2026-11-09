/**
 * Observer (V1 — front-end) pedido pelo professor em 04/11/2026: o objeto
 * observado guarda a lista de telas registradas; quando o dado muda, chama
 * update() de cada uma; se não mudou, ninguém é notificado.
 *
 * Esta classe é framework-agnostic de propósito — não sabe o que é React,
 * EstadoContext, nem a API. Ela só resolve "quem está assinando" e "mudou
 * ou não mudou". Isso é o que permite trocar o produtor (quem chama
 * `publicar`) por um push real do back-end (V2, SSE) sem mexer em quem
 * assina — o contrato assinar()/update() não muda entre as duas versões.
 */
export class EstadoObservavel {
  constructor() {
    this._observadores = new Set()
    this._ultimoEstado = null
    this._ultimaAssinaturaDados = null
  }

  /**
   * Registra uma tela/observador. `observador.update(estado)` é chamado
   * sempre que o dado muda — nunca em timer fixo. Retorna a função de
   * cancelamento (useEffect cleanup).
   */
  assinar(observador) {
    this._observadores.add(observador)
    if (this._ultimoEstado) {
      observador.update(this._ultimoEstado)
    }
    return () => {
      this._observadores.delete(observador)
    }
  }

  /** Publica um novo estado. Só notifica os observadores se o dado mudou. */
  publicar(novoEstado) {
    const assinaturaDados = JSON.stringify(novoEstado)
    if (assinaturaDados === this._ultimaAssinaturaDados) {
      return false
    }
    this._ultimaAssinaturaDados = assinaturaDados
    this._ultimoEstado = novoEstado
    for (const observador of this._observadores) {
      observador.update(novoEstado)
    }
    return true
  }
}
