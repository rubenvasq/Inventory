import { useId, useState } from 'react'
import type { CategoryStockReport } from '../api/types'
import './VisualDetails.css'

const colors = ['#4488f5', '#8b71e8', '#23b5aa', '#edaa4c', '#e27b9e', '#5aafdb', '#91b65b', '#c78ae6']
export function InventoryChart({ categories, metric }: { categories: CategoryStockReport[]; metric: 'totalUnits' | 'totalValue' }) {
  const id = useId().replace(/:/g, '')
  const [selected, setSelected] = useState<number | null>(null)
  const format = (value: number) => new Intl.NumberFormat('es-CO', metric === 'totalValue' ? { style: 'currency', currency: 'COP', maximumFractionDigits: 0 } : {}).format(value)
  const total = categories.reduce((sum, c) => sum + c[metric], 0)
  const maximum = Math.max(1, ...categories.map(c => c[metric]))
  const active = categories.find(c => c.categoryId === selected) ?? categories[0]
  if (!active) return <p className="dashboard-empty">Todavía no hay categorías con inventario.</p>
  return <div className="rich-chart">
    <div className="chart-insight"><div><span>{metric === 'totalValue' ? 'Valor en las categorías principales' : 'Unidades en las categorías principales'}</span><strong>{format(total)}</strong></div><span className="chart-count">{categories.length} categorías</span></div>
    <svg className="inventory-bars" viewBox="0 0 640 256" role="img" aria-label="Comparación del inventario por categoría">
      <defs>{colors.map((color, i) => <linearGradient key={color} id={`${id}-${i}`} x1="0" y1="0" x2="0" y2="1"><stop stopColor={color} stopOpacity=".65"/><stop offset="1" stopColor={color}/></linearGradient>)}</defs>
      {[0, .25, .5, .75, 1].map(t => <g key={t}><line x1="65" x2="628" y1={210 - t * 170} y2={210 - t * 170} stroke="currentColor" strokeOpacity=".12" strokeDasharray="4 5"/><text x="55" y={214 - t * 170} textAnchor="end" fill="currentColor" fontSize="11">{new Intl.NumberFormat('es-CO', { notation: 'compact', maximumFractionDigits: 1 }).format(maximum * t)}</text></g>)}
      {categories.map((c, i) => { const step = 552 / categories.length; const width = Math.min(42, step * .6); const x = 75 + i * step + (step - width) / 2; const height = c[metric] / maximum * 170; return <g key={c.categoryId} opacity={selected !== null && selected !== c.categoryId ? .4 : 1}><rect x={x} y="40" width={width} height="170" rx="7" fill={colors[i % colors.length]} opacity=".06"/><rect x={x} y={210 - height} width={width} height={height} rx="7" fill={`url(#${id}-${i % colors.length})`}><title>{c.categoryName}: {format(c[metric])}</title></rect><text x={x + width / 2} y={Math.max(26, 200 - height)} textAnchor="middle" fill="currentColor" fontSize="11" fontWeight="600">{new Intl.NumberFormat('es-CO', { notation: 'compact' }).format(c[metric])}</text><text x={x + width / 2} y="236" textAnchor="middle" fill="currentColor" fontSize="11">{c.categoryName.length > 9 ? `${c.categoryName.slice(0, 8)}…` : c.categoryName}</text></g> })}
    </svg>
    <div className="category-chips">{categories.map((c, i) => <button type="button" key={c.categoryId} aria-pressed={active.categoryId === c.categoryId} onClick={() => setSelected(c.categoryId)}><i style={{ background: colors[i % colors.length] }}/>{c.categoryName}</button>)}</div>
    <div className="chart-detail"><strong>{active.categoryName}</strong><span>{format(active[metric])} · {total ? (active[metric] / total * 100).toFixed(1) : 0}% del gráfico</span></div>
  </div>
}
