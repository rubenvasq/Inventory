import { useEffect, useState, type FormEvent } from 'react'
import { productsApi, projectsApi } from '../api/endpoints'
import { errorMessage, fieldErrors } from '../api/formError'
import { useAuth } from '../auth/AuthContext'
import type {
  AssignmentRequest,
  Movement,
  Page,
  Project,
  ProjectAssignment,
  ProjectMaterialRequest,
  ProjectRequest,
  ProjectStatus,
} from '../api/types'
import { Field } from '../components/Field'
import { Modal } from '../components/Modal'
import { useToast } from '../components/Toast'

const STATUS_LABELS: Record<ProjectStatus, string> = {
  PLANNED: 'Planificado',
  IN_PROGRESS: 'En ejecución',
  COMPLETED: 'Completado',
  CANCELLED: 'Cancelado',
}

const STATUS_TAG: Record<ProjectStatus, string> = {
  PLANNED: 'tag-status-planned',
  IN_PROGRESS: 'tag-status-progress',
  COMPLETED: 'tag-status-done',
  CANCELLED: 'tag-status-cancelled',
}

const STATUSES: { value: ProjectStatus | ''; label: string }[] = [
  { value: '', label: 'Todos' },
  { value: 'PLANNED', label: 'Planificados' },
  { value: 'IN_PROGRESS', label: 'En ejecución' },
  { value: 'COMPLETED', label: 'Completados' },
  { value: 'CANCELLED', label: 'Cancelados' },
]

const PAGE_SIZE = 20

