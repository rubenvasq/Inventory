import { useEffect, useState } from 'react'
import { dashboardApi, productsApi } from '../api/endpoints'
import { errorMessage } from '../api/formError'
import type { CategoryStockReport, DashboardSummary, Product } from '../api/types'
import { useToast } from '../components/Toast'

const currency = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })
const number = new Intl.NumberFormat('es-CO')

export function DashboardView() {
  const toast = useToast()
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [byCategory, setByCategory] = useState<CategoryStockReport[]>([])
  const [lowStock, setLowStock] = useState<Product[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([dashboardApi.summary(), dashboardApi.byCategory(), productsApi.lowStock()])
      .then(([s, c, l]) => {
        setSummary(s)
        setByCategory(c)
        setLowStock(l)
      })
      .catch((e) => toast.error(errorMessage(e)))
      .finally(() => setLoading(false))
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  if (loading) return <p className="muted"><span className="spinner" />Cargando…</p>
  if (!summary) return <p className="muted">No se pudo cargar el panel.</p>

  const maxValue = Math.max(1, ...byCategory.map((c) => c.totalValue))

  return (
    <section>
      <div className="view-header">
        <h1>Panel</h1>
      </div>

      <div className="kpi-grid">
        <Kpi label="Productos" value={number.format(summary.totalProducts)} />
        <Kpi label="Categorías" value={number.format(summary.totalCategories)} />
        <Kpi label="Proveedores" value={number.format(summary.totalSuppliers)} />
        <Kpi
          label="Bajo stock"
          value={number.format(summary.lowStockCount)}
          tone={summary.lowStockCount > 0 ? 'warn' : 'ok'}
        />
        <Kpi label="Unidades en stock" value={number.format(summary.totalStockUnits)} />
        <Kpi label="Valor de inventario" value={currency.format(summary.totalInventoryValue)} wide />
      </div>

      <div className="panel-grid">
        <div className="panel">
          <h2>Valor por categoría</h2>
          {byCategory.length === 0 ? (
            <p className="muted">Sin datos.</p>
          ) : (
            <div className="bars">
              {byCategory.map((c) => (
                <div className="bar-row" key={c.categoryId}>
                  <div className="bar-label">
                    {c.categoryName}
                    <small className="muted">
                      {' '}
                      · {number.format(c.productCount)} prod · {number.format(c.totalUnits)} u
                    </small>
                  </div>
                  <div className="bar-track">
                    <div className="bar-fill" style={{ width: `${(c.totalValue / maxValue) * 100}%` }} />
                  </div>
                  <div className="bar-value">{currency.format(c.totalValue)}</div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="panel">
          <h2>Productos con bajo stock</h2>
          {lowStock.length === 0 ? (
            <p className="muted">Todo en orden: ningún producto por debajo del mínimo.</p>
          ) : (
            <table className="data-table compact">
              <thead>
                <tr>
                  <th>Producto</th>
                  <th className="num">Stock</th>
                  <th className="num">Mínimo</th>
                </tr>
              </thead>
              <tbody>
                {lowStock.map((p) => (
                  <tr key={p.id}>
                    <td>
                      <span className="mono">{p.sku}</span> · {p.name}
                    </td>
                    <td className="num">
                      <span className="badge badge-warn">
                        {p.stock} {p.unit}
                      </span>
                    </td>
                    <td className="num muted">{p.minStock}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </section>
  )
}

function Kpi({
  label,
  value,
  tone,
  wide,
}: {
  label: string
  value: string
  tone?: 'ok' | 'warn'
  wide?: boolean
}) {
  return (
    <div className={`kpi${wide ? ' kpi-wide' : ''}`}>
      <span className="kpi-label">{label}</span>
      <span className={`kpi-value${tone === 'warn' ? ' kpi-warn' : ''}`}>{value}</span>
    </div>
  )
}
