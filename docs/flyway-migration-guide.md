# Flyway 적용 가이드

DB를 사용하는 각 서비스는 자신의 마이그레이션을 직접 관리합니다.

- 적용 대상: user, hub, company, order, delivery, slack
- 적용 제외: gateway, eureka, common
- 서비스별 데이터베이스와 `public` 스키마를 사용합니다.

## 서비스 담당자 작업

### 1. 의존성 추가

각 서비스의 `build.gradle`에 추가합니다. `common`에는 추가하지 않습니다.

```gradle
implementation 'org.flywaydb:flyway-core'
runtimeOnly 'org.flywaydb:flyway-database-postgresql'
```

### 2. 최초 마이그레이션 작성

```text
서비스명/src/main/resources/db/migration/V1__init_schema.sql
```

담당 서비스의 엔티티를 기준으로 다음 항목을 작성합니다.

- 테이블과 컬럼
- PK, FK
- `NOT NULL`, `UNIQUE`
- 인덱스와 CHECK 제약조건
- BaseEntity 공통 컬럼

### 3. 설정 변경

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    default-schema: ${DB_SCHEMA:public}
    clean-disabled: true
    validate-migration-naming: true

  jpa:
    hibernate:
      ddl-auto: ${DDL_AUTO:validate}
    properties:
      hibernate:
        default_schema: ${DB_SCHEMA:public}
```

Hibernate의 `create_namespaces` 설정은 제거합니다.

### 4. 로컬 테스트

보존할 데이터가 없는지 확인하고 담당 서비스 DB의 `public` 스키마만 초기화합니다.

```powershell
docker exec sparta-postgres psql `
  -U postgres `
  -d 서비스_DB_이름 `
  -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

서비스를 실행한 뒤 확인합니다.

```sql
SELECT version, description, success
FROM flyway_schema_history;
```

- V1이 성공했는지 확인합니다.
- Hibernate `validate`가 통과하는지 확인합니다.
- 서비스를 재실행했을 때 `No migration necessary`가 출력되는지 확인합니다.
- 기존 테스트와 주요 API가 정상 동작하는지 확인합니다.

## 이후 DB 변경

한 번 적용된 마이그레이션 파일은 수정하지 않습니다. 변경이 필요하면 새로운 파일을 추가합니다.

```text
V2__add_example_column.sql
V3__add_example_index.sql
```

엔티티 변경과 관련 마이그레이션은 같은 PR에 포함합니다.

## 운영 배포 순서

`dev` 병합 시 자동 배포되므로 DB 준비 전에는 PR을 병합하지 않습니다.

1. PR 리뷰를 완료합니다.
2. 대상 서비스 DB를 백업합니다.
3. 대상 서비스만 중지합니다.
4. 대상 서비스 DB의 `public` 스키마만 초기화합니다.
5. 빈 스키마를 확인한 후 PR을 병합합니다.
6. GitHub Actions 배포 결과를 확인합니다.
7. 서비스 로그에서 Flyway 적용과 Hibernate 검증 성공을 확인합니다.
8. `flyway_schema_history`, Eureka, Swagger 및 주요 API를 확인합니다.

서비스별로 하나씩 순차 적용하며 PostgreSQL 전체를 초기화하지 않습니다.

## 주의사항

- 운영 DB 작업 전에는 반드시 백업합니다.
- `docker compose down -v`를 사용하지 않습니다.
- 다른 서비스 DB와 Redis 볼륨을 삭제하지 않습니다.
- 데이터가 0건이어도 기존 테이블이 있으면 빈 스키마가 아닙니다.
- 이미 적용된 `V1`, `V2` 등의 파일을 수정하지 않습니다.
- 운영 비밀번호나 토큰을 마이그레이션 SQL에 작성하지 않습니다.
