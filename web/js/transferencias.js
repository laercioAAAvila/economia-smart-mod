import { api } from './api.js'
import { initPage } from './page.js'
import { clearNotice, showNotice } from './layout.js'
import { money } from './format.js'

await initPage('transfer')
const account = await api('/account')
document.getElementById('availableBalance').textContent = money(account.availableBalance)
document.getElementById('sourceAccount').textContent = account.accountNumber || '—'
const form = document.getElementById('transferForm')
const dialog = document.getElementById('confirmDialog')
let pending = null

form.addEventListener('submit', (event) => {
  event.preventDefault(); clearNotice()
  const destinationAccount = document.getElementById('destinationAccount').value.trim()
  const amount = Number(document.getElementById('amount').value)
  if (!/^\d{6}$/.test(destinationAccount) || !Number.isSafeInteger(amount) || amount <= 0) return showNotice('Confira a conta e o valor da transferência.', true)
  if (amount > Number(account.availableBalance)) return showNotice('O valor é maior que o saldo disponível.', true)
  pending = { destinationAccount, amount }
  document.getElementById('confirmDestination').textContent = destinationAccount
  document.getElementById('confirmAmount').textContent = money(amount)
  dialog.showModal()
})

document.getElementById('cancelTransfer').addEventListener('click', () => { pending = null; dialog.close() })
document.getElementById('confirmTransfer').addEventListener('click', async (event) => {
  if (!pending) return
  const button = event.currentTarget
  button.disabled = true
  try {
    const result = await api('/transfers', { method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() }, body: JSON.stringify(pending) })
    dialog.close(); pending = null
    form.reset()
    showNotice(result.status === 'DUPLICATE_COMPLETED' ? 'Essa transferência já havia sido processada.' : 'Transferência concluída com segurança.')
    const updated = await api('/account')
    account.availableBalance = updated.availableBalance
    document.getElementById('availableBalance').textContent = money(updated.availableBalance)
  } catch (error) {
    dialog.close()
    const messages = { insufficient_balance: 'Saldo insuficiente.', destination_not_found: 'Conta de destino não encontrada.', same_account: 'A conta de destino deve ser diferente.', idempotency_conflict: 'Conflito de segurança. Faça uma nova tentativa.', too_many_requests: 'Muitas transferências em pouco tempo. Aguarde um momento.' }
    showNotice(messages[error.message] || 'Não foi possível concluir a transferência.', true)
  } finally { button.disabled = false }
})
