# 🔗 URL Shortener

A full-stack URL Shortener application that allows users to create, manage, and track shortened URLs. The platform provides secure authentication, custom short links, QR code generation, and click analytics through a modern dashboard.

## 🚀 Features

- User Authentication (JWT)
- Create and manage shortened URLs
- Custom short aliases
- QR Code generation for every short link
- Click analytics and visit tracking
- Dashboard with URL statistics
- Responsive and modern UI
- RESTful API architecture

## 🛠️ Tech Stack

### Frontend
- React.js
- Vite
- Tailwind CSS
- Axios
- React Router

### Backend
- Spring Boot
- Java 21
- Spring Security (JWT)
- Spring Data JPA

### Database & Cache
- PostgreSQL
- Redis

## 📂 Project Structure

```
URL-Shortener/
├── backend/      # Spring Boot API
├── frontend/     # React + Vite Application
├── docker-compose.yml
└── README.md
```

## ⚙️ Getting Started

### Clone the repository

```bash
git clone https://github.com/Himani1207/URL-Shortener.git
cd URL-Shortener
```

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## 🌐 API Endpoints

| Method | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | User login |
| GET | `/api/auth/me` | Get current user |
| POST | `/api/urls` | Create short URL |
| GET | `/api/urls` | Get all URLs |
| PUT | `/api/urls/{id}` | Update URL |
| DELETE | `/api/urls/{id}` | Delete URL |
| GET | `/{shortCode}` | Redirect to original URL |
| GET | `/api/urls/{shortCode}/analytics` | URL analytics |
| GET | `/api/urls/{shortCode}/qr` | Generate QR Code |

## 📊 Key Features

<<<<<<< HEAD
- Secure JWT Authentication
- URL shortening with custom aliases
- QR code generation
- Click tracking and analytics
- Dashboard with URL statistics
- Redis caching for improved performance
- Responsive design for desktop and mobile

## 🔮 Future Enhancements

- Password-protected URLs
- URL expiration scheduling
- Custom domains
- User profile management
- Advanced analytics dashboard
- Email notifications

=======
API on **Render**, database on **Neon**, frontend on **Vercel**. All three build
independently.

The database is deliberately not Render's own: its free PostgreSQL is deleted
after 30 days, while Neon's free tier persists.

The pieces end up on different domains, which makes three things matter that
never do in development — CORS becomes real, the frontend has to be told the
API's address, and short links resolve on the API's domain rather than the UI's.
Most of the steps below exist to settle those.

### 1. Database — Neon

Create a project. Neon gives you a libpq connection string, which **cannot be
pasted into a Java setting as-is**:

```
postgresql://USER:PASSWORD@HOST/neondb?sslmode=require&channel_binding=require
```

Split it into three values:

| Variable | Value |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST/neondb?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | the role, e.g. `neondb_owner` |
| `SPRING_DATASOURCE_PASSWORD` | the password |

What changed and why: the `jdbc:` prefix is required or no driver is selected;
credentials move out of the URL so the password stays out of anything that logs
a connection string; and `channel_binding` is dropped, since it is a libpq
option with no JDBC equivalent — the driver ignores it rather than failing, so
leaving it in is quietly misleading. Keep `sslmode=require`.

Use the **`-pooler`** host. It runs PgBouncer in transaction mode, which usually
breaks server-side prepared statements, but Neon negotiates them at protocol
level so Hibernate needs no `prepareThreshold=0`.

### 2. API — Render

Render → **New** → **Blueprint**, pointed at this repository. It reads
[`render.yaml`](render.yaml).

It will prompt for the values it cannot know. Generate the secret with
`openssl rand -base64 64`, and leave the URLs blank on this first pass — neither
service exists yet.

| Variable | Value |
|---|---|
| the three `SPRING_DATASOURCE_*` above | from Neon |
| `JWT_SECRET` | a fresh base64 key, at least 256 bits |
| `APP_BASE_URL` | `https://<api>.onrender.com` |
| `FRONTEND_URL` | `https://<app>.vercel.app` |
| `CORS_ALLOWED_ORIGINS` | `https://<app>.vercel.app` |

Hibernate creates the schema on first boot (`JPA_DDL_AUTO=update`), so a fresh
Neon database needs no migration step.

### 3. Frontend — Vercel

Import the repository and set **Root Directory** to `frontend`. Everything else
comes from [`frontend/vercel.json`](frontend/vercel.json), including the SPA
rewrite that deep links such as `/dashboard/create` need in order to survive a
page refresh.

Add two environment variables, both set to the Render API's origin:

| Variable | Value |
|---|---|
| `VITE_API_BASE_URL` | `https://<api>.onrender.com` |
| `VITE_SHORT_BASE_URL` | `https://<api>.onrender.com` |

These are inlined at **build** time, not read at runtime — changing one needs a
redeploy, not just a restart.

### 4. Close the loop

Once both are live, go back to Render and fill in `APP_BASE_URL`,
`FRONTEND_URL` and `CORS_ALLOWED_ORIGINS` with the real domains, then redeploy.
Until this is done the API rejects the browser with `403 Invalid CORS request`,
and password-protected links redirect to `localhost`.

