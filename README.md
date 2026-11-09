# Kardex

Sistema de gestión de inventario para **DOMIO** (soluciones tecnológicas, domótica,
cámaras de seguridad, redes y dispositivos electrónicos).

Permite gestionar equipos y materiales, controlar entradas y salidas con libro mayor
inmutable, asignar y entregar material a proyectos e instalaciones de clientes, y
consultar existencias con búsqueda y paginación del lado del servidor.

> Nombre de producto: **Kardex**. Los identificadores técnicos internos (paquete
> Java `co.domio.stockcontrol`, base de datos y contenedor Docker) conservan el
> nombre histórico `stockcontrol` para no romper despliegues existentes.

## Arquitectura

Tres capas desacopladas:

```
React + TS (Vite)  --HTTP/JSON-->  Spring Boot (API REST)  --JDBC-->  PostgreSQL
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
│   │   ├── config/           # configuración (CORS, OpenAPI, guarda de prod)
│   │   ├── common/           # dto compartido (paginación) y excepciones
│   │   ├── project/          # proyectos e instalaciones de clientes
│   │   └── ...               # product, movement, user, category, supplier, auth
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-prod.yml
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
| `JWT_SECRET`             | valor de desarrollo                           | backend             |
| `JWT_EXPIRATION_MINUTES` | `480` (8 h)                                   | backend             |
| `ADMIN_USERNAME`         | `admin`                                       | backend             |
| `ADMIN_PASSWORD`         | `admin123` **(solo dev)**                     | backend             |
| `CORS_ALLOWED_ORIGINS`  | `http://localhost:5173`                       | backend             |
| `POSTGRES_DB/USER/PASSWORD` | `stockcontrol`                             | docker-compose      |
| `VITE_API_BASE_URL`      | `/api`                                        | frontend            |

Copia `backend/.env.example` y `frontend/.env.example` a `.env` para personalizar.
Los archivos `.env` están excluidos de git.

## Perfil de producción

El perfil `prod` no arranca con valores de desarrollo. Una guarda de
configuración (`config/ProductionConfigGuard`) valida al arranque y **detiene la
aplicación con un mensaje claro** si algo falta o conserva un valor conocido de
desarrollo. Nunca imprime el valor de los secretos, solo el nombre de la variable.

**Obligatorias en `prod`** (defínelas todas antes de arrancar):

| Variable                | Requisito                                                             |
|-------------------------|-----------------------------------------------------------------------|
| `JWT_SECRET`            | Aleatorio, mínimo 32 caracteres, distinto del valor de dev            |
| `ADMIN_PASSWORD`        | Mínimo 8 caracteres, distinta de `admin123`                           |
| `ADMIN_USERNAME`        | Definido                                                              |
| `CORS_ALLOWED_ORIGINS` | Orígenes explícitos separados por coma, **sin** `localhost`/`127.0.0.1` |

