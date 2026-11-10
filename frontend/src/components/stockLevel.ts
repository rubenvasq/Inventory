export function stockLevel(stock: number, minStock: number, active = true) {
  const quantity = Number.isFinite(stock) ? Math.max(0, stock) : 0
  const minimum = Number.isFinite(minStock) ? Math.max(0, minStock) : 0
  const target = minimum * 2
  const percent = target > 0 ? Math.min(100, quantity / target * 100) : quantity > 0 ? 100 : 0
  const status = !active ? 'Archivado' : quantity === 0 ? 'Sin stock' : quantity < minimum ? 'Stock bajo' : 'Disponible'
  const tone = !active ? 'inactive' : quantity === 0 ? 'empty' : quantity < minimum ? 'low' : 'ok'
  return { percent, status, tone, minimum, quantity, target }
}

export function summarizeStock(products: { stock: number; minStock: number; active: boolean }[]) {
  const active = products.filter(product => product.active)
  const levels = active.map(product => stockLevel(product.stock, product.minStock))
  const empty = levels.filter(level => level.tone === 'empty').length
  const low = levels.filter(level => level.tone === 'low').length
  const healthy = levels.length - empty - low
  return { total: active.length, empty, low, healthy,
    percent: active.length ? Math.round(healthy / active.length * 100) : 0,
    status: !active.length ? 'Sin productos activos' : empty ? `${empty} sin stock` : low ? `${low} con stock bajo` : 'Saludable',
    tone: !active.length ? 'inactive' : empty ? 'empty' : low ? 'low' : 'ok',
  }
}