### What the free tier actually costs you

- **The API sleeps after 15 minutes idle.** Waking a JVM takes 40–60 seconds,
  and every short link goes through it — so a link clicked after a quiet spell
  is slow to redirect. Nothing in the code causes this.
- **Neon sleeps too**, adding roughly 3 seconds on the first query after an idle
  period. It compounds with the above rather than overlapping it.
- **No Redis.** Caching falls back to an in-memory map: correct, but per-instance
  and empty after every restart. Add a Key Value service and set `CACHE_TYPE=redis`,
  `HEALTH_REDIS_ENABLED=true` plus `REDIS_HOST`/`REDIS_PORT` to restore it.

---

## Tests

```bash
cd backend && ./mvnw verify
```

77 unit tests (Surefire) plus 31 integration tests (Failsafe, MockMvc against H2).
`./mvnw test` runs only the unit tests — integration tests are named `*IT` and run
in the `integration-test` phase.

---

## API

| Method | Path | Auth | Purpose |
|---|---|:--:|---|
| `POST` | `/api/auth/register` | — | Create an account, returns a JWT |
| `POST` | `/api/auth/login` | — | Sign in, returns a JWT |
| `GET` | `/api/auth/me` | ✓ | Current user |
| `GET` | `/{shortCode}` | — | **Public redirect** — records the click |
| `POST` | `/api/urls` | ✓ | Create a short link |
| `GET` | `/api/urls` | ✓ | List your links |
| `GET` | `/api/urls/stats` | ✓ | Dashboard totals |
| `PUT` | `/api/urls/{id}` | ✓ | Update a link |
| `PATCH` | `/api/urls/{id}/toggle` | ✓ | Pause / resume |
| `DELETE` | `/api/urls/{id}` | ✓ | Delete a link and its history |
| `GET` | `/api/urls/{shortCode}/qr` | ✓ | QR code as PNG |
| `GET` | `/api/urls/{shortCode}/analytics` | ✓ | Click history |
| `GET` | `/api/urls/{shortCode}/summary` | ✓ | Aggregated analytics |
| `GET` | `/api/urls/{shortCode}` | ✓ | Legacy redirect — prefer `/{shortCode}` |

Every per-link endpoint is scoped to its owner.

---

## Layout

The two halves are independent builds — Maven never invokes npm, and the frontend
talks to the API over HTTP only. Each can be built, tested and deployed on its own.

```
backend/            Spring Boot · Maven · Dockerfile
frontend/           React · Vite · Tailwind
docker-compose.yml  API + PostgreSQL + Redis
```

```
backend/src/main/java/com/example/url_shortner/
  cache/        CachedUrl, CacheNames
  config/       Security, CORS, Cache, OpenAPI, Scheduling
  Controller/   Auth, Url, Redirect, PublicLink
  dto/          request/ · response/
  entity/       User, Url, ClickAnalytics
  exception/    Typed exceptions + GlobalExceptionHandler
  repository/   Spring Data JPA + projections
  scheduler/    UrlExpirationScheduler
  security/     JWT filter, service, entry point, denied handler
  service/      Interfaces + impl/
  util/         UserAgentParser, ClientIpResolver, ShortCodeGenerator, UrlMapper

frontend/src/
  components/   ui/ · layout/ · links/ · analytics/ · create/
  context/      AuthContext, ToastContext
  hooks/        useLinks, useCountUp
  lib/          api, format, qr
  pages/        Landing, Login, Register, ProtectedLink, dashboard/
  routes/       ProtectedRoute, ScrollToTop
```

---

## Notes on a few design choices

**Redis caching lives in its own bean.** Spring's cache annotations are proxy-based:
a call from one method of a class to another in the same class bypasses the proxy
entirely and the annotation silently does nothing. `UrlCacheService` is a separate
collaborator so `@Cacheable` actually takes effect. A `CacheErrorHandler` makes
Redis a soft dependency — an outage degrades latency, not availability.

**Click counts use an atomic `UPDATE`.** `SET click_count = click_count + 1` rather
than read-modify-write, because redirects are exactly the endpoint that receives
concurrent traffic and a read-modify-write silently loses clicks.

**Redirects are served from the root path.** `/{shortCode}` is regex-constrained to
the short-code alphabet so it cannot swallow `/favicon.ico` or `/swagger-ui.html`.
Custom aliases are checked against a reserved-word list for the same reason.

---

## Configuration

Every setting reads from an environment variable with a local-development default,
so nothing needs editing to run from the IDE. `.env.example` lists what Docker
Compose uses; copy it to `.env` to override.

Two worth knowing about:

- `APP_BASE_URL` — the origin that short links and QR codes encode. It defaults to
  `http://localhost:8080`, which is correct locally and wrong anywhere else.
- `JWT_SECRET` — Base64, at least 256 bits. The default is a development
  placeholder. Generate a real one with `openssl rand -base64 64`.

Hosting is not configured — this repo currently targets local development only.
>>>>>>> b70b8f0 (update)
