# Contributing Guide

스파르타 물류 MSA 프로젝트에 기여해 주셔서 감사합니다.
이 문서는 팀원들이 일관된 방식으로 협업할 수 있도록 코드 컨벤션, 브랜치 전략, PR 정책 등을 정의합니다.

---

## 개발 환경

| 항목 | 버전/도구 |
|------|-----------|
| Language | Java 17+ |
| Framework | Spring Boot 3.x |
| Build | Gradle |
| Database | PostgreSQL |
| Cache | Redis |
| Container | Docker & Docker Compose |
| API 문서 | Swagger (SpringDoc OpenAPI) |
| 분산 추적 | Zipkin |

---

## 브랜치 전략

```
main
 └── develop
      ├── feature/{이슈번호}-{기능명}
      ├── fix/{이슈번호}-{버그명}
      └── hotfix/{이슈번호}-{수정명}
```

| 브랜치 | 설명 |
|--------|------|
| `main` | 배포 브랜치. 직접 push 금지 |
| `develop` | 개발 통합 브랜치. PR을 통해서만 merge |
| `feature/{이슈번호}-{기능명}` | 기능 개발 브랜치 |
| `fix/{이슈번호}-{버그명}` | 버그 수정 브랜치 |
| `hotfix/{이슈번호}-{수정명}` | 긴급 수정 브랜치 (main에서 분기) |

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
- `develop` → `main` PR은 팀 전원 리뷰 필요
- 모든 CI 체크(빌드, 테스트) 통과 후 merge 가능
- PR 설명에 변경 내용, 테스트 방법, 스크린샷(해당 시) 포함

---

## 코드 컨벤션

### 네이밍 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| 패키지 | 소문자, 단어 구분 없음 | `com.sparta.logistics.hub` |
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

## 서비스별 개발 가이드

### 마이크로서비스 목록

| 서비스 | 설명 |
|--------|------|
| `gateway-service` | Spring Cloud Gateway, 인증/인가 처리 |
| `eureka-service` | Spring Cloud Eureka, 서비스 디스커버리 |
| `user-service` | 사용자 관리, JWT 인증 |
| `hub-service` | 허브 관리, 허브 간 이동정보 |
| `company-service` | 업체 관리, 상품 관리 |
| `order-service` | 주문 관리 |
| `delivery-service` | 배송 관리, 배송 경로 기록 |
| `slack-service` | 슬랙 메시지 발송/저장 |

### 신규 서비스 생성 시

1. `eureka-service`에 서비스 등록 설정 추가
2. `gateway-service`에 라우팅 규칙 추가
3. `docker-compose.yml`에 컨테이너 설정 추가
4. Swagger 문서 작성 및 게이트웨이를 통해 통합 조회 가능하도록 구성
5. Zipkin 분산 추적 설정 추가

---

## 보안 가이드

- JWT 토큰 기반 인증 (Spring Security + Keycloak 또는 직접 구현)
- 비밀번호: Bcrypt 해시 알고리즘 사용
- 서버 측 데이터 유효성 검사: Spring Validator 사용
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
