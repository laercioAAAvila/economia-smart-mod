import { api, clearToken, getToken, setToken } from './api.js'

export async function login(oneTimeToken) {
  const result = await api('/auth/token', {
    method: 'POST',
    body: JSON.stringify({ token: oneTimeToken }),
  })
  setToken(result.token)
  return result
}

export async function session() {
  if (!getToken()) return null
  try { return await api('/auth/session') } catch (_) { return null }
}

export async function requireAuth() {
  const result = await session()
  if (!result) {
    clearToken()
    const prefix = location.pathname.includes('/pages/') ? './' : './pages/'
    location.replace(`${prefix}login.html`)
    throw new Error('unauthorized')
  }
  return result
}

export async function logout() {
  try { await api('/auth/logout', { method: 'POST' }) } catch (_) {}
  clearToken()
  location.replace('./login.html')
}
