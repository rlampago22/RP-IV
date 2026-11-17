/**
 * Seletor de reator — o mesmo componente no topo do conteúdo de cada aba.
 *
 * A escolha é compartilhada (ver `state/reatorSelecionado.js`): escolher o R-03 aqui faz as
 * outras abas abrirem filtradas no R-03. Cada opção mostra a lâmpada do reator, então o seletor
 * também serve de resumo do estado da usina.
 *
 * Como o filtro acompanha a troca de aba, ele pode ser esquecido. Por isso o
 * `AvisoOutrosReatores` avisa quando há alarme ativo fora do reator filtrado.
 */

import { useEstado } from '../state/EstadoContext.jsx'
import { TODOS, useReatorSelecionado } from '../state/reatorSelecionado.js'
import { NIVEL, nomeReator } from '../data/reatores.js'

export default function SeletorReator({ rotulo = 'Reator', desabilitarTodos = false }) {
  const { estado } = useEstado()
  const [selecionado, selecionar] = useReatorSelecionado()
  const reatores = estado.reatores ?? []

  if (reatores.length === 0) return null

  return (
    <div className="seletor-reator" role="radiogroup" aria-label={`Filtrar por ${rotulo.toLowerCase()}`}>
      <span className="seletor-reator-rotulo">{rotulo}</span>
      {!desabilitarTodos && (
        <button
          type="button"
          role="radio"
          aria-checked={selecionado === TODOS}
          className={`seletor-opcao${selecionado === TODOS ? ' ativa' : ''}`}
          onClick={() => selecionar(TODOS)}
        >
          Todos
        </button>
      )}
      {reatores.map((reator) => {
        const info = NIVEL[reator.nivel] ?? NIVEL.ESTAVEL
        const ativo = selecionado === reator.id
        return (
          <button
            key={reator.id}
            type="button"
            role="radio"
            aria-checked={ativo}
            className={`seletor-opcao${ativo ? ' ativa' : ''}`}
            title={`${nomeReator(reator.id)} · ${info.texto}`}
            onClick={() => selecionar(reator.id)}
          >
            <span className={`mon-lampada pequena ${info.classe}`} aria-hidden="true" />
            {reator.id}
          </button>
        )
      })}
    </div>
  )
}

/** Alarmes ativos fora do reator filtrado — o lembrete de que o filtro esconde algo. */
export function AvisoOutrosReatores() {
  const { estado } = useEstado()
  const [selecionado, selecionar] = useReatorSelecionado()
  if (selecionado === TODOS) return null

  const fora = (estado.alarmes ?? []).filter((a) => a.status === 'ATIVO' && a.reatorId !== selecionado)
  if (fora.length === 0) return null
  const reatores = [...new Set(fora.map((a) => a.reatorId))].sort()

  return (
    <div className="aviso-outros-reatores" role="status">
      <span>
        <strong>{fora.length} alarme{fora.length > 1 ? 's' : ''} ativo{fora.length > 1 ? 's' : ''}</strong>{' '}
        em outros reatores ({reatores.join(', ')}) — o filtro de {nomeReator(selecionado)} está escondendo.
      </span>
      <button type="button" className="scada-btn" onClick={() => selecionar(TODOS)}>
        Ver todos
      </button>
    </div>
  )
}

/** Se o item pertence ao reator selecionado ("Todos" aceita qualquer um). */
export function doReator(selecionado, reatorId) {
  return selecionado === TODOS || reatorId === selecionado
}
