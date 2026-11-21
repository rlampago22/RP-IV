/**
 * Monitoramento dos 5 reatores — tela única pedida pelo professor.
 *
 * Dois níveis:
 *  - visão geral: um bloco por reator, com a lâmpada de nível, os gráficos principais completos
 *    (radiação e temperatura) e os secundários compactos (pressão e fluxo);
 *  - reator maximizado: clique num bloco para ver os 4 gráficos completos daquele reator; os
 *    demais ficam numa faixa compacta, para a supervisão deles não se perder. Esc volta.
 *
 * Maximizar = selecionar o reator (`?reator=`), o mesmo filtro que as outras abas seguem. Abrir o
 * R-03 aqui e ir para Alarmes já mostra os alarmes do R-03.
 *
 * O dado chega por push (EstadoContext). Cada bloco é memorizado pelo instante da última leitura
 * do seu reator: quando chega um evento de outro reator, este bloco não redesenha. É isso que
 * deixa ver "esse aqui alterou, aquele lá não".
 */

import { memo, useEffect } from 'react'
import { useEstado } from '../state/EstadoContext.jsx'
import GraficoAoVivo, { TendenciaCompacta } from '../components/GraficoAoVivo.jsx'
import { IconPlay, IconCore } from '../components/icons.jsx'
import SeletorReator from '../components/SeletorReator.jsx'
import { TODOS, useReatorSelecionado } from '../state/reatorSelecionado.js'
import {
  NIVEL,
  PRINCIPAIS,
  SECUNDARIOS,
  TODOS_PARAMETROS,
  formatarHora,
  formatarValor,
  nomeReator,
  sensorDoTipo,
} from '../data/reatores.js'

function Lampada({ nivel, grande = false }) {
  const info = NIVEL[nivel] ?? NIVEL.ESTAVEL
  return (
    <span
      className={`mon-lampada ${info.classe}${grande ? ' grande' : ''}`}
      role="img"
      aria-label={`Nível ${info.texto}`}
      title={`Nível ${info.texto}`}
    />
  )
}

function Atualizado({ instante }) {
  // A key reinicia a animação a cada leitura nova deste reator — o destaque some sozinho.
  return (
    <span key={instante} className="mon-atualizado">
      atualizado {formatarHora(instante)}
    </span>
  )
}

function ChipAlarmes({ quantidade }) {
  if (!quantidade) return null
  return (
    <span className="scada-pill crit">
      {quantidade} alarme{quantidade > 1 ? 's' : ''} ativo{quantidade > 1 ? 's' : ''}
    </span>
  )
}

function mesmoReator(antes, depois) {
  return (
    antes.reator.atualizadoEm === depois.reator.atualizadoEm
    && antes.reator.nivel === depois.reator.nivel
    && antes.reator.alarmesAtivos === depois.reator.alarmesAtivos
    && antes.onAbrir === depois.onAbrir
  )
}

const ReatorBloco = memo(function ReatorBloco({ reator, onAbrir }) {
  const info = NIVEL[reator.nivel] ?? NIVEL.ESTAVEL
  return (
    <article
      className={`scada-card mon-bloco ${info.classe}`}
      role="button"
      tabIndex={0}
      aria-label={`${nomeReator(reator.id)}, nível ${info.texto}. Abrir todos os gráficos.`}
      onClick={() => onAbrir(reator.id)}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault()
          onAbrir(reator.id)
        }
      }}
    >
      <header className="mon-bloco-topo">
        <Lampada nivel={reator.nivel} />
        <strong>{nomeReator(reator.id)}</strong>
        <span className={`mon-nivel ${info.classe}`}>{info.texto}</span>
      </header>
      <div className="mon-bloco-meta">
        <Atualizado instante={reator.atualizadoEm} />
        <ChipAlarmes quantidade={reator.alarmesAtivos} />
      </div>

      {PRINCIPAIS.map((tipo) => (
        <GraficoAoVivo key={tipo} tipo={tipo} sensor={sensorDoTipo(reator, tipo)} largura={300} altura={150} />
      ))}

      <div className="mon-secundarios">
        {SECUNDARIOS.map((tipo) => (
          <TendenciaCompacta key={tipo} tipo={tipo} sensor={sensorDoTipo(reator, tipo)} />
        ))}
      </div>

      <span className="mon-dica">Clique para ver todos os gráficos</span>
    </article>
  )
}, mesmoReator)

