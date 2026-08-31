import { api } from './api.js'
import { initPage } from './page.js'
import { money } from './format.js'
const session = await initPage('security')
const account = await api('/account')
document.getElementById('username').textContent = account.username || '—'
document.getElementById('accountNumber').textContent = account.accountNumber || '—'
document.getElementById('balance').textContent = money(account.balance)
document.getElementById('availableBalance').textContent = money(account.availableBalance)
document.getElementById('expiresAt').textContent = session.expiresAt ? new Date(session.expiresAt).toLocaleString('pt-BR') : '—'
document.getElementById('tokenMode').textContent = 'Bearer token temporário'
