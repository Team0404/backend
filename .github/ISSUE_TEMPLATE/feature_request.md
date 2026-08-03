---
name: Feature Request
about: 새로운 기능을 제안하거나 구현 작업을 등록할 때 사용해 주세요.
title: "[FEAT] "
labels: feature
assignees: ""
---

## 기능 요약

구현하려는 기능을 한 줄로 요약해 주세요.

## 관련 서비스

기능이 속하는 마이크로서비스를 선택해 주세요.

- [ ] gateway-service
- [ ] eureka-service
- [ ] user-service
- [ ] hub-service
- [ ] company-service
- [ ] order-service
- [ ] delivery-service
- [ ] slack-service
- [ ] 기타: 

## 기능 상세 설명

구현하려는 기능의 상세 내용을 작성해 주세요.

### 배경 및 목적

이 기능이 필요한 이유와 해결하려는 문제를 설명해 주세요.

### 요구사항

- [ ] 
- [ ] 
- [ ] 

## API 설계 (해당 시)

| 항목 | 내용 |
|------|------|
| Method | `GET / POST / PUT / DELETE` |
| URL | `/api/v1/...` |
| 권한 | `MASTER / HUB_MANAGER / DELIVERY_MANAGER / SUPPLIER_MANAGER` |

**Request 예시:**
```json

```

**Response 예시:**
```json

```

## 권한 정책

| 역할 | 생성 | 수정 | 삭제 | 조회 |
|------|------|------|------|------|
| MASTER | | | | |
| HUB_MANAGER | | | | |
| DELIVERY_MANAGER | | | | |
| SUPPLIER_MANAGER | | | | |

## DB 변경 사항 (해당 시)

새로운 테이블이나 컬럼 변경이 필요한 경우 작성해 주세요.

```sql
-- 예시
CREATE TABLE p_example (
    id UUID PRIMARY KEY,
    ...
    created_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
);
```

## 완료 조건 (Definition of Done)

- [ ] 기능 구현 완료
- [ ] 단위 테스트 작성
- [ ] Swagger 문서 업데이트
- [ ] Soft Delete 처리 적용
- [ ] 권한 검증 로직 적용
- [ ] 코드 리뷰 완료

## 참고 자료

관련 문서, 링크, 스크린샷 등을 첨부해 주세요.
