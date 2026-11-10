import { useState, type FormEvent } from 'react'
import { errorMessage } from '../api/formError'
import { useAuth } from '../auth/AuthContext'
import { Field } from '../components/Field'
import './LoginView.css'

export function LoginView() {
  const { login } = useAuth()
  const [username, setUsername] = useState(() => localStorage.getItem('sc_login_username') ?? '')
  const [remember, setRemember] = useState(() => Boolean(localStorage.getItem('sc_login_username')))
  const [notice, setNotice] = useState<string | null>(null)
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(username, password)
      if (remember) localStorage.setItem('sc_login_username', username)
      else localStorage.removeItem('sc_login_username')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="kanban-login">
      <div className="login-identity" aria-label="Kardex">
        <img className="kardex-login-brand" src="/design/kardex-logo.png" alt="Kardex" width="2172" height="724" />
      </div>
      <section className="login-content" aria-labelledby="login-title">
        <header className="login-heading">
          <img className="kardex-login-logo" src="/design/kardex-logo.png" alt="Kardex" width="2172" height="724" />
          <div>
            <h1 id="login-title">Inicia sesión en tu cuenta</h1>
            <p>¡Bienvenido! Ingresa tus datos para continuar.</p>
          </div>
        </header>
      <form className="login-form" onSubmit={submit}>

        <Field label="Usuario" htmlFor="l-user">
          <input id="l-user" name="username" autoComplete="username" placeholder="Ingresa tu usuario" required value={username} onChange={(e) => setUsername(e.target.value)} />
        </Field>
        <Field label="Contraseña" htmlFor="l-pass">
          <input
            id="l-pass"
            type="password"
            name="password"
            autoComplete="current-password"
            placeholder="••••••••"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </Field>

        <div className="login-options">
          <label className="login-remember"><input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} />Recordar usuario</label>
          <button type="button" className="login-link" onClick={() => setNotice('Para recuperar tu contraseña, contacta al administrador del sistema.')}>¿Olvidaste tu contraseña?</button>
        </div>
        {error && <div className="login-error" role="alert">{error}</div>}

        <button className="login-submit" type="submit" disabled={submitting}>
          {submitting ? 'Entrando…' : 'Iniciar sesión'}
        </button>
        <button className="login-google" type="button" onClick={() => setNotice('El acceso con Google aún no está habilitado. Ingresa con tu usuario y contraseña.')}><img src="/design/google.svg" alt="" width="24" height="24" />Iniciar sesión con Google</button>
      </form>
      <p className="login-signup">¿No tienes una cuenta? <button className="login-link" type="button" onClick={() => setNotice('Contacta al administrador para solicitar una cuenta de acceso.')}>Regístrate</button></p>
      {notice && <p className="login-notice" role="status">{notice}</p>}
      </section>
    </main>
  )
}
