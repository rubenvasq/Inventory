import { useEffect, useState, type FormEvent } from 'react'
import { categoriesApi } from '../api/endpoints'
import { errorMessage, fieldErrors } from '../api/formError'
import { useAuth } from '../auth/AuthContext'
import type { Category, CategoryRequest } from '../api/types'
import { Field } from '../components/Field'
import { Modal } from '../components/Modal'
import { useToast } from '../components/Toast'

const EMPTY: CategoryRequest = { name: '', description: '', active: true }

export function CategoriesView() {
  const toast = useToast()
  const { isAdmin } = useAuth()
  const [items, setItems] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<Category | null>(null)
  const [creating, setCreating] = useState(false)

  const load = () => {
    setLoading(true)
    categoriesApi
      .list()
      .then(setItems)
      .catch((e) => toast.error(errorMessage(e)))
      .finally(() => setLoading(false))
  }
  useEffect(load, []) // eslint-disable-line react-hooks/exhaustive-deps

  const remove = async (c: Category) => {
    if (!confirm(`¿Eliminar la categoría "${c.name}"?`)) return
    try {
      await categoriesApi.remove(c.id)
      toast.success('Categoría eliminada')
      load()
    } catch (e) {
      toast.error(errorMessage(e))
    }
  }

  const open = creating || editing !== null

  return (
    <section>
      <div className="view-header">
        <h1>Categorías</h1>
        {isAdmin && (
          <button className="btn btn-primary" onClick={() => setCreating(true)}>
            + Nueva categoría
          </button>
        )}
      </div>

      {loading ? (
        <p className="muted"><span className="spinner" />Cargando…</p>
      ) : items.length === 0 ? (
        <p className="muted">No hay categorías. Crea la primera.</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Descripción</th>
              <th>Estado</th>
              <th className="col-actions">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {items.map((c) => (
              <tr key={c.id}>
                <td>{c.name}</td>
                <td className="muted">{c.description ?? '—'}</td>
                <td>
                  <span className={`badge ${c.active ? 'badge-ok' : 'badge-off'}`}>
                    {c.active ? 'Activa' : 'Inactiva'}
                  </span>
                </td>
                <td className="col-actions">
                  {isAdmin && (
                    <>
                      <button className="btn btn-sm" onClick={() => setEditing(c)}>
                        Editar
                      </button>
                      <button className="btn btn-sm btn-danger" onClick={() => remove(c)}>
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

      {open && (
        <CategoryForm
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
    </section>
  )
}

function CategoryForm({
  initial,
  onClose,
  onSaved,
}: {
  initial: Category | null
  onClose: () => void
  onSaved: () => void
}) {
  const toast = useToast()
  const [form, setForm] = useState<CategoryRequest>(
    initial
      ? { name: initial.name, description: initial.description ?? '', active: initial.active }
      : EMPTY,
  )
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setErrors({})
    try {
      if (initial) {
        await categoriesApi.update(initial.id, form)
        toast.success('Categoría actualizada')
      } else {
        await categoriesApi.create(form)
        toast.success('Categoría creada')
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
      title={initial ? 'Editar categoría' : 'Nueva categoría'}
      onClose={onClose}
      footer={
        <>
          <button className="btn" onClick={onClose} disabled={saving}>
            Cancelar
          </button>
          <button className="btn btn-primary" form="category-form" type="submit" disabled={saving}>
            {saving ? 'Guardando…' : 'Guardar'}
          </button>
        </>
      }
    >
      <form id="category-form" onSubmit={submit} className="form">
        <Field label="Nombre" htmlFor="c-name" required error={errors.name}>
          <input
            id="c-name"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            autoFocus
          />
        </Field>
        <Field label="Descripción" htmlFor="c-desc" error={errors.description}>
          <textarea
            id="c-desc"
            rows={3}
            value={form.description ?? ''}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
          />
        </Field>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={form.active ?? true}
            onChange={(e) => setForm({ ...form, active: e.target.checked })}
          />
          Activa
        </label>
      </form>
    </Modal>
  )
}
