# Vellum Ledger API

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

[MIT License](https://github.com/rudradave1/vellum-ledger-api/LICENCE.md)
## Local Setup
1. Create a local PostgreSQL database: `createdb vellum_ledger`
2. Set environment variables:
   - `DATABASE_URL`
   - `DATABASE_USER`
   - `DATABASE_PASSWORD`
   - `JWT_SECRET`
   - `OPENROUTER_API_KEY`
3. Run the application: `./gradlew run`
