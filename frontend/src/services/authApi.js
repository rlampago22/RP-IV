const API_BASE_URL = import.meta.env.VITE_AUTH_API_BASE_URL ?? import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081'

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  })
  if (response.status === 204) return null
  const body = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(body.mensagem ?? 'Não foi possível concluir a solicitação.')
  return body
}

export const authApi = {
  login: (email, senha) => request('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, senha }) }),
  me: () => request('/api/auth/me'),
  logout: () => request('/api/auth/logout', { method: 'POST' }),
  centralStatus: () => request('/api/central/status'),
  // RNF08 — segundo fator: login devolve { etapa: 'BIOMETRIA', opcoes } quando há passkey cadastrada.
  confirmarBiometria: (credencial) => request('/api/auth/login/biometria', { method: 'POST', body: JSON.stringify(credencial) }),
  listarBiometrias: () => request('/api/auth/biometria'),
  opcoesCadastroBiometria: () => request('/api/auth/biometria/cadastro/opcoes', { method: 'POST' }),
  cadastrarBiometria: (apelido, credencial) => request('/api/auth/biometria/cadastro', { method: 'POST', body: JSON.stringify({ apelido, credencial }) }),
  removerBiometria: (id) => request(`/api/auth/biometria/${id}/remover`, { method: 'POST' }),
}
