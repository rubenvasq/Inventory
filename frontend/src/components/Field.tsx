import type { ReactNode } from 'react'

interface FieldProps {
  label: string
  htmlFor?: string
  required?: boolean
  error?: string
  children: ReactNode
}

/** Campo de formulario etiquetado con mensaje de error opcional. */
export function Field({ label, htmlFor, required, error, children }: FieldProps) {
  return (
    <div className={`field${error ? ' field-error' : ''}`}>
      <label htmlFor={htmlFor}>
        {label}
        {required && <span className="req"> *</span>}
      </label>
      {children}
      {error && <small className="field-msg">{error}</small>}
    </div>
  )
}
