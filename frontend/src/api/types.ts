// Tipos que reflejan los DTOs del backend (co.domio.stockcontrol).

export interface Category {
  id: number
  name: string
  description: string | null
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface CategoryRequest {
  name: string
  description?: string | null
  active?: boolean | null
}

export interface Supplier {
  id: number
  name: string
  contactName: string | null
  email: string | null
  phone: string | null
  address: string | null
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface SupplierRequest {
  name: string
  contactName?: string | null
  email?: string | null
  phone?: string | null
  address?: string | null
  active?: boolean | null
}

export interface Product {
  id: number
  sku: string
  name: string
  description: string | null
  imageUrl: string | null
  categoryId: number
  categoryName: string
  supplierId: number | null
  supplierName: string | null
  unit: string
  unitPrice: number | null
  stock: number
  minStock: number
  lowStock: boolean
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface ProductRequest {
  sku: string
  name: string
  description?: string | null
  imageUrl?: string | null
  categoryId: number
  supplierId?: number | null
  unit?: string | null
  unitPrice?: number | null
  stock?: number | null
  minStock?: number | null
}

export type MovementType = 'IN' | 'OUT' | 'ADJUSTMENT'

export interface Movement {
  id: number
  productId: number
  productSku: string
  productName: string
  type: MovementType
  quantity: number
  previousStock: number
  newStock: number
  note: string | null
  projectId: number | null
  projectName: string | null
  createdAt: string
}

export interface MovementRequest {
  type: MovementType
  quantity: number
  note?: string | null
}

/** Pagina devuelta por los listados paginados del backend. */
export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** Filtros de busqueda de productos (paginados en el servidor). */
export interface ProductFilters {
  search?: string
  categoryId?: number
  lowStock?: boolean
  page?: number
  size?: number
}

/** Filtros de busqueda de movimientos (paginados en el servidor). */
export interface MovementFilters {
  product?: string
  type?: MovementType
  from?: string
  to?: string
  page?: number
  size?: number
}

export type ProjectStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

export interface ProjectAssignment {
  id: number
  productId: number
  productSku: string
  productName: string
  unit: string
  assignedQuantity: number
  deliveredQuantity: number
  returnedQuantity: number
  pendingQuantity: number
  createdAt: string
  updatedAt: string
}

export interface Project {
  id: number
  name: string
  client: string
  status: ProjectStatus
  startDate: string | null
  endDate: string | null
  notes: string | null
  assignments: ProjectAssignment[]
  createdAt: string
  updatedAt: string
}

export interface ProjectRequest {
  name: string
  client: string
  status?: ProjectStatus | null
  startDate?: string | null
  endDate?: string | null
  notes?: string | null
}

export interface AssignmentRequest {
  productId: number
  quantity: number
}

export interface ProjectMaterialRequest {
  productId: number
  quantity: number
  note?: string | null
}

export interface ProjectFilters {
  search?: string
  status?: ProjectStatus
  page?: number
  size?: number
}

export type Role = 'ADMIN' | 'OPERADOR'

export interface LoginRequest {
  username: string
  password: string
}

export interface AuthUser {
  token: string | null
  tokenType: string
  expiresInMinutes: number
  username: string
  fullName: string
  role: Role
}

export interface AppUser {
  id: number
  username: string
  fullName: string
  role: Role
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateUserRequest {
  username: string
  password: string
  fullName: string
  role: Role
  enabled?: boolean | null
}

export interface UpdateUserRequest {
  fullName: string
  role: Role
  enabled?: boolean | null
  password?: string | null
}

export interface DashboardSummary {
  totalProducts: number
  totalCategories: number
  totalSuppliers: number
  lowStockCount: number
  totalStockUnits: number
  totalInventoryValue: number
}

export interface CategoryStockReport {
  categoryId: number
  categoryName: string
  productCount: number
  totalUnits: number
  totalValue: number
}

// Cuerpo de error estandar devuelto por el backend (ApiError).
export interface ApiErrorBody {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
  details?: { field: string; message: string }[] | null
}
