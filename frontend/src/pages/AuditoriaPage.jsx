import { useMemo, useState } from 'react'
import { useEstado } from '../state/EstadoContext.jsx'
import { interpretarEvento } from '../data/eventos.js'
import {
  IconShield,
  IconBell,
  IconCheck,
  IconLock,
  IconAlertTriangle,
  IconActivity,
} from '../components/icons.jsx'

const ICONE_TIPO = {
  ALARME_EMITIDO: IconBell,
  ALARME_RECONHECIDO: IconCheck,
  ALARME_RESOLVIDO: IconCheck,
  ACESSO_REGISTRADO: IconLock,
  FALHA_SENSOR_DETECTADA: IconAlertTriangle,
  OBSERVACAO_REGISTRADA: IconAlertTriangle,
  MEDICAO_REGISTRADA: IconActivity,
}

function formatarHora(iso) {
  try {
    return new Date(iso).toLocaleTimeString('pt-BR', { hour12: false })
  } catch {
    return iso
  }
}

export default function AuditoriaPage() {
  const { estado } = useEstado()
  const eventos = estado.eventos ?? []
  const [mostrarRotina, setMostrarRotina] = useState(false)

  const interpretados = useMemo(
    () => eventos.map((row, i) => ({ ...row, seq: eventos.length - i, ...interpretarEvento(row.tipo, row.resumo) })),
    [eventos]
  )
  const importantes = interpretados.filter((e) => e.importante)
  const rotina = interpretados.filter((e) => !e.importante)
  const visiveis = mostrarRotina ? interpretados : importantes

  return (
    <section>
      <p className="scada-kicker">CONFORMIDADE E RASTREABILIDADE OPERACIONAL</p>
      <h1 className="scada-heading">Trilha de Auditoria</h1>
      <p className="scada-lead">
        O que aconteceu, em ordem, com quem agiu e por quê — cada linha vem de um evento real do
        EventBus, protegido por uma cadeia de hash SHA-256 (não dá para editar ou apagar um
        registro sem quebrar a cadeia).
      </p>

      <div className="scada-card" style={{ marginTop: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap', marginBottom: 14 }}>
          <h2 className="panel-heading" style={{ margin: 0 }}>
            <IconShield size={13} />
            Linha do tempo
          </h2>
          <button
            type="button"
            className={`filter-toggle${mostrarRotina ? ' active' : ''}`}
            onClick={() => setMostrarRotina((v) => !v)}
          >
            {mostrarRotina ? 'Ocultar medições de rotina' : `Mostrar também medições de rotina (${rotina.length})`}
          </button>
        </div>

        {visiveis.length === 0 ? (
          <p className="scada-lead">Sem eventos de auditoria nesta sessão.</p>
        ) : (
          <div>
            {visiveis.map((evento) => {
              const Icone = ICONE_TIPO[evento.tipo] ?? IconActivity
              const rotineiro = !evento.importante
              return (
                <div className={`audit-row${rotineiro ? ' routine' : ''}`} key={`${evento.ocorridoEm}-${evento.tipo}-${evento.seq}`}>
                  <div className="audit-icon" style={{ color: evento.cor }}>
                    <Icone size={16} />
                  </div>
                  <div className="audit-body">
                    <div className="audit-title" style={{ color: rotineiro ? 'var(--muted)' : 'var(--text)' }}>
                      {evento.titulo}
                    </div>
                    <div className="audit-desc">{evento.descricao}</div>
                    {!rotineiro && <div className="audit-raw">{evento.tipo} · {evento.resumo}</div>}
                  </div>
                  <div className="audit-time">#{evento.seq} · {formatarHora(evento.ocorridoEm)}</div>
                </div>
              )
            })}
          </div>
        )}

        <p className="scada-note" style={{ marginTop: 14 }}>
          Arquivo físico: <code>mvp/dados/auditoria.log</code> (cadeia de hash SHA-256 encadeada,
          verificável com <code>mvp/3-ABRIR-LOG-AUDITORIA.bat</code>). O filtro acima só esconde
          medições de rotina nesta tela — nada é removido do log.
        </p>
      </div>
    </section>
  )
}
