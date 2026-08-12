-- 허브 운영 상태 컬럼 보장
ALTER TABLE public.p_hubs
    ADD COLUMN IF NOT EXISTS status VARCHAR(20);

UPDATE public.p_hubs
SET status = 'ACTIVE'
WHERE status IS NULL;

ALTER TABLE public.p_hubs
    ALTER COLUMN status SET DEFAULT 'ACTIVE',
    ALTER COLUMN status SET NOT NULL;


-- 허브 상태 CHECK 제약조건이 없을 때만 추가
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_p_hubs_status'
          AND conrelid = 'public.p_hubs'::regclass
    ) THEN
ALTER TABLE public.p_hubs
    ADD CONSTRAINT ck_p_hubs_status
        CHECK (
            status IN (
                       'ACTIVE',
                       'DEACTIVATING',
                       'INACTIVE'
                )
            );
END IF;
END
$$;


-- 허브 종류 컬럼 보장
ALTER TABLE public.p_hubs
    ADD COLUMN IF NOT EXISTS hub_type VARCHAR(20);

UPDATE public.p_hubs
SET hub_type = 'REGIONAL'
WHERE hub_type IS NULL;

ALTER TABLE public.p_hubs
    ALTER COLUMN hub_type SET DEFAULT 'REGIONAL',
    ALTER COLUMN hub_type SET NOT NULL;


-- 허브 종류 CHECK 제약조건이 없을 때만 추가
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_p_hubs_hub_type'
          AND conrelid = 'public.p_hubs'::regclass
    ) THEN
ALTER TABLE public.p_hubs
    ADD CONSTRAINT ck_p_hubs_hub_type
        CHECK (hub_type IN ('CENTRAL', 'REGIONAL'));
END IF;
END
$$;


-- 중앙 허브 설정
UPDATE public.p_hubs
SET hub_type = 'CENTRAL'
WHERE hub_id IN (
                 '71dd4a49-0341-4a45-b545-4518f41bd4f8',
                 '9a01d94f-537e-410d-9c0e-1f4e222a93f0',
                 'fcd2f93f-1837-42bc-a39f-36ca10d624e1'
    );