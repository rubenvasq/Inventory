import { useEffect, useState } from 'react'
import { movementsApi } from '../api/endpoints'
import { errorMessage } from '../api/formError'
import type { Movement, MovementType, Page } from '../api/types'
import { useToast } from '../components/Toast'

const TYPES: { value: MovementType | ''; label: string }[] = [
  { value: '', label: 'Todos' },
  { value: 'IN', label: 'Entradas' },
  { value: 'OUT', label: 'Salidas' },
  { value: 'ADJUSTMENT', label: 'Ajustes' },
]

const PAGE_SIZE = 20

export function MovementsView() {
  const toast = useToast()
  const [items, setItems] = useState<Movement[]>([])
  const [pageData, setPageData] = useState<Page<Movement> | null>(null)
  const [loading, setLoading] = useState(true)
  const [productFilter, setProductFilter] = useState('')
  const [typeFilter, setTypeFilter] = useState<MovementType | ''>('')
  const [fromFilter, setFromFilter] = useState('')
  const [toFilter, setToFilter] = useState('')
  const [page, setPage] = useState(0)

  const load = (targetPage = page) => {
    setLoading(true)
    movementsApi
      .search({
        product: productFilter || undefined,
        type: typeFilter === '' ? undefined : typeFilter,
        from: fromFilter || undefined,
        to: toFilter || undefined,
        page: targetPage,
        size: PAGE_SIZE,
      })
      .then((result) => {
        setItems(result.content)
        setPageData(result)
      })
      .catch((e) => toast.error(errorMessage(e)))
      .finally(() => setLoading(false))
  }

  // Al cambiar un filtro se vuelve a la primera pagina.
  useEffect(() => {
    const timer = setTimeout(() => {
      setPage(0)
      load(0)
    }, 300)
    return () => clearTimeout(timer)
  }, [productFilter, typeFilter, fromFilter, toFilter]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(load, [page]) // eslint-disable-line react-hooks/exhaustive-deps

  const totalPages = pageData?.totalPages ?? 0

  return (
    <section>
      <div className="view-header">
        <h1>Movimientos</h1>
      </div>

      <div className="filters">
        <input
          className="filter-search"
          placeholder="Buscar por producto o SKU…"
          value={productFilter}
          onChange={(e) => setProductFilter(e.target.value)}
        />
        <label>
          Tipo:{' '}
          <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value as MovementType | '')}>
            {TYPES.map((t) => (
              <option key={t.value} value={t.value}>
                {t.label}
              </option>
            ))}
          </select>
        </label>
        <label>
          Desde:{' '}
          <input type="date" value={fromFilter} onChange={(e) => setFromFilter(e.target.value)} />
        </label>
        <label>
          Hasta:{' '}
          <input type="date" value={toFilter} onChange={(e) => setToFilter(e.target.value)} />
        </label>
      </div>

      {loading ? (
        <p className="muted"><span className="spinner" />Cargando…</p>
      ) : items.length === 0 ? (
        <p className="muted">No hay movimientos que coincidan con la búsqueda.</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Producto</th>
              <th>Tipo</th>
              <th className="num">Cant.</th>
              <th className="num">Antes</th>
              <th className="num">Después</th>
              <th>Proyecto</th>
              <th>Nota</th>
            </tr>
          </thead>
          <tbody>
            {items.map((m) => (
              <tr key={m.id}>
                <td className="muted">{new Date(m.createdAt).toLocaleString('es-CO')}</td>
                <td>
                  <span className="mono">{m.productSku}</span> · {m.productName}
                </td>
                <td>
                  <span className={`tag tag-${m.type.toLowerCase()}`}>{m.type}</span>
                </td>
                <td className="num">{m.quantity}</td>
                <td className="num muted">{m.previousStock}</td>
                <td className="num">{m.newStock}</td>
                <td className="muted">{m.projectName ?? '—'}</td>
                <td className="muted">{m.note ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {pageData && totalPages > 1 && (
        <div className="pagination">
          <button className="btn btn-sm" onClick={() => setPage(page - 1)} disabled={page <= 0}>
            ← Anterior
          </button>
          <span className="muted">
            Página {pageData.page + 1} de {totalPages} · {pageData.totalElements} resultados
          </span>
          <button
            className="btn btn-sm"
            onClick={() => setPage(page + 1)}
            disabled={pageData.page >= totalPages - 1}
          >
            Siguiente →
          </button>
        </div>
      )}
    </section>
  )
}
