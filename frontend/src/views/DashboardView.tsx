import { useEffect, useState, type ReactNode } from 'react'
import { dashboardApi, productsApi } from '../api/endpoints'
import { errorMessage } from '../api/formError'
import type { CategoryStockReport, DashboardSummary, Product } from '../api/types'
import { useToast } from '../components/Toast'
import { DesignIcon } from '../components/DesignIcon'

const currency = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })
const number = new Intl.NumberFormat('es-CO')

export function DashboardView({ onProducts }: { onProducts: (low?: boolean) => void }) {
  const toast = useToast()
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [categories, setCategories] = useState<CategoryStockReport[]>([])
  const [lowStock, setLowStock] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)
  const [metric, setMetric] = useState<'totalUnits' | 'totalValue'>('totalUnits')
  const [reload, setReload] = useState(0)
  useEffect(() => {
    let live = true
    Promise.all([dashboardApi.summary(), dashboardApi.byCategory(), productsApi.lowStock()])
      .then(([s, c, l]) => { if (live) { setSummary(s); setCategories(c); setLowStock(l) } })
      .catch(e => { if (live) { setFailed(true); toast.error(errorMessage(e)) } })
      .finally(() => { if (live) setLoading(false) })
    return () => { live = false }
  }, [reload]) // eslint-disable-line react-hooks/exhaustive-deps
  if (loading) return <div className="dashboard-status"><span className="spinner" />Cargando tu inventario…</div>
  if (failed || !summary) return <div className="dashboard-status"><h2>No se pudo cargar el panel</h2><p className="muted">Comprueba la conexión con el servidor.</p><button className="btn btn-primary" onClick={() => { setLoading(true); setFailed(false); setReload(v => v + 1) }}>Reintentar</button></div>
  const healthy = Math.max(0, summary.totalProducts - summary.lowStockCount)
  const healthyPercent = summary.totalProducts ? Math.round(healthy / summary.totalProducts * 100) : 0
  const ranked = [...categories].sort((a, b) => b.totalValue - a.totalValue)
  const chart = [...categories].sort((a, b) => b[metric] - a[metric]).slice(0, 8)
  const maximum = Math.max(1, ...chart.map(c => c[metric]))
  return <section className="inventory-dashboard" aria-label="Panel de inventario">
    <SummaryCard title="Resumen del inventario" className="overview-card">
      <div className="summary-metrics four-metrics">
        <Metric icon="sales" color="blue" label="Productos" value={number.format(summary.totalProducts)} />
        <Metric icon="revenue" color="purple" label="Valor del inventario" value={currency.format(summary.totalInventoryValue)} />
        <Metric icon="profit" color="orange" label="Stock bajo" value={number.format(summary.lowStockCount)} />
        <Metric icon="cost" color="green" label="Unidades" value={number.format(summary.totalStockUnits)} />
      </div>
    </SummaryCard>
    <SummaryCard title="Existencias disponibles" className="stock-summary-card">
      <div className="summary-metrics"><Metric icon="quantity" color="orange" label="Unidades en stock" value={number.format(summary.totalStockUnits)} /><Metric icon="way" color="purple" label="Productos por reponer" value={number.format(summary.lowStockCount)} /></div>
    </SummaryCard>
    <SummaryCard title="Estado del catálogo" className="catalog-card">
      <div className="summary-metrics four-metrics"><Metric icon="purchase" color="cyan" label="Stock suficiente" value={number.format(healthy)} /><Metric icon="cost" color="green" label="Disponibilidad" value={`${healthyPercent}%`} /><Metric icon="cancel" color="purple" label="Alertas de stock" value={number.format(summary.lowStockCount)} /><Metric icon="profit" color="orange" label="Valor medio / unidad" value={currency.format(summary.totalStockUnits ? summary.totalInventoryValue / summary.totalStockUnits : 0)} /></div>
    </SummaryCard>
    <SummaryCard title="Resumen de productos" className="product-summary-card"><div className="summary-metrics"><Metric icon="suppliers" color="cyan" label="Proveedores" value={number.format(summary.totalSuppliers)} /><Metric icon="categories" color="purple" label="Categorías" value={number.format(summary.totalCategories)} /></div></SummaryCard>
    <SummaryCard title="Inventario por categoría" className="category-chart-card" action={<select aria-label="Métrica del gráfico" className="chart-select" value={metric} onChange={e => setMetric(e.target.value as typeof metric)}><option value="totalUnits">Unidades</option><option value="totalValue">Valor en COP</option></select>}>
      {chart.length ? <><div className="category-chart"><div className="chart-y">{[1, .75, .5, .25, 0].map(n => <span key={n}>{number.format(Math.round(maximum * n))}</span>)}</div><div className="chart-plot">{chart.map(c => <div className="chart-column" key={c.categoryId}><div className="chart-bar-area"><div className="chart-bar" style={{ height: `${Math.max(1, c[metric] / maximum * 100)}%` }} title={`${c.categoryName}: ${metric === 'totalValue' ? currency.format(c[metric]) : number.format(c[metric])}`} /><span className="chart-value">{number.format(c[metric])}</span></div><span className="chart-category" title={c.categoryName}>{c.categoryName}</span></div>)}</div></div><div className="chart-legend"><i />{metric === 'totalUnits' ? 'Unidades disponibles' : 'Valor del inventario (COP)'} · {chart.length} categorías principales</div></> : <p className="dashboard-empty">Todavía no hay categorías con inventario.</p>}
    </SummaryCard>
    <SummaryCard title="Salud del inventario" className="health-card"><div className="health-chart"><div className="health-donut" style={{ background: summary.totalProducts ? `conic-gradient(var(--chart-ok) 0% ${healthyPercent}%, var(--chart-warn) ${healthyPercent}% 100%)` : 'var(--border)' }} role="img" aria-label={`${healthyPercent}% de productos con stock suficiente`}><div><strong>{healthyPercent}%</strong><span>Stock suficiente</span></div></div></div><div className="health-legend"><span><i />Disponibles <strong>{number.format(healthy)}</strong></span><span><i />Stock bajo <strong>{number.format(summary.lowStockCount)}</strong></span></div></SummaryCard>
    <SummaryCard title="Valor por categoría" className="category-table-card" action={<button className="text-link" onClick={() => onProducts()}>Ver productos</button>}><div className="dashboard-table-wrap"><table className="data-table"><thead><tr><th>Nombre</th><th>Productos</th><th>Unidades</th><th>Valor</th></tr></thead><tbody>{ranked.slice(0, 5).map(c => <tr key={c.categoryId}><td>{c.categoryName}</td><td>{number.format(c.productCount)}</td><td>{number.format(c.totalUnits)}</td><td>{currency.format(c.totalValue)}</td></tr>)}</tbody></table>{!ranked.length && <p className="dashboard-empty">Sin categorías registradas.</p>}</div></SummaryCard>
    <SummaryCard title="Productos con stock bajo" className="low-stock-card" action={<button className="text-link" onClick={() => onProducts(true)}>Ver todos</button>}>{lowStock.length ? <div className="low-stock-list">{lowStock.slice(0, 4).map(p => <div className="low-stock-item" key={p.id}><ProductImage product={p} /><div><strong>{p.name}</strong><small>Disponible: {p.stock} {p.unit} · Mín.: {p.minStock}</small></div><span className="low-stock-badge">Bajo</span></div>)}</div> : <p className="dashboard-empty">Todo en orden. No hay productos por debajo del mínimo.</p>}</SummaryCard>
  </section>
}
function SummaryCard({ title, className, action, children }: { title: string; className: string; action?: ReactNode; children: ReactNode }) { return <article className={`summary-card ${className}`}><div className="summary-card-header"><h2>{title}</h2>{action}</div>{children}</article> }
function Metric({ icon, color, label, value }: { icon: string; color: string; label: string; value: string }) { return <div className="summary-metric"><span className={`metric-icon metric-${color}`}><DesignIcon name={icon} /></span><strong>{value}</strong><span>{label}</span></div> }
function ProductImage({ product }: { product: Product }) { const [failed, setFailed] = useState(false); return product.imageUrl && !failed ? <img className="low-stock-image" src={product.imageUrl} alt={product.name} onError={() => setFailed(true)} /> : <span className="low-stock-image product-initial">{product.name.charAt(0)}</span> }
