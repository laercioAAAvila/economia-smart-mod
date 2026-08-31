import { requireAuth } from './auth.js'
import { mountLayout } from './layout.js'

export async function initPage(active) {
  const user = await requireAuth()
  mountLayout(active, user)
  return user
}
