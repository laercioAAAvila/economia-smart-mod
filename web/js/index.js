import { session } from './auth.js'
const active = await session()
location.replace(active ? './pages/dashboard.html' : './pages/login.html')
