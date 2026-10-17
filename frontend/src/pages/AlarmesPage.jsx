import { clsSeveridade } from '../data/alarmes.js'
import { useEstado } from '../state/EstadoContext.jsx'
import { IconBell, IconCheck, IconClock } from '../components/icons.jsx'

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
  const alarmes = estado.alarmes ?? []
  const pendentes = alarmes.filter((a) => a.status !== 'RESOLVIDO')
  const resolvidos = alarmes.filter((a) => a.status === 'RESOLVIDO')

  return (
    <section>
      <p className="scada-kicker">SISTEMA DE SEGURANÇA E PROTEÇÃO RADIOLÓGICA</p>
      <h1 className="scada-heading">Gestão de Alarmes</h1>
      <p className="scada-lead">
        Painel central de ocorrências com protocolo formal de reconhecimento operacional e encerramento de alertas.
      </p>

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
            Nenhum alarme pendente nesta sessão.
          </div>
        ) : (
          <ul className="scada-alarm-list">
            {pendentes.map((a) => (
              <li key={a.id} className={`scada-alarm-row ${clsSeveridade(a.severidade)}`}>
                <div className="scada-alarm-main">
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
                    <th>Sev</th>
                    <th>Mensagem</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {resolvidos.map((a) => (
                    <tr key={`r-${a.id}`}>
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
          Backend: <code>AvaliadorLimiar</code> (Strategy), <code>AlarmeFactory</code>,{' '}
          <code>AlarmeFacade</code> + API <code>/api/alarmes/&#123;id&#125;/reconhecer|resolver</code>.
        </div>
        {erroApi && <p className="scada-api-error" role="alert">{erroApi}</p>}
      </div>
    </section>
  )
}
