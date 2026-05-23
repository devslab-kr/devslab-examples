# easy-paging-sb4-demo

[English](README.md) · **한국어**

> ✨ **Spring Boot 4 라인.** 이 데모는 `easy-paging-spring-boot-starter`의 활성 `0.5.x` 라인(Spring Boot 4 / Spring Framework 7 / Jackson 3)을 사용. Spring Boot 3.3–3.5 maintenance 버전은 [`easy-paging-demo`](../easy-paging-demo/) 참조.

[`easy-paging-spring-boot-starter`](https://github.com/devslab-kr/easy-paging-spring-boot-starter) — Spring Boot + MyBatis용 어노테이션 기반 페이지네이션의 실행 가능한 예제.

이 데모는 스타터를 최소 Spring Boot 앱에 연결한 형태로, 인메모리 H2 데이터베이스, 137개의 시드 report 행, 그리고 단 하나의 `@AutoPaginate` 어노테이션이 붙은 컨트롤러로 구성. 외부 서비스도, DB 설정도 없음 — clone, run, curl 끝.

## 전제조건

- JDK 21+
- 그 외 없음. H2는 인메모리, 의존성은 최초 빌드 시 다운로드됨.

## 실행

```bash
cd easy-paging-demo
./gradlew bootRun
```

앱은 `http://localhost:8080`에 뜸. 시작할 때마다 H2 DB가 인메모리로 생성되고 `reports` 테이블에 137행이 시드됨.

## 시험해보기

### 첫 페이지 (Spring Data 기본 0-based)
```bash
curl 'http://localhost:8080/reports?page=0&size=5'
```
```json
{
  "content": [ { "id": 1, "title": "Report #1", "createdAt": "..." }, ... ],
  "page": 0,
  "size": 5,
  "totalElements": 137,
  "totalPages": 28,
  "first": true,
  "last": false,
  "empty": false
}
```

### 정렬 (SQL 인젝션 방어 검증됨)
```bash
# 다중 컬럼 정렬, createdAt 내림차순 후 id 오름차순
curl 'http://localhost:8080/reports?page=0&size=5&sort=createdAt,desc&sort=id,asc'

# 인젝션 시도는 HTTP 400으로 거절됨
curl -i 'http://localhost:8080/reports?sort=id;DROP%20TABLE%20reports'
```

### 페이지 크기 clamping
```bash
# 컨트롤러가 @AutoPaginate(maxSize = 50)을 선언
# 9999 요청 → 조용히 50으로 clamping
curl 'http://localhost:8080/reports?page=0&size=9999' | jq '.size, .content | length'
# → 50
# → 50
```

### 범위 초과 페이지
```bash
# Page 999는 존재 안 함. reasonable=true (기본값)에서 스타터가 마지막 페이지로 clamping
curl 'http://localhost:8080/reports?page=999&size=20' | jq '.page, .empty'
# → 6  (137/20의 마지막 페이지 인덱스)
# → false
```

## 읽을 만한 파일

흥미로운 부분, 순서대로:

| 파일 | 왜 |
| --- | --- |
| `build.gradle.kts` | `spring-boot-starter-web` 외 추가하는 의존성은 `kr.devslab:easy-paging-spring-boot-starter:0.4.0` 하나뿐 |
| `report/ReportController.java` | 전체 페이지네이션 계약은 `@AutoPaginate(maxSize = 50)` 어노테이션 + `PageResponse<Report>` 반환 타입 |
| `report/ReportMapper.java` + `resources/mapper/ReportMapper.xml` | 평범한 `SELECT` — `LIMIT`, `OFFSET`, `COUNT` 모두 없음. 런타임에 aspect가 주입 |
| `resources/application.yml` | `easy-paging` 전역 cap + 기본값 |

## 고급: 응답 봉투 교체

많은 팀이 전사 표준 봉투 형식 — `{ ok, data, meta: { page, size, total, pages } }` 같은 — 을 가지고 모든 paginated 엔드포인트가 그 형식을 반환하도록 합니다. 스타터는 기본 `PageResponse`를 자기 형식으로 교체할 2가지 방법을 지원. 이 데모는 둘 다 나란히 포함하므로 wire JSON을 비교해서 자기 코드베이스에 맞는 패턴을 고를 수 있어요.

여기서 사용하는 커스텀 봉투는 [`CompanyPage`](src/main/java/kr/devslab/examples/easypaging/envelope/CompanyPage.java) record:

```json
{
  "ok": true,
  "data": [ { "id": 1, "title": "Report #1", "createdAt": "..." }, ... ],
  "meta": { "page": 0, "size": 5, "total": 137, "pages": 28 }
}
```

### 패턴 1 — 커스텀 반환 타입 + 정적 팩토리 (권장)

컨트롤러가 반환 타입을 `CompanyPage<Report>`로 선언하고 `CompanyPage.from(...)`을 명시적으로 호출:

```bash
curl 'http://localhost:8080/reports/company?page=0&size=5'
```

[`CompanyPageReportController`](src/main/java/kr/devslab/examples/easypaging/report/CompanyPageReportController.java) 참고 — 메서드 본문은 `CompanyPage.from(reports.findAll(), pageable)` 한 줄. `@AutoPaginate` aspect는 여전히 PageHelper 셋업, 정렬 검증, 크기 clamping 담당; 봉투 생성만 안 함.

### 패턴 2 — `Object` 반환 + `PageResponseFactory` 빈

컨트롤러가 반환 타입을 `Object`로 선언하고 raw `List<Report>`를 돌려줌. [`PageResponseFactory`](src/main/java/kr/devslab/examples/easypaging/envelope/CompanyEnvelopeConfig.java) 빈이 aspect에게 그 리스트를 회사 봉투로 wrap하는 방법을 알려줌:

```bash
curl 'http://localhost:8080/reports/auto-envelope?page=0&size=5'
```

`/reports/company`와 동일한 JSON 출력 — wire 형식은 같고 코드 경로만 다름. [`AutoEnvelopeReportController`](src/main/java/kr/devslab/examples/easypaging/report/AutoEnvelopeReportController.java) 참고.

### 하나 고르기 — 트레이드오프

| | 패턴 1 (커스텀 타입 + `.from()`) | 패턴 2 (`Object` + 팩토리 빈) |
| --- | --- | --- |
| 타입 안전성 | 완전함 — 반환 타입이 `CompanyPage<Report>` | 없음 — 반환 타입이 `Object` |
| 엔드포인트별 wiring | 컨트롤러마다 `CompanyPage.from(...)` 호출 | 없음 — 팩토리는 한 번만 정의 |
| 같은 앱에서 봉투 섞기 | 쉬움 — 엔드포인트별 opt-in | 어려움 — 팩토리가 모든 `Object`/`List` 반환에 영향 |
| 테스트 | 정적 메서드, Spring 불필요 | `@SpringBootTest` 필요 (빈이 컨텍스트에 있어야) |
| 적합한 경우 | 한두 개 엔드포인트만 커스텀 형식 | 모든 paginated 엔드포인트가 같은 형식 사용 |

기본 `/reports` 엔드포인트는 여전히 스타터의 `PageResponse<Report>`를 반환 — 명시적 `PageResponse<T>` (그리고 `CompanyPage<T>`) 반환 타입은 aspect를 통과하지 않으므로, 팩토리 빈 등록이 영향 미치지 않음. [`CustomEnvelopeTest`](src/test/java/kr/devslab/examples/easypaging/CustomEnvelopeTest.java)가 이 속성을 end-to-end로 검증.

스타터의 전체 기능 (keyset 페이지네이션, WebFlux/R2DBC 지원)은 이 repo의 자매 데모들이 다룸 — 인덱스는 [최상위 README](../README.md) 참고.

## 빌드 검증

```bash
./gradlew build
```

두 테스트 클래스 실행:
- `ReportControllerTest` — 앱 부팅 후 `/reports` 호출, 기본 페이지네이션 봉투 형식 + `maxSize` clamping 검증
- `CustomEnvelopeTest` — `/reports/company`와 `/reports/auto-envelope` 호출해서 둘 다 같은 `CompanyPage` JSON 형식을 생성하는지, `PageResponseFactory` 빈이 `/reports` (여전히 기본 `PageResponse` 반환)에 새지 않는지 검증
