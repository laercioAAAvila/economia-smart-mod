import { api } from './api.js'
import { initPage } from './page.js'
import { money } from './format.js'
await initPage('gold')
const gold = await api('/gold')
document.getElementById('nugget').textContent = money(gold.nuggetValue)
document.getElementById('ingot').textContent = money(gold.ingotValue)
document.getElementById('block').textContent = money(gold.blockValue)
document.getElementById('buyPercent').textContent = `${gold.buyPercent}%`
document.getElementById('sellPercent').textContent = `${gold.sellPercent}%`
document.getElementById('pricingMode').textContent = gold.dynamicPricing ? 'Dinâmica' : 'Fixa'
document.getElementById('demandLevel').textContent = String(gold.demandLevel ?? 0)
document.getElementById('idleLevel').textContent = String(gold.idleLevel ?? 0)
