# StartupHub — Spring Boot REST + WebSocket + Vanilla JS

Startup Incubator & Growth Simulation Platform.

- **Backend**: Java Spring Boot — REST API (`/api/**`) + STOMP-over-WebSocket (`/ws`) for live notifications
- **Frontend**: plain HTML/CSS/JavaScript in `src/main/resources/static` (no framework, no build step)
- **Database**: MySQL via XAMPP

## Run it

1. Start **MySQL** in the XAMPP control panel. (Apache is not needed — Spring Boot serves the frontend itself on port 8080.)
2. `application.properties` auto-creates `startuphub_db`, so there is no SQL to run by hand (defaults: user `root`, no password, port 3306).
3. From the project folder:
   ```bash
   mvn clean compile
   mvn spring-boot:run
   ```
4. Open `http://localhost:8080`. `index.html` checks your session and sends you to the login page or the dashboard.
5. Register, log in, create a startup, and work through the tabs.

No new Maven dependency was added for the redesign. Chart.js, SockJS and STOMP are checked
into `static/js/vendor/`, so charts and live notifications work with no internet connection —
which matters when you are demonstrating on university wifi.

No Lombok: every entity has hand-written getters and setters, so there is nothing for an
IDE/annotation-processor mismatch to break.

## Frontend structure

```
static/
  index.html        session check, then redirect
  login.html        split-screen login, password reveal, inline validation
  register.html     role picker (Founder / Investor / Mentor / Admin)
  dashboard.html    one page, several hash-routed views:
                      #overview  KPI cards, trend chart, activity feed
                      #startups  your startups as cards
                      #discover  every startup, with search / stage filter / sort
                      #portfolio investor portfolio (investor role)
                      #milestones open milestones (mentor role)
                      #notifications
                      #admin     platform stats and users (admin role)
  startup.html      one startup across 8 tabs:
                      Overview (KPIs, stage timeline, health score, trend chart,
                                MVP progress, revenue projection), Team, Finance
                                (incl. automatic revenue calculator), Investments,
                                Growth, MVP, Roadmap, Competitors
  profile.html      profile, appearance and password
  css/app.css       design tokens + components; [data-theme="dark"] re-points the
                    same custom properties, so dark mode adds no duplicate rules
  js/api.js         fetch wrapper; throws ApiError carrying status + field errors
  js/ui.js          formatting, toasts, modals, confirm dialogs, validation,
                    skeletons, empty states, theme switch
  js/nav.js         application shell: role-aware nav, bell, profile menu
  js/charts.js      Chart.js helpers that read colours from CSS variables and
                    rebuild every chart when the theme changes
  js/ws.js          STOMP/SockJS client
  js/revenue-calculator.js   Finance tab: automatic revenue calculator UI
  js/mvp.js         MVP tab: MVP details, feature list, progress
  js/auth.js  js/dashboard.js  js/startup.js  js/profile.js   page logic
  js/vendor/        chart.umd.js, sockjs.min.js, stomp.umd.min.js
```

## How auth works (session-based, not JWT)

- `POST /api/auth/register` — plain JSON body.
- `POST /api/auth/login` — handled by Spring Security's form-login filter, so the frontend sends
  it as `x-www-form-urlencoded` (see `loginRequest()` in `api.js`). The success and failure
  handlers are customised to return JSON instead of an HTTP redirect.
- A `JSESSIONID` cookie is set on login and every later `fetch()` sends it via
  `credentials: 'include'`. Same-origin, so no CORS configuration is needed.
- `GET /api/auth/me` returns the signed-in user or `401`; every page checks it on load.
- CSRF is disabled — a common trade-off for a same-origin JSON API in a coursework project,
  and worth one line in your report if you are asked about it.

## Authorization

`security/AccessGuard.java` is the real permission check. Controllers call it before any
state-changing operation:

