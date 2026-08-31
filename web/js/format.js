export function money(value) {
  const number = Number(value ?? 0)
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 }).format(Number.isFinite(number) ? number : 0)
}

export function dateTime(value) {
  if (!value) return '—'
  const normalized = String(value).includes('T') ? value : String(value).replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('pt-BR')
}

export function transactionName(type) {
  const labels = {
    TRANSFER: 'Transferência', DEPOSIT: 'Depósito', WITHDRAW: 'Saque',
    DEBIT_PURCHASE: 'Compra no débito', CREDIT_PURCHASE: 'Compra no crédito',
    CARD_ISSUE: 'Emissão de cartão', CREDIT_PAYMENT: 'Pagamento de crédito',
    GOLD_MINT: 'Venda de ouro', GOLD_REDEEM: 'Compra de ouro', MAIL_PAYMENT: 'Correio',
    CLAIM_PURCHASE: 'Compra de território', CLAIM_UPGRADE: 'Upgrade de território',
  }
  return labels[type] || type || 'Operação'
}

export function cardType(type) {
  return ({ DEBIT: 'Débito', CREDIT: 'Crédito', DEBIT_CREDIT: 'Débito e crédito' })[type] || type || 'Cartão'
}

export function cardStatus(status) {
  return ({ ACTIVE: 'Ativo', BLOCKED: 'Bloqueado', DISABLED: 'Desativado', EXPIRED: 'Expirado' })[status] || status || '—'
}

export function invoiceType(type) {
  return ({ DAILY_INTEREST: 'Juros diário', PURCHASE: 'Compra', CREDIT_PURCHASE: 'Compra no crédito' })[type] || type || 'Lançamento'
}

export function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char])
}
