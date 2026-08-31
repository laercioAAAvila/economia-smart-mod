import { login, session } from './auth.js'

if (await session()) location.replace('./dashboard.html')

const form = document.getElementById('loginForm')
const notice = document.getElementById('notice')
const tokenField = document.getElementById('loginToken')

tokenField.addEventListener('input', () => {
  let raw = tokenField.value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 16)
  tokenField.value = raw.match(/.{1,4}/g)?.join('-') || ''
})

form.addEventListener('submit', async (event) => {
  event.preventDefault()
  notice.classList.add('hidden')
  const button = event.submitter
  button.disabled = true
  try {
    await login(tokenField.value.trim())
    tokenField.value = ''
    location.replace('./dashboard.html')
  } catch (error) {
    tokenField.value = ''
    const messages = {
      invalid_token: 'Token inválido, expirado ou já utilizado. Gere um novo token no ATM.',
      too_many_attempts: 'Muitas tentativas. Aguarde e tente novamente.',
      service_unavailable: 'Serviço bancário indisponível.',
    }
    notice.textContent = messages[error.message] || 'Não foi possível entrar.'
    notice.classList.remove('hidden')
  } finally { button.disabled = false }
})
