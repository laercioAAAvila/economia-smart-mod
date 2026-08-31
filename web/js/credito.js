import { api } from './api.js'
import { initPage } from './page.js'
import { dateTime, escapeHtml, invoiceType, money } from './format.js'
await initPage('credit')
const credit = await api('/credit')
document.getElementById('configuredLimit').textContent = money(credit.configuredLimit)
document.getElementById('availableCredit').textContent = money(credit.availableCredit)
document.getElementById('principal').textContent = money(credit.principalOutstanding)
document.getElementById('interest').textContent = money(credit.interestOutstanding)
document.getElementById('totalDebt').textContent = money(credit.totalDebt)
document.getElementById('dueDay').textContent = `Dia ${credit.dueDay}`
const root = document.getElementById('invoiceEntries')
const rows = credit.entries || []
if (!rows.length) root.innerHTML = '<div class="empty">Nenhum lançamento aberto na fatura.</div>'
else root.innerHTML = `<div class="table-wrap"><table><thead><tr><th>Data</th><th>Descrição</th><th>Tipo</th><th>Estabelecimento</th><th>Valor aberto</th></tr></thead><tbody>${rows.map(entry => `<tr><td>${escapeHtml(entry.businessDate || dateTime(entry.createdAt))}</td><td>${escapeHtml(entry.description || 'Lançamento de crédito')}</td><td>${escapeHtml(invoiceType(entry.type))}</td><td>${escapeHtml(entry.merchant || '—')}</td><td>${money(entry.amount)}</td></tr>`).join('')}</tbody></table></div>`
