import { useEffect, useState } from 'react'
import { movementsApi } from '../api/endpoints'
import { errorMessage } from '../api/formError'
import type { Movement, MovementType } from '../api/types'
import { useToast } from '../components/Toast'

const TYPES: { value: MovementType | ''; label: string }[] = [
  { value: '', label: 'Todos' },
  { value: 'IN', label: 'Entradas' },
  { value: 'OUT', label: 'Salidas' },
  { value: 'ADJUSTMENT', label: 'Ajustes' },
]

export function MovementsView() {
  const toast = useToast()
  const [items, setItems] = useState<Movement[]>([])
  const [loading, setLoading] = useState(true)
  const [typeFilter, setTypeFilter] = useState<MovementType | ''>('')

  useEffect(() => {
    setLoading(true)
    movementsApi
      .list(typeFilter === '' ? undefined : typeFilter)
      .then(setItems)
      .catch((e) => toast.error(errorMessage(e)))
      .finally(() => setLoading(false))
  }, [typeFilter]) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <section>
      <div className="view-header">
        <h1>Movimientos</h1>
      </div>

      <div className="filters">
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
      </div>

      {loading ? (
        <p className="muted"><span className="spinner" />Cargando…</p>
      ) : items.length === 0 ? (
        <p className="muted">No hay movimientos registrados.</p>
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
                <td className="muted">{m.note ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
