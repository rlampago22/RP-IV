import React, { createContext, useContext, useState } from 'react'
import {
  FAIXAS_VAZIAS,
  PERIODO_VAZIO,
  buscarDetalhe,
  buscarDias,
} from '../api/historico.js'

/**
 * Estado da busca do T06, mantido acima do roteador.
 *
 * O roteador desmonta a página ao trocar de aba, e com ela iria todo o `useState` — o operador
 * perderia a busca só por conferir um alarme na T03. Com o estado aqui, sair e voltar devolve a
 * consulta como estava.
 *
 * Diferente do `EstadoContext`, este provider não faz polling: só guarda o que o usuário pediu e
 * o que a API respondeu.
 */
const HistoricoContext = createContext(null)

export function HistoricoProvider({ children }) {
  const [periodo, setPeriodo] = useState(PERIODO_VAZIO)
  const [saidas, setSaidas] = useState(null)
  const [selecionada, setSelecionada] = useState(null)
  const [faixas, setFaixas] = useState(FAIXAS_VAZIAS)
  const [detalhe, setDetalhe] = useState(null)
  const [faixasAplicadas, setFaixasAplicadas] = useState(null)
  const [relatorioAberto, setRelatorioAberto] = useState(false)
  const [graficosAbertos, setGraficosAbertos] = useState(true)
  const [anexarGraficos, setAnexarGraficos] = useState(false)
  const [erro, setErro] = useState(null)
  const [carregando, setCarregando] = useState(false)

  function alterarPeriodo(campo, valor) {
    setPeriodo((atual) => ({ ...atual, [campo]: valor }))
  }

  function alterarFaixa(campo, valor) {
    setFaixas((atual) => ({ ...atual, [campo]: valor }))
  }

  async function pesquisarSaidas() {
    setCarregando(true)
    setErro(null)
    try {
      setSaidas(await buscarDias(periodo))
    } catch (falha) {
      setErro(falha.message)
      setSaidas(null)
    } finally {
      setCarregando(false)
    }
  }

  function limparPeriodo() {
    setPeriodo(PERIODO_VAZIO)
    setSaidas(null)
    setErro(null)
  }

  async function abrirSaida(saida, faixasIniciais = FAIXAS_VAZIAS) {
    setSelecionada(saida)
    setFaixas(faixasIniciais)
    setRelatorioAberto(false)
    setErro(null)
    setCarregando(true)
    try {
      setDetalhe(await buscarDetalhe(saida, faixasIniciais))
      setFaixasAplicadas(faixasIniciais)
    } catch (falha) {
      setErro(falha.message)
      setDetalhe(null)
    } finally {
      setCarregando(false)
    }
  }

  async function pesquisarDetalhe() {
    setCarregando(true)
    setErro(null)
    try {
      setDetalhe(await buscarDetalhe(selecionada, faixas))
      setFaixasAplicadas(faixas)
    } catch (falha) {
      setErro(falha.message)
    } finally {
      setCarregando(false)
    }
  }

  function limparFaixas() {
    abrirSaida(selecionada, FAIXAS_VAZIAS)
  }

  function voltarParaSaidas() {
    setSelecionada(null)
    setDetalhe(null)
    setFaixasAplicadas(null)
    setRelatorioAberto(false)
    setErro(null)
  }

  const valor = {
    periodo,
    saidas,
    selecionada,
    faixas,
    detalhe,
    faixasAplicadas,
    relatorioAberto,
    graficosAbertos,
    anexarGraficos,
    erro,
    carregando,
    alterarPeriodo,
    alterarFaixa,
    pesquisarSaidas,
    limparPeriodo,
    abrirSaida,
    pesquisarDetalhe,
    limparFaixas,
    voltarParaSaidas,
    alternarRelatorio: () => setRelatorioAberto((aberto) => !aberto),
    fecharRelatorio: () => setRelatorioAberto(false),
    alternarGraficos: () => setGraficosAbertos((abertos) => !abertos),
    // Anexar exige os gráficos montados: se estiverem recolhidos, marcar a opção os abre,
    // senão o operador marcaria a caixa e o PDF sairia sem gráfico nenhum.
    definirAnexarGraficos: (anexar) => {
      setAnexarGraficos(anexar)
      if (anexar) {
        setGraficosAbertos(true)
      }
    },
  }

  return <HistoricoContext.Provider value={valor}>{children}</HistoricoContext.Provider>
}

export function useHistorico() {
  const contexto = useContext(HistoricoContext)
  if (!contexto) {
    throw new Error('useHistorico precisa estar dentro de <HistoricoProvider>')
  }
  return contexto
}
