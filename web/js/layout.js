import { logout } from './auth.js'

const links = [
  ['dashboard', 'Visão geral', 'dashboard.html'],
  ['transfer', 'Transferências', 'transferencias.html'],
  ['history', 'Histórico', 'historico.html'],
  ['cards', 'Cartões', 'cartoes.html'],
  ['credit', 'Crédito e faturas', 'credito.html'],
  ['gold', 'Cotação do ouro', 'ouro.html'],
  ['security', 'Segurança', 'seguranca.html'],
]

export function mountLayout(active, session) {
  const sidebar = document.getElementById('sidebar')
  if (!sidebar) return
  sidebar.className = 'sidebar'
  sidebar.innerHTML = `
    <div class="brand"><img src="../assets/logo.png" alt=""><div class="brand-text"><strong>Economia Smart</strong><span>Internet Banking</span></div></div>
    <nav class="nav" aria-label="Navegação principal">
      ${links.map(([id, label, href]) => `<a class="${id === active ? 'active' : ''}" href="./${href}"><span class="nav-mark"></span>${label}</a>`).join('')}
    </nav>
    <div class="sidebar-footer">
      <div class="user-mini"><strong>${escapeHtml(session?.username || 'Conta')}</strong><span>Conta ${escapeHtml(session?.accountNumber || '—')}</span></div>
      <button id="logoutButton" class="button secondary w-full" type="button">Sair</button>
    </div>`
  document.getElementById('logoutButton')?.addEventListener('click', logout)

  const mobile = document.getElementById('mobileHeader')
  if (mobile) {
    mobile.className = 'mobile-header'
    mobile.innerHTML = `<div class="mobile-brand"><img src="../assets/logo.png" alt="">Economia Smart</div><button id="menuButton" class="icon-button" type="button" aria-label="Abrir menu">Menu</button>`
    document.getElementById('menuButton')?.addEventListener('click', () => document.body.classList.toggle('nav-open'))
  }
  document.addEventListener('click', (event) => {
    if (document.body.classList.contains('nav-open') && !sidebar.contains(event.target) && event.target?.id !== 'menuButton') {
      document.body.classList.remove('nav-open')
    }
  })
}

export function showNotice(message, error = false) {
  const notice = document.getElementById('notice')
  if (!notice) return
  notice.textContent = message
  notice.classList.toggle('error', error)
  notice.classList.remove('hidden')
}

export function clearNotice() {
  document.getElementById('notice')?.classList.add('hidden')
}

function escapeHtml(value) {
  return String(value).replace(/[&<>'"]/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' })[char])
}
