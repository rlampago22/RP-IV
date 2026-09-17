import { clsSeveridade } from '../data/alarmes.js'
import { usePlant } from '../state/PlantContext.jsx'

export default function AlarmesPage() {
  const { alarmes, reconhecer, resolver } = usePlant()
  const pendentes = alarmes.filter((a) => a.estado !== 'RESOLVIDO')
  const resolvidos = alarmes.filter((a) => a.estado === 'RESOLVIDO')

  return (
    <section>
      <p className="scada-kicker">T03 · UC01 / RF-2</p>
      <h1 className="scada-heading">Alarmes</h1>
      <p className="scada-lead">
        Emitido → Reconhecido → Resolvido. Mock Opção A; back GoF/Strategy em{' '}
        <code>mvp/.../alarmes/</code> (débito #42).
      </p>

      <div className="scada-card" style={{ marginTop: 16 }}>
        <h2>Gestão de alarmes & notificações</h2>

        {pendentes.length === 0 ? (
          <div className="scada-alarm-box" style={{ color: 'var(--ok)', borderColor: 'rgb(16 185 129 / 35%)' }}>
            Nenhum alarme pendente nesta sessão.
          </div>
        ) : (
          <ul className="scada-alarm-list">
            {pendentes.map((a) => (
              <li key={a.id} className={`scada-alarm-row ${clsSeveridade(a.severidade)}`}>
                <div className="scada-alarm-main">
                  <span className={`scada-pill ${clsSeveridade(a.severidade)}`}>[{a.severidade}]</span>
                  <span className="scada-alarm-msg">
                    {a.mensagem} <span className="scada-alarm-code">({a.codigo})</span>
                  </span>
                </div>
                <div className="scada-alarm-meta">
                  <span>{a.emitidoEm}</span>
                  <span className={`scada-pill ${a.estado === 'ATIVO' ? 'crit' : 'warn'}`}>
                    {a.estado}
                  </span>
                </div>
                <div className="scada-actions" style={{ marginTop: 0 }}>
                  <button
                    type="button"
                    className="scada-btn scada-btn-amber"
                    disabled={a.estado !== 'ATIVO'}
                    onClick={() => reconhecer(a.id)}
                  >
                    Validar / Reconhecer
                  </button>
                  <button
                    type="button"
                    className="scada-btn scada-btn-green"
                    onClick={() => resolver(a.id)}
                  >
                    Normalizar / Encerrar
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}

        <div className="scada-actions">
          <button type="button" className="scada-btn scada-btn-amber" onClick={() => reconhecer()}>
            Reconhecer todos ATIVOS
          </button>
          <button type="button" className="scada-btn scada-btn-green" onClick={() => resolver()}>
            Encerrar todos pendentes
          </button>
        </div>

        {resolvidos.length > 0 && (
          <>
            <h2 style={{ marginTop: 16 }}>Encerrados nesta sessão</h2>
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
                        <span className={`scada-pill ${clsSeveridade(a.severidade)}`}>{a.severidade}</span>
                      </td>
                      <td className="ev">
                        {a.mensagem} ({a.codigo})
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
          Backend evidenciado: <code>AvaliadorLimiar</code> (Strategy),{' '}
          <code>AlarmeFactory</code> (Factory), <code>AlarmeFacade</code> assinante do EventBus.
        </div>
      </div>
    </section>
  )
}
