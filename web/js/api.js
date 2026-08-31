const API_BASE = '/api/v1'
const TOKEN_KEY = 'economia_web_token'

export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  if (token) sessionStorage.setItem(TOKEN_KEY, token)
  else sessionStorage.removeItem(TOKEN_KEY)
}

export function clearToken() {
  sessionStorage.removeItem(TOKEN_KEY)
}

export async function api(path, options = {}) {
  const headers = new Headers(options.headers || {})
  headers.set('Accept', 'application/json')
  if (options.body) headers.set('Content-Type', 'application/json')
  const token = getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
    cache: 'no-store',
    credentials: 'omit',
  })
  let data = {}
  try { data = await response.json() } catch (_) {}
  if (!response.ok) {
    if (response.status === 401) clearToken()
    const error = new Error(data?.error || `http_${response.status}`)
    error.status = response.status
    throw error
  }
  return data
}
