import { useEstado } from '../state/EstadoContext.jsx'
import { interpretarEvento, reatorDoEvento } from '../data/eventos.js'
import {
  IconThermometer,
  IconGauge,
  IconRadiation,
  IconDroplet,
  IconCore,
  IconActivity,
  IconBell,
  IconShield,
  IconClock,
  IconPlay,
  IconCheck,
} from '../components/icons.jsx'
import SeletorReator, { AvisoOutrosReatores } from '../components/SeletorReator.jsx'
import { TODOS, useReatorSelecionado } from '../state/reatorSelecionado.js'
import { NIVEL, formatarHora as horaCurta, formatarValor, nomeReator, sensorDoTipo } from '../data/reatores.js'

const NOME_SENSOR = {
  TEMPERATURA: 'Temperatura',
  PRESSAO: 'Pressão',
  RADIACAO: 'Radiação',
  FLUXO_RESFRIAMENTO: 'Fluxo resfriamento',
}

const ICONE_SENSOR = {
  TEMPERATURA: IconThermometer,
  PRESSAO: IconGauge,
  RADIACAO: IconRadiation,
  FLUXO_RESFRIAMENTO: IconDroplet,
}

const UNIDADE_EXIBICAO = {
  Celsius: '°C',
  bar: 'bar',
  'mSv/h': 'mSv/h',
  'm3/h': 'm³/h',
}

const STATUS_TEXTO = { ESTAVEL: 'ESTÁVEL', ATENCAO: 'ATENÇÃO', CRITICO: 'CRÍTICO' }

function percentualNaFaixa(sensor) {
  const faixa = sensor.limiteMaximo - sensor.limiteMinimo
  if (faixa <= 0) return 0
  const percentual = ((sensor.valor - sensor.limiteMinimo) / faixa) * 100
  return Math.max(0, Math.min(100, percentual))
}

function corSensor(sensor, alarmes) {
  const temAlarmeAtivo = alarmes.some((a) => a.sensorId === sensor.id && a.status === 'ATIVO')
  if (temAlarmeAtivo) return 'var(--crit)'
  const temAlarmeReconhecido = alarmes.some(
    (a) => a.sensorId === sensor.id && a.status === 'RECONHECIDO'
  )
  if (temAlarmeReconhecido) return 'var(--warn)'
  return 'var(--cyan)'
}

function formatarHora(iso) {
  return new Date(iso).toLocaleTimeString('pt-BR', { hour12: false })
}

/**
 * Status do núcleo de um reator — mesma regra do `status` do snapshot (EstadoAgregador), aplicada
 * aos alarmes e observações daquele reator: CRÍTICO com alarme ativo, ATENÇÃO com alarme
 * reconhecido ou sensor em observação, senão ESTÁVEL.
 */
function statusDoReator(reator, alarmes) {
  if (alarmes.some((a) => a.status === 'ATIVO')) return 'CRITICO'
  if (alarmes.some((a) => a.status === 'RECONHECIDO')) return 'ATENCAO'
  if (reator?.sensores?.some((s) => s.emAtencao)) return 'ATENCAO'
  return 'ESTAVEL'
}

function Linhatempo({ eventos, comReator }) {
  if (eventos.length === 0) return <p className="scada-lead">Sem eventos ainda.</p>
  return (
    <ul className="timeline2">
      {eventos.slice(0, 8).map((evento, indice) => {
        const info = interpretarEvento(evento.tipo, evento.resumo)
        return (
          <li key={`${evento.ocorridoEm}-${indice}`}>
            <span className="dot" style={{ background: info.cor }} />
            <div className="time">{formatarHora(evento.ocorridoEm)}</div>
            <div className="label">{info.titulo}</div>
            <div className="detail">
              {comReator ? info.descricao : info.descricao.replace(/^(Reator-\d+|R-\d+) · /, '')}
            </div>
          </li>
        )
      })}
    </ul>
  )
}

