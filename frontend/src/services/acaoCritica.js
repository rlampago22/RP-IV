import { startAuthentication } from '@simplewebauthn/browser'
import { authApi } from './authApi.js'

/**
 * RNF08 / 4.2.10 — executa uma ação crítica. Se o backend responder 428 (falta confirmar a digital),
 * pede a digital (Windows Hello ou celular), confirma e tenta a ação de novo. A confirmação vale alguns
 * minutos, então ações críticas seguidas não pedem a digital a cada clique.
 *
 * Uso: await executarAcaoCritica(() => authApi.emitirCodigoAtivacao(id))
 */
export async function executarAcaoCritica(acao) {
  try {
    return await acao()
  } catch (error) {
    if (error.status !== 428) throw error
  }
  const opcoes = await authApi.opcoesConfirmacaoDigital()
  let credencial
  try {
    credencial = await startAuthentication({ optionsJSON: opcoes })
  } catch {
    throw new Error('Confirmação da digital cancelada. A ação não foi feita.')
  }
  await authApi.confirmarDigital(credencial)
  return acao()
}
