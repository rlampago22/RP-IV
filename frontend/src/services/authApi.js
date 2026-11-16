export const API_BASE_URL = import.meta.env.VITE_AUTH_API_BASE_URL ?? import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081'

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  })
  if (response.status === 204) return null
  const body = await response.json().catch(() => ({}))
  if (!response.ok) {
    const erro = new Error(body.mensagem ?? 'Não foi possível concluir a solicitação.')
    erro.status = response.status
    throw erro
  }
  return body
}

export const authApi = {
  login: (email, senha) => request('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, senha }) }),
  // RF18 — 2FA por celular: concluir o login com o código ou depois de aprovar no celular.
  segundoFatorCodigo: (codigo) => request('/api/auth/segundo-fator/codigo', { method: 'POST', body: JSON.stringify({ codigo }) }),
  segundoFatorAprovacao: () => request('/api/auth/segundo-fator/aprovacao', { method: 'POST' }),
  celularNotificacoes: () => request('/api/celular-simulado/notificacoes'),
  celularAprovar: (id) => request(`/api/celular-simulado/${id}/aprovar`, { method: 'POST' }),
  celularRecusar: (id) => request(`/api/celular-simulado/${id}/recusar`, { method: 'POST' }),
  auditoriaAutenticacao: () => request('/api/admin/auditoria/autenticacao'),
  me: () => request('/api/auth/me'),
  logout: () => request('/api/auth/logout', { method: 'POST' }),
  centralStatus: () => request('/api/central/status'),
  // RNF08 — digital como segundo fator das ações críticas. Cadastro só com o código de ativação do Administrador.
  listarBiometrias: () => request('/api/auth/biometria'),
  opcoesCadastroBiometria: (codigo) => request('/api/auth/biometria/cadastro/opcoes', { method: 'POST', body: JSON.stringify({ codigo }) }),
  cadastrarBiometria: (apelido, credencial) => request('/api/auth/biometria/cadastro', { method: 'POST', body: JSON.stringify({ apelido, credencial }) }),
  removerBiometria: (id) => request(`/api/auth/biometria/${id}/remover`, { method: 'POST' }),
  opcoesConfirmacaoDigital: () => request('/api/auth/biometria/confirmacao/opcoes', { method: 'POST' }),
  confirmarDigital: (credencial) => request('/api/auth/biometria/confirmacao', { method: 'POST', body: JSON.stringify(credencial) }),
  // Administrador do Sistema (UC02): cadastro prévio e revogação.
  listarUsuarios: () => request('/api/admin/usuarios'),
  emitirCodigoAtivacao: (id) => request(`/api/admin/usuarios/${id}/codigo-ativacao`, { method: 'POST' }),
  revogarBiometriaDe: (id) => request(`/api/admin/usuarios/${id}/biometria/revogar`, { method: 'POST' }),
}
