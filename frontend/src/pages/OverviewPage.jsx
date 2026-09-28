import { useEstado } from '../state/EstadoContext.jsx'

const NOME_SENSOR = {
  TEMPERATURA: 'Temperatura',
  PRESSAO: 'Pressão',
  RADIACAO: 'Radiação',
  FLUXO_RESFRIAMENTO: 'Fluxo resfriamento',
}

const UNIDADE_EXIBICAO = {
  Celsius: '°C',
  bar: 'bar',
  'mSv/h': 'mSv/h',
  'm3/h': 'm³/h',
}

const STATUS_TEXTO = { ESTAVEL: 'ESTÁVEL', ATENCAO: 'ATENÇÃO', CRITICO: 'CRÍTICO' }
const STATUS_CLASSE = { ESTAVEL: '', ATENCAO: 'warn', CRITICO: 'crit' }

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

export default function OverviewPage() {
  const { estado, iniciarTempoReal, pausarTempoReal, reconhecerAlarme, resolverAlarme } =
    useEstado()
  const { status, tempoReal, sensores, contadores, alarmes, eventos } = estado

  const alarmePendenteReconhecer = alarmes.find((a) => a.status === 'ATIVO')
  const alarmePendenteResolver = alarmes.find((a) => a.status === 'RECONHECIDO')
  const alarmesNaoResolvidos = alarmes.filter((a) => a.status !== 'RESOLVIDO')

  return (
    <section>
      <p className="scada-kicker">T01 · Overview</p>
      <h1 className="scada-heading">Visão Geral</h1>
      <p className="scada-lead">
        Núcleo + telemetria RF-1 + timeline EDA, consumidos de <code>/api/estado</code> (
        docs/api-estado-contrato.md). T02/T03 compartilham o mesmo EstadoContext.
      </p>

      <div className="scada-split">
        <div className="scada-card">
          <h2>Núcleo do reator</h2>
          <div className="scada-reactor">
            <div className={`scada-core ${STATUS_CLASSE[status] ?? ''}`.trim()}>
              {STATUS_TEXTO[status] ?? status}
            </div>
          </div>
          <div className="scada-sensors">
            {sensores.length === 0 && (
              <p className="scada-lead">Aguardando a primeira leitura dos sensores…</p>
            )}
            {sensores.map((sensor) => (
              <div className="scada-sensor" key={sensor.id}>
                <div className="name">{NOME_SENSOR[sensor.tipo] ?? sensor.tipo}</div>
                <div className="val" style={{ color: corSensor(sensor, alarmes) }}>
                  {sensor.valor.toFixed(2)} {UNIDADE_EXIBICAO[sensor.unidade] ?? sensor.unidade}
                </div>
                <div className="scada-bar">
                  <i
                    style={{
                      width: `${percentualNaFaixa(sensor)}%`,
                      background: corSensor(sensor, alarmes),
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="scada-card">
          <h2>Contadores EDA</h2>
          <div className="scada-metrics">
            <div className="scada-metric">
              <span>MEDIÇÕES</span>
              <strong>{contadores.medicoes}</strong>
            </div>
            <div className="scada-metric">
              <span>ALARMES</span>
              <strong style={{ color: contadores.alarmes > 0 ? 'var(--crit)' : undefined }}>
                {contadores.alarmes}
              </strong>
            </div>
            <div className="scada-metric">
              <span>AUDIT</span>
              <strong style={{ color: 'var(--cyan)' }}>{contadores.auditoria}</strong>
            </div>
            <div className="scada-metric">
              <span>STATUS</span>
              <strong
                style={{
                  color: `var(--${status === 'CRITICO' ? 'crit' : status === 'ATENCAO' ? 'warn' : 'ok'})`,
                  fontSize: 14,
                }}
              >
                {STATUS_TEXTO[status] ?? status}
              </strong>
            </div>
          </div>
          <h2>Linha do tempo de eventos</h2>
          <div className="scada-table-wrap">
            <table className="scada-table">
              <thead>
                <tr>
                  <th>Hora</th>
                  <th>Evento</th>
                  <th>Detalhe</th>
                </tr>
              </thead>
              <tbody>
                {eventos.length === 0 && (
                  <tr>
                    <td colSpan={3}>Sem eventos ainda.</td>
                  </tr>
                )}
                {eventos.map((evento, indice) => (
                  <tr key={`${evento.ocorridoEm}-${indice}`}>
                    <td>{formatarHora(evento.ocorridoEm)}</td>
                    <td className="ev">{evento.tipo}</td>
                    <td>{evento.resumo}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="scada-alarm-box">
            {alarmesNaoResolvidos.length === 0
              ? 'Nenhum alarme ativo.'
              : alarmesNaoResolvidos
                  .map((a) => `[${a.severidade}] ${a.mensagem} · ${a.status}`)
                  .join('\n')}
          </div>
          <div className="scada-actions">
            <button
              type="button"
              className="scada-btn"
              onClick={() => (tempoReal ? pausarTempoReal() : iniciarTempoReal())}
            >
              {tempoReal ? 'Pausar Tempo Real' : 'Iniciar Tempo Real'}
            </button>
            <button
              type="button"
              className="scada-btn scada-btn-amber"
              disabled={!alarmePendenteReconhecer}
              onClick={() =>
                alarmePendenteReconhecer && reconhecerAlarme(alarmePendenteReconhecer.id)
              }
            >
              Validar / Reconhecer
            </button>
            <button
              type="button"
              className="scada-btn scada-btn-green"
              disabled={!alarmePendenteResolver}
              onClick={() => alarmePendenteResolver && resolverAlarme(alarmePendenteResolver.id)}
            >
              Normalizar / Encerrar
            </button>
          </div>
        </div>
      </div>
    </section>
  )
}