function Contadores({ contadores }) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
      <div className="stat-tile">
        <IconActivity size={20} style={{ color: 'var(--sky)' }} />
        <div>
          <div className="num">{contadores.medicoes}</div>
          <div className="lbl">MEDIÇÕES</div>
        </div>
      </div>
      <div className="stat-tile">
        <IconBell size={20} style={{ color: contadores.alarmes > 0 ? 'var(--crit)' : 'var(--muted)' }} />
        <div>
          <div className="num" style={{ color: contadores.alarmes > 0 ? 'var(--crit)' : undefined }}>
            {contadores.alarmes}
          </div>
          <div className="lbl">ALARMES</div>
        </div>
      </div>
      <div className="stat-tile">
        <IconShield size={20} style={{ color: 'var(--sky)' }} />
        <div>
          <div className="num">{contadores.auditoria}</div>
          <div className="lbl">AUDITORIA</div>
        </div>
      </div>
    </div>
  )
}

function Acoes({ tempoReal, alarmes, iniciarTempoReal, pausarTempoReal, reconhecerAlarme, resolverAlarme, mostrarReator }) {
  const alarmePendenteReconhecer = alarmes.find((a) => a.status === 'ATIVO')
  const alarmePendenteResolver = alarmes.find((a) => a.status === 'RECONHECIDO')
  const sufixo = (alarme) => (mostrarReator && alarme ? ` · ${alarme.reatorId ?? 'R-01'}` : '')
  return (
    <div style={{ marginTop: 'auto', paddingTop: 14, display: 'flex', gap: 10, flexWrap: 'wrap' }}>
      <button
        type="button"
        className="action-btn2"
        style={{ background: '#0f766e' }}
        onClick={() => (tempoReal ? pausarTempoReal() : iniciarTempoReal())}
      >
        <IconPlay size={14} />
        {tempoReal ? 'Pausar Tempo Real' : 'Iniciar Tempo Real'}
      </button>
      <button
        type="button"
        className="action-btn2"
        style={{ background: '#b45309' }}
        disabled={!alarmePendenteReconhecer}
        onClick={() => alarmePendenteReconhecer && reconhecerAlarme(alarmePendenteReconhecer.id)}
      >
        <IconCheck size={14} />
        Validar / Reconhecer{sufixo(alarmePendenteReconhecer)}
      </button>
      <button
        type="button"
        className="action-btn2"
        style={{ background: '#059669' }}
        disabled={!alarmePendenteResolver}
        onClick={() => alarmePendenteResolver && resolverAlarme(alarmePendenteResolver.id)}
      >
        <IconCheck size={14} />
        Normalizar / Encerrar{sufixo(alarmePendenteResolver)}
      </button>
    </div>
  )
}

