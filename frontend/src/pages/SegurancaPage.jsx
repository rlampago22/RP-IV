import { useEffect, useState } from 'react'
import { startRegistration } from '@simplewebauthn/browser'
import { authApi } from '../services/authApi.js'
import { IconLock } from '../components/icons.jsx'

function formatarData(iso) {
  return iso ? new Date(iso).toLocaleString('pt-BR', { hour12: false }) : '—'
}

/** RNF08 — cadastro do celular como segundo fator do login (passkey / WebAuthn). */
export default function SegurancaPage() {
  // null = carregando
  const [biometrias, setBiometrias] = useState(null)
  const [apelido, setApelido] = useState('Meu celular')
  const [ocupado, setOcupado] = useState(false)
  const [aviso, setAviso] = useState(null)

  async function carregar() {
    try {
      setBiometrias(await authApi.listarBiometrias())
    } catch (error) {
      setBiometrias([])
      setAviso({ tipo: 'erro', texto: error.message })
    }
  }

  useEffect(() => {
    carregar()
  }, [])

  async function cadastrar() {
    setOcupado(true)
    setAviso(null)
    try {
      const opcoes = await authApi.opcoesCadastroBiometria()
      let credencial
      try {
        credencial = await startRegistration({ optionsJSON: opcoes })
      } catch {
        throw new Error('Cadastro cancelado ou não concluído no celular.')
      }
      await authApi.cadastrarBiometria(apelido, credencial)
      setAviso({ tipo: 'ok', texto: 'Biometria cadastrada. A partir do próximo login, a Central vai pedir a confirmação no celular.' })
      await carregar()
    } catch (error) {
      setAviso({ tipo: 'erro', texto: error.message })
    } finally {
      setOcupado(false)
    }
  }

  async function remover(biometria) {
    if (!window.confirm(`Remover "${biometria.apelido}"? Ele deixa de servir para confirmar o login.`)) return
    setOcupado(true)
    setAviso(null)
    try {
      await authApi.removerBiometria(biometria.id)
      await carregar()
    } catch (error) {
      setAviso({ tipo: 'erro', texto: error.message })
    } finally {
      setOcupado(false)
    }
  }

  const ativo = (biometrias?.length ?? 0) > 0

  return (
    <section>
      <p className="scada-kicker">RNF08 · AUTENTICAÇÃO EM DOIS FATORES</p>
      <h1 className="scada-heading">Segurança do acesso</h1>
      <p className="scada-lead">
        Além da senha, o login pode exigir a confirmação no seu celular com digital ou rosto
        (passkey / WebAuthn). A biometria nunca sai do aparelho: a Central guarda só uma chave
        pública e confere a assinatura que o celular devolve.
      </p>

      <div className="scada-card" style={{ marginTop: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap', marginBottom: 14 }}>
          <h2 className="panel-heading" style={{ margin: 0 }}>
            <IconLock size={13} />
            Biometria do celular
          </h2>
          {biometrias !== null && (
            <span className={`scada-pill ${ativo ? 'ok' : 'warn'}`}>
              {ativo ? 'DOIS FATORES ATIVO' : 'SÓ SENHA'}
            </span>
          )}
        </div>

        {biometrias === null && <p className="scada-lead">Carregando…</p>}
        {biometrias !== null && biometrias.length === 0 && (
          <p className="scada-lead">Nenhum celular cadastrado. Hoje o seu login usa só a senha.</p>
        )}
        {biometrias !== null && biometrias.length > 0 && (
          <div className="scada-table-wrap" style={{ maxHeight: 'none' }}>
            <table className="scada-table">
              <thead>
                <tr>
                  <th>Aparelho</th>
                  <th>Cadastrado em</th>
                  <th>Último uso</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {biometrias.map((biometria) => (
                  <tr key={biometria.id}>
                    <td className="ev">{biometria.apelido}</td>
                    <td>{formatarData(biometria.criadoEm)}</td>
                    <td>{formatarData(biometria.ultimoUsoEm)}</td>
                    <td style={{ textAlign: 'right' }}>
                      <button type="button" className="scada-btn scada-btn-ghost" disabled={ocupado} onClick={() => remover(biometria)}>
                        Remover
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="auth-form" style={{ marginTop: 16, maxWidth: 420 }}>
          <label>
            Nome do aparelho
            <input value={apelido} maxLength={80} onChange={(event) => setApelido(event.target.value)} />
          </label>
          <button type="button" className="scada-btn" disabled={ocupado} onClick={cadastrar}>
            {ocupado ? 'Aguardando o celular…' : 'Cadastrar celular'}
          </button>
        </div>

        {aviso && (
          <p className={`scada-note ${aviso.tipo === 'ok' ? 'scada-note-ok' : 'scada-note-erro'}`} role="status" style={{ marginTop: 14 }}>
            {aviso.texto}
          </p>
        )}

        <p className="scada-note" style={{ marginTop: 14 }}>
          Como funciona: ao cadastrar, o navegador mostra um QR code; escaneie com a câmera do
          celular (Bluetooth do computador ligado) e confirme com a digital ou o rosto. Se preferir,
          o navegador também aceita o Windows Hello deste computador.
        </p>
      </div>
    </section>
  )
}
