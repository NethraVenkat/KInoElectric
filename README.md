# PadaShakti — Backend (Spring Boot)

Smart Urban Subway Kinetic Floor Energy Harvesting Network.

**Stack:** Java 21 · Spring Boot 4.1.1 · Spring Data JPA · MySQL 8 · Flyway · Spring Security (JWT) · WebSocket · Swagger

## Package structure
```
com.backend.javaleap
 ├─ JavaleapApplication.java
 ├─ SecurityConfig.java / CorsConfig.java / OpenApiConfig.java   (Spring configuration)
 ├─ controller/   REST APIs
 ├─ service/      Business logic, mappers, JWT & login services
 ├─ repository/   Spring Data JPA repositories (+ projection/ for query views)
 ├─ entity/       JPA entities (+ enums/)
 ├─ exception/    Custom exceptions, GlobalExceptionHandler, 401/403 handlers
 └─ model/        DTOs (request/, response/), ApiResponse, AppProperties, constants
```

## Profiles
| File | Use |
|---|---|
| application.properties | Common settings + `app.*` config |
| application-dev.properties | Local MySQL (default profile) |
| application-prod.properties | Deployment — everything from env vars |
| test/application-test.properties | H2 in-memory for tests |

## Run locally
1. Start MySQL 8 (database `padashakti` is auto-created on first run).
2. `mvnw spring-boot:run`  (Windows) or `./mvnw spring-boot:run`
3. Check:
   - http://localhost:8080/api/health
   - http://localhost:8080/swagger-ui.html
   - http://localhost:8080/actuator/health

## Tests
`mvnw test` — uses H2, no MySQL needed.

## Security (Step 4)
Stateless JWT (HS256). Log in, then send `Authorization: Bearer <accessToken>`.

| Access | Endpoints |
|---|---|
| Public | `POST /api/auth/login`, `/api/health`, `/actuator/health`, Swagger, `/ws/**` |
| Any logged-in user | `GET /api/**`, `/api/auth/me`, `/api/auth/change-password` |
| OPERATOR (+ADMIN) | `POST /api/simulation/**` |
| ENGINEER (+ADMIN) | `PUT/PATCH /api/tiles/**` |
| ADMIN | `/api/users/**` |

Demo users: `admin/Admin@123`, `operator/Operator@123`, `engineer/Engineer@123`.
Brute-force protection: 5 wrong passwords lock the username for 5 minutes.
Swagger: click **Authorize** and paste the token.

## Kinetic energy core (Phase B)

| Method | Endpoint | Who |
|---|---|---|
| POST | `/api/kinetic/sectors` | ADMIN - creates sector + tile grid + analytic account |
| GET | `/api/kinetic/sectors`, `/sectors/{id}`, `/sectors/{id}/tiles`, `/tiles/{id}` | ADMIN, ACCOUNTANT |
| PATCH | `/api/kinetic/sectors/{id}`, `/tiles/{id}/status` | ADMIN |
| GET / PUT | `/api/kinetic/conversion-rates`, `/conversion-rates/{PIEZO|ELECTROMAGNETIC}` | read all / ADMIN |
| POST | `/api/kinetic/telemetry` | SYSTEM (`X-API-KEY`) or ADMIN |
| GET | `/api/kinetic/telemetry/recent`, `/api/kinetic/generation/daily` | ADMIN, ACCOUNTANT |
| GET / POST | `/api/simulation/status`, `start`, `stop`, `speed`, `rush-hour`, `inject-fault` | read all / ADMIN |
| GET / POST / DELETE | `/api/admin/api-keys` | ADMIN |

**Energy model:** energy per step = rated J x conversion efficiency x tile health.
Batteries charge (95% efficiency) up to 80%; the surplus is exported to the grid and logged
per sector per day in `energy_generation_daily` (the basis for grid invoices).
Crowd density per sector uses Little's Law (arrival rate x dwell time).

Demo users: `admin/Admin@123` (Transit Energy Director), `accountant/Accountant@123`, `system/System@123`.
Demo gateway key: `pdk_sys_ChcKineticTelemetry2026Demo` (dev only).

## Accounting core (Phase C)

| Method | Endpoint | Who |
|---|---|---|
| GET / POST / PUT / PATCH | `/api/accounting/contacts` (`?type=CUSTOMER|VENDOR&q=`) | ADMIN, ACCOUNTANT |
| GET / POST / PUT / PATCH | `/api/accounting/products` | ADMIN, ACCOUNTANT |
| GET | `/api/accounting/accounts`, `/accounts/{id}`, `/accounts/{id}/ledger` | ADMIN, ACCOUNTANT |
| POST / PATCH / DELETE | `/api/accounting/accounts` | ADMIN |
| GET / PATCH | `/api/accounting/journals` | read all / ADMIN |
| GET / POST / PUT / DELETE | `/api/accounting/journal-entries` | ADMIN, ACCOUNTANT |
| POST | `/journal-entries/{id}/post`, `/cancel`, `/reverse` | ADMIN, ACCOUNTANT |
| GET | `/api/accounting/trial-balance?asOf=` | ADMIN, ACCOUNTANT |

**Double-entry rules:** every entry balances; a line is either debit or credit; group/inactive accounts
cannot be posted; receivable/payable lines need a contact; posting assigns a gap-free number per journal
and year (`INV/2026/0001`, `BILL/...`, `BNK/...`, `JE/...`) under a row lock; posted entries are immutable
and are corrected with reversal entries.

**Seeded:** 20-account chart (use-case accounts + receivable, GST, equity), journals SAL/PUR/CSH/BNK/MISC,
fictional contacts (Chennai Regional Power Grid Corporation, KinetiFloor Technologies, VoltEdge Inverter
Services), 5 products, and an opening balance entry `JE/2026/0001` of Rs 54,30,000.

## Transaction flows (Phase D)

| Flow | Endpoints |
|---|---|
| Purchase | `POST /api/purchase/orders` → `/{id}/confirm` (PO/2026/0001) → `/{id}/bill` → `POST /api/invoices/{id}/post` (BILL/2026/0001) |
| Energy sales | `POST /api/sales/orders/energy` {customerId, stationCode, periodStart, periodEnd} → `/{id}/confirm` (SO/...) → `/{id}/invoice` → `POST /api/invoices/{id}/post` (INV/...) |
| Payments | `POST /api/payments` {invoiceId, amount, method} (BNK/...), `/{id}/cancel` |
| Lists | `GET /api/purchase/orders`, `/api/sales/orders`, `/api/invoices?type=&paymentState=&overdueOnly=`, `/api/payments` |

**Postings:** vendor bill = Dr expense/asset (+analytic) + Dr GST input / Cr vendor payable;
customer invoice = Dr grid receivable / Cr energy revenue per sector (+analytic) + Cr GST output;
receipt = Dr bank / Cr receivable; vendor payment = Dr payable / Cr bank.
GST is split CGST+SGST for Tamil Nadu (state code 33) contacts, IGST otherwise.
The energy sales order reads kWh exported per sector from `energy_generation_daily`
and refuses overlapping periods (no double billing). Cancelling reverses entries
(payments must be cancelled before their invoice).
