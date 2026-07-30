# Contributing Guide

스파르타 물류 MSA 프로젝트에 기여해 주셔서 감사합니다.
이 문서는 팀원들이 일관된 방식으로 협업할 수 있도록 코드 컨벤션, 브랜치 전략, PR 정책 등을 정의합니다.

---

## 개발 환경

| 항목 | 버전/도구 |
|------|-----------|
| Language | Java 17 (LTS) |
| Framework | Spring Boot 3.5.16 |
| Spring Cloud | 2025.0.3 (Northfields) |
| Build | Gradle 8.14.3 (Wrapper 포함) |
| Database | PostgreSQL |
| Cache | Redis |
| Container | Docker & Docker Compose |
| API 문서 | Swagger (springdoc-openapi 2.8.9) |
| 분산 추적 | Zipkin (Micrometer Tracing) |
| AI | Spring AI 1.1.6 (Google Gemini) |

> 버전은 루트 `build.gradle`의 `ext` 블록에서 중앙 관리합니다. 개별 서비스에서 버전을 직접 명시하지 마세요.

---

## 시작하기 (로컬 실행)

1. **환경변수 준비** — `.env.example`을 복사해 `.env`를 만들고 값을 채웁니다.
   ```bash
   cp .env.example .env
   ```
   `.env`는 git에 커밋하지 않습니다. (DB 비밀번호, JWT 시크릿, Gemini API 키, Slack 토큰 등)

2. **인프라 실행** — PostgreSQL, Redis, Zipkin이 필요합니다. (추후 `docker-compose.yml` 제공 예정)

3. **빌드** — Gradle 설치 없이 Wrapper로 빌드합니다.
   ```bash
   ./gradlew build            # 전체 빌드
   ./gradlew :hub-service:build   # 특정 서비스만
   ```

4. **실행** — Eureka → Gateway → 나머지 서비스 순으로 기동합니다.
   ```bash
   ./gradlew :eureka-service:bootRun
   ./gradlew :gateway-service:bootRun
   ./gradlew :user-service:bootRun
   ```

> yml은 로컬 기본값이 들어 있어 환경변수 없이도 대부분 동작하며, 시크릿 값만 `.env`로 주입하면 됩니다. 도커 환경에서는 컨테이너명(host)을 환경변수로 오버라이드합니다.

---

## 브랜치 전략

```
main
 └── dev
      ├── feature/{이슈번호}-{기능명}
      ├── fix/{이슈번호}-{버그명}
      └── hotfix/{이슈번호}-{수정명}
```

| 브랜치 | 설명 |
|--------|------|
| `main` | 배포 브랜치. 직접 push 금지 |
| `dev` | 개발 통합 브랜치. PR을 통해서만 merge |
| `feature/{이슈번호}-{기능명}` | 기능 개발 브랜치 (`dev`에서 분기) |
| `fix/{이슈번호}-{버그명}` | 버그 수정 브랜치 (`dev`에서 분기) |
| `hotfix/{이슈번호}-{수정명}` | 긴급 수정 브랜치 (`main`에서 분기) |

**브랜치 이름 예시:**
```
feature/12-hub-crud
fix/23-delivery-sequence-bug
hotfix/45-jwt-expiry-issue
```

---

## 커밋 메시지 컨벤션

```
타입: 내용 (#이슈번호)
```

| 타입 | 설명 |
|------|------|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 코드 리팩토링 (기능 변경 없음) |
| `test` | 테스트 코드 추가/수정 |
| `docs` | 문서 추가/수정 |
| `chore` | 빌드 설정, 의존성 변경 등 |
| `style` | 코드 포맷팅, 세미콜론 누락 등 |

**예시:**
```
feat: 허브 엔티티 CRUD 구현 (#12)
fix: 배송 담당자 순번 배정 로직 수정 (#23)
refactor: 주문 서비스 FeignClient 분리 (#31)
docs: 허브 간 이동정보 API 명세서 업데이트 (#18)
```

---

## PR 규칙

- PR 단위: 하나의 이슈 또는 기능 단위
- 최소 **1명** 이상의 팀원 코드 리뷰 승인 필요
- PR 제목 형식: `타입: 내용 (#이슈번호)`
- `dev` → `main` PR은 팀 전원 리뷰 필요
- 모든 CI 체크(빌드, 테스트) 통과 후 merge 가능
- PR 설명에 변경 내용, 테스트 방법, 스크린샷(해당 시) 포함

---

## 코드 컨벤션

### 네이밍 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| 패키지 | 소문자, `com.sparta.{서비스}` | `com.sparta.hub`, `com.sparta.order` |
| 클래스/인터페이스 | PascalCase | `HubService`, `DeliveryRepository` |
| 메서드/변수 | camelCase | `findHubById`, `deliveryManagerId` |
| 상수 | UPPER_SNAKE_CASE | `MAX_DELIVERY_COUNT` |
| DB 테이블 | `p_` 접두사 + snake_case | `p_hubs`, `p_delivery_routes` |
| DB 컬럼 | snake_case | `created_at`, `hub_manager_id` |

### 엔티티 공통 규칙

- 모든 엔티티의 PK는 **UUID** 사용
- 모든 테이블에 Audit 필드 포함:
  ```
  created_at, created_by, updated_at, updated_by, deleted_at, deleted_by
  ```
- 삭제는 반드시 **Soft Delete** 처리 (`deleted_at`, `deleted_by` 활용)
- `deleted_at`이 null인 데이터만 조회/검색 대상

### 아키텍처 규칙

