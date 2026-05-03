# Vellum Ledger API

Sync backend for VellumLedger. Ktor + PostgreSQL + JWT. Handles push/pull transaction sync with timestamp-based conflict resolution.

## Tech Stack
- **Language:** Kotlin
- **Runtime:** JVM 17
- **Framework:** Ktor 2.3.x
- **Database:** PostgreSQL + Exposed ORM
- **Auth:** JWT (jjwt)
- **Deploy target:** Railway

## API Endpoints
- `GET /health` - Health check
- `POST /auth/register` - Register a new user
- `POST /auth/login` - Login and get JWT
- `POST /transactions/push` - Push transactions to server (Auth required)
- `GET /transactions/pull` - Pull transactions from server (Auth required)

## Local Setup
1. Create a local PostgreSQL database: `createdb vellum_ledger`
2. Set environment variables:
   - `DATABASE_URL`
   - `DATABASE_USER`
   - `DATABASE_PASSWORD`
   - `JWT_SECRET`
3. Run the application: `./gradlew run`
