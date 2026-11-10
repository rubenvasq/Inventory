import { useState } from 'react'
import './App.css'
import './Kanban.css'
import { DesignIcon } from './components/DesignIcon'
import { UserProfileMenu } from './components/UserProfileMenu'
import { useAuth } from './auth/AuthContext'
import { useTheme } from './theme'
import { CategoriesView } from './views/CategoriesView'
import { DashboardView } from './views/DashboardView'
import { LoginView } from './views/LoginView'
import { MovementsView } from './views/MovementsView'
import { ProductsView } from './views/ProductsView'
import { ProfileView } from './views/ProfileView'
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
  const [globalSearch, setGlobalSearch] = useState('')
  const [productSearch, setProductSearch] = useState('')
  const [lowStockView, setLowStockView] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const [theme, setTheme] = useTheme()

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
          <img className="app-logo kardex-wordmark" src="/design/kardex-logo.png" alt="Kardex" width="2172" height="724" />
        </div>
        <nav aria-label="Navegación principal">
          {tabs.map((t) => (
            <button
              key={t.id}
              className={`nav-item${activeTab === t.id ? ' active' : ''}`}
              aria-current={activeTab === t.id ? 'page' : undefined}
              onClick={() => setTab(t.id)}
            >
              <DesignIcon name={{ dashboard: 'home', products: 'inventory', categories: 'categories', suppliers: 'suppliers', movements: 'purchase', projects: 'quantity', users: 'suppliers' }[t.id]} />{t.label}
            </button>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div className="user-chip">
            <strong>{user.fullName}</strong>
            <small className="muted">{user.role}</small>
          </div>
          <button className="btn btn-sm" onClick={logout}>
            <DesignIcon name="logout" />Cerrar sesión
          </button>
        </div>
      </aside>

      <div className="main-workspace">
        <header className="topbar">
          <form className="global-search" onSubmit={e => { e.preventDefault(); setProductSearch(globalSearch); setLowStockView(false); setTab('products') }}>
            <DesignIcon name="search" />
            <input aria-label="Buscar productos" placeholder="Buscar producto por nombre o SKU" value={globalSearch} onChange={e => setGlobalSearch(e.target.value)} />
          </form>
          <div className="topbar-user"><button className="notification-button" aria-label="Ver productos con stock bajo" onClick={() => { setGlobalSearch(''); setProductSearch(''); setLowStockView(true); setTab('products') }}><DesignIcon name="bell" /></button><UserProfileMenu user={user} theme={theme} onToggleTheme={() => setTheme(theme === 'dark' ? 'light' : 'dark')} onOpenProfile={() => setProfileOpen(true)} onLogout={logout} /></div>
        </header>
      <main className="content">
        <div className="view-anim" key={activeTab}>
          {activeTab === 'dashboard' && <DashboardView onProducts={(low = false) => { setGlobalSearch(''); setProductSearch(''); setLowStockView(low); setTab('products') }} />}
          {activeTab === 'products' && <ProductsView key={`${productSearch}:${lowStockView}`} initialSearch={productSearch} initialLowStock={lowStockView} />}
          {activeTab === 'categories' && <CategoriesView />}
          {activeTab === 'suppliers' && <SuppliersView />}
          {activeTab === 'movements' && <MovementsView />}
          {activeTab === 'projects' && <ProjectsView />}
          {activeTab === 'users' && <UsersView />}
        </div>
        </main>
        {profileOpen && <ProfileView onClose={() => setProfileOpen(false)} />}
      </div>
    </div>
  )
}

export default App
