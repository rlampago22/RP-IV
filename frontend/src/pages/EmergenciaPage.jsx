import { useEffect, useState } from 'react'
import { authApi } from '../services/authApi.js'
import { executarAcaoCritica } from '../services/acaoCritica.js'
import { IconAlertTriangle, IconShield } from '../components/icons.jsx'
import { EVENTO_EMERGENCIAS } from '../components/FaixaEmergencia.jsx'

const TIPOS = {
  SUPERAQUECIMENTO: 'Superaquecimento do núcleo',
  VAZAMENTO_RADIOATIVO: 'Vazamento radioativo',
  FALHA_REFRIGERACAO: 'Falha de refrigeração',
  OUTRA: 'Outra',
}

const STATUS = {
  ABERTA: { texto: 'ABERTA', classe: 'crit' },
  PROTOCOLO_ATIVO: { texto: 'PROTOCOLO ATIVO', classe: 'warn' },
  ENCERRADA: { texto: 'ENCERRADA', classe: 'ok' },
}

function formatarData(iso) {
  return iso ? new Date(iso).toLocaleString('pt-BR', { hour12: false }) : '—'
}

/**
 * RF09 (criar emergência) e RF10 (ativar protocolo de contingência). Registrar é imediato; ativar o
 * protocolo e encerrar são ações críticas e pedem a digital (4.2.10 do PDF: dois fatores).
 */
