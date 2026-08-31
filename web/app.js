'use strict'

const API = '/api/v1'
let token = null

const $ = (id) => document.getElementById(id)
const loginView = $('loginView')
const appView = $('appView')
const notice = $('notice')
const logoutButton = $('logoutButton')

function showNotice(message, error = false) {
  notice.textContent = message
  notice.classList.remove('hidden', 'error')
  if (error) notice.classList.add('error')
}

function clearNotice() {
  notice.classList.add('hidden')
  notice.textContent = ''
}

function money(value) {
  const parsed = Number(value ?? 0)
  return `R$ ${Number.isFinite(parsed) ? parsed.toLocaleString('pt-BR') : '0'}`
}

async function api(path, options = {}) {
  const headers = new Headers(options.headers || {})
  headers.set('Accept', 'application/json')
  if (options.body) headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const response = await fetch(`${API}${path}`, { ...options, headers, cache: 'no-store' })
  let data = null
  try { data = await response.json() } catch (_) { data = {} }
  if (!response.ok) {
    const error = new Error(data?.error || `http_${response.status}`)
    error.status = response.status
    throw error
  }
  return data
}

function setAuthenticated(value) {
  loginView.classList.toggle('hidden', value)
  appView.classList.toggle('hidden', !value)
  logoutButton.classList.toggle('hidden', !value)
}

async function loadAccount() {
  const account = await api('/account')
  $('accountNumber').textContent = account.accountNumber || '—'
  $('balance').textContent = money(account.balance)
  $('availableBalance').textContent = money(account.availableBalance)
  $('totalDebt').textContent = money(account.totalDebt)
}

function transactionLabel(tx) {
  const names = {
    TRANSFER: 'Transferência',
    DEPOSIT: 'Depósito',
    WITHDRAW: 'Saque',
    DEBIT_PURCHASE: 'Compra no débito',
    CREDIT_PURCHASE: 'Compra no crédito',
    CARD_ISSUE: 'Emissão de cartão',
    CREDIT_PAYMENT: 'Pagamento de crédito',
  }
  return names[tx.type] || tx.type || 'Operação'
}

async function loadTransactions() {
  const result = await api('/transactions')
  const root = $('transactions')
  root.replaceChildren()
  const transactions = Array.isArray(result.transactions) ? result.transactions : []
  if (!transactions.length) {
    const empty = document.createElement('div')
    empty.className = 'empty'
    empty.textContent = 'Nenhuma movimentação encontrada.'
    root.appendChild(empty)
    return
  }
  for (const tx of transactions) {
    const row = document.createElement('div')
    row.className = 'transaction'
    const title = document.createElement('strong')
    title.textContent = transactionLabel(tx)
    const amount = document.createElement('span')
    amount.className = 'amount'
    amount.textContent = money(tx.amount)
    const meta = document.createElement('small')
    const when = tx.createdAt ? new Date(tx.createdAt).toLocaleString('pt-BR') : ''
    meta.textContent = `${tx.origin || 'MINECRAFT'}${when ? ` • ${when}` : ''}`
    const balance = document.createElement('small')
    balance.textContent = `Saldo após: ${money(tx.balanceAfter)}`
    row.append(title, amount, meta, balance)
    root.appendChild(row)
  }
}

async function refreshAll() {
  await Promise.all([loadAccount(), loadTransactions()])
}

$('loginForm').addEventListener('submit', async (event) => {
  event.preventDefault()
  clearNotice()
  const button = event.submitter
  button.disabled = true
  try {
    const result = await api('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username: $('username').value.trim(), password: $('password').value }),
    })
    token = result.token
    $('password').value = ''
    setAuthenticated(true)
    await refreshAll()
    showNotice('Login realizado com sucesso.')
  } catch (error) {
    $('password').value = ''
    showNotice(error.message === 'invalid_credentials' ? 'Usuário ou senha inválidos.' : 'Não foi possível entrar.', true)
  } finally {
    button.disabled = false
  }
})

$('transferForm').addEventListener('submit', async (event) => {
  event.preventDefault()
  clearNotice()
  const button = event.submitter
  button.disabled = true
  const passwordField = $('transferPassword')
  try {
    const idempotencyKey = crypto.randomUUID()
    const result = await api('/transfers', {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey },
      body: JSON.stringify({
        destinationAccount: $('destinationAccount').value,
        amount: Number($('amount').value),
        password: passwordField.value,
      }),
    })
    passwordField.value = ''
    $('amount').value = ''
    showNotice(result.status === 'DUPLICATE_COMPLETED' ? 'Transferência já processada anteriormente.' : 'Transferência concluída.')
    await refreshAll()
  } catch (error) {
    passwordField.value = ''
    const messages = {
      insufficient_balance: 'Saldo insuficiente.',
      destination_not_found: 'Conta de destino não encontrada.',
      invalid_credentials: 'Senha inválida.',
      idempotency_conflict: 'Conflito de segurança na requisição. Tente novamente.',
      same_account: 'A conta de destino deve ser diferente.',
    }
    showNotice(messages[error.message] || 'Não foi possível concluir a transferência.', true)
  } finally {
    button.disabled = false
  }
})

$('refreshButton').addEventListener('click', async () => {
  clearNotice()
  try { await refreshAll() } catch (_) { showNotice('Falha ao atualizar os dados.', true) }
})

logoutButton.addEventListener('click', async () => {
  try { if (token) await api('/auth/logout', { method: 'POST' }) } catch (_) {}
  token = null
  setAuthenticated(false)
  clearNotice()
})

setAuthenticated(false)