- **Layered Architecture** 준수: `Controller → Service → Domain → Infrastructure`
- 계층 간 의존은 상위 → 하위 방향만 허용
- Entity와 DTO 분리 필수
- 서비스 간 통신은 **FeignClient** 사용 (REST API 호출)
- 각 마이크로서비스는 독립된 DB 스키마 사용

### 검색/조회 공통 규칙

- 정렬 기준: 생성일순(`created_at`), 수정일순(`updated_at`) 기본 제공
- 페이지 크기: 10건, 30건, 50건만 허용 (이외는 10건으로 고정)

---

## 프로젝트 구조

Gradle 멀티모듈 모노레포로 구성되어 있습니다.

```
backend/
├── build.gradle            # 부모 빌드 (버전/공통 의존성 중앙 관리)
├── settings.gradle         # 모듈 등록
├── gradlew / gradlew.bat   # Gradle Wrapper (별도 Gradle 설치 불필요)
├── common/                 # 공통 라이브러리 모듈 (서비스 아님)
├── eureka-service/
├── gateway-service/
├── user-service/
├── hub-service/
├── company-service/
├── order-service/
├── delivery-service/
├── slack-service/
├── .env.example            # 환경변수 템플릿
└── CONTRIBUTING.md
```

### 마이크로서비스 목록

| 서비스 | 포트 | 설명 |
|--------|------|------|
| `eureka-service` | 8761 | Spring Cloud Eureka, 서비스 디스커버리 |
| `gateway-service` | 8080 | Spring Cloud Gateway, 인증/인가 처리 |
| `user-service` | 19091 | 사용자 관리, JWT 발급 |
| `hub-service` | 19092 | 허브 관리, 허브 간 이동정보 (Redis 캐싱) |
| `company-service` | 19093 | 업체 관리, 상품 관리 |
| `order-service` | 19094 | 주문 관리, AI 발송 시한 (Gemini) |
| `delivery-service` | 19095 | 배송 관리, 배송 경로 기록 |
| `slack-service` | 19096 | 슬랙 메시지 발송/저장 |

### common 모듈

서비스 간 중복 코드를 모아둔 **라이브러리 모듈**입니다(실행되는 서비스 아님). 각 서비스는 `implementation project(':common')`로 의존합니다.

| 클래스 | 용도 |
|--------|------|
| `ApiResponse<T>` | 통일된 API 응답 포맷 |
| `ErrorCode` | 공통 에러 코드 enum |
| `BusinessException` | 비즈니스 예외 최상위 타입 |
| `GlobalExceptionHandler` | 전역 예외 처리 (`@RestControllerAdvice`) |
| `BaseEntity` | Audit 필드 + Soft Delete (`@MappedSuperclass`) |
| `JpaAuditingConfig` | JPA Auditing 자동 설정 (`X-Username` 헤더 기반 감사자) |
| `UserRole` | 전역 권한 enum |
| `AuthHeaders` | 게이트웨이→서비스 인증 헤더 상수 |

> **중요:** business 서비스의 메인 클래스는 `@SpringBootApplication(scanBasePackages = "com.sparta")`로 선언해야 common의 `GlobalExceptionHandler`·`JpaAuditingConfig`가 자동 적용됩니다. 엔티티는 `BaseEntity`를 상속하세요.

### 신규 서비스 생성 시

1. `settings.gradle`에 모듈 등록 (`include '...'`)
2. 서비스 폴더에 `build.gradle` 작성 (서비스별 의존성만 선언)
3. `eureka-service`에 자동 등록 (Eureka Client 의존성 추가)
4. `gateway-service`의 라우팅 규칙 추가
5. `docker-compose.yml`에 컨테이너 설정 추가
6. Swagger 문서 작성 및 게이트웨이를 통해 통합 조회 가능하도록 구성

---

## 보안 가이드

- JWT 토큰 기반 인증 (Spring Security + **jjwt** 직접 구현, Keycloak 미사용)
- **인증 흐름**: `user-service`가 JWT 발급 → `gateway-service`가 요청마다 토큰 검증 → 검증된 사용자 정보를 `X-User-Id` / `X-Username` / `X-User-Role` 헤더로 다운스트림 서비스에 전달 (`AuthHeaders` 상수 사용)
- 각 서비스는 게이트웨이가 전달한 헤더를 신뢰하여 인가 처리
- 비밀번호: Bcrypt 해시 알고리즘 사용
- 서버 측 데이터 유효성 검사: Spring Validator(`@Valid`) 사용
- 권한 확인은 요청마다 수행

### 권한 체계

| 권한 | 설명 |
|------|------|
| `MASTER` | 모든 기능 접근 가능 |
| `HUB_MANAGER` | 담당 허브 내 기능 관리 |
| `DELIVERY_MANAGER` | 배송 관련 조회 및 본인 배송 수정 |
| `SUPPLIER_MANAGER` | 본인 업체/상품 관리 |

---

## 이슈 및 PR 참고

- 버그 발견 시 → Bug Report 이슈 템플릿 사용
- 기능 제안 시 → Feature Request 이슈 템플릿 사용
- PR 작성 시 → PR 템플릿에 맞게 작성
- 문제 발생 시 문제 상황, 원인, 해결 방법, 개선 효과를 이슈에 기록하여 팀 전체가 공유

---

## 트러블슈팅 공유

개발 중 발생한 문제는 이슈로 등록하고 해결 후 내용을 업데이트해 주세요.
팀원의 간접 경험을 통한 성장을 지향합니다.