export default function EmergenciaPage() {
  const [emergencias, setEmergencias] = useState(null)
  const [protocolos, setProtocolos] = useState([])
  const [tipo, setTipo] = useState('SUPERAQUECIMENTO')
  const [descricao, setDescricao] = useState('')
  const [escolhido, setEscolhido] = useState({})
  const [ocupado, setOcupado] = useState(false)
  const [aviso, setAviso] = useState(null)

  async function carregar() {
    try {
      setEmergencias(await authApi.listarEmergencias())
    } catch (error) {
      setAviso({ tipo: 'erro', texto: error.message })
      setEmergencias([])
    }
  }

  useEffect(() => {
    carregar()
    authApi.listarProtocolos().then(setProtocolos).catch(() => setProtocolos([]))
  }, [])

  async function executar(acao, textoOk) {
    setOcupado(true)
    setAviso(null)
    try {
      await acao()
      setAviso({ tipo: 'ok', texto: textoOk })
      await carregar()
      window.dispatchEvent(new Event(EVENTO_EMERGENCIAS)) // avisa a faixa do topo, sem polling
    } catch (error) {
      setAviso({ tipo: 'erro', texto: error.message })
    } finally {
      setOcupado(false)
    }
  }

  function registrar(event) {
    event.preventDefault()
    executar(async () => {
      await authApi.criarEmergencia(tipo, descricao)
      setDescricao('')
    }, 'Emergência registrada. Escolha o protocolo de contingência e ative com a sua digital.')
  }

  function ativar(emergencia) {
    const codigo = escolhido[emergencia.id] ?? protocolos[0]?.codigo
    const nome = protocolos.find((p) => p.codigo === codigo)?.nome ?? codigo
    if (!window.confirm(`Ativar o protocolo "${nome}"? A Central vai pedir a sua digital.`)) return
    executar(
      () => executarAcaoCritica(() => authApi.ativarProtocolo(emergencia.id, codigo)),
      `Protocolo "${nome}" ativado e registrado na trilha de auditoria.`,
    )
  }

  function encerrar(emergencia) {
    if (!window.confirm('Encerrar esta emergência? A Central vai pedir a sua digital.')) return
    executar(
      () => executarAcaoCritica(() => authApi.encerrarEmergencia(emergencia.id)),
      'Emergência encerrada e registrada na trilha de auditoria.',
    )
  }

  return (
    <section>
      <p className="scada-kicker">RF09 · RF10 · GESTÃO DE EMERGÊNCIAS</p>
      <h1 className="scada-heading">Emergência e protocolo de contingência</h1>
      <p className="scada-lead">
        Registrar uma emergência é imediato. <strong>Ativar o protocolo de contingência</strong> e{' '}
        <strong>encerrar</strong> são ações críticas: além da sessão aberta, pedem a sua digital, para evitar
        ativação acidental (dois fatores). Cada passo fica na trilha de auditoria da aba Segurança.
      </p>

      <div className="scada-card">
        <h2 className="panel-heading">
          <IconAlertTriangle size={13} />
          Registrar emergência (RF09)
        </h2>
        <form className="auth-form" style={{ maxWidth: 520 }} onSubmit={registrar}>
          <label>
            Tipo
            <select value={tipo} onChange={(event) => setTipo(event.target.value)}>
              {Object.entries(TIPOS).map(([codigo, texto]) => (
                <option key={codigo} value={codigo}>{texto}</option>
              ))}
            </select>
          </label>
          <label>
            O que está acontecendo
            <input
              value={descricao}
              maxLength={400}
              onChange={(event) => setDescricao(event.target.value)}
              placeholder="Ex.: temperatura do núcleo acima de 350 °C e subindo"
              required
            />
          </label>
          <button type="submit" className="scada-btn" disabled={ocupado}>
            Registrar emergência
          </button>
        </form>
        {aviso && (
          <p
            className={`scada-note ${aviso.tipo === 'ok' ? 'scada-note-ok' : 'scada-note-erro'}`}
            role="status"
            style={{ marginTop: 14 }}
          >
            {aviso.texto}
          </p>
        )}
      </div>

      <div className="scada-card" style={{ marginTop: 16 }}>
        <h2 className="panel-heading">
          <IconShield size={13} />
          Emergências e protocolos (RF10)
        </h2>
        {emergencias === null && <p className="scada-lead">Carregando…</p>}
        {emergencias !== null && emergencias.length === 0 && (
          <p className="scada-lead" style={{ marginTop: 0 }}>Nenhuma emergência registrada.</p>
        )}
        {emergencias?.map((emergencia) => (
          <article key={emergencia.id} className="emergencia-item">
            <header>
              <strong>
                #{emergencia.id} · {TIPOS[emergencia.tipo] ?? emergencia.tipo}
              </strong>
              <span className={`scada-pill ${STATUS[emergencia.status]?.classe ?? ''}`}>
                {STATUS[emergencia.status]?.texto ?? emergencia.status}
              </span>
            </header>
            <p>{emergencia.descricao}</p>
            <small>
              Registrada por {emergencia.criadaPor} em {formatarData(emergencia.criadaEm)}
            </small>

            {emergencia.protocolo && (
              <div className="emergencia-protocolo">
                <strong>Protocolo: {emergencia.protocolo.nome}</strong>
                <small>
                  Ativado por {emergencia.protocoloAtivadoPor} em {formatarData(emergencia.protocoloAtivadoEm)}{' '}
                  (confirmado com a digital)
                </small>
                <ol>
                  {emergencia.protocolo.passos.map((passo) => (
                    <li key={passo}>{passo}</li>
                  ))}
                </ol>
              </div>
            )}

            {emergencia.status === 'ENCERRADA' && (
              <small>
                Encerrada por {emergencia.encerradaPor} em {formatarData(emergencia.encerradaEm)}
              </small>
            )}

            {emergencia.status !== 'ENCERRADA' && (
              <div className="emergencia-acoes">
                {emergencia.status === 'ABERTA' && (
                  <>
                    <select
                      aria-label="Protocolo de contingência"
                      value={escolhido[emergencia.id] ?? protocolos[0]?.codigo ?? ''}
                      onChange={(event) => setEscolhido({ ...escolhido, [emergencia.id]: event.target.value })}
                    >
                      {protocolos.map((protocolo) => (
                        <option key={protocolo.codigo} value={protocolo.codigo}>{protocolo.nome}</option>
                      ))}
                    </select>
                    <button
                      type="button"
                      className="scada-btn scada-btn-amber"
                      disabled={ocupado || protocolos.length === 0}
                      onClick={() => ativar(emergencia)}
                    >
                      Ativar protocolo (pede a digital)
                    </button>
                  </>
                )}
                <button
                  type="button"
                  className="scada-btn scada-btn-ghost"
                  disabled={ocupado}
                  onClick={() => encerrar(emergencia)}
                >
                  Encerrar emergência
                </button>
              </div>
            )}
          </article>
        ))}
      </div>
    </section>
  )
}
