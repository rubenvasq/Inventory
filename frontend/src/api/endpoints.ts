import { http } from './client'
import type {
  AppUser,
  AssignmentRequest,
  AuthUser,
  Category,
  CategoryRequest,
  CategoryStockReport,
  ChangePasswordRequest,
  CreateUserRequest,
  DashboardSummary,
  LoginRequest,
  Movement,
  MovementFilters,
  MovementRequest,
  Page,
  Product,
  ProductFilters,
  ProductRequest,
  Project,
  ProjectAssignment,
  ProjectFilters,
  ProjectMaterialRequest,
  ProjectRequest,
  Supplier,
  SupplierRequest,
  UpdateProfileRequest,
  UpdateUserRequest,
} from './types'

/** Convierte filtros opcionales en query string (sin pares vacios). */
function query(filters: Record<string, unknown>): string {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(filters)) {
    if (value !== undefined && value !== null && value !== '' && value !== false) {
      params.set(key, String(value))
    }
  }
  const qs = params.toString()
  return qs ? `?${qs}` : ''
}

export const authApi = {
  login: (body: LoginRequest) => http.post<AuthUser>('/auth/login', body),
  me: () => http.get<AuthUser>('/auth/me'),
}

/** Perfil del usuario autenticado (disponible para ADMIN y OPERADOR). */
export const profileApi = {
  get: () => http.get<AppUser>('/profile'),
  update: (body: UpdateProfileRequest) => http.put<AppUser>('/profile', body),
  changePassword: (body: ChangePasswordRequest) => http.put<AppUser>('/profile/password', body),
}

export const usersApi = {
  list: () => http.get<AppUser[]>('/users'),
  create: (body: CreateUserRequest) => http.post<AppUser>('/users', body),
  update: (id: number, body: UpdateUserRequest) => http.put<AppUser>(`/users/${id}`, body),
  remove: (id: number) => http.delete<void>(`/users/${id}`),
}

export const categoriesApi = {
  list: () => http.get<Category[]>('/categories'),
  create: (body: CategoryRequest) => http.post<Category>('/categories', body),
  update: (id: number, body: CategoryRequest) => http.put<Category>(`/categories/${id}`, body),
  remove: (id: number) => http.delete<void>(`/categories/${id}`),
}

export const suppliersApi = {
  list: () => http.get<Supplier[]>('/suppliers'),
  create: (body: SupplierRequest) => http.post<Supplier>('/suppliers', body),
  update: (id: number, body: SupplierRequest) => http.put<Supplier>(`/suppliers/${id}`, body),
  remove: (id: number) => http.delete<void>(`/suppliers/${id}`),
}

export const productsApi = {
  search: (filters: ProductFilters) => http.get<Page<Product>>(`/products${query({ ...filters })}`),
  lowStock: () => http.get<Product[]>('/products/low-stock'),
  get: (id: number) => http.get<Product>(`/products/${id}`),
  create: (body: ProductRequest) => http.post<Product>('/products', body),
  update: (id: number, body: ProductRequest) => http.put<Product>(`/products/${id}`, body),
  archive: (id: number) => http.patch<Product>(`/products/${id}/archive`),
  restore: (id: number) => http.patch<Product>(`/products/${id}/restore`),
  remove: (id: number) => http.delete<void>(`/products/${id}`),
}

export const dashboardApi = {
  summary: () => http.get<DashboardSummary>('/dashboard/summary'),
  byCategory: () => http.get<CategoryStockReport[]>('/dashboard/by-category'),
}

export const movementsApi = {
  search: (filters: MovementFilters) => http.get<Page<Movement>>(`/movements${query({ ...filters })}`),
  byProduct: (productId: number) => http.get<Movement[]>(`/products/${productId}/movements`),
  register: (productId: number, body: MovementRequest) =>
    http.post<Movement>(`/products/${productId}/movements`, body),
}

export const projectsApi = {
  search: (filters: ProjectFilters) => http.get<Page<Project>>(`/projects${query({ ...filters })}`),
  get: (id: number) => http.get<Project>(`/projects/${id}`),
  create: (body: ProjectRequest) => http.post<Project>('/projects', body),
  update: (id: number, body: ProjectRequest) => http.put<Project>(`/projects/${id}`, body),
  remove: (id: number) => http.delete<void>(`/projects/${id}`),
  assign: (projectId: number, body: AssignmentRequest) =>
    http.post<ProjectAssignment>(`/projects/${projectId}/assignments`, body),
  removeAssignment: (projectId: number, assignmentId: number) =>
    http.delete<void>(`/projects/${projectId}/assignments/${assignmentId}`),
  deliver: (projectId: number, body: ProjectMaterialRequest) =>
    http.post<Movement>(`/projects/${projectId}/deliveries`, body),
  returnMaterial: (projectId: number, body: ProjectMaterialRequest) =>
    http.post<Movement>(`/projects/${projectId}/returns`, body),
  movements: (projectId: number) => http.get<Movement[]>(`/projects/${projectId}/movements`),
}