/** Um reator: a tela original do T01, alimentada com os dados daquele reator. */
function VisaoReator({ reatorId, status, sensores, alarmes, eventos, contadores, acoes }) {
  const alarmesNaoResolvidos = alarmes.filter((a) => a.status !== 'RESOLVIDO')
  const pico = sensores.length ? Math.max(...sensores.map((s) => percentualNaFaixa(s))) : 0
  const cor = status === 'CRITICO' ? 'var(--crit)' : status === 'ATENCAO' ? 'var(--warn)' : 'var(--ok)'

  return (
    <div className="scada-split">
      <div
        className="scada-card"
        style={{ background: 'radial-gradient(circle at 50% 20%, var(--card2) 0%, var(--card) 60%)' }}
      >
        <h2 className="panel-heading">
          <IconCore size={13} />
          Status do Núcleo · {nomeReator(reatorId)}
        </h2>

        <div style={{ display: 'flex', justifyContent: 'center', padding: '10px 0 18px' }}>
          <div className="ring ring-lg" style={{ background: `conic-gradient(${cor} ${pico}%, var(--border) 0)` }}>
            <div className="ring-value">
              <div className="mono" style={{ fontSize: 30, fontWeight: 700, color: cor }}>
                {sensores.length ? `${pico.toFixed(0)}%` : '—'}
              </div>
              <div style={{ fontSize: 10, fontWeight: 700, letterSpacing: '0.14em', color: cor, marginTop: 4 }}>
                {STATUS_TEXTO[status] ?? status}
              </div>
            </div>
          </div>
        </div>

        {sensores.length === 0 ? (
          <p className="scada-lead">Aguardando a primeira leitura dos sensores…</p>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
            {sensores.map((sensor) => {
              const corTile = corSensor(sensor, alarmes)
              const Icone = ICONE_SENSOR[sensor.tipo] ?? IconActivity
              const pct = percentualNaFaixa(sensor)
              return (
                <div className="sensor-tile2" key={sensor.id}>
                  <div className="ring ring-sm" style={{ background: `conic-gradient(${corTile} ${pct}%, var(--border) 0)` }}>
                    <Icone size={18} className="ring-icon" style={{ color: 'var(--text)' }} />
                  </div>
                  <div className="meta">
                    <div className="name">{(NOME_SENSOR[sensor.tipo] ?? sensor.tipo).toUpperCase()}</div>
                    <div className="value mono">
                      {sensor.valor.toFixed(2)}
                      <small>{UNIDADE_EXIBICAO[sensor.unidade] ?? sensor.unidade}</small>
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
        <Contadores contadores={contadores} />

        <div className="scada-card" style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0 }}>
          <h2 className="panel-heading">
            <IconClock size={13} />
            Histórico Operacional em Tempo Real · {nomeReator(reatorId)}
          </h2>
          <div className="scada-table-wrap" style={{ maxHeight: 220 }}>
            <Linhatempo eventos={eventos} comReator={false} />
          </div>

          {alarmesNaoResolvidos.length > 0 && (
            <div className="scada-alarm-box" style={{ marginTop: 12 }}>
              {alarmesNaoResolvidos.map((a) => `[${a.severidade}] ${a.mensagem} · ${a.status}`).join('\n')}
            </div>
          )}

          <Acoes {...acoes} alarmes={alarmes} mostrarReator={false} />
        </div>
      </div>
    </div>
  )
}

/** "Todos": resumo da usina — um cartão por reator, alarmes pendentes e a linha do tempo de todos. */
function ResumoUsina({ reatores, alarmes, eventos, contadores, acoes, onSelecionar }) {
  const pendentes = alarmes.filter((a) => a.status !== 'RESOLVIDO')
  return (
    <>
      <div className="usina-grade">
        {reatores.map((reator) => {
          const info = NIVEL[reator.nivel] ?? NIVEL.ESTAVEL
          const alarmesDoReator = alarmes.filter((a) => a.reatorId === reator.id)
          const status = statusDoReator(reator, alarmesDoReator)
          const radiacao = sensorDoTipo(reator, 'RADIACAO')
          const temperatura = sensorDoTipo(reator, 'TEMPERATURA')
          const pendentesDoReator = alarmesDoReator.filter((a) => a.status !== 'RESOLVIDO').length
          return (
            <button
              key={reator.id}
              type="button"
              className={`scada-card usina-cartao ${info.classe}`}
              onClick={() => onSelecionar(reator.id)}
              aria-label={`${nomeReator(reator.id)}: nível ${info.texto}, núcleo ${STATUS_TEXTO[status]}. Abrir detalhes.`}
            >
              <span className="usina-cartao-topo">
                <span className={`mon-lampada ${info.classe}`} aria-hidden="true" />
                <strong>{nomeReator(reator.id)}</strong>
              </span>
              <span className={`mon-nivel ${info.classe}`}>NÍVEL {info.texto}</span>
              <span className="usina-cartao-valores">
                <span>Radiação <b className="mono">{radiacao ? formatarValor(radiacao.valor, 'RADIACAO') : '—'}</b> mSv/h</span>
                <span>Temperatura <b className="mono">{temperatura ? formatarValor(temperatura.valor, 'TEMPERATURA') : '—'}</b> °C</span>
              </span>
              <span className="usina-cartao-rodape">
                {pendentesDoReator > 0 ? (
                  <span className="scada-pill crit">{pendentesDoReator} alarme{pendentesDoReator > 1 ? 's' : ''} pendente{pendentesDoReator > 1 ? 's' : ''}</span>
                ) : (
                  <span className="scada-pill ok">sem alarmes</span>
                )}
                <span className="mon-atualizado-fixo">{horaCurta(reator.atualizadoEm)}</span>
              </span>
            </button>
          )
        })}
      </div>

      <div className="scada-split" style={{ marginTop: 18 }}>
        <div className="scada-card" style={{ display: 'flex', flexDirection: 'column' }}>
          <h2 className="panel-heading">
            <IconBell size={13} />
            Alarmes pendentes na usina · {pendentes.length}
          </h2>
          {pendentes.length === 0 ? (
            <p className="scada-lead">Nenhum alarme pendente.</p>
          ) : (
            <div className="scada-alarm-box">
              {pendentes.map((a) => `[${a.severidade}] ${a.mensagem} · ${a.status}`).join('\n')}
            </div>
          )}
          <Acoes {...acoes} alarmes={alarmes} mostrarReator />
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
          <Contadores contadores={contadores} />
          <div className="scada-card" style={{ flex: 1 }}>
            <h2 className="panel-heading">
              <IconClock size={13} />
              Linha do tempo da usina
            </h2>
            <div className="scada-table-wrap" style={{ maxHeight: 260 }}>
              <Linhatempo eventos={eventos} comReator />
            </div>
          </div>
        </div>
      </div>
    </>
  )
}

export default function OverviewPage() {
  const { estado, iniciarTempoReal, pausarTempoReal, reconhecerAlarme, resolverAlarme } = useEstado()
  const [reator, selecionar] = useReatorSelecionado()
  const { status, tempoReal, sensores, contadores, alarmes, eventos } = estado
  const reatores = estado.reatores ?? []
  const acoes = { tempoReal, iniciarTempoReal, pausarTempoReal, reconhecerAlarme, resolverAlarme }

  const reatorAtual = reator !== TODOS ? reatores.find((r) => r.id === reator) : null
  const resumo = reatores.length > 0 && !reatorAtual

  let conteudo
  if (reatores.length === 0) {
    // API antiga ou mock: tela original, só do reator principal.
    conteudo = (
      <VisaoReator
        reatorId="R-01"
        status={status}
        sensores={sensores}
        alarmes={alarmes}
        eventos={eventos}
        contadores={contadores}
        acoes={acoes}
      />
    )
  } else if (resumo) {
    conteudo = (
      <ResumoUsina
        reatores={reatores}
        alarmes={alarmes}
        eventos={eventos}
        contadores={contadores}
        acoes={acoes}
        onSelecionar={selecionar}
      />
    )
  } else {
    const reatorPorAlarme = new Map(alarmes.map((a) => [a.id, a.reatorId]))
    const alarmesDoReator = alarmes.filter((a) => a.reatorId === reatorAtual.id)
    conteudo = (
      <VisaoReator
        reatorId={reatorAtual.id}
        status={statusDoReator(reatorAtual, alarmesDoReator)}
        sensores={reatorAtual.sensores}
        alarmes={alarmesDoReator}
        eventos={eventos.filter((e) => reatorDoEvento(e, reatorPorAlarme) === reatorAtual.id)}
        contadores={contadores}
        acoes={acoes}
      />
    )
  }

  return (
    <section>
      <p className="scada-kicker">
        PAINEL DE SUPERVISÃO CENTRAL • {resumo ? `USINA · ${reatores.length} REATORES` : reatorAtual?.id ?? 'R-01'}
      </p>
      <h1 className="scada-heading">Visão Geral Operacional</h1>
      <p className="scada-lead">
        {resumo
          ? 'Situação de cada reator, alarmes pendentes da usina e a linha do tempo de todos os eventos. Clique num reator para ver o núcleo dele.'
          : 'Supervisão contínua do reator, telemetria de resfriamento e linha do tempo de eventos em tempo real.'}
      </p>

      <SeletorReator />
      <AvisoOutrosReatores />

      <div style={{ marginTop: 14 }}>{conteudo}</div>
    </section>
  )
}