| Action | Who |
|---|---|
| Advance stage, **edit/delete startup**, hire/promote/**edit**/remove staff, add/**edit/delete** revenue and expenses, use the revenue calculator, launch/**edit/delete** campaigns, record campaign results, add/**edit/delete** competitors, create/**edit/delete** roadmaps and milestones, create/edit/delete the MVP and its features | Founder of that startup, or an admin |
| Edit or delete an investment | Founder of that startup, admin, **or the investor who made it** |
| Complete or update a milestone | Founder, admin, **or any mentor** |
| Invest in a startup | Any signed-in user — that is the point of the platform |
| `/api/admin/**` | `ROLE_ADMIN`, enforced in `SecurityConfig` |

The frontend hides buttons a role cannot use, but that is convenience only. Log in as a second
user and call a guarded endpoint directly and you get a `403` with a JSON message.

## WebSocket / live notifications

- `WebSocketConfig` registers a STOMP-over-SockJS endpoint at `/ws` with a simple broker on `/topic`.
- When an investment lands, a milestone is completed, an MVP feature is completed or the MVP is launched, `StartupEventListener` (the Observer
  implementation) saves a `Notification` **and** pushes it live via `SimpMessagingTemplate` to
  `/topic/notifications-{userId}`.
- Every page subscribes to its own channel after login (`js/ws.js`), shows a toast and updates
  the unread count on the navbar bell. Open two browsers — founder in one, investor in the other —
  and invest; the toast appears without a refresh.

## Design patterns

| Pattern | Where |
|---|---|
| Singleton | Spring's singleton-scoped beans (`@Service`/`@Repository`), plus the classic hand-written `pattern/singleton/AppConfigManager.java` |
| Factory | `pattern/factory/NotificationFactory.java` — one place that knows how to phrase each kind of notification |
| Observer | `pattern/observer/*Event.java` + `StartupEventListener.java`; publishers never know notifications or WebSockets exist |
| Strategy | `pattern/strategy/RevenueStrategy.java` + `Subscription/Transaction/AdRevenueStrategy`. `RevenueStrategyResolver` picks the strategy for the chosen `RevenueModel`; `calculate()` powers the **Automatic revenue calculator** (Finance tab), while the original `projectMonthlyRevenue()` still drives the what-if form on the Overview tab |
| Command | `pattern/command/Command.java` with `HireEmployeeCommand`, `UpdateEmployeeCommand`, `LaunchCampaignCommand`, `UpdateCampaignCommand` and `UpdateCampaignMetricsCommand`, all run through `CommandInvoker`, which keeps an audit history |

## Where the dashboard numbers come from

Nothing on screen is hard-coded or random.

- `DashboardServiceImpl` scopes totals by role — founders see their own startups, investors see
  the ones they actually funded, mentors and admins see the platform.
- Month-over-month deltas return `null` when there is no previous month to compare with, and the
  frontend then shows no arrow rather than a made-up percentage.
- `AnalyticsServiceImpl.health()` scores a startup out of 100 from six weighted factors —
  Finance 25, Team 15, Roadmap 15, Investment 15, Growth 15, MVP 15 — each computed from real rows. A
  section with no data scores low and says so.
- Monthly revenue/expense series are grouped from the `recordedOn` date on each entry.

## Notes and trade-offs

- `spring.jpa.hibernate.ddl-auto=update` creates and updates the tables, including the columns
  added to `milestones` (`description`, `priority`, `progress`). No migration to run.
- DTO records are used everywhere instead of serialising entities, which avoids infinite
  recursion and most lazy-loading problems. `open-in-view` is left at its default.
- Passwords are BCrypt-hashed by Spring Security.
- `StartupServiceImpl.delete()` removes child rows inside a transaction before the startup —
  deleting the startup row alone fails on a foreign key constraint in MySQL.
- There is no pagination anywhere, and the mentor milestone view makes one request per startup
  (capped at 8). Both are fine for a course demo, not for a real dataset.

## Automatic revenue calculator

Finance tab -> *Automatic revenue calculator*. Only the inputs of the selected model are shown.

| Model | Inputs | Formula |
|---|---|---|
| Subscription | paying customers, monthly price | customers x price |
| Transaction / commission | transactions, average value, commission % | transactions x average x commission / 100 |
| Advertisement | impressions, CPM | impressions / 1000 x CPM |

`POST /api/startups/{id}/finance/revenue/calculate` returns the server-calculated amount and the
formula text (nothing stored). `POST .../finance/revenue/calculated` recalculates from the same
inputs and stores **one** normal `Revenue` row (with `revenueModel` + `calculationDetails`), so every
existing total, chart, dashboard figure and the health score include it with no extra wiring.
All maths uses `BigDecimal` (rounded HALF_UP to cents); invalid input returns HTTP 400 with a message.

## MVP module

A startup has one `Mvp` (name, description, version, `MvpStatus`, target users, launch date) that owns
many `MvpFeature`s (title, description, completed, priority, timestamps). This is separate from the
MVP *stage* of the IDEA -> MVP -> GROWTH -> SCALE lifecycle. Progress is derived, never typed:
`completed features / total features x 100` (0 when there are no features).

| Method | Path |
|---|---|
| GET | `/api/startups/{sid}/mvp` (200, or 204 when none exists) |
| POST / PUT / DELETE | `/api/startups/{sid}/mvp` , `/api/startups/{sid}/mvp/{mvpId}` |
| POST | `/api/startups/{sid}/mvp/{mvpId}/features` |
| PUT / DELETE | `/api/startups/{sid}/mvp/{mvpId}/features/{featureId}` |
| PATCH | `.../features/{featureId}/complete` and `.../reopen` |

## Edit / delete endpoints (added)

`PUT /api/startups/{id}` - `PUT /api/employees/{id}` - `PUT|DELETE /api/startups/{sid}/finance/revenue/{id}` -
`PUT|DELETE /api/startups/{sid}/finance/expense/{id}` - `PUT|DELETE /api/investments/{id}` -
`PUT|DELETE /api/campaigns/{id}` - `PUT|DELETE /api/roadmaps/{id}` - `PUT|DELETE /api/milestones/{id}` -
`PUT|DELETE /api/startups/{sid}/competitors/{id}`

## Tests

`mvn test` runs plain unit tests for the three revenue strategies (including the 100 x $20 = $2,000
example) and for MVP progress.
