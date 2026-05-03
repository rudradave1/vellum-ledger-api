# vellum-ledger-api

Sync backend for [VellumLedger](https://github.com/rudradave1/VellumLedger). Handles push/pull transaction sync with JWT authentication and timestamp-based conflict resolution.

Built with Ktor + PostgreSQL + Exposed ORM. Deployed on Railway.

**Live base URL:** `https://vellum-ledger-api-production.up.railway.app`

---

## Try it now

```bash
# Health check
curl https://vellum-ledger-api-production.up.railway.app/health

# Register
curl -X POST https://vellum-ledger-api-production.up.railway.app/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# Login
curl -X POST https://vellum-ledger-api-production.up.railway.app/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# Pull transactions (replace TOKEN)
curl "https://vellum-ledger-api-production.up.railway.app/transactions/pull?lastSync=0" \
  -H "Authorization: Bearer TOKEN"
```

---

## API

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/health` | No | Service health check |
| POST | `/auth/register` | No | Create account, returns JWT |
| POST | `/auth/login` | No | Login, returns JWT |
| POST | `/transactions/push` | JWT | Push local transactions to server |
| GET | `/transactions/pull?lastSync=0` | JWT | Pull transactions since timestamp |

### Push request body
```json
{
  "transactions": [
    {
      "id": "client-generated-uuid",
      "userId": "user-id",
      "amount": 250.0,
      "category": "Food",
      "note": "Dinner",
      "type": "EXPENSE",
      "createdAt": 1700000000000,
      "updatedAt": 1700000000000
    }
  ]
}
```

### Push response
```json
{
  "synced": 3,
  "conflicts": 0,
  "failed": 0
}
```

---

## Conflict resolution

Every transaction carries a `updatedAt` timestamp. On push:

- If the transaction doesn't exist on the server → insert, status `SYNCED`
- If incoming `updatedAt` > server `updatedAt` → update, status `SYNCED`
- If server `updatedAt` is newer → reject, status `CONFLICT`

The server is the final arbiter. The mobile client pulls after push to reconcile any conflicts.

---

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin (JVM 17) |
| Framework | Ktor Server 2.3 |
| ORM | Exposed |
| Database | PostgreSQL |
| Auth | JWT — 30-day expiry |
| Connection pool | HikariCP |
| Password hashing | BCrypt |
| Deploy | Railway (Dockerfile) |

---

## Running locally

**Prerequisites:** JDK 17+, PostgreSQL running locally

```bash
# Create database
createdb vellum_ledger

# Set environment variables
export DATABASE_URL=jdbc:postgresql://localhost:5432/vellum_ledger
export DATABASE_USER=postgres
export DATABASE_PASSWORD=password
export JWT_SECRET=your-secret-min-32-chars
export JWT_ISSUER=vellum-ledger
export JWT_AUDIENCE=vellum-ledger-users

# Run
./gradlew run
```

Hit `http://localhost:8080/health` to verify.

---

## Deploying to Railway

1. Fork this repo
2. Create a new Railway project → Deploy from GitHub
3. Add a PostgreSQL plugin
4. Set environment variables in Railway dashboard:

```
DATABASE_URL        → from Railway PostgreSQL plugin
DATABASE_USER       → postgres
DATABASE_PASSWORD   → from Railway PostgreSQL plugin
JWT_SECRET          → generate with: openssl rand -base64 32
JWT_ISSUER          → vellum-ledger
JWT_AUDIENCE        → vellum-ledger-users
```

5. Railway builds via `Dockerfile` automatically on push to main

---

## Project structure

```
src/main/kotlin/com/vellum/api/
├── Application.kt
├── plugins/
│   ├── Auth.kt           ← JWT configuration
│   ├── Routing.kt        ← route registration
│   ├── Serialization.kt
│   └── HTTP.kt           ← CORS, status pages
├── routes/
│   ├── AuthRoutes.kt
│   └── TransactionRoutes.kt
├── domain/
│   ├── model/            ← Transaction, User, SyncModels
│   └── service/          ← AuthService, TransactionService
└── data/
    ├── DatabaseFactory.kt ← HikariCP + Exposed setup
    ├── tables/            ← UsersTable, TransactionsTable
    └── dao/               ← UserDao, TransactionDao
```

---

Mobile client: [VellumLedger](https://github.com/rudradave1/VellumLedger)

MIT License
