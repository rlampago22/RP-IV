import { clsSeveridade } from '../data/alarmes.js'
import { useEstado } from '../state/EstadoContext.jsx'
import { IconBell, IconCheck, IconClock } from '../components/icons.jsx'
import SeletorReator, { AvisoOutrosReatores, doReator } from '../components/SeletorReator.jsx'
import { TODOS, useReatorSelecionado } from '../state/reatorSelecionado.js'
import { nomeReator } from '../data/reatores.js'

function formatarHora(iso) {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleTimeString('pt-BR', { hour12: false })
  } catch {
    return iso
  }
}

export default function AlarmesPage() {
  const { estado, erroApi, reconhecerAlarme, resolverAlarme } = useEstado()
  const [reator] = useReatorSelecionado()
  const alarmes = (estado.alarmes ?? []).filter((a) => doReator(reator, a.reatorId ?? 'R-01'))
  const pendentes = alarmes.filter((a) => a.status !== 'RESOLVIDO')
  const resolvidos = alarmes.filter((a) => a.status === 'RESOLVIDO')

  return (
    <section>
      <p className="scada-kicker">SISTEMA DE SEGURANÇA E PROTEÇÃO RADIOLÓGICA</p>
      <h1 className="scada-heading">Gestão de Alarmes</h1>
      <p className="scada-lead">
        Painel central de ocorrências com protocolo formal de reconhecimento operacional e encerramento de alertas.
      </p>

      <SeletorReator />
      <AvisoOutrosReatores />

      <div className="scada-card" style={{ marginTop: 16 }}>
        <h2 className="panel-heading">
          <IconBell size={13} />
          Gestão de alarmes & notificações
        </h2>

        {pendentes.length === 0 ? (
          <div
            className="scada-alarm-box"
            style={{ color: 'var(--ok)', borderColor: 'rgb(16 185 129 / 35%)' }}
          >
            {reator === TODOS
              ? 'Nenhum alarme pendente nesta sessão.'
              : `Nenhum alarme pendente no ${nomeReator(reator)}.`}
          </div>
        ) : (
          <ul className="scada-alarm-list">
            {pendentes.map((a) => (
              <li key={a.id} className={`scada-alarm-row ${clsSeveridade(a.severidade)}`}>
                <div className="scada-alarm-main">
                  <span className="scada-pill reator-tag">{nomeReator(a.reatorId ?? 'R-01')}</span>
                  <span className={`scada-pill ${clsSeveridade(a.severidade)}`}>[{a.severidade}]</span>
                  <span className="scada-alarm-msg">
                    {a.mensagem}{' '}
                    <span className="scada-alarm-code">(sensor {a.sensorId})</span>
                  </span>
                </div>
                <div className="scada-alarm-meta">
                  <span>{formatarHora(a.criadoEm)}</span>
                  <span className={`scada-pill ${a.status === 'ATIVO' ? 'crit' : 'warn'}`}>
                    {a.status}
                  </span>
                </div>
                <div className="scada-actions" style={{ marginTop: 0 }}>
                  <button
                    type="button"
                    className="action-btn2"
                    style={{ background: '#b45309', flex: 'none' }}
                    disabled={a.status !== 'ATIVO'}
                    onClick={() => reconhecerAlarme(a.id)}
                  >
                    <IconCheck size={13} />
                    Validar / Reconhecer
                  </button>
                  <button
                    type="button"
                    className="action-btn2"
                    style={{ background: '#059669', flex: 'none' }}
                    disabled={a.status !== 'RECONHECIDO'}
                    onClick={() => resolverAlarme(a.id)}
                  >
                    <IconCheck size={13} />
                    Normalizar / Encerrar
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}

        {resolvidos.length > 0 && (
          <>
            <h2 className="panel-heading" style={{ marginTop: 16 }}>
              <IconClock size={13} />
              Encerrados nesta sessão
            </h2>
            <div className="scada-table-wrap" style={{ maxHeight: 160 }}>
              <table className="scada-table">
                <thead>
                  <tr>
                    <th>Reator</th>
                    <th>Sev</th>
                    <th>Mensagem</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {resolvidos.map((a) => (
                    <tr key={`r-${a.id}`}>
                      <td>{nomeReator(a.reatorId ?? 'R-01')}</td>
                      <td>
                        <span className={`scada-pill ${clsSeveridade(a.severidade)}`}>
                          {a.severidade}
                        </span>
                      </td>
                      <td className="ev">
                        {a.mensagem} (sensor {a.sensorId})
                      </td>
                      <td>RESOLVIDO</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}

        <div className="scada-note">
          Cada ocorrência segue o ciclo: o operador reconhece o alarme e o engenheiro de turno o encerra.
          Um sensor fora da faixa mantém um único alarme aberto até o encerramento.
        </div>
        {erroApi && <p className="scada-api-error" role="alert">{erroApi}</p>}
      </div>
    </section>
  )
}
