import { useState, type FormEvent } from 'react'
import { profileApi } from '../api/endpoints'
import { errorMessage, fieldErrors } from '../api/formError'
import { useAuth } from '../auth/AuthContext'
import { Field } from '../components/Field'
import { Modal } from '../components/Modal'
import { useToast } from '../components/Toast'

/**
 * Modal del perfil propio del usuario autenticado: datos de la cuenta,
 * edicion del nombre completo y cambio de contrasena. El nombre de usuario, el
 * rol y el estado los gestiona el ADMIN desde la vista de usuarios.
 */
export function ProfileView({ onClose }: { onClose: () => void }) {
  const { user, refreshUser } = useAuth()
  const toast = useToast()

  const [fullName, setFullName] = useState(user?.fullName ?? '')
  const [savingName, setSavingName] = useState(false)
  const [nameError, setNameError] = useState<string | null>(null)

  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [savingPassword, setSavingPassword] = useState(false)
  const [passwordError, setPasswordError] = useState<string | null>(null)

  if (!user) return null

  const saveName = async (e: FormEvent) => {
    e.preventDefault()
    setNameError(null)
    const value = fullName.trim()
    if (!value) {
      setNameError('El nombre es obligatorio')
      return
    }
    setSavingName(true)
    try {
      await profileApi.update({ fullName: value })
      await refreshUser()
      toast.success('Perfil actualizado')
    } catch (err) {
      setNameError(fieldErrors(err).fullName ?? errorMessage(err))
    } finally {
      setSavingName(false)
    }
  }

  const savePassword = async (e: FormEvent) => {
    e.preventDefault()
    setPasswordError(null)
    if (newPassword.length < 6) {
      setPasswordError('La nueva contraseña debe tener al menos 6 caracteres')
      return
    }
    if (newPassword !== confirmPassword) {
      setPasswordError('Las contraseñas nuevas no coinciden')
      return
    }
    setSavingPassword(true)
    try {
      await profileApi.changePassword({ currentPassword, newPassword })
      toast.success('Contraseña actualizada')
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
    } catch (err) {
      const fields = fieldErrors(err)
      setPasswordError(fields.newPassword ?? fields.currentPassword ?? errorMessage(err))
    } finally {
      setSavingPassword(false)
    }
  }

  return (
    <Modal title="Mi perfil" onClose={onClose}>
      <div className="profile-modal">
        <section className="profile-account">
          <span className="profile-account-avatar" aria-hidden="true">
            {user.fullName.charAt(0).toUpperCase()}
          </span>
          <div>
            <strong>{user.fullName}</strong>
            <small className="muted">
              @{user.username} · {user.role}
            </small>
          </div>
        </section>

        <form className="form" onSubmit={saveName}>
          <Field label="Nombre completo" htmlFor="p-name" required error={nameError ?? undefined}>
            <input
              id="p-name"
              value={fullName}
              maxLength={150}
              onChange={(e) => setFullName(e.target.value)}
              required
            />
          </Field>
          <p className="hint">El usuario y el rol los gestiona el administrador del sistema.</p>
          <div className="modal-footer-inner">
            <button className="btn btn-primary" type="submit" disabled={savingName}>
              {savingName ? 'Guardando…' : 'Guardar cambios'}
            </button>
          </div>
        </form>

        <form className="form" onSubmit={savePassword}>
          <h2 className="profile-section-title">Cambiar contraseña</h2>
          <Field label="Contraseña actual" htmlFor="p-current" required>
            <input
              id="p-current"
              type="password"
              autoComplete="current-password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              required
            />
          </Field>
          <div className="grid-2">
            <Field label="Nueva contraseña" htmlFor="p-new" required error={passwordError ?? undefined}>
              <input
                id="p-new"
                type="password"
                autoComplete="new-password"
                minLength={6}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
              />
            </Field>
            <Field label="Confirmar contraseña" htmlFor="p-confirm" required>
              <input
                id="p-confirm"
                type="password"
                autoComplete="new-password"
                minLength={6}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
              />
            </Field>
          </div>
          <div className="modal-footer-inner">
            <button className="btn btn-primary" type="submit" disabled={savingPassword}>
              {savingPassword ? 'Cambiando…' : 'Cambiar contraseña'}
            </button>
          </div>
        </form>
      </div>
    </Modal>
  )
}
