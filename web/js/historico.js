import { api } from './api.js'
import { initPage } from './page.js'
import { dateTime, escapeHtml, money, transactionName } from './format.js'
await initPage('history')
const result = await api('/transactions')
const transactions = result.transactions || []
const search = document.getElementById('search')
const direction = document.getElementById('direction')
const root = document.getElementById('historyTable')
function render() {
  const q = search.value.trim().toLowerCase()
  const dir = direction.value
  const rows = transactions.filter(tx => (!dir || tx.direction === dir) && (!q || transactionName(tx.type).toLowerCase().includes(q) || String(tx.counterpartyAccount || '').includes(q)))
  if (!rows.length) { root.innerHTML = '<div class="empty">Nenhuma movimentação para este filtro.</div>'; return }
  root.innerHTML = `<div class="table-wrap"><table><thead><tr><th>Data</th><th>Operação</th><th>Conta relacionada</th><th>Origem</th><th>Valor</th><th>Saldo após</th></tr></thead><tbody>${rows.map(tx => `<tr><td>${escapeHtml(dateTime(tx.createdAt))}</td><td>${escapeHtml(transactionName(tx.type))}</td><td>${escapeHtml(tx.counterpartyAccount || '—')}</td><td>${escapeHtml(tx.origin || 'MINECRAFT')}</td><td class="${tx.direction === 'CREDIT' ? 'amount-in' : 'amount-out'}">${tx.direction === 'CREDIT' ? '+' : '-'} ${money(tx.amount)}</td><td>${money(tx.balanceAfter)}</td></tr>`).join('')}</tbody></table></div>`
}
search.addEventListener('input', render); direction.addEventListener('change', render); render()
