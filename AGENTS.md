# AI Agent Guidance — Schlierelacht Admin

This file provides architectural context, coding standards, and development workflows for any AI coding agent working in
this repository. It is intentionally tool-agnostic.

## 🏗️ Architecture & Technology Stack

- **Backend:** Spring Boot 4.1.0 (Java 25)
- **Frontend:** Vaadin 25 (server-side UI)
- **Database:** PostgreSQL (schema managed with Flyway migrations)
- **Persistence:** jOOQ for type-safe SQL (no JPA)
- **Mapping:** MapStruct (+ Lombok) for DTO conversions
- **Security:** Spring Security (Vaadin-integrated for the admin UI, stateless for `/api/**`)
- **Integrations:** Cloudflare (image hosting/delivery)
- **Build:** Maven (use the wrapper `./mvnw`)
- **Testing:** JUnit 5, ArchUnit (architectural consistency)

## 📂 Project Structure

Root package: `ch.schlierelacht.admin`

- `src/main/java/ch/schlierelacht/admin/`
    - `dto/`: Data transfer objects (classes ending in `DTO`). The website's TypeScript types are generated from these —
      see "Public API coupling" below.
    - `jooq/`: jOOQ-generated code (tables, DAOs, records) — **never hand-edit**. Contains the custom
      `AbstractSpringDAOImpl` base that generated DAOs extend.
    - `mapper/`: MapStruct mappers (e.g. `EnumMapper`).
    - `rest/`: Public, stateless REST endpoints (`*Endpoint.java`, mapped under `/api/v<n>/**` — see "Public API
      versioning" below).
    - `security/`: Authentication and authorization logic.
    - `service/`: Business logic and external service integrations (e.g. Cloudflare).
    - `util/`: Shared helpers.
    - `views/`: Vaadin views, organized by feature. `MainLayout` is the parent layout for authenticated views.
    - `Application.java`: Entry point (`@SpringBootApplication`, `@EnableAsync`, `@EnableScheduling`), registers Lumo +
      `styles.css` stylesheets and `app.*` configuration properties.
- `src/main/resources/db/migration/`: Flyway SQL migrations.
- `src/main/resources/META-INF/resources/styles.css`: Custom CSS.
- `src/main/docker/`: Docker Compose for local development (PostgreSQL).

## 🛠️ Development Workflow

### Run the application

```bash
./mvnw                           # Dev mode (default goal: spring-boot:run) → http://localhost:8080
./mvnw spring-boot:run           # Explicit dev mode
```

### Database (Docker must be running)

```bash
docker compose -p schlierelacht -f src/main/docker/postgres.yml up --build
```

### jOOQ code generation

jOOQ codegen is **skipped by default** (`jooq-codegen-skip=true`). It starts a Postgres testcontainer, applies the
Flyway migrations, and regenerates classes into `ch.schlierelacht.admin.jooq`. Re-run it after adding a Flyway migration
(Docker must be running):

```bash
mvn clean test-compile -Djooq-codegen-skip=false
```

### Testing

```bash
./mvnw test                                  # All tests
./mvnw test -Dtest=SomeServiceTest           # Single test class
./mvnw test -Dtest=SomeServiceTest#method    # Single test method
```

### Build & deploy

Since Vaadin 25 the Vaadin frontend is built automatically as part of the `package` phase, so **no
`-Pproduction` profile is required**:

```bash
./mvnw clean package                         # Production-ready JAR
docker build -t schlierelacht-admin .        # Docker image (builds & runs on JDK/JRE 25)
```

## 🔌 Public API coupling

The public website depends on TypeScript types generated from the Java DTOs. The
`typescript-generator-maven-plugin` scans `ch.schlierelacht.admin.**DTO` and writes directly into the sibling website
repo. It is **not** bound to the build lifecycle — run it explicitly after changing a DTO:

```bash
./mvnw process-classes               # compile the changed DTOs
./mvnw typescript-generator:generate # rewrite the website's shared/types/rest.ts
```

Field optionality follows nullability: a DTO field becomes optional in TypeScript only when it lacks
`@NotNull` (primitives and `@NotNull` fields become required).

## 🔢 Public API versioning

This repository has a single `main` branch, while the website runs `main` (PROD) and `develop`
(STAGING), and a released mobile app build keeps calling the API for as long as it stays installed. A response shape
therefore can never change in place. Endpoints are versioned **per resource**:

```java
@RequestMapping(value = "/api/v1/news")          // each resource carries its own version, so
@RequestMapping(value = "/api/v2/attraction")    // bumping one leaves the others untouched
```

`ArchUnitTest#rest_endpoints_should_be_versioned` fails the build if a `@RestController` is mapped without a version.
The full policy is documented in `rest/package-info.java`.

**Bump only on a breaking change**: removing or renaming a field, changing a field's type, making an optional field
required, changing a path or status code, or changing the meaning of an existing value. Additive changes — a new
optional field, a new endpoint, a new enum constant clients handle leniently — stay on the current version.

**To bump**: add a `/api/v<n+1>/<resource>` mapping with the new shape and leave the old mapping and its DTO untouched,
so existing clients keep their responses. Point the consumers at it one at a time (website `develop`, then `main`, then
the app's next release), and delete the old version once nothing calls it.

There are no unversioned `/api/<resource>` paths any more: the transitional forwarding filter was removed once every
consumer called a versioned path, so an unversioned call is now simply not routed.

## 📜 Coding Conventions

### Backend

- **Feature-based packages**, not layered: each feature has its view (s) under `views/<feature>/`, a
  `service/<Feature>Service`, and a `rest/<Feature>Endpoint` where applicable.
- **Data access:** jOOQ only. Generated DAOs extend the custom `AbstractSpringDAOImpl` for proper Spring transaction
  integration. Never hand-edit the `jooq/` package.
- **Mapping:** Use MapStruct for all DTO transformations; map PostgreSQL custom enums consistently via `EnumMapper`.
- **Service layer:** Keep business logic in `@Service` classes; use `@Transactional` for writes and
  `@Transactional(readOnly = true)` for reads. Avoid logic in Vaadin views.
- **Dependency injection:** Constructor injection throughout (no field `@Autowired`).

### Frontend (Vaadin)

- **Server-side rendering:** UI components are Java classes extending Vaadin components.
- **View organization:** Follow the feature-based structure under `ch.schlierelacht.admin.views`; use
  `MainLayout` as the parent for authenticated views.
- **Navigation:** `@Route` for paths and `@Menu` for navigation entries (order, icon, title);
  `MainLayout` builds the drawer navigation from `@Menu` annotations.
- **Styling:** Lumo theme + LineAwesome icons; add custom CSS to
  `src/main/resources/META-INF/resources/styles.css`.
- **Grid lazy loading:** Use `VaadinSpringDataHelpers.toSpringPageRequest(query)` for pagination.

### Database & Migrations

- **Flyway:** Never modify existing migration files. Create new ones using the
  `VXXX__description.sql` naming convention.
- **Schema:** Use descriptive table/column names; include audit fields where appropriate.

## ✅ Quality Standards

- **ArchUnit:** All code must pass the architectural checks (`ArchUnitTest`).
- **Naming:** `PascalCase` for classes, `camelCase` for variables/methods, `snake_case` for database identifiers.
- **Formatting:** Adhere to standard Java/Spring conventions.
