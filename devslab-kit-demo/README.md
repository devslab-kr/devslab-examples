# devslab-kit-demo

> A minimal app that consumes **[devslab-kit](https://github.com/devslab-kr/devslab-kit)** — the Spring Boot 4 platform starter — on PostgreSQL + Redis.

[한국어](./README.ko.md)

The app has **no platform code of its own**. Adding the starter and pointing it at
a database gives you authentication, RBAC + groups + ABAC, multi-tenancy, dynamic
menus, audit logging, a first-admin bootstrap, and an admin REST API — all from
auto-configuration.

> **No wiring required.** A plain `@SpringBootApplication` in the app's own package
> (`kr.devslab.example.*`) is all it takes — see
> [`DevslabKitDemoApplication`](src/main/java/kr/devslab/example/devslabkit/DevslabKitDemoApplication.java).
> No `scanBasePackages`, no `@EntityScan`, no `@EnableJpaRepositories`: the starter's
> auto-configuration registers the kit's JPA entities, repositories, and the admin
> REST API itself, broadening scanning rather than making you widen it.

## Prerequisites

- Java 21+
- Docker (for PostgreSQL + Redis via Compose / Testcontainers)

## Get just this demo

Each demo is a standalone Gradle project, so you can grab this one folder without
cloning the whole `devslab-examples` repo.

**With git (sparse checkout):**

```bash
git clone --filter=blob:none --sparse https://github.com/devslab-kr/devslab-examples.git
cd devslab-examples
git sparse-checkout set devslab-kit-demo
cd devslab-kit-demo
```

**Without git (folder only):**

```bash
curl -sL https://github.com/devslab-kr/devslab-examples/archive/refs/heads/main.tar.gz \
  | tar -xz --strip-components=2 devslab-examples-main/devslab-kit-demo
cd devslab-kit-demo
```

## Run

```bash
./gradlew bootRun
```

`spring-boot-docker-compose` auto-starts `compose.yaml` (Postgres + Redis), the kit
runs its Flyway migrations, and the first-admin bootstrap seeds an `admin`/`admin`
user in the `default` tenant.

Log in to get a JWT:

```bash
curl -s localhost:8080/admin/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"tenantId":"default","loginId":"admin","password":"admin"}'
```

Point the [admin console](https://github.com/devslab-kr/devslab-kit-admin-ui) at
`http://localhost:8080` for a UI over the same API.

## Run the tests (Docker only)

```bash
./gradlew test
```

Testcontainers starts throwaway Postgres + Redis; the test boots the full context
and asserts the platform beans are wired.

## What this demo shows

- **Just add the starter.** `build.gradle.kts` declares
  `kr.devslab:devslab-kit-spring-boot-starter` plus the Spring starters the
  platform uses (web, security, JPA, Flyway, data-redis) — the kit stays
  unopinionated about your runtime.
- **Config, not code.** Everything is driven from `application.yaml` under
  `devslab.kit.*` — tenant mode/resolver, the JWT secret, the cache backend, and
  the bootstrap admin. See the [configuration reference](https://devslab-kit.devslab.kr/reference/configuration/).
- **Distributed cache via one property.** `devslab.kit.cache.type: redis` makes the
  per-user menu tree (and any `@Cacheable` you add) cache as JSON in Redis — no
  `Serializable`, no serializer wiring. Flip to `in-memory` to drop Redis.
- **First-admin bootstrap.** A fresh database reaches a usable admin login on first
  boot, without a permanent backdoor.

## Docs

Full documentation: **[devslab-kit.devslab.kr](https://devslab-kit.devslab.kr)**.
