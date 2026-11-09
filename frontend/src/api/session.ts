// Almacen del token JWT y del manejador de sesion expirada.
// El token se guarda en localStorage para sobrevivir recargas de pagina.

const TOKEN_KEY = 'sc_token'

let unauthorizedHandler: (() => void) | null = null

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

/** Registra el callback que se ejecuta cuando el backend rechaza el token (401). */
export function onUnauthorized(handler: () => void): void {
  unauthorizedHandler = handler
}

export function notifyUnauthorized(): void {
  unauthorizedHandler?.()
}
