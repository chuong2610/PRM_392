# WayFlo

WayFlo là hệ thống bản đồ trong nhà cho kiosk và web, gồm backend Spring Boot và frontend Vite/React. Dự án tập trung vào nhập dữ liệu bản đồ, chuẩn hóa geometry, tìm kiếm địa điểm và điều hướng trong tòa nhà.

## Công nghệ

- Backend: Java 21, Spring Boot 3.3.x
- Persistence: Spring Data JPA, PostgreSQL
- Spatial: PostGIS, Hibernate Spatial, JTS
- Migration: Flyway
- Cache: Redis
- API docs: Springdoc OpenAPI / Swagger UI
- Frontend: React 18, Vite, Three.js

## Cấu trúc

- `src/main/java`: mã nguồn backend
- `src/main/resources`: cấu hình, migration và seed data
- `fe/`: frontend Vite
- `docker-compose.yml`: Postgres/PostGIS và Redis local
- `plan.md`: tài liệu định hướng triển khai backend

## Yêu cầu

- JDK 21
- Maven 3.9+
- Node.js 18+ hoặc 20+
- Docker Desktop nếu muốn chạy Postgres/PostGIS và Redis bằng Compose

## Chạy local

### 1. Khởi động hạ tầng

```bash
docker compose up -d
```

Compose sẽ mở:

- PostgreSQL/PostGIS tại `localhost:5432`
- Redis tại `localhost:6379`

Thông tin mặc định của database:

- Database: `wayflo`
- User: `wayflo`
- Password: `wayflo`

### 2. Chạy backend

```bash
mvn spring-boot:run
```

Backend dùng cấu hình mặc định trong `src/main/resources/application.yml`:

- Datasource: `jdbc:postgresql://localhost:5432/wayflo`
- Redis: `localhost:6379`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Actuator health: `http://localhost:8080/actuator/health`

### 3. Chạy frontend

```bash
cd fe
npm install
npm run dev
```

Frontend Vite chạy ở `http://127.0.0.1:5173`.

### 4. Build frontend

```bash
cd fe
npm run build
```

### 5. Kiểm tra UI

```bash
cd fe
npm run verify:ui
```

## Seed data

Backend có hỗ trợ seed dữ liệu bản đồ từ `classpath*:seed/buildings/*/floor-*.json`, nhưng mặc định đang tắt bằng cấu hình:

```yaml
wayflo:
  seed:
    enabled: false
```

Nếu muốn import seed khi khởi động, hãy bật lại cấu hình này trong `application.yml` hoặc profile riêng.

## Migration

Flyway tự động chạy khi backend khởi động. Các migration hiện có nằm ở:

- `src/main/resources/db/migration/V1__enable_extensions.sql`
- `src/main/resources/db/migration/V2__create_wayflo_schema.sql`

## API hữu ích

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`
- Health check: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## Ghi chú

- `spring.jpa.hibernate.ddl-auto` đang để `validate`, nên schema phải khớp với migration.
- Dự án không triển khai định vị người dùng ngoài trời hoặc indoor positioning tự động.
- Frontend và backend là hai ứng dụng riêng, nên cần chạy cả hai nếu muốn kiểm thử end-to-end.
