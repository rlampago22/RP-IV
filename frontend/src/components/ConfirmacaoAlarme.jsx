import { useEffect, useRef } from 'react'
import { IconBell, IconCheck } from './icons.jsx'

export default function ConfirmacaoAlarme({ acao, onConfirmar, onCancelar }) {
  const dialogoRef = useRef(null)
  const reconhecer = acao?.tipo === 'reconhecer'
  const alarme = acao?.alarme

  useEffect(() => {
    const dialogo = dialogoRef.current
    if (!dialogo || !acao) return

    dialogo.showModal()
    return () => {
      if (dialogo.open) dialogo.close()
    }
  }, [acao])

  if (!acao || !alarme) return null

  return (
    <dialog
      ref={dialogoRef}
      className="alarm-confirm-dialog"
      aria-labelledby="alarm-confirm-title"
      onCancel={(evento) => {
        evento.preventDefault()
        onCancelar()
      }}
      onClick={(evento) => {
        if (evento.target === evento.currentTarget) onCancelar()
      }}
    >
      <div className="alarm-confirm-icon">
        <IconBell size={20} />
      </div>
      <p className="scada-kicker">CONFIRMAÇÃO OPERACIONAL</p>
      <h2 id="alarm-confirm-title">
        {reconhecer ? 'Reconhecer este alarme?' : 'Normalizar e encerrar este alarme?'}
      </h2>
      <p className="alarm-confirm-message">{alarme.mensagem}</p>
      <div className="alarm-confirm-meta">
        <span>Sensor {alarme.sensorId}</span>
        <span className={`scada-pill ${alarme.severidade === 'OBSERVACAO' ? 'warn' : 'crit'}`}>
          {alarme.severidade}
        </span>
      </div>
      <p className="alarm-confirm-note">
        {reconhecer
          ? 'O reconhecimento será registrado com sua identidade na auditoria.'
          : 'Confirme que os parâmetros foram normalizados antes de encerrar a ocorrência.'}
      </p>
      <div className="alarm-confirm-actions">
        <button type="button" className="scada-btn scada-btn-ghost" autoFocus onClick={onCancelar}>
          Cancelar
        </button>
        <button
          type="button"
          className={`scada-btn ${reconhecer ? 'scada-btn-amber' : 'alarm-confirm-submit'}`}
          onClick={onConfirmar}
        >
          <IconCheck size={14} />
          {reconhecer ? 'Confirmar reconhecimento' : 'Confirmar encerramento'}
        </button>
      </div>
    </dialog>
  )
}
