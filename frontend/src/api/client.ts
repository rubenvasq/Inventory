import { getToken, notifyUnauthorized } from './session'
import type { ApiErrorBody } from './types'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'

/** Error enriquecido con el estado HTTP y los detalles de validacion del backend. */
export class ApiError extends Error {
  readonly status: number
  readonly details?: { field: string; message: string }[] | null

  constructor(status: number, message: string, details?: { field: string; message: string }[] | null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const token = getToken()
  const headers: Record<string, string> = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers['Authorization'] = `Bearer ${token}`

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  if (!response.ok) {
    // Token invalido o expirado mientras habia sesion: cerrar sesion.
    if (response.status === 401 && token) {
      notifyUnauthorized()
    }
    let message = `Error ${response.status}`
    let details: ApiErrorBody['details']
    try {
      const data = (await response.json()) as ApiErrorBody
      message = data.message || message
      details = data.details
    } catch {
      // respuesta sin cuerpo JSON: se conserva el mensaje por defecto
    }
    throw new ApiError(response.status, message, details)
  }

  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

export const http = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body: unknown) => request<T>('POST', path, body),
  put: <T>(path: string, body: unknown) => request<T>('PUT', path, body),
  patch: <T>(path: string) => request<T>('PATCH', path),
  delete: <T>(path: string) => request<T>('DELETE', path),
}
