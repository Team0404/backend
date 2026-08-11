-- 제약 조건 설정

-- 활성 상태인 동일 출발·도착 허브 경로의 중복 등록 방지
-- Soft Delete된 경로(deleted_at IS NOT NULL)는 중복 검사에서 제외
CREATE UNIQUE INDEX uk_hub_routes_active_departure_arrival
    ON p_hub_routes (departure_hub_id, arrival_hub_id)
    WHERE deleted_at IS NULL;


-- 허브 운영 상태 컬럼 추가
-- 기존 데이터와 신규 데이터의 기본 상태는 ACTIVE
ALTER TABLE public.p_hubs
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- 허브 운영 상태에 허용되는 값 제한
ALTER TABLE public.p_hubs
    ADD CONSTRAINT ck_p_hubs_status
        CHECK (status IN ('ACTIVE', 'DEACTIVATING', 'INACTIVE'));


-- 허브 종류 컬럼 추가
ALTER TABLE public.p_hubs
    ADD COLUMN hub_type VARCHAR(20) NOT NULL DEFAULT 'REGIONAL';

-- 허브 종류로 사용할 수 있는 값 제한
ALTER TABLE public.p_hubs
    ADD CONSTRAINT ck_p_hubs_hub_type
        CHECK (hub_type IN ('CENTRAL', 'REGIONAL'));

-- 경기남부, 대전, 대구를 중앙 허브로 설정
UPDATE public.p_hubs
SET hub_type = 'CENTRAL'
WHERE hub_id IN (
                 '71dd4a49-0341-4a45-b545-4518f41bd4f8',
                 '9a01d94f-537e-410d-9c0e-1f4e222a93f0',
                 'fcd2f93f-1837-42bc-a39f-36ca10d624e1'
    );