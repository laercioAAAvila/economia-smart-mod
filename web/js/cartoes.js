import { api } from './api.js'
import { initPage } from './page.js'
import { cardStatus, cardType, escapeHtml, money } from './format.js'
await initPage('cards')
const result = await api('/cards')
const root = document.getElementById('cardsList')
const cards = result.cards || []
document.getElementById('cardsCount').textContent = String(cards.length)
if (!cards.length) root.innerHTML = '<div class="empty">Nenhum cartão ativo ou bloqueado encontrado.</div>'
else root.innerHTML = cards.map((card, index) => `<article class="card bank-card"><div class="bank-card-head"><div><div class="bank-card-name">${escapeHtml(card.name || `Cartão ${index + 1}`)}</div><div class="bank-card-meta">${escapeHtml(cardType(card.type))}</div></div><span class="badge ${card.status === 'ACTIVE' ? 'good' : card.status === 'BLOCKED' ? 'warn' : 'bad'}">${escapeHtml(cardStatus(card.status))}</span></div><div class="bank-card-values"><div><span>Limite de crédito</span><strong>${money(card.creditLimit)}</strong></div><div><span>Dívida</span><strong>${money(card.debt)}</strong></div><div><span>Limite diário débito</span><strong>${money(card.debitDailyLimit)}</strong></div><div><span>Uso no site</span><strong>Consulta</strong></div></div></article>`).join('')