export function ProjectsView() {
  const toast = useToast()
  const { isAdmin } = useAuth()
  const [items, setItems] = useState<Project[]>([])
  const [pageData, setPageData] = useState<Page<Project> | null>(null)
  const [loading, setLoading] = useState(true)
  const [searchFilter, setSearchFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState<ProjectStatus | ''>('')
  const [page, setPage] = useState(0)

  const [detailFor, setDetailFor] = useState<Project | null>(null)
  const [editing, setEditing] = useState<Project | null>(null)
  const [creating, setCreating] = useState(false)

  const load = (targetPage = page) => {
    setLoading(true)
    projectsApi
      .search({
        search: searchFilter || undefined,
        status: statusFilter === '' ? undefined : statusFilter,
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

  useEffect(() => {
    const timer = setTimeout(() => {
      setPage(0)
      load(0)
    }, 300)
    return () => clearTimeout(timer)
  }, [searchFilter, statusFilter]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(load, [page]) // eslint-disable-line react-hooks/exhaustive-deps

  const remove = async (p: Project) => {
    if (!confirm(`¿Eliminar el proyecto "${p.name}"?`)) return
    try {
      await projectsApi.remove(p.id)
      toast.success('Proyecto eliminado')
      load()
    } catch (e) {
      toast.error(errorMessage(e))
      load()
    }
  }

  const totalPages = pageData?.totalPages ?? 0

  return (
    <section>
      <div className="view-header">
        <h1>Proyectos</h1>
        {isAdmin && (
          <button className="btn btn-primary" onClick={() => setCreating(true)}>
            + Nuevo proyecto
          </button>
        )}
      </div>

      <div className="filters">
        <input
          className="filter-search"
          placeholder="Buscar por nombre o cliente…"
          value={searchFilter}
          onChange={(e) => setSearchFilter(e.target.value)}
        />
        <label>
          Estado:{' '}
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as ProjectStatus | '')}
          >
            {STATUSES.map((s) => (
              <option key={s.value} value={s.value}>
                {s.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      {loading ? (
        <p className="muted"><span className="spinner" />Cargando…</p>
      ) : items.length === 0 ? (
        <p className="muted">No hay proyectos que coincidan con la búsqueda.</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Cliente</th>
              <th>Estado</th>
              <th>Inicio</th>
              <th>Fin</th>
              <th className="num">Materiales</th>
              <th className="col-actions">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {items.map((p) => (
              <tr key={p.id}>
                <td>{p.name}</td>
                <td className="muted">{p.client}</td>
                <td>
                  <span className={`tag ${STATUS_TAG[p.status]}`}>{STATUS_LABELS[p.status]}</span>
                </td>
                <td className="muted">{p.startDate ?? '—'}</td>
                <td className="muted">{p.endDate ?? '—'}</td>
                <td className="num muted">
                  {p.assignments.length} producto{p.assignments.length === 1 ? '' : 's'}
                </td>
                <td className="col-actions">
                  <button className="btn btn-sm btn-primary" onClick={() => setDetailFor(p)}>
                    Ver detalle
                  </button>
                  {isAdmin && (
                    <>
                      <button className="btn btn-sm" onClick={() => setEditing(p)}>
                        Editar
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

      {(creating || editing !== null) && (
        <ProjectForm
          initial={editing}
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

      {detailFor && (
        <ProjectDetail
          project={detailFor}
          onClose={() => setDetailFor(null)}
          onChanged={() => load()}
        />
      )}
    </section>
  )
}

function ProjectForm({
  initial,
  onClose,
  onSaved,
}: {
  initial: Project | null
  onClose: () => void
  onSaved: () => void
}) {
  const toast = useToast()
  const [form, setForm] = useState<ProjectRequest>(
    initial
      ? {
          name: initial.name,
          client: initial.client,
          status: initial.status,
          startDate: initial.startDate ?? '',
          endDate: initial.endDate ?? '',
          notes: initial.notes ?? '',
        }
      : { name: '', client: '', status: 'PLANNED', startDate: '', endDate: '', notes: '' },
  )
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setErrors({})
    try {
      if (initial) {
        await projectsApi.update(initial.id, form)
        toast.success('Proyecto actualizado')
      } else {
        await projectsApi.create(form)
        toast.success('Proyecto creado')
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
      title={initial ? 'Editar proyecto' : 'Nuevo proyecto'}
      onClose={onClose}
      footer={
        <>
          <button className="btn" onClick={onClose} disabled={saving}>
            Cancelar
          </button>
          <button className="btn btn-primary" form="project-form" type="submit" disabled={saving}>
            {saving ? 'Guardando…' : 'Guardar'}
          </button>
        </>
      }
    >
      <form id="project-form" onSubmit={submit} className="form">
        <div className="grid-2">
          <Field label="Nombre" htmlFor="pr-name" required error={errors.name}>
            <input
              id="pr-name"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              autoFocus
            />
          </Field>
          <Field label="Cliente" htmlFor="pr-client" required error={errors.client}>
            <input
              id="pr-client"
              value={form.client}
              onChange={(e) => setForm({ ...form, client: e.target.value })}
            />
          </Field>
        </div>
        <div className="grid-3">
          <Field label="Estado" htmlFor="pr-status" error={errors.status}>
            <select
              id="pr-status"
              value={form.status ?? 'PLANNED'}
              onChange={(e) => setForm({ ...form, status: e.target.value as ProjectStatus })}
            >
              {(Object.keys(STATUS_LABELS) as ProjectStatus[]).map((s) => (
                <option key={s} value={s}>
                  {STATUS_LABELS[s]}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Fecha de inicio" htmlFor="pr-start" error={errors.startDate}>
            <input
              id="pr-start"
              type="date"
              value={form.startDate ?? ''}
              onChange={(e) => setForm({ ...form, startDate: e.target.value })}
            />
          </Field>
          <Field label="Fecha de fin" htmlFor="pr-end" error={errors.endDate}>
            <input
              id="pr-end"
              type="date"
              value={form.endDate ?? ''}
              onChange={(e) => setForm({ ...form, endDate: e.target.value })}
            />
          </Field>
        </div>
        <Field label="Notas" htmlFor="pr-notes" error={errors.notes}>
          <textarea
            id="pr-notes"
            rows={2}
            value={form.notes ?? ''}
            onChange={(e) => setForm({ ...form, notes: e.target.value })}
          />
        </Field>
      </form>
    </Modal>
  )
}

function ProjectDetail({
  project,
  onClose,
  onChanged,
}: {
  project: Project
  onClose: () => void
  onChanged: () => void
}) {
  const toast = useToast()
  const { isAdmin } = useAuth()
  const [current, setCurrent] = useState<Project>(project)
  const [movements, setMovements] = useState<Movement[] | null>(null)
  const [assigning, setAssigning] = useState(false)
  const [delivering, setDelivering] = useState(false)
  const [returning, setReturning] = useState(false)

  const reload = () => {
    projectsApi
      .get(project.id)
      .then(setCurrent)
      .catch((e) => toast.error(errorMessage(e)))
    projectsApi
      .movements(project.id)
      .then(setMovements)
      .catch((e) => {
        toast.error(errorMessage(e))
        setMovements([])
      })
  }

  useEffect(reload, [project.id]) // eslint-disable-line react-hooks/exhaustive-deps

  const canDeliver =
    current.status === 'PLANNED' || current.status === 'IN_PROGRESS'

  const removeAssignment = async (a: ProjectAssignment) => {
    if (!confirm(`¿Retirar la asignación de "${a.productName}"?`)) return
    try {
      await projectsApi.removeAssignment(project.id, a.id)
      toast.success('Asignación retirada')
      reload()
      onChanged()
    } catch (e) {
      toast.error(errorMessage(e))
    }
  }

  return (
    <Modal title={`Proyecto · ${current.name}`} onClose={onClose}>
      <div className="project-summary">
        <div>
          <strong>Cliente:</strong> {current.client}
        </div>
        <div>
          <span className={`tag ${STATUS_TAG[current.status]}`}>{STATUS_LABELS[current.status]}</span>
        </div>
        <div className="muted">
          {current.startDate ? `Inicio: ${current.startDate}` : 'Sin fecha de inicio'}
          {current.endDate ? ` · Fin: ${current.endDate}` : ''}
        </div>
        {current.notes && <div className="muted">{current.notes}</div>}
      </div>

      <div className="view-header">
        <h2>Material asignado</h2>
        <div className="header-actions">
          {isAdmin && (
            <button className="btn btn-sm" onClick={() => setAssigning(true)}>
              + Asignar material
            </button>
          )}
          <button
            className="btn btn-sm btn-primary"
            onClick={() => setDelivering(true)}
            disabled={!canDeliver || current.assignments.length === 0}
            title={canDeliver ? undefined : 'El proyecto no admite entregas en su estado actual'}
          >
            Entregar material
          </button>
          <button
            className="btn btn-sm"
            onClick={() => setReturning(true)}
            disabled={current.assignments.every((a) => a.deliveredQuantity - a.returnedQuantity <= 0)}
            title="Devolver material sobrante al inventario"
          >
            Devolver material
          </button>
        </div>
      </div>

      {current.assignments.length === 0 ? (
        <p className="muted">Sin material asignado todavía.</p>
      ) : (
        <table className="data-table compact">
          <thead>
            <tr>
              <th>Producto</th>
              <th className="num">Asignado</th>
              <th className="num">Entregado</th>
              <th className="num">Devuelto</th>
              <th className="num">Pendiente</th>
              {isAdmin && <th className="col-actions"></th>}
            </tr>
          </thead>
          <tbody>
            {current.assignments.map((a) => (
              <tr key={a.id}>
                <td>
                  <span className="mono">{a.productSku}</span> · {a.productName}
                </td>
                <td className="num">{a.assignedQuantity}</td>
                <td className="num">{a.deliveredQuantity}</td>
                <td className="num muted">{a.returnedQuantity}</td>
                <td className="num">{a.pendingQuantity}</td>
                {isAdmin && (
                  <td className="col-actions">
                    {a.deliveredQuantity === 0 && (
                      <button className="btn btn-sm btn-danger" onClick={() => removeAssignment(a)}>
                        Retirar
                      </button>
                    )}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <h2>Historial de movimientos</h2>
      {movements === null ? (
        <p className="muted"><span className="spinner" />Cargando…</p>
      ) : movements.length === 0 ? (
        <p className="muted">Sin movimientos vinculados al proyecto.</p>
      ) : (
        <table className="data-table compact">
          <thead>
            <tr>
              <th>Fecha</th>
              <th>Producto</th>
              <th>Tipo</th>
              <th className="num">Cant.</th>
              <th className="num">Después</th>
              <th>Nota</th>
            </tr>
          </thead>
          <tbody>
            {movements.map((m) => (
              <tr key={m.id}>
                <td className="muted">{new Date(m.createdAt).toLocaleString('es-CO')}</td>
                <td>
                  <span className="mono">{m.productSku}</span> · {m.productName}
                </td>
                <td>
                  <span className={`tag tag-${m.type.toLowerCase()}`}>
                    {m.type === 'IN' ? 'Entrada' : m.type === 'OUT' ? 'Salida' : 'Ajuste'}
                  </span>
                </td>
                <td className="num">{m.quantity}</td>
                <td className="num">{m.newStock}</td>
                <td className="muted">{m.note ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {assigning && (
        <AssignMaterialForm
          project={current}
          onClose={() => setAssigning(false)}
          onSaved={() => {
            setAssigning(false)
            reload()
            onChanged()
          }}
        />
      )}

      {(delivering || returning) && (
        <ProjectMovementForm
          project={current}
          mode={delivering ? 'deliver' : 'return'}
          onClose={() => {
            setDelivering(false)
            setReturning(false)
          }}
          onSaved={() => {
            setDelivering(false)
            setReturning(false)
            reload()
            onChanged()
          }}
        />
      )}
    </Modal>
  )
}

function AssignMaterialForm({
  project,
  onClose,
  onSaved,
}: {
  project: Project
  onClose: () => void
  onSaved: () => void
}) {
  const toast = useToast()
  const [products, setProducts] = useState<{ id: number; label: string; stock: number }[]>([])
  const [form, setForm] = useState<AssignmentRequest>({ productId: 0, quantity: 1 })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    productsApi
      .search({ size: 200 })
      .then((page) =>
        setProducts(
          page.content.map((p) => ({ id: p.id, label: `${p.sku} · ${p.name}`, stock: p.stock })),
        ),
      )
      .catch((e) => toast.error(errorMessage(e)))
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setErrors({})
    try {
      await projectsApi.assign(project.id, form)
      toast.success('Material asignado al proyecto')
      onSaved()
    } catch (err) {
      setErrors(fieldErrors(err))
      toast.error(errorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  const selected = products.find((p) => p.id === form.productId)

  return (
    <Modal title="Asignar material" onClose={onClose}>
      <form id="assign-form" onSubmit={submit} className="form">
        <Field label="Producto" htmlFor="a-product" required error={errors.productId}>
          <select
            id="a-product"
            value={form.productId}
            onChange={(e) => setForm({ ...form, productId: Number(e.target.value) })}
          >
            <option value="">— Selecciona un producto —</option>
            {products.map((p) => (
              <option key={p.id} value={p.id}>
                {p.label}
              </option>
            ))}
          </select>
        </Field>
        {selected && (
          <p className="hint">
            Stock disponible: <strong>{selected.stock}</strong>. Si el producto ya estaba asignado
            al proyecto, la cantidad se suma a la asignación existente.
          </p>
        )}
        <Field label="Cantidad a asignar" htmlFor="a-qty" required error={errors.quantity}>
          <input
            id="a-qty"
            type="number"
            min="1"
            value={form.quantity}
            onChange={(e) => setForm({ ...form, quantity: Number(e.target.value) })}
            autoFocus
          />
        </Field>
        <p className="hint">La asignación planifica material; no descuenta existencias.</p>
        <div className="modal-footer-inner">
          <button className="btn" onClick={onClose} disabled={saving}>
            Cancelar
          </button>
          <button className="btn btn-primary" form="assign-form" type="submit" disabled={saving}>
            {saving ? 'Asignando…' : 'Asignar'}
          </button>
        </div>
      </form>
    </Modal>
  )
}

function ProjectMovementForm({
  project,
  mode,
  onClose,
  onSaved,
}: {
  project: Project
  mode: 'deliver' | 'return'
  onClose: () => void
  onSaved: () => void
}) {
  const toast = useToast()
  const [form, setForm] = useState<ProjectMaterialRequest>({ productId: 0, quantity: 1, note: '' })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)

  const isDeliver = mode === 'deliver'
  const options = project.assignments.filter((a) =>
    isDeliver ? a.pendingQuantity > 0 : a.deliveredQuantity - a.returnedQuantity > 0,
  )

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setErrors({})
    try {
      if (isDeliver) {
        const result = await projectsApi.deliver(project.id, form)
        toast.success(`Material entregado. Nuevo stock: ${result.newStock}`)
      } else {
        const result = await projectsApi.returnMaterial(project.id, form)
        toast.success(`Material devuelto. Nuevo stock: ${result.newStock}`)
      }
      onSaved()
    } catch (err) {
      setErrors(fieldErrors(err))
      toast.error(errorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  const selected = project.assignments.find((a) => a.productId === form.productId)

  return (
    <Modal title={isDeliver ? 'Entregar material' : 'Devolver material'} onClose={onClose}>
      <form id="pm-form" onSubmit={submit} className="form">
        <Field
          label={isDeliver ? 'Producto a entregar' : 'Producto a devolver'}
          htmlFor="pm-product"
          required
          error={errors.productId}
        >
          <select
            id="pm-product"
            value={form.productId}
            onChange={(e) => setForm({ ...form, productId: Number(e.target.value) })}
          >
            <option value="">— Selecciona un producto —</option>
            {options.map((a) => (
              <option key={a.productId} value={a.productId}>
                {a.productSku} · {a.productName} (
                {isDeliver
                  ? `pendiente ${a.pendingQuantity}`
                  : `entregado sin devolver ${a.deliveredQuantity - a.returnedQuantity}`}
                )
              </option>
            ))}
          </select>
        </Field>
        {selected && !isDeliver && (
          <p className="hint">
            Se pueden devolver como máximo {selected.deliveredQuantity - selected.returnedQuantity}{' '}
            unidades (entregadas y no devueltas).
          </p>
        )}
        <Field label="Cantidad" htmlFor="pm-qty" required error={errors.quantity}>
          <input
            id="pm-qty"
            type="number"
            min="1"
            value={form.quantity}
            onChange={(e) => setForm({ ...form, quantity: Number(e.target.value) })}
            autoFocus
          />
        </Field>
        <Field label="Nota" htmlFor="pm-note" error={errors.note}>
          <input
            id="pm-note"
            value={form.note ?? ''}
            placeholder="Ej. Entrega en obra, acta 002…"
            onChange={(e) => setForm({ ...form, note: e.target.value })}
          />
        </Field>
        <div className="modal-footer-inner">
          <button className="btn" onClick={onClose} disabled={saving}>
            Cancelar
          </button>
          <button className="btn btn-primary" form="pm-form" type="submit" disabled={saving}>
            {saving ? 'Registrando…' : isDeliver ? 'Entregar' : 'Devolver'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
