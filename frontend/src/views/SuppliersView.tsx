import { useEffect, useState, type FormEvent } from 'react'
import { suppliersApi } from '../api/endpoints'
import { errorMessage, fieldErrors } from '../api/formError'
import { useAuth } from '../auth/AuthContext'
import type { Supplier, SupplierRequest } from '../api/types'
import { Field } from '../components/Field'
import { Modal } from '../components/Modal'
import { useToast } from '../components/Toast'

const EMPTY: SupplierRequest = {
  name: '',
  contactName: '',
  email: '',
  phone: '',
  address: '',
  active: true,
}

export function SuppliersView() {
  const toast = useToast()
  const { isAdmin } = useAuth()
  const [items, setItems] = useState<Supplier[]>([])
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<Supplier | null>(null)
  const [creating, setCreating] = useState(false)

  const load = () => {
    setLoading(true)
    suppliersApi
      .list()
      .then(setItems)
      .catch((e) => toast.error(errorMessage(e)))
      .finally(() => setLoading(false))
  }
  useEffect(load, []) // eslint-disable-line react-hooks/exhaustive-deps

  const remove = async (s: Supplier) => {
    if (!confirm(`¿Eliminar el proveedor "${s.name}"?`)) return
    try {
      await suppliersApi.remove(s.id)
      toast.success('Proveedor eliminado')
      load()
    } catch (e) {
      toast.error(errorMessage(e))
    }
  }

  const open = creating || editing !== null

  return (
    <section>
      <div className="view-header">
        <h1>Proveedores</h1>
        {isAdmin && (
          <button className="btn btn-primary" onClick={() => setCreating(true)}>
            + Nuevo proveedor
          </button>
        )}
      </div>

      {loading ? (
        <p className="muted"><span className="spinner" />Cargando…</p>
      ) : items.length === 0 ? (
        <p className="muted">No hay proveedores. Crea el primero.</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Contacto</th>
              <th>Correo</th>
              <th>Teléfono</th>
              <th>Estado</th>
              <th className="col-actions">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {items.map((s) => (
              <tr key={s.id}>
                <td>{s.name}</td>
                <td className="muted">{s.contactName ?? '—'}</td>
                <td className="muted">{s.email ?? '—'}</td>
                <td className="muted">{s.phone ?? '—'}</td>
                <td>
                  <span className={`badge ${s.active ? 'badge-ok' : 'badge-off'}`}>
                    {s.active ? 'Activo' : 'Inactivo'}
                  </span>
                </td>
                <td className="col-actions">
                  {isAdmin && (
                    <>
                      <button className="btn btn-sm" onClick={() => setEditing(s)}>
                        Editar
                      </button>
                      <button className="btn btn-sm btn-danger" onClick={() => remove(s)}>
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
        <SupplierForm
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

function SupplierForm({
  initial,
  onClose,
  onSaved,
}: {
  initial: Supplier | null
  onClose: () => void
  onSaved: () => void
}) {
  const toast = useToast()
  const [form, setForm] = useState<SupplierRequest>(
    initial
      ? {
          name: initial.name,
          contactName: initial.contactName ?? '',
          email: initial.email ?? '',
          phone: initial.phone ?? '',
          address: initial.address ?? '',
          active: initial.active,
        }
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
        await suppliersApi.update(initial.id, form)
        toast.success('Proveedor actualizado')
      } else {
        await suppliersApi.create(form)
        toast.success('Proveedor creado')
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
      title={initial ? 'Editar proveedor' : 'Nuevo proveedor'}
      onClose={onClose}
      footer={
        <>
          <button className="btn" onClick={onClose} disabled={saving}>
            Cancelar
          </button>
          <button className="btn btn-primary" form="supplier-form" type="submit" disabled={saving}>
            {saving ? 'Guardando…' : 'Guardar'}
          </button>
        </>
      }
    >
      <form id="supplier-form" onSubmit={submit} className="form">
        <Field label="Nombre" htmlFor="s-name" required error={errors.name}>
          <input
            id="s-name"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            autoFocus
          />
        </Field>
        <div className="grid-2">
          <Field label="Contacto" htmlFor="s-contact" error={errors.contactName}>
            <input
              id="s-contact"
              value={form.contactName ?? ''}
              onChange={(e) => setForm({ ...form, contactName: e.target.value })}
            />
          </Field>
          <Field label="Teléfono" htmlFor="s-phone" error={errors.phone}>
            <input
              id="s-phone"
              value={form.phone ?? ''}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
            />
          </Field>
        </div>
        <Field label="Correo" htmlFor="s-email" error={errors.email}>
          <input
            id="s-email"
            type="email"
            value={form.email ?? ''}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
          />
        </Field>
        <Field label="Dirección" htmlFor="s-address" error={errors.address}>
          <input
            id="s-address"
            value={form.address ?? ''}
            onChange={(e) => setForm({ ...form, address: e.target.value })}
          />
        </Field>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={form.active ?? true}
            onChange={(e) => setForm({ ...form, active: e.target.checked })}
          />
          Activo
        </label>
      </form>
    </Modal>
  )
}
