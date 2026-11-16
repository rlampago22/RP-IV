import { useState } from 'react'
import { clsSeveridade } from '../data/alarmes.js'
import { useEstado } from '../state/EstadoContext.jsx'
import { IconBell, IconCheck, IconClock } from '../components/icons.jsx'
import ConfirmacaoAlarme from '../components/ConfirmacaoAlarme.jsx'

function formatarHora(iso) {
  if (!iso) return '—'
  try {
    return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'medium' })
  } catch {
    return iso
  }
}

export default function AlarmesPage() {
  const { estado, erroApi, reconhecerAlarme, resolverAlarme } = useEstado()
  const [acaoPendente, setAcaoPendente] = useState(null)
  const alarmes = estado.alarmes ?? []
  const pendentes = alarmes.filter((a) => a.status !== 'RESOLVIDO')
  const resolvidos = alarmes.filter((a) => a.status === 'RESOLVIDO')

  return (
    <>
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
                  <span>Disparado: {formatarHora(a.criadoEm)}</span>
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
                    onClick={() => setAcaoPendente({ tipo: 'reconhecer', alarme: a })}
                  >
                    <IconCheck size={13} />
                    Validar / Reconhecer
                  </button>
                  <button
                    type="button"
                    className="action-btn2"
                    style={{ background: '#059669', flex: 'none' }}
                    disabled={a.status !== 'RECONHECIDO'}
                    onClick={() => setAcaoPendente({ tipo: 'resolver', alarme: a })}
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
                    <th>Disparado em</th>
                    <th>Resolvido por</th>
                    <th>Resolvido em</th>
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
                      <td>{formatarHora(a.criadoEm)}</td>
                      <td>{a.resolvidoPor || 'Não informado'}</td>
                      <td>{formatarHora(a.resolvidoEm)}</td>
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
    <ConfirmacaoAlarme
      acao={acaoPendente}
      onCancelar={() => setAcaoPendente(null)}
      onConfirmar={() => {
        const { tipo, alarme } = acaoPendente
        setAcaoPendente(null)
        if (tipo === 'reconhecer') reconhecerAlarme(alarme.id)
        else resolverAlarme(alarme.id)
      }}
    />
    </>
  )
}
