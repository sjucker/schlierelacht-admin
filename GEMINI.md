# Schlierelacht Admin - Project Overview & Guidelines

This document provides architectural context, coding standards, and development workflows for the Schlierelacht Admin project.

## 🏗️ Architecture & Technology Stack

- **Backend:** Spring Boot 4.0.3 (Java 21)
- **Frontend:** Vaadin 25 (Server-side UI)
- **Database:** PostgreSQL (managed with Flyway migrations)
- **Persistence:** jOOQ for type-safe SQL queries
- **Security:** Spring Security (Vaadin-integrated for UI, stateless for `/api/**`)
- **Integrations:** Cloudflare (Image hosting/delivery)
- **Mapping:** MapStruct for DTO/Entity conversions
- **Testing:** JUnit 5, ArchUnit (architectural consistency)

## 📂 Project Structure

- `src/main/java/ch/schlierelacht/admin/`
    - `jooq/`: Custom jOOQ extensions and generated code.
    - `mapper/`: MapStruct mappers.
    - `rest/`: Public/Stateless REST endpoints.
    - `security/`: Authentication and authorization logic.
    - `service/`: Business logic and external service integrations (e.g., Cloudflare).
    - `views/`: Vaadin views organized by feature.
- `src/main/resources/db/migration/`: Flyway SQL migration files.
- `src/main/docker/`: Docker Compose for local development (PostgreSQL).

## 🛠️ Development Workflow

### Local Development
1.  **Database:** Start the development database using Docker:
    ```bash
    docker-compose -f src/main/docker/postgres.yml up -d
    ```
2.  **Code Generation:** jOOQ code is generated automatically during the Maven `generate-sources` phase using Testcontainers.
3.  **Run Application:** Use the Maven Wrapper:
    ```bash
    ./mvnw spring-boot:run
    ```

### Build & Deploy
- **Build Package:**
    ```bash
    ./mvnw clean package -Pproduction
    ```
- **Docker Build:**
    ```bash
    docker build -t schlierelacht-admin .
    ```

## 📜 Coding Conventions

### Backend
- **jOOQ DAOs:** Always extend `AbstractSpringDAOImpl` for custom DAOs to ensure proper Spring transaction integration.
- **Mapping:** Use MapStruct for all DTO/Entity transformations.
- **Service Layer:** Keep business logic in `@Service` classes; avoid logic in Vaadin views.
- **Enums:** Use PostgreSQL custom enums (e.g., `location_type`) and map them consistently using `EnumMapper`.

### Frontend (Vaadin)
- **View Organization:** Follow the feature-based structure in `ch.schlierelacht.admin.views`.
- **Styling:** Use Vaadin Lumo theme tokens and LineAwesome icons. Custom CSS should be added to `META-INF/resources/styles.css`.
- **Layouts:** Use `MainLayout` as the parent for all authenticated views.

### Database & Migrations
- **Flyway:** Never modify existing migration files. Create new ones using the `VXXX__description.sql` naming convention.
- **Schema:** Use descriptive names for tables and columns; include audit fields where necessary.

## ✅ Quality Standards
- **ArchUnit:** All code must pass the architectural checks defined in `ArchUnitTest.java`.
- **Formatting:** Adhere to standard Java/Spring coding styles.
- **Naming:** Follow `PascalCase` for classes, `camelCase` for variables/methods, and `snake_case` for database entities.
