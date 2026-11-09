import { http } from './client'
import type {
  AppUser,
  AuthUser,
  Category,
  CategoryRequest,
  CategoryStockReport,
  CreateUserRequest,
  DashboardSummary,
  LoginRequest,
  Movement,
  MovementRequest,
  MovementType,
  Product,
  ProductRequest,
  Supplier,
  SupplierRequest,
  UpdateUserRequest,
} from './types'

export const authApi = {
  login: (body: LoginRequest) => http.post<AuthUser>('/auth/login', body),
  me: () => http.get<AuthUser>('/auth/me'),
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
  list: (categoryId?: number) =>
    http.get<Product[]>(`/products${categoryId ? `?categoryId=${categoryId}` : ''}`),
  lowStock: () => http.get<Product[]>('/products/low-stock'),
  get: (id: number) => http.get<Product>(`/products/${id}`),
  create: (body: ProductRequest) => http.post<Product>('/products', body),
  update: (id: number, body: ProductRequest) => http.put<Product>(`/products/${id}`, body),
  remove: (id: number) => http.delete<void>(`/products/${id}`),
}

export const dashboardApi = {
  summary: () => http.get<DashboardSummary>('/dashboard/summary'),
  byCategory: () => http.get<CategoryStockReport[]>('/dashboard/by-category'),
}

export const movementsApi = {
  list: (type?: MovementType) =>
    http.get<Movement[]>(`/movements${type ? `?type=${type}` : ''}`),
  byProduct: (productId: number) => http.get<Movement[]>(`/products/${productId}/movements`),
  register: (productId: number, body: MovementRequest) =>
    http.post<Movement>(`/products/${productId}/movements`, body),
}