Ejemplo de arranque:

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL="jdbc:postgresql://tu-servidor:5432/stockcontrol"
export DB_USERNAME=... DB_PASSWORD=...
export JWT_SECRET="$(openssl rand -base64 48)"
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD="$(openssl rand -base64 16)"
export CORS_ALLOWED_ORIGINS="https://inventario.tu-dominio.co"
./mvnw spring-boot:run
```

Si falta algo, verás en el arranque un bloque tipo:

```
Configuracion de produccion invalida. Corrige lo siguiente y vuelve a arrancar:
- JWT_SECRET no esta definido. Genera un secreto aleatorio de al menos 32 caracteres.
- CORS_ALLOWED_ORIGINS contiene el origen local 'http://localhost:5173', ...
```

### Provisionar el primer administrador

El backend crea el usuario administrador inicial **solo si la tabla de usuarios
está vacía**, usando `ADMIN_USERNAME` y `ADMIN_PASSWORD` (cifrada con BCrypt). Dos
formas de proceder:

1. **Primer despliegue:** define `ADMIN_USERNAME`/`ADMIN_PASSWORD` en el entorno,
   arranca una vez y el admin queda creado. Cambia la contraseña después desde la
   pestaña Usuarios si lo necesitas.
2. **Despliegue existente:** si ya hay usuarios, el inicializador no hace nada;
   crea los usuarios que necesites desde la sesión de otro ADMIN (o de la consola
   de base de datos insertando un usuario con la contraseña ya cifrada con BCrypt).

En `dev` el usuario sembrado es `admin` / `admin123` (solo para desarrollo).

## Permisos por rol

La autorización se verifica **en el backend** (`security/SecurityConfig`); ocultar
botones en la interfaz es solo mejora de usabilidad, no control de acceso.

| Operación                                        | ADMIN | OPERADOR |
|--------------------------------------------------|:-----:|:--------:|
| Ver panel, productos, categorías, proveedores, movimientos y proyectos | ✅ | ✅ |
| Registrar entradas, salidas y ajustes de inventario | ✅ | ✅ |
| Entregar y devolver material a proyectos          | ✅ | ✅ |
| Crear/editar/eliminar productos, categorías y proveedores | ✅ | ❌ |
| Crear/editar/eliminar proyectos y sus asignaciones de material | ✅ | ❌ |
| Archivar/reactivar productos                      | ✅ | ❌ |
| Crear/editar/eliminar usuarios                    | ✅ | ❌ |

Un `OPERADOR` que llame directamente a la API con una operación reservada recibe
`403 Acceso denegado`.

## Trazabilidad del stock (libro mayor)

- Todo cambio de existencias queda registrado en `stock_movement` con el stock
  anterior y el resultante; los movimientos no se editan ni se borran.
- **Al crear un producto con stock inicial > 0** se registra en la misma
  transacción un movimiento de apertura (`IN`, desde 0): si falla el movimiento,
  no se crea el producto.
- Los productos creados antes de esta funcionalidad recibieron un saldo de
  apertura mediante la migración `V7__opening_balances.sql` (solo los que tenían
  existencias y ningún movimiento; no duplica historiales existentes).
- Entradas y salidas exigen cantidad > 0; los ajustes no permiten stock
  negativo; las salidas no pueden exceder las existencias disponibles.
- **Eliminar un producto con movimientos** devuelve un error de negocio (422):
  usa *Archivar* en su lugar para conservar el historial. Archivar desactiva el
  producto para listados y formularios nuevos, sin perder datos.

## Búsquedas y listados

- `GET /api/products?search=&categoryId=&lowStock=&page=&size=` — búsqueda por
  nombre o SKU, filtro por categoría y bajo stock, paginado (por defecto 20 por
  página, ordenado por nombre).
- `GET /api/movements?product=&type=&from=&to=&page=&size=` — filtro por
  texto de producto (nombre/SKU), tipo (`IN`/`OUT`/`ADJUSTMENT`) y rango de
  fechas, paginado (por defecto 20, más recientes primero).
- Ambos devuelven un envoltorio `PageResponse`: `content`, `page`, `size`,
  `totalElements`, `totalPages`.

## Proyectos e instalaciones (asignación de material)

Un proyecto agrupa material del inventario que se entrega en una instalación de
cliente. Ciclo: **planificar → asignar material → entregar → (opcional) devolver → cerrar**.

- **Crear proyecto** (`POST /api/projects`): nombre, cliente, estado
  (`PLANNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`), fechas y notas. Solo ADMIN.
- **Asignar material** (`POST /api/projects/{id}/assignments`): producto y
  cantidad prevista. Planifica: **no descuenta stock**. Si el producto ya estaba
  asignado, la cantidad se suma. Solo ADMIN.
- **Entregar material** (`POST /api/projects/{id}/deliveries`): descuenta stock,
  registra el movimiento `OUT` vinculado al proyecto y avanza el acumulado
  entregado — todo en una transacción con bloqueo del producto. ADMIN u OPERADOR.
- **Devolver material** (`POST /api/projects/{id}/returns`): reintegra stock y
  registra un movimiento `IN` vinculado. Disponible en cualquier estado del
  proyecto (al cerrar o cancelar puede sobrar material). ADMIN u OPERADOR.
- **Historial del proyecto** (`GET /api/projects/{id}/movements`): movimientos
  vinculados, aunque el proyecto esté cerrado o cancelado.

Reglas de negocio aplicadas:

- No se puede entregar más de lo asignado (evita descontar dos veces o exceder
  la asignación) ni más de lo que hay en stock.
- No se puede devolver más unidades de las entregadas y no devueltas.
- Un proyecto `COMPLETED` o `CANCELLED` no admite entregas, pero sí devoluciones,
  y **conserva todo su historial**.
- Eliminar un proyecto con asignaciones o movimientos devuelve error de negocio
  (422): cancela el proyecto cambiando su estado en su lugar.

En la interfaz, la pestaña **Proyectos** permite consultar proyectos, su material
asignado (asignado/entregado/devuelto/pendiente) y el historial de movimientos.

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
- [x] **Etapa 8 — Trazabilidad, roles, búsquedas y proyectos:**
      movimiento de apertura al crear productos (y migración de saldos
      existentes), archivado de productos con historial, permisos por rol
      verificados en el backend, guarda de configuración de producción,
      búsqueda/filtros/paginación del lado del servidor para productos y
      movimientos, y proyectos/instalaciones de clientes con entrega y
      devolución de material de forma atómica.
