# Microservices Project

Spring Boot microservices demo with **central authentication**, **API gateway**, and **Netflix Eureka** service discovery.

- **Java 21**, **Spring Boot 4.1.x**, **Spring Cloud 2025.1.x**
- **auth-service** issues JWT and browser sessions; other services validate via shared **`common-security`** library
- **MySQL** per service (separate schemas)

---

## Architecture

```text
Browser / API client
        │
        ▼
  api-gateway :8080
        │
        ├── auth-service :8081      (login, register, JWT, session)
        ├── user-service :8082      (→ Feign → order-service)
        ├── order-service :8083
        ├── payment-service :8084
        └── inventory-service :8085

  service-discovery (Eureka) :8079
  notification-service :8086 (optional)
```

| Component | Port | Description |
|-----------|------|-------------|
| api-gateway | 8080 | Public entry; routes to services via Eureka |
| service-discovery | 8079 | Eureka server |
| auth-service | 8081 | Login, register, JWT, `AUTH_SESSION` cookie |
| user-service | 8082 | User APIs; calls order-service with Feign |
| order-service | 8083 | Order APIs |
| payment-service | 8084 | Payment APIs |
| inventory-service | 8085 | Inventory APIs |
| notification-service | 8086 | Notifications (Kafka disabled in Docker by default) |

---

## Authentication

Two ways to call protected APIs:

1. **Browser (SSO session)**
    - Register: `GET /api/auth/register`
    - Sign in: `GET /api/auth/home` (form login)
    - Session cookie: **`AUTH_SESSION`** (auth-service)
    - Use the **same host** everywhere (`localhost`, not mixed with `127.0.0.1`)

2. **API / Postman (JWT)**
    - `POST /api/auth/public/login` with JSON `{ "email", "password" }`
    - Header: `Authorization: Bearer <accessToken>`

**Gateway** and **user-service** / **order-service** enforce auth via **`common-security`** (JWT + session validation against auth-service).

Example (after login):

- Gateway: `http://localhost:8080/api/users/orders/order333`
- Register: `http://localhost:8080/api/auth/register`

---

## Prerequisites

### Local (IntelliJ / Maven Wrapper)

- **JDK 21**
- **MySQL** on `localhost:3306` (root/root) with databases created by JPA or manually:  
  `auth_db`, `user_db`, `order_db`, `payment_db`, `inventory_db`
- **Maven not required on PATH** — use each service’s **`mvnw.cmd`** (Windows) or **`mvnw`** (Git Bash)

### Docker

- **Docker Desktop** (Compose v2)
- Host **3307** mapped to MySQL in Compose (avoids conflict with local MySQL on 3306)

---

## Shared module: `common-security`

Not a runnable service — a **Maven JAR** used by user-service, order-service, and api-gateway.

After changing `common-security`:

```bash
cd common-security
../user-service/mvnw clean install -DskipTests
```

Installed JAR location:

```text
~/.m2/repository/com/example/common-security/0.0.1-SNAPSHOT/common-security-0.0.1-SNAPSHOT.jar
```

In IntelliJ: Maven → **common-security** → **install**, then **Reload** dependent modules.

JWT secret must match **auth-service** (`app.jwt.secret` / `APP_JWT_SECRET`).

---

## Run locally (manual order)

1. **service-discovery** (8079) — wait until Eureka is up
2. **auth-service** (8081)
3. **user-service**, **order-service**, **payment-service**, **inventory-service**
4. **api-gateway** (8080)

Example (Git Bash, from a service folder):

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-21"   # adjust path
./mvnw spring-boot:run -Dmaven.test.skip=true
```

IntelliJ: run each `*Application` class with **Project SDK = 21**. Prefer **Delegate build/run to Maven**.

---

## Run with Docker

From repository root:

```bash
docker compose up --build -d
docker compose ps
docker compose logs -f api-gateway
```

Stop:

```bash
docker compose down
```

**Startup order** (via `depends_on` + healthchecks): MySQL → Eureka → auth → other services → gateway.

Compose **overrides** datasource and Eureka URLs via environment variables (e.g. `jdbc:mysql://mysql:3306/...`) so the same `application.yaml` works locally with `localhost`.

**URLs**

| URL | Purpose |
|-----|---------|
| http://localhost:8080 | Gateway |
| http://localhost:8080/api/auth/home | Sign in |
| http://localhost:8080/api/auth/register | Register |
| http://localhost:8079 | Eureka dashboard |

Maven runs **inside** the Docker build image; you do not need Maven installed on the host for Docker builds.

---

## Configuration notes

- **Local** `application.yaml`: MySQL and Eureka use **`localhost`**
- **Docker** `docker-compose.yml`: sets `SPRING_DATASOURCE_*`, `EUREKA_*`, `APP_AUTH_*` for container hostnames
- **Demo** Feign on startup: `app.demo.run-on-startup` in user-service (keep `false` in normal use)

---

## Troubleshooting

| Issue | Likely cause |
|-------|----------------|
| `ERR_CONNECTION_REFUSED` on 8080 | Gateway not running |
| **401** on `/api/users/...` | Not logged in or missing Bearer token |
| **401** after “login” with JSON only | No browser session; use form login or Bearer |
| Docker: only Eureka running | MySQL port conflict — Compose uses host **3307** |
| `Keys` / JWT errors on startup | Rebuild `common-security` and reload Maven |
| IntelliJ **`out/`** folder | IDE compile output; safe to delete; add `/out/` to `.gitignore` |

---

## Project layout

```text
microservices-project/
├── common-security/          # Shared JWT + session security (Maven library)
├── service-discovery/        # Eureka server
├── api-gateway/
├── auth-service/
├── user-service/
├── order-service/
├── payment-service/
├── inventory-service/
├── notification-service/
├── docker-compose.yml
├── Dockerfile                # Build arg SERVICE_DIR=<module>
└── docker/mysql/             # DB init scripts
```

---