function ReatorMaximizado({ reator, onVoltar }) {
  const info = NIVEL[reator.nivel] ?? NIVEL.ESTAVEL
  return (
    <section className={`scada-card mon-maximizado ${info.classe}`} aria-label={`${nomeReator(reator.id)} maximizado`}>
      <header className="mon-bloco-topo">
        <Lampada nivel={reator.nivel} grande />
        <strong className="mon-titulo-grande">{nomeReator(reator.id)}</strong>
        <span className={`mon-nivel ${info.classe}`}>{info.texto}</span>
        <Atualizado instante={reator.atualizadoEm} />
        <ChipAlarmes quantidade={reator.alarmesAtivos} />
        <button type="button" className="scada-btn mon-voltar" onClick={onVoltar}>
          ← Voltar à visão geral
        </button>
      </header>
      <div className="mon-grade-maximizada">
        {TODOS_PARAMETROS.map((tipo) => (
          <GraficoAoVivo key={tipo} tipo={tipo} sensor={sensorDoTipo(reator, tipo)} largura={480} altura={200} />
        ))}
      </div>
    </section>
  )
}

function FaixaCompacta({ reatores, onAbrir }) {
  return (
    <nav className="mon-faixa" aria-label="Outros reatores">
      {reatores.map((reator) => {
        const radiacao = sensorDoTipo(reator, 'RADIACAO')
        return (
          <button key={reator.id} type="button" className="mon-faixa-item" onClick={() => onAbrir(reator.id)}>
            <Lampada nivel={reator.nivel} />
            <strong>{nomeReator(reator.id)}</strong>
            {radiacao && (
              <span className="mono">
                {formatarValor(radiacao.valor, 'RADIACAO')} mSv/h
              </span>
            )}
            {reator.alarmesAtivos > 0 && <span className="scada-pill crit">{reator.alarmesAtivos}</span>}
          </button>
        )
      })}
    </nav>
  )
}

export default function MonitoramentoPage() {
  const { estado, origemMock, iniciarTempoReal, pausarTempoReal } = useEstado()
  const [maximizado, setMaximizado] = useReatorSelecionado()
  const reatores = estado.reatores ?? []
  const selecionado = maximizado === TODOS ? undefined : reatores.find((r) => r.id === maximizado)

  useEffect(() => {
    if (maximizado === TODOS) return undefined
    const aoTeclar = (e) => {
      if (e.key === 'Escape') setMaximizado(TODOS)
    }
    window.addEventListener('keydown', aoTeclar)
    return () => window.removeEventListener('keydown', aoTeclar)
  }, [maximizado, setMaximizado])

  const contagem = reatores.reduce((acc, r) => ({ ...acc, [r.nivel]: (acc[r.nivel] ?? 0) + 1 }), {})

  return (
    <section>
      <p className="scada-kicker">PAINEL DE MONITORAMENTO • {reatores.length || 5} REATORES</p>
      <h1 className="scada-heading">Monitoramento dos Reatores</h1>
      <p className="scada-lead">
        Cada reator tem simulação própria e atualiza no seu ritmo. A lâmpada mostra o nível atual:
        verde dentro da faixa ideal, amarelo na faixa de atenção, vermelho acima do limite seguro.
      </p>

      <SeletorReator />

      <div className="mon-barra">
        <div className="mon-resumo">
          <span><Lampada nivel="ESTAVEL" /> {contagem.ESTAVEL ?? 0} estável</span>
          <span><Lampada nivel="ATENCAO" /> {contagem.ATENCAO ?? 0} atenção</span>
          <span><Lampada nivel="CRITICO" /> {contagem.CRITICO ?? 0} crítico</span>
        </div>
        <button
          type="button"
          className="action-btn2"
          style={{ background: '#0f766e' }}
          onClick={() => (estado.tempoReal ? pausarTempoReal() : iniciarTempoReal())}
          disabled={!estado.tempoReal && Boolean(estado.contingencia?.ativa)}
          title={!estado.tempoReal && estado.contingencia?.ativa ? 'Protocolo de contingência ativo: religar o tempo real fica travado até a emergência ser encerrada.' : undefined}
        >
          <IconPlay size={14} />
          {estado.tempoReal ? 'Pausar Tempo Real' : 'Iniciar Tempo Real'}
        </button>
      </div>

      {reatores.length === 0 ? (
        <div className="scada-card mon-sem-dados">
          <IconCore size={18} />
          {origemMock
            ? 'O painel dos reatores precisa da API do núcleo. Rode mvp/4-EXECUTAR-API-ESTADO.bat.'
            : 'Aguardando a primeira leitura dos reatores…'}
        </div>
      ) : selecionado ? (
        <>
          <ReatorMaximizado reator={selecionado} onVoltar={() => setMaximizado(TODOS)} />
          <FaixaCompacta reatores={reatores.filter((r) => r.id !== selecionado.id)} onAbrir={setMaximizado} />
        </>
      ) : (
        <div className="mon-grade">
          {reatores.map((reator) => (
            <ReatorBloco key={reator.id} reator={reator} onAbrir={setMaximizado} />
          ))}
        </div>
      )}
    </section>
  )
}
