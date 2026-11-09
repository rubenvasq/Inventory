import { useState } from 'react'
import './App.css'
import { useAuth } from './auth/AuthContext'
import { CategoriesView } from './views/CategoriesView'
import { DashboardView } from './views/DashboardView'
import { LoginView } from './views/LoginView'
import { MovementsView } from './views/MovementsView'
import { ProductsView } from './views/ProductsView'
import { ProjectsView } from './views/ProjectsView'
import { SuppliersView } from './views/SuppliersView'
import { UsersView } from './views/UsersView'

type Tab = 'dashboard' | 'products' | 'categories' | 'suppliers' | 'movements' | 'projects' | 'users'

interface TabDef {
  id: Tab
  label: string
  adminOnly?: boolean
}

const TABS: TabDef[] = [
  { id: 'dashboard', label: 'Panel' },
  { id: 'products', label: 'Productos' },
  { id: 'categories', label: 'Categorías' },
  { id: 'suppliers', label: 'Proveedores' },
  { id: 'movements', label: 'Movimientos' },
  { id: 'projects', label: 'Proyectos' },
  { id: 'users', label: 'Usuarios', adminOnly: true },
]

function App() {
  const { user, loading, logout, isAdmin } = useAuth()
  const [tab, setTab] = useState<Tab>('dashboard')

  if (loading) {
    return <div className="login-screen"><span className="spinner" />Cargando…</div>
  }

  if (!user) {
    return <LoginView />
  }

  const tabs = TABS.filter((t) => !t.adminOnly || isAdmin)
  // Si el usuario no puede ver la pestaña actual (p. ej. operador en 'users'), caer al panel.
  const activeTab = tabs.some((t) => t.id === tab) ? tab : 'dashboard'

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">KX</span>
          <div>
            <strong>Kardex</strong>
            <small>DOMIO</small>
          </div>
        </div>
        <nav>
          {tabs.map((t) => (
            <button
              key={t.id}
              className={`nav-item${activeTab === t.id ? ' active' : ''}`}
              onClick={() => setTab(t.id)}
            >
              {t.label}
            </button>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div className="user-chip">
            <strong>{user.fullName}</strong>
            <small className="muted">{user.role}</small>
          </div>
          <button className="btn btn-sm" onClick={logout}>
            Cerrar sesión
          </button>
        </div>
      </aside>

      <main className="content">
        <div className="view-anim" key={activeTab}>
          {activeTab === 'dashboard' && <DashboardView />}
          {activeTab === 'products' && <ProductsView />}
          {activeTab === 'categories' && <CategoriesView />}
          {activeTab === 'suppliers' && <SuppliersView />}
          {activeTab === 'movements' && <MovementsView />}
          {activeTab === 'projects' && <ProjectsView />}
          {activeTab === 'users' && <UsersView />}
        </div>
      </main>
    </div>
  )
}

export default App
