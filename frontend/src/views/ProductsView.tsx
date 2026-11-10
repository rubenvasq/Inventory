import { useEffect, useState, type FormEvent } from 'react'
import { categoriesApi, movementsApi, productsApi, suppliersApi } from '../api/endpoints'
import { errorMessage, fieldErrors } from '../api/formError'
import { useAuth } from '../auth/AuthContext'
import type {
  Category,
  Movement,
  MovementRequest,
  MovementType,
  Page,
  Product,
  ProductRequest,
  Supplier,
} from '../api/types'
import { Field } from '../components/Field'
import { ProductPhoto } from '../components/ProductPhoto'
import { StockGauge } from '../components/StockGauge'
import { stockLevel, summarizeStock } from '../components/stockLevel'
import { Modal } from '../components/Modal'
import { useToast } from '../components/Toast'

const currency = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP' })
const PAGE_SIZE = 20

export function ProductsView({ initialSearch = '', initialLowStock = false }: { initialSearch?: string; initialLowStock?: boolean }) {
  const toast = useToast()
  const { isAdmin } = useAuth()
  const [items, setItems] = useState<Product[]>([])
  const [pageData, setPageData] = useState<Page<Product> | null>(null)
  const [categories, setCategories] = useState<Category[]>([])
  const [suppliers, setSuppliers] = useState<Supplier[]>([])
  const [loading, setLoading] = useState(true)
  const [loadFailed, setLoadFailed] = useState(false)
  const [searchFilter, setSearchFilter] = useState(initialSearch)
  const [categoryFilter, setCategoryFilter] = useState<number | ''>('')
  const [lowStockOnly, setLowStockOnly] = useState(initialLowStock)
  const [page, setPage] = useState(0)

  const [editing, setEditing] = useState<Product | null>(null)
  const [creating, setCreating] = useState(false)
  const [movementFor, setMovementFor] = useState<Product | null>(null)
  const [historyFor, setHistoryFor] = useState<Product | null>(null)

  const load = (targetPage = page) => {
    setLoading(true)
    setLoadFailed(false)
    productsApi
      .search({
        search: searchFilter || undefined,
        categoryId: categoryFilter === '' ? undefined : categoryFilter,
        lowStock: lowStockOnly || undefined,
        page: targetPage,
        size: PAGE_SIZE,
      })
      .then((result) => {
        setItems(result.content)
        setPageData(result)
      })
      .catch((e) => { setLoadFailed(true); toast.error(errorMessage(e)) })
      .finally(() => setLoading(false))
  }

  // Buscar al escribir con un pequeño debounce para no saturar la API.
  useEffect(() => {
    const timer = setTimeout(() => {
      setPage(0)
      load(0)
    }, 300)
    return () => clearTimeout(timer)
  }, [searchFilter, categoryFilter, lowStockOnly]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(load, [page]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    categoriesApi.list().then(setCategories).catch(() => {})
    suppliersApi.list().then(setSuppliers).catch(() => {})
  }, [])

  const remove = async (p: Product) => {
    if (!confirm(`¿Eliminar el producto "${p.name}" (${p.sku})?`)) return
    try {
      await productsApi.remove(p.id)
      toast.success('Producto eliminado')
      load()
    } catch (e) {
      // Si tiene movimientos, el backend rechaza el borrado y sugiere archivar.
      toast.error(errorMessage(e))
      load()
    }
  }

  const toggleArchive = async (p: Product) => {
    try {
      if (p.active) {
        await productsApi.archive(p.id)
        toast.success('Producto archivado (conserva su historial)')
      } else {
        await productsApi.restore(p.id)
        toast.success('Producto reactivado')
      }
      load()
    } catch (e) {
      toast.error(errorMessage(e))
    }
  }

  const openForm = creating || editing !== null
  const totalPages = pageData?.totalPages ?? 0
  const stockSummary = summarizeStock(items)

  return (
    <section className="products-view">
      <div className="view-header">
        <div><span className="eyebrow">INVENTARIO / PRODUCTOS</span><h1>Todos los productos</h1></div>
        {isAdmin && (
          <button className="btn btn-primary" onClick={() => setCreating(true)}>
            + Nuevo producto
          </button>
        )}
      </div>

      <div className="product-statistics">
        <div className="statistics-heading"><h2>Estadísticas de productos</h2><span className="live-label">● Inventario en tiempo real</span></div>
        <div className="product-stat-grid">
          <div><span>Productos encontrados</span><strong>{loading ? '—' : (pageData?.totalElements ?? 0).toLocaleString('es-CO')} <small>productos</small></strong></div>
          <div><span>Categorías disponibles</span><strong>{categories.length} <small>categorías</small></strong></div>
          <div className={`stock-summary stock-summary-${stockSummary.tone}`}>
            <span>Estado de stock · esta página</span>
            <strong className="health-stat" role="status"><span className="stock-health-dot" aria-hidden="true" />{loading ? 'Cargando…' : loadFailed ? 'No disponible' : stockSummary.status}</strong>
            {!loading && !loadFailed && stockSummary.total > 0 && <small className="stock-health-detail">{stockSummary.healthy}/{stockSummary.total} con stock suficiente ({stockSummary.percent}%){stockSummary.empty > 0 && stockSummary.low > 0 ? ` · ${stockSummary.low} con stock bajo` : ''}</small>}
          </div>
          <div><span>Unidades · esta página</span><strong>{loading ? '—' : items.reduce((total, p) => total + p.stock, 0).toLocaleString('es-CO')} <small>unidades</small></strong></div>
        </div>
      </div>

      <div className="filters">
        <input
          className="filter-search"
          aria-label="Buscar productos por nombre o SKU"
          placeholder="Buscar producto por nombre o SKU…"
          value={searchFilter}
          onChange={(e) => setSearchFilter(e.target.value)}
        />
        <label>
          Categoría:{' '}
          <select
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value === '' ? '' : Number(e.target.value))}
          >
            <option value="">Todas</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </label>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={lowStockOnly}
            onChange={(e) => setLowStockOnly(e.target.checked)}
          />
          Solo bajo stock
        </label>
      </div>

      {loading ? (
        <p className="muted"><span className="spinner" />Cargando…</p>
      ) : items.length === 0 ? (
        <p className="muted">No hay productos que coincidan con la búsqueda.</p>
      ) : (
        <div className="product-table-scroll"><table className="data-table product-table">
          <thead>
            <tr>
              <th colSpan={2}>Producto</th>
              <th>Disponibilidad</th>
              <th>Stock</th>
              <th>Precio unitario</th>
              <th className="col-actions">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {items.map((p) => (
              <tr key={p.id} className={p.active ? undefined : 'row-inactive'}>
                <td className="col-thumb">
                  <ProductPhoto key={`${p.id}-${p.imageUrl}`} product={p} />
                </td>
                <td className="product-identity">
                  <strong>{p.name}</strong>
                  <small>{p.sku} · {p.categoryName}</small>
                  {!p.active && <span className="badge badge-off">Archivado</span>}
                </td>
                <td><div className="availability"><div><span className="cell-label">Estado</span><span className="muted">{stockLevel(p.stock, p.minStock, p.active).status}</span><small className="muted">Mínimo: {p.minStock} {p.unit}</small></div><StockGauge stock={p.stock} minStock={p.minStock} active={p.active} unit={p.unit} /></div></td>
                <td><span className="cell-label">Existencias</span><span className={`stock-number${p.lowStock ? ' stock-warning' : ''}`}>◇ {p.stock} <small>{p.unit}</small></span></td>
                <td><span className="cell-label">Precio unitario</span>{p.unitPrice != null ? currency.format(p.unitPrice) : 'Sin precio'}<small className="product-supplier muted">{p.supplierName ?? 'Sin proveedor'}</small></td>
                <td className="col-actions">
                  <button className="btn btn-sm btn-primary" onClick={() => setMovementFor(p)}>
                    Movimiento
                  </button>
                  <button className="btn btn-sm" onClick={() => setHistoryFor(p)}>
                    Historial
                  </button>
                  {isAdmin && (
                    <>
                      <button className="btn btn-sm" onClick={() => setEditing(p)}>
                        Editar
                      </button>
                      <button className="btn btn-sm" onClick={() => toggleArchive(p)}>
                        {p.active ? 'Archivar' : 'Reactivar'}
                      </button>
                      <button className="btn btn-sm btn-danger" onClick={() => remove(p)}>
                        Eliminar
                      </button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table></div>
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

      {openForm && (
        <ProductForm
          initial={editing}
          categories={categories}
          suppliers={suppliers}
          onClose={() => {
            setCreating(false)
            setEditing(null)
          }}
          onSaved={() => {
            setCreating(false)
            setEditing(null)
            load()
          }}
        />
      )}

      {movementFor && (
        <MovementForm
          product={movementFor}
          onClose={() => setMovementFor(null)}
          onSaved={() => {
            setMovementFor(null)
            load()
          }}
        />
      )}

      {historyFor && <HistoryModal product={historyFor} onClose={() => setHistoryFor(null)} />}
    </section>
  )
}

function ProductForm({
  initial,
  categories,
  suppliers,
  onClose,
  onSaved,
}: {
  initial: Product | null
  categories: Category[]
  suppliers: Supplier[]
  onClose: () => void
  onSaved: () => void
}) {
  const toast = useToast()
  const [form, setForm] = useState<ProductRequest>(
    initial
      ? {
          sku: initial.sku,
          name: initial.name,
          description: initial.description ?? '',
          imageUrl: initial.imageUrl ?? '',
          categoryId: initial.categoryId,
          supplierId: initial.supplierId,
          unit: initial.unit,
          unitPrice: initial.unitPrice,
          minStock: initial.minStock,
        }
      : {
          sku: '',
          name: '',
          description: '',
          imageUrl: '',
          categoryId: categories[0]?.id ?? 0,
          supplierId: null,
          unit: 'unidad',
          unitPrice: null,
          stock: 0,
          minStock: 0,
        },
  )
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)

  const num = (v: string): number | null => (v === '' ? null : Number(v))

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setErrors({})
    try {
      if (initial) {
        await productsApi.update(initial.id, form)
        toast.success('Producto actualizado')
      } else {
        await productsApi.create(form)
        toast.success(
          (form.stock ?? 0) > 0
            ? 'Producto creado con su movimiento de saldo inicial'
            : 'Producto creado',
        )
      }
      onSaved()
    } catch (err) {
      setErrors(fieldErrors(err))
      toast.error(errorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      title={initial ? 'Editar producto' : 'Nuevo producto'}
      onClose={onClose}
      footer={
        <>
          <button className="btn" onClick={onClose} disabled={saving}>
            Cancelar
          </button>
          <button className="btn btn-primary" form="product-form" type="submit" disabled={saving}>
            {saving ? 'Guardando…' : 'Guardar'}
          </button>
        </>
      }
    >
      <form id="product-form" onSubmit={submit} className="form">
        <div className="grid-2">
          <Field label="SKU" htmlFor="p-sku" required error={errors.sku}>
            <input
              id="p-sku"
              value={form.sku}
              onChange={(e) => setForm({ ...form, sku: e.target.value })}
              autoFocus
            />
          </Field>
          <Field label="Unidad" htmlFor="p-unit" error={errors.unit}>
            <input
              id="p-unit"
              value={form.unit ?? ''}
              onChange={(e) => setForm({ ...form, unit: e.target.value })}
            />
          </Field>
        </div>
        <Field label="Nombre" htmlFor="p-name" required error={errors.name}>
          <input
            id="p-name"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
          />
        </Field>
        <Field label="Descripción" htmlFor="p-desc" error={errors.description}>
          <textarea
            id="p-desc"
            rows={2}
            value={form.description ?? ''}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
          />
        </Field>
        <Field label="Imagen (URL o ruta)" htmlFor="p-img" error={errors.imageUrl}>
          <div className="image-field">
            {form.imageUrl ? (
              <img className="thumb thumb-lg" src={form.imageUrl} alt="Vista previa" />
            ) : (
              <span className="thumb thumb-lg thumb-placeholder" aria-hidden>
                ?
              </span>
            )}
            <input
              id="p-img"
              value={form.imageUrl ?? ''}
              placeholder="/product-images/mi-producto.png o https://…"
              onChange={(e) => setForm({ ...form, imageUrl: e.target.value })}
            />
          </div>
        </Field>
        <div className="grid-2">
          <Field label="Categoría" htmlFor="p-cat" required error={errors.categoryId}>
            <select
              id="p-cat"
              value={form.categoryId}
              onChange={(e) => setForm({ ...form, categoryId: Number(e.target.value) })}
            >
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Proveedor" htmlFor="p-sup" error={errors.supplierId}>
            <select
              id="p-sup"
              value={form.supplierId ?? ''}
              onChange={(e) =>
                setForm({ ...form, supplierId: e.target.value === '' ? null : Number(e.target.value) })
              }
            >
              <option value="">— Sin proveedor —</option>
              {suppliers.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </Field>
        </div>
        <div className="grid-3">
          <Field label="Precio unitario" htmlFor="p-price" error={errors.unitPrice}>
            <input
              id="p-price"
              type="number"
              min="0"
              step="0.01"
              value={form.unitPrice ?? ''}
              onChange={(e) => setForm({ ...form, unitPrice: num(e.target.value) })}
            />
          </Field>
          {!initial && (
            <Field label="Stock inicial" htmlFor="p-stock" error={errors.stock}>
              <input
                id="p-stock"
                type="number"
                min="0"
                value={form.stock ?? 0}
                onChange={(e) => setForm({ ...form, stock: num(e.target.value) ?? 0 })}
              />
            </Field>
          )}
          <Field label="Stock mínimo" htmlFor="p-min" error={errors.minStock}>
            <input
              id="p-min"
              type="number"
              min="0"
              value={form.minStock ?? 0}
              onChange={(e) => setForm({ ...form, minStock: num(e.target.value) ?? 0 })}
            />
          </Field>
        </div>
        {!initial && (
          <p className="hint">
            Si el stock inicial es mayor que cero, se registra automáticamente el movimiento de
            saldo inicial que explica esas existencias.
          </p>
        )}
        {initial && (
          <p className="hint">
            Las existencias ({initial.stock} {initial.unit}) se ajustan mediante movimientos, no
            desde este formulario.
          </p>
        )}
      </form>
    </Modal>
  )
}

const MOVEMENT_LABELS: Record<MovementType, string> = {
  IN: 'Entrada (sumar al stock)',
  OUT: 'Salida (restar del stock)',
  ADJUSTMENT: 'Ajuste (fijar valor absoluto)',
}

function MovementForm({
  product,
  onClose,
  onSaved,
}: {
  product: Product
  onClose: () => void
  onSaved: () => void
}) {
  const toast = useToast()
  const [form, setForm] = useState<MovementRequest>({ type: 'IN', quantity: 1, note: '' })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setErrors({})
    try {
      const result = await movementsApi.register(product.id, form)
      toast.success(`Movimiento registrado. Nuevo stock: ${result.newStock}`)
      onSaved()
    } catch (err) {
      setErrors(fieldErrors(err))
      toast.error(errorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      title={`Movimiento · ${product.name}`}
      onClose={onClose}
      footer={
        <>
          <button className="btn" onClick={onClose} disabled={saving}>
            Cancelar
          </button>
          <button className="btn btn-primary" form="movement-form" type="submit" disabled={saving}>
            {saving ? 'Registrando…' : 'Registrar'}
          </button>
        </>
      }
    >
      <form id="movement-form" onSubmit={submit} className="form">
        <p className="hint">
          Stock actual: <strong>{product.stock}</strong> {product.unit}
        </p>
        <Field label="Tipo" htmlFor="m-type" required error={errors.type}>
          <select
            id="m-type"
            value={form.type}
            onChange={(e) => setForm({ ...form, type: e.target.value as MovementType })}
          >
            {(Object.keys(MOVEMENT_LABELS) as MovementType[]).map((t) => (
              <option key={t} value={t}>
                {MOVEMENT_LABELS[t]}
              </option>
            ))}
          </select>
        </Field>
        <Field
          label={form.type === 'ADJUSTMENT' ? 'Nuevo stock' : 'Cantidad'}
          htmlFor="m-qty"
          required
          error={errors.quantity}
        >
          <input
            id="m-qty"
            type="number"
            min="0"
            value={form.quantity}
            onChange={(e) => setForm({ ...form, quantity: Number(e.target.value) })}
            autoFocus
          />
        </Field>
        <Field label="Nota" htmlFor="m-note" error={errors.note}>
          <input
            id="m-note"
            value={form.note ?? ''}
            placeholder="Ej. Compra factura 001, instalación cliente…"
            onChange={(e) => setForm({ ...form, note: e.target.value })}
          />
        </Field>
      </form>
    </Modal>
  )
}

function HistoryModal({ product, onClose }: { product: Product; onClose: () => void }) {
  const toast = useToast()
  const [items, setItems] = useState<Movement[] | null>(null)

  useEffect(() => {
    movementsApi
      .byProduct(product.id)
      .then(setItems)
      .catch((e) => {
        toast.error(errorMessage(e))
        setItems([])
      })
  }, [product.id]) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <Modal title={`Historial · ${product.name}`} onClose={onClose}>
      {items === null ? (
        <p className="muted"><span className="spinner" />Cargando…</p>
      ) : items.length === 0 ? (
        <p className="muted">Sin movimientos registrados.</p>
      ) : (
        <table className="data-table compact">
          <thead>
            <tr>
              <th>Fecha</th>
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
    </Modal>
  )
}
