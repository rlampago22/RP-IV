import { useEffect, useState } from 'react'
import { buscarIntegridadeAuditoria } from '../api/estado.js'
import { useEstado } from '../state/EstadoContext.jsx'

const INTERVALO_INTEGRIDADE_MS = 5000

export default function AuditoriaPage() {
  const { estado, origemMock } = useEstado()
  const eventos = estado.eventos ?? []
  // null = verificando · 'indisponivel' = API fora · objeto = { integra, entradas, ultimoHash }
  const [integridade, setIntegridade] = useState(null)

  useEffect(() => {
    let ativo = true
    const verificar = async () => {
      try {
        const resultado = await buscarIntegridadeAuditoria()
        if (ativo) setIntegridade(resultado)
      } catch {
        if (ativo) setIntegridade('indisponivel')
      }
    }
    verificar()
    const intervalo = setInterval(verificar, INTERVALO_INTEGRIDADE_MS)
    return () => {
      ativo = false
      clearInterval(intervalo)
    }
  }, [])

  const indisponivel = origemMock || integridade === 'indisponivel'

  return (
    <section>
      <p className="scada-kicker">CONFORMIDADE E RASTREABILIDADE OPERACIONAL</p>
      <h1 className="scada-heading">Trilha de Auditoria</h1>
      <p className="scada-lead">
        Registro cronológico de eventos operacionais, gravado em arquivo com cadeia de hash
        criptográfica SHA-256 para detectar qualquer adulteração.
      </p>

      <div className="scada-card" style={{ marginTop: 16 }}>
        <div className="scada-integridade" role="status">
          {indisponivel && (
            <span className="scada-pill warn">VERIFICAÇÃO INDISPONÍVEL</span>
          )}
          {!indisponivel && integridade === null && (
            <span className="scada-pill">VERIFICANDO CADEIA…</span>
          )}
          {!indisponivel && integridade && integridade !== 'indisponivel' && (
            <>
              <span className={`scada-pill ${integridade.integra ? 'ok' : 'crit'}`}>
                {integridade.integra ? 'CADEIA ÍNTEGRA' : 'INTEGRIDADE COMPROMETIDA'}
              </span>
              <span className="scada-integridade-detalhe">
                {integridade.entradas} registros
                {integridade.ultimoHash ? ` · último selo ${integridade.ultimoHash}` : ''}
              </span>
            </>
          )}
        </div>

        <div className="scada-table-wrap" style={{ maxHeight: 'none' }}>
          <table className="scada-table">
            <thead>
              <tr>
                <th>Timestamp</th>
                <th>Evento</th>
                <th>Detalhe</th>
              </tr>
            </thead>
            <tbody>
              {eventos.length === 0 && (
                <tr>
                  <td colSpan={3}>Sem eventos de auditoria nesta sessão.</td>
                </tr>
              )}
              {eventos.map((row, i) => (
                <tr key={`${row.ocorridoEm}-${row.tipo}-${i}`}>
                  <td>{row.ocorridoEm}</td>
                  <td className="ev">{row.tipo}</td>
                  <td>{row.resumo}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="scada-note">
          Exibindo os {eventos.length} eventos mais recentes da sessão; o registro completo é mantido
          em arquivo.
        </div>
      </div>
    </section>
  )
}
