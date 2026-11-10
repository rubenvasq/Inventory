import { useCallback, useEffect, useState } from 'react'

/**
 * Tema visual de la aplicacion (claro/oscuro). La preferencia se guarda en
 * localStorage y se aplica como atributo data-theme en <html>, de modo que el
 * CSS puede derivar todos los colores de ese unico atributo. Si no hay
 * preferencia guardada se respeta la del sistema operativo.
 */
export type Theme = 'light' | 'dark'

const STORAGE_KEY = 'sc_theme'

/** Tema inicial: preferencia guardada o, en su defecto, la del sistema. */
export function initialTheme(): Theme {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved === 'light' || saved === 'dark') return saved
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

/** Aplica el tema al documento (atributo data-theme en <html>). */
export function applyTheme(theme: Theme): void {
  document.documentElement.dataset.theme = theme
}

/**
 * Hook de tema: devuelve el tema actual y un setter que lo aplica al
 * documento y lo persiste para las siguientes visitas.
 */
export function useTheme(): [Theme, (theme: Theme) => void] {
  const [theme, setTheme] = useState<Theme>(initialTheme)

  useEffect(() => {
    applyTheme(theme)
  }, [theme])

  const update = useCallback((next: Theme) => {
    setTheme(next)
    localStorage.setItem(STORAGE_KEY, next)
  }, [])

  return [theme, update]
}
