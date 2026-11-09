import { useEffect, useState, type FormEvent } from 'react'
import { usersApi } from '../api/endpoints'
import { errorMessage, fieldErrors } from '../api/formError'
import type { AppUser, CreateUserRequest, Role, UpdateUserRequest } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { Field } from '../components/Field'
import { Modal } from '../components/Modal'
import { useToast } from '../components/Toast'

const ROLES: Role[] = ['ADMIN', 'OPERADOR']

export function UsersView() {
  const toast = useToast()
  const { user: current } = useAuth()
  const [items, setItems] = useState<AppUser[]>([])
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState<AppUser | null>(null)
  const [creating, setCreating] = useState(false)

  const load = () => {
    setLoading(true)
    usersApi
      .list()
      .then(setItems)
      .catch((e) => toast.error(errorMessage(e)))
      .finally(() => setLoading(false))
  }
  useEffect(load, []) // eslint-disable-line react-hooks/exhaustive-deps

  const remove = async (u: AppUser) => {
    if (!confirm(`¿Eliminar al usuario "${u.username}"?`)) return
    try {
      await usersApi.remove(u.id)
      toast.success('Usuario eliminado')
      load()
    } catch (e) {
      toast.error(errorMessage(e))
    }
  }

  const open = creating || editing !== null

  return (
    <section>
      <div className="view-header">
        <h1>Usuarios</h1>
        <button className="btn btn-primary" onClick={() => setCreating(true)}>
          + Nuevo usuario
        </button>
      </div>

      {loading ? (
        <p className="muted"><span className="spinner" />Cargando…</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Usuario</th>
              <th>Nombre</th>
              <th>Rol</th>
              <th>Estado</th>
              <th className="col-actions">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {items.map((u) => (
              <tr key={u.id}>
                <td className="mono">
                  {u.username}
                  {current?.username === u.username && <small className="muted"> (tú)</small>}
                </td>
                <td>{u.fullName}</td>
                <td>
                  <span className={`tag ${u.role === 'ADMIN' ? 'tag-adjustment' : 'tag-in'}`}>
                    {u.role}
                  </span>
                </td>
                <td>
                  <span className={`badge ${u.enabled ? 'badge-ok' : 'badge-off'}`}>
                    {u.enabled ? 'Activo' : 'Inactivo'}
                  </span>
                </td>
                <td className="col-actions">
                  <button className="btn btn-sm" onClick={() => setEditing(u)}>
                    Editar
                  </button>
                  <button className="btn btn-sm btn-danger" onClick={() => remove(u)}>
                    Eliminar
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {open && (
        <UserForm
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

function UserForm({
  initial,
  onClose,
  onSaved,
}: {
  initial: AppUser | null
  onClose: () => void
  onSaved: () => void
}) {
  const toast = useToast()
  const [username, setUsername] = useState(initial?.username ?? '')
  const [fullName, setFullName] = useState(initial?.fullName ?? '')
  const [role, setRole] = useState<Role>(initial?.role ?? 'OPERADOR')
  const [enabled, setEnabled] = useState(initial?.enabled ?? true)
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setErrors({})
    try {
      if (initial) {
        const body: UpdateUserRequest = { fullName, role, enabled, password: password || null }
        await usersApi.update(initial.id, body)
        toast.success('Usuario actualizado')
      } else {
        const body: CreateUserRequest = { username, password, fullName, role, enabled }
        await usersApi.create(body)
        toast.success('Usuario creado')
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
      title={initial ? 'Editar usuario' : 'Nuevo usuario'}
      onClose={onClose}
      footer={
        <>
          <button className="btn" onClick={onClose} disabled={saving}>
            Cancelar
          </button>
          <button className="btn btn-primary" form="user-form" type="submit" disabled={saving}>
            {saving ? 'Guardando…' : 'Guardar'}
          </button>
        </>
      }
    >
      <form id="user-form" onSubmit={submit} className="form">
        {!initial && (
          <Field label="Usuario" htmlFor="u-user" required error={errors.username}>
            <input id="u-user" value={username} onChange={(e) => setUsername(e.target.value)} autoFocus />
          </Field>
        )}
        <Field label="Nombre completo" htmlFor="u-name" required error={errors.fullName}>
          <input id="u-name" value={fullName} onChange={(e) => setFullName(e.target.value)} />
        </Field>
        <div className="grid-2">
          <Field label="Rol" htmlFor="u-role" required error={errors.role}>
            <select id="u-role" value={role} onChange={(e) => setRole(e.target.value as Role)}>
              {ROLES.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
          </Field>
          <Field
            label={initial ? 'Nueva contraseña (opcional)' : 'Contraseña'}
            htmlFor="u-pass"
            required={!initial}
            error={errors.password}
          >
            <input
              id="u-pass"
              type="password"
              value={password}
              placeholder={initial ? 'Dejar en blanco para no cambiar' : ''}
              onChange={(e) => setPassword(e.target.value)}
            />
          </Field>
        </div>
        <label className="checkbox">
          <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
          Activo
        </label>
      </form>
    </Modal>
  )
}
