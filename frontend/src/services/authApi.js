const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

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
}
