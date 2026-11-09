# StockControl

Sistema de gestión de inventario para **DOMIO** (soluciones tecnológicas, domótica,
cámaras de seguridad, redes y dispositivos electrónicos).

Permite gestionar equipos y materiales, controlar entradas y salidas, registrar
movimientos, consultar existencias y —en etapas posteriores— asignar materiales a
proyectos e instalaciones de clientes.

## Arquitectura

Tres capas desacopladas:

```
React + TS (Vite)  --HTTP/JSON-->  Spring Boot (API REST)  --JDBC-->  PostgreSQL 16
      :5173                               :8080                            :5432
```

- El **frontend** nunca accede a la base de datos; solo consume la API REST.
- En desarrollo, Vite reenvía `/api` al backend (proxy) para evitar problemas de CORS.
- El **esquema de la base de datos** lo gestiona Flyway con migraciones versionadas;
  Hibernate solo valida (`ddl-auto=validate`).

## Stack

| Capa        | Tecnología                                            |
|-------------|-------------------------------------------------------|
| Backend     | Java 21, Spring Boot 4.1.1, Maven (wrapper)           |
| Persistencia| Spring Data JPA / Hibernate, Flyway                   |
| Validación  | Jakarta Validation                                    |
| Base de datos | PostgreSQL 16 (vía Docker Compose)                  |
| Frontend    | React 19, TypeScript, Vite 8                          |
| Infra dev   | Docker Compose                                        |

## Estructura del repositorio

```
server/
├── docker-compose.yml        # PostgreSQL para desarrollo
├── backend/                  # API Spring Boot
│   ├── src/main/java/co/domio/stockcontrol/
│   │   ├── config/           # configuración (CORS, OpenAPI, ...)
│   │   └── common/exception/ # manejo central de errores
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-dev.yml
│       └── db/migration/     # migraciones Flyway
└── frontend/                 # SPA React + TS
    └── src/
```

## Requisitos previos

- **JDK 21** (ej. Eclipse Temurin). Maven NO es necesario: se usa el wrapper `mvnw`.
- **Docker Desktop** (para PostgreSQL).
- **Node.js 20+** y npm.

> El proyecto está en una carpeta sincronizada por OneDrive. Si notas bloqueos de
> archivos al compilar, considera moverlo a una ruta local fuera de OneDrive.

## Cómo ejecutar en local

### 1. Base de datos (PostgreSQL)

```bash
# desde la raíz del repo
docker compose up -d
docker compose ps        # verifica que 'stockcontrol-db' esté "healthy"
```

### 2. Backend

```bash
cd backend
# (opcional) cp .env.example .env    # los valores por defecto ya coinciden
./mvnw spring-boot:run               # en Windows: mvnw.cmd spring-boot:run
```

El backend arranca en `http://localhost:8080`. Verificación:

```bash
curl http://localhost:8080/actuator/health   # => {"status":"UP"}
```

### 3. Frontend

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```

## Configuración por variables de entorno

Nada sensible va en el código. Variables (con sus valores por defecto para dev):

| Variable                 | Por defecto                                   | Dónde se usa        |
|--------------------------|-----------------------------------------------|---------------------|
| `SPRING_PROFILES_ACTIVE` | `dev`                                         | backend             |
| `DB_URL`                 | `jdbc:postgresql://localhost:5432/stockcontrol` | backend           |
| `DB_USERNAME`            | `stockcontrol`                                | backend             |
| `DB_PASSWORD`            | `stockcontrol`                                | backend             |
| `SERVER_PORT`            | `8080`                                        | backend             |
| `POSTGRES_DB/USER/PASSWORD` | `stockcontrol`                             | docker-compose      |
| `VITE_API_BASE_URL`      | `/api`                                        | frontend            |

Copia `backend/.env.example` y `frontend/.env.example` a `.env` para personalizar.
Los archivos `.env` están excluidos de git.

## Estado del proyecto (plan por etapas)

- [x] **Etapa 1 — Cimientos:** estructura monorepo, Docker Compose (Postgres),
      esqueleto Spring Boot que arranca, proyecto Vite que arranca.
- [x] **Etapa 2 — Dominio base + CRUD** de Categorías y Proveedores (Flyway, DTOs,
      validación, manejo central de errores, Swagger en `/swagger-ui.html`).
- [x] **Etapa 3 — Productos y existencias** (entidad `Product` con FK a categoría
      y proveedor, SKU único, precio, stock y stock mínimo, filtro por categoría y
      consulta de bajo stock en `/api/products/low-stock`).
- [x] **Etapa 4 — Movimientos de inventario** (libro mayor inmutable `StockMovement`
      con tipos IN/OUT/ADJUSTMENT; actualiza el stock del producto de forma atómica
      con bloqueo; rechaza salidas sin existencias (422). `POST/GET
      /api/products/{id}/movements` y `GET /api/movements`).
- [x] **Etapa 5 — Frontend funcional** (SPA React+TS: navegación por pestañas,
      CRUD de Categorías, Proveedores y Productos, registro de movimientos con
      historial, filtro por categoría y bajo stock; capa de API tipada con manejo
      de errores y notificaciones).
- [x] **Etapa 6 — Dashboard y reportes** (endpoints `/api/dashboard/summary` y
      `/api/dashboard/by-category`; panel con KPIs, valor de inventario por
      categoría y listado de productos con bajo stock).
- [x] **Etapa 7 — Usuarios, roles y pruebas** (Spring Security + JWT, roles
      ADMIN/OPERADOR, login en el frontend, gestión de usuarios para ADMIN, y
      pruebas de integración con Testcontainers de los servicios clave).
