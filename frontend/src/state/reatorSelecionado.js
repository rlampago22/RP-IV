/**
 * Reator selecionado, compartilhado entre as abas.
 *
 * Fica na URL (`?reator=R-03`), e não num estado interno, por três motivos:
 *  - sobrevive à troca de aba (o menu repassa o parâmetro) e ao recarregar a página;
 *  - um link já abre filtrado — o Monitoramento usa isso ao maximizar um reator;
 *  - o filtro fica visível no endereço, além do seletor de cada aba.
 *
 * Sem parâmetro = "Todos".
 */

import { useCallback } from 'react'
import { useSearchParams } from 'react-router-dom'

export const TODOS = 'TODOS'
const FORMATO_REATOR = /^R-\d{2}$/

export function useReatorSelecionado() {
  const [parametros, setParametros] = useSearchParams()
  const bruto = parametros.get('reator')
  const reator = bruto && FORMATO_REATOR.test(bruto) ? bruto : TODOS

  const selecionar = useCallback(
    (id) => {
      setParametros(
        (atuais) => {
          const novos = new URLSearchParams(atuais)
          if (!id || id === TODOS) novos.delete('reator')
          else novos.set('reator', id)
          return novos
        },
        { replace: true },
      )
    },
    [setParametros],
  )

  return [reator, selecionar]
}

/** Query string a repassar nos links do menu: só o reator, nada mais da aba atual. */
export function buscaDoReator(search) {
  const reator = new URLSearchParams(search).get('reator')
  return reator && FORMATO_REATOR.test(reator) ? `?reator=${reator}` : ''
}
