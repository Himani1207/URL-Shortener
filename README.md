# URL Shortener

Production URL shortener: short links, click analytics, QR codes, Redis-backed
resolution, JWT auth, and a React dashboard.

- **Backend** — Spring Boot 3.5 · Java 21 · PostgreSQL · Redis · Spring Security (JWT)
- **Frontend** — React 18 · Vite · Tailwind CSS
- **Docs** — Swagger UI at `/swagger-ui.html`

---

## Run it

### Docker (everything at once)

```bash
cp .env.example .env
docker compose up --build
```

API on `http://localhost:8080`, with PostgreSQL and Redis alongside. The API waits
for both to report healthy before starting.

### Locally

Backend — needs PostgreSQL and Redis running:

```bash
cd backend && ./mvnw spring-boot:run
```

Frontend:

```bash
npm install --prefix frontend
npm run dev --prefix frontend
```

Dashboard on `http://localhost:5173`. The Vite dev server proxies `/api` to
`:8080`, so CORS does not come into play in development.

---

## Deploy

Backend and database on **Render**, frontend on **Vercel**. Both halves build
independently, so they deploy independently.

The two services end up on different domains, which changes three things that
never matter in development: CORS becomes real, the frontend must be told the
API's address, and short links resolve on the API's domain rather than the UI's.
The steps below exist mostly to settle those.

### 1. Backend + database — Render

Render → **New** → **Blueprint**, pointed at this repository. It reads
[`render.yaml`](render.yaml) and creates the web service and a PostgreSQL
instance, wiring the database credentials automatically.

It will prompt for four values it cannot know. Generate the secret with
`openssl rand -base64 64`; leave the two URLs blank on the first pass, since
neither service exists yet.

| Variable | Value |
|---|---|
| `JWT_SECRET` | a fresh base64 key, at least 256 bits |
| `APP_BASE_URL` | `https://<api>.onrender.com` |
| `FRONTEND_URL` | `https://<app>.vercel.app` |
| `CORS_ALLOWED_ORIGINS` | `https://<app>.vercel.app` |

Hibernate creates the schema on first boot (`JPA_DDL_AUTO=update`), so there is
no migration step for a fresh database.

### 2. Frontend — Vercel

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

### 3. Close the loop

Once both are live, go back to Render and fill in `APP_BASE_URL`,
`FRONTEND_URL` and `CORS_ALLOWED_ORIGINS` with the real domains, then redeploy.
Until this is done the API rejects the browser with `403 Invalid CORS request`,
and password-protected links redirect to `localhost`.

### What the free tier actually costs you

- **The API sleeps after 15 minutes idle.** Waking a JVM takes 40–60 seconds,
  and every short link goes through it — so a link clicked after a quiet spell
  is slow to redirect. Nothing in the code causes this.
- **Free PostgreSQL is deleted after 30 days.**
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
