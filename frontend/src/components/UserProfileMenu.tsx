import { useEffect, useRef, useState } from 'react'
import type { AuthUser } from '../api/types'
import type { Theme } from '../theme'
import { DesignIcon } from './DesignIcon'

interface UserProfileMenuProps {
  user: AuthUser
  theme: Theme
  onToggleTheme: () => void
  onOpenProfile: () => void
  onLogout: () => void
}

/**
 * Menu desplegable del avatar del topbar: perfil propio, modo oscuro y
 * cerrar sesion. Se cierra con clic fuera, Escape o al elegir una opcion.
 */
export function UserProfileMenu({ user, theme, onToggleTheme, onOpenProfile, onLogout }: UserProfileMenuProps) {
  const [open, setOpen] = useState(false)
  const anchorRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const onPointerDown = (e: PointerEvent) => {
      if (!anchorRef.current?.contains(e.target as Node)) setOpen(false)
    }
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('pointerdown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('pointerdown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  const dark = theme === 'dark'

  return (
    <div className="profile-menu-anchor" ref={anchorRef}>
      <button
        type="button"
        className="user-avatar"
        aria-haspopup="menu"
        aria-expanded={open}
        title={user.fullName}
        onClick={() => setOpen((v) => !v)}
      >
        {user.fullName.charAt(0).toUpperCase()}
      </button>

      {open && (
        <div className="profile-menu" role="menu" aria-label="Opciones del usuario">
          <div className="profile-menu-header">
            <span className="profile-menu-avatar" aria-hidden="true">
              {user.fullName.charAt(0).toUpperCase()}
            </span>
            <div>
              <strong>{user.fullName}</strong>
              <small>
                @{user.username} · {user.role}
              </small>
            </div>
          </div>

          <button
            type="button"
            role="menuitem"
            className="profile-menu-item"
            onClick={() => {
              setOpen(false)
              onOpenProfile()
            }}
          >
            <DesignIcon name="user" />
            Mi perfil
          </button>

          <button
            type="button"
            role="menuitem"
            className="profile-menu-item"
            aria-pressed={dark}
            onClick={onToggleTheme}
          >
            <DesignIcon name={dark ? 'sun' : 'moon'} />
            {dark ? 'Modo claro' : 'Modo oscuro'}
            <span className="theme-switch" aria-hidden="true" />
          </button>

          <div className="profile-menu-separator" />

          <button type="button" role="menuitem" className="profile-menu-item danger" onClick={onLogout}>
            <DesignIcon name="logout" />
            Cerrar sesión
          </button>
        </div>
      )}
    </div>
  )
}
