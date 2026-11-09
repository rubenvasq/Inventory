import { ApiError } from './client'

/**
 * Extrae los errores de validacion por campo de un ApiError (status 400),
 * para mostrarlos junto a cada input. Devuelve un objeto campo -> mensaje.
 */
export function fieldErrors(err: unknown): Record<string, string> {
  if (err instanceof ApiError && err.details) {
    return Object.fromEntries(err.details.map((d) => [d.field, d.message]))
  }
  return {}
}

export function errorMessage(err: unknown): string {
  if (err instanceof ApiError) return err.message
  if (err instanceof Error) return err.message
  return 'Error inesperado'
}
