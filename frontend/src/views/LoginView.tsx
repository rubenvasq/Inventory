import { useState, type FormEvent } from 'react'
import { errorMessage } from '../api/formError'
import { useAuth } from '../auth/AuthContext'
import { Field } from '../components/Field'

export function LoginView() {
  const { login } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(username, password)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="login-screen">
      <form className="login-card" onSubmit={submit}>
        <div className="brand login-brand">
          <span className="brand-mark">KX</span>
          <div>
            <strong>Kardex</strong>
            <small>DOMIO</small>
          </div>
        </div>
        <h1>Iniciar sesión</h1>

        <Field label="Usuario" htmlFor="l-user" required>
          <input id="l-user" value={username} onChange={(e) => setUsername(e.target.value)} autoFocus />
        </Field>
        <Field label="Contraseña" htmlFor="l-pass" required>
          <input
            id="l-pass"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </Field>

        {error && <div className="login-error">{error}</div>}

        <button className="btn btn-primary login-btn" type="submit" disabled={submitting}>
          {submitting ? 'Entrando…' : 'Entrar'}
        </button>
      </form>
    </div>
  )
}
