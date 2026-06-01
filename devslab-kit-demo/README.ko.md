# devslab-kit-demo

> **[devslab-kit](https://github.com/devslab-kr/devslab-kit)**(Spring Boot 4 플랫폼 스타터)를 PostgreSQL + Redis에서 사용하는 최소 앱.

[English](./README.md)

이 앱에는 **플랫폼 코드가 전혀 없습니다.** 스타터를 추가하고 데이터베이스를 가리키기만
하면 인증, RBAC + 그룹 + ABAC, 멀티테넌시, 동적 메뉴, 감사 로깅, 최초 관리자 부트스트랩,
관리자 REST API가 모두 자동 구성으로 제공됩니다.

> **설정 불필요.** 앱 자체 패키지(`kr.devslab.example.*`)에 평범한
> `@SpringBootApplication` 하나면 충분합니다 —
> [`DevslabKitDemoApplication`](src/main/java/kr/devslab/example/devslabkit/DevslabKitDemoApplication.java)
> 참고. `scanBasePackages`·`@EntityScan`·`@EnableJpaRepositories` 모두 불필요:
> 스타터의 자동 구성이 kit의 JPA 엔티티·리포지토리와 관리자 REST API를 직접
> 등록하며, 소비자가 스캔 범위를 넓히는 게 아니라 스타터가 알아서 넓힙니다.

## 요구 사항

- Java 21+
- Docker (Compose / Testcontainers로 PostgreSQL + Redis 기동)

## 이 데모만 받기

각 데모는 독립 Gradle 프로젝트라, `devslab-examples` 저장소 전체를 clone하지 않고
이 폴더만 받을 수 있습니다.

**git 사용 (sparse checkout):**

```bash
git clone --filter=blob:none --sparse https://github.com/devslab-kr/devslab-examples.git
cd devslab-examples
git sparse-checkout set devslab-kit-demo
cd devslab-kit-demo
```

**git 없이 (폴더만):**

```bash
curl -sL https://github.com/devslab-kr/devslab-examples/archive/refs/heads/main.tar.gz \
  | tar -xz --strip-components=2 devslab-examples-main/devslab-kit-demo
cd devslab-kit-demo
```

## 실행

```bash
./gradlew bootRun
```

`spring-boot-docker-compose`가 `compose.yaml`(Postgres + Redis)을 자동 기동하고, kit이
Flyway 마이그레이션을 실행하며, 최초 관리자 부트스트랩이 `default` 테넌트에 `admin`/`admin`
사용자를 시드합니다.

로그인해서 JWT 받기:

```bash
curl -s localhost:8080/admin/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"tenantId":"default","loginId":"admin","password":"admin"}'
```

[관리자 콘솔](https://github.com/devslab-kr/devslab-kit-admin-ui)을
`http://localhost:8080`에 연결하면 같은 API 위에서 UI를 쓸 수 있습니다.

## 테스트 실행 (Docker만 필요)

```bash
./gradlew test
```

Testcontainers가 일회용 Postgres + Redis를 띄우고, 테스트가 전체 컨텍스트를 부팅해
플랫폼 빈이 배선됐는지 확인합니다.

## 이 데모가 보여주는 것

- **스타터만 추가하면 됩니다.** `build.gradle.kts`는
  `kr.devslab:devslab-kit-spring-boot-starter`와 플랫폼이 사용하는 Spring 스타터(web,
  security, JPA, Flyway, data-redis)를 선언합니다 — kit은 런타임 선택을 강요하지 않습니다.
- **코드가 아니라 설정.** 모든 것은 `application.yaml`의 `devslab.kit.*`로 제어합니다 —
  테넌트 모드/리졸버, JWT 시크릿, 캐시 백엔드, 부트스트랩 관리자.
  [설정 레퍼런스](https://devslab-kit.devslab.kr/ko/reference/configuration/) 참고.
- **property 한 줄로 분산 캐시.** `devslab.kit.cache.type: redis`로 두면 사용자별 메뉴
  트리(그리고 직접 추가한 `@Cacheable`)가 Redis에 JSON으로 캐시됩니다 — `Serializable`도,
  직렬화기 설정도 필요 없습니다. `in-memory`로 바꾸면 Redis를 뺄 수 있습니다.
- **최초 관리자 부트스트랩.** 빈 데이터베이스가 영구 백도어 없이 첫 부팅에 사용 가능한
  관리자 로그인에 도달합니다.

## 문서

전체 문서: **[devslab-kit.devslab.kr](https://devslab-kit.devslab.kr)**.
