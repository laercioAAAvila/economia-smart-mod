import { api } from './api.js'
import { initPage } from './page.js'
import { escapeHtml, money, transactionName } from './format.js'

await initPage('dashboard')
const [account, transactions, cards, gold] = await Promise.all([api('/account'), api('/transactions'), api('/cards'), api('/gold')])
document.getElementById('balance').textContent = money(account.balance)
document.getElementById('availableBalance').textContent = money(account.availableBalance)
document.getElementById('creditAvailable').textContent = money(account.globalCreditAvailable)
document.getElementById('totalDebt').textContent = money(account.totalDebt)
document.getElementById('accountNumber').textContent = account.accountNumber || '—'
document.getElementById('cardCount').textContent = String(cards.cards?.length || 0)
document.getElementById('goldNugget').textContent = money(gold.nuggetValue)
document.getElementById('goldPercent').textContent = `${gold.buyPercent}% da base`
const root = document.getElementById('recentTransactions')
const list = (transactions.transactions || []).slice(0, 6)
if (!list.length) root.innerHTML = '<div class="empty">Nenhuma movimentação encontrada.</div>'
else root.innerHTML = `<div class="table-wrap"><table><thead><tr><th>Operação</th><th>Direção</th><th>Valor</th><th>Saldo</th></tr></thead><tbody>${list.map(tx => `<tr><td>${escapeHtml(transactionName(tx.type))}</td><td>${tx.direction === 'CREDIT' ? 'Entrada' : 'Saída'}</td><td class="${tx.direction === 'CREDIT' ? 'amount-in' : 'amount-out'}">${tx.direction === 'CREDIT' ? '+' : '-'} ${money(tx.amount)}</td><td>${money(tx.balanceAfter)}</td></tr>`).join('')}</tbody></table></div>`
