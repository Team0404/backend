-- delivery-service 초기 스키마
-- 배송(p_deliveries) / 배송 경로(p_delivery_routes)
-- 배송 담당자(p_delivery_managers) / 라운드로빈 커서(p_delivery_manager_cursors)

CREATE TABLE p_deliveries
(
    delivery_id                 UUID         NOT NULL,
    order_id                    UUID         NOT NULL,
    status                      VARCHAR(255) NOT NULL,
    origin_hub_id               UUID         NOT NULL,
    dest_hub_id                 UUID         NOT NULL,
    delivery_address            VARCHAR(255) NOT NULL,
    recipient_name              VARCHAR(255) NOT NULL,
    recipient_slack_id          VARCHAR(255),
    -- 업체 -> 허브 / 허브 -> 업체 구간을 담당하는 배송 담당자(user_id)
    company_delivery_manager_id UUID,
    -- 주문 취소 보상 트랜잭션 기록. 삭제(deleted_at)와 구분한다.
    cancel_reason               VARCHAR(255),
    cancelled_at                TIMESTAMP(6),
    created_at                  TIMESTAMP(6),
    created_by                  UUID,
    updated_at                  TIMESTAMP(6),
    updated_by                  UUID,
    deleted_at                  TIMESTAMP(6),
    deleted_by                  UUID,

    CONSTRAINT pk_p_deliveries PRIMARY KEY (delivery_id),
    CONSTRAINT uk_p_deliveries_order_id UNIQUE (order_id),
    CONSTRAINT ck_p_deliveries_status CHECK (
        status IN ('HUB_WAIT', 'HUB_MOVING', 'DEST_HUB_ARRIVED',
                   'OUT_FOR_DELIVERY', 'COMPANY_MOVING', 'DELIVERED', 'CANCELLED')
        )
);

CREATE TABLE p_delivery_routes
(
    id                      UUID        NOT NULL,
    delivery_id             UUID        NOT NULL,
    sequence                INTEGER     NOT NULL,
    origin_hub_id           UUID        NOT NULL,
    dest_hub_id             UUID        NOT NULL,
    expected_distance_km    NUMERIC(38, 2),
    expected_duration_min   INTEGER,
    actual_distance_km      NUMERIC(38, 2),
    actual_duration_min     INTEGER,
    status                  VARCHAR(20),
    hub_delivery_manager_id UUID,
    created_at              TIMESTAMP(6),
    created_by              UUID,
    updated_at              TIMESTAMP(6),
    updated_by              UUID,
    deleted_at              TIMESTAMP(6),
    deleted_by              UUID,

    CONSTRAINT pk_p_delivery_routes PRIMARY KEY (id),
    CONSTRAINT fk_p_delivery_routes_delivery_id
        FOREIGN KEY (delivery_id) REFERENCES p_deliveries (delivery_id),
    CONSTRAINT ck_p_delivery_routes_status CHECK (
        status IN ('HUB_MOVE_WAIT', 'HUB_MOVING', 'DEST_HUB_ARRIVED', 'OUT_FOR_DELIVERY', 'CANCELLED')
        )
);

CREATE INDEX idx_p_delivery_routes_delivery_id ON p_delivery_routes (delivery_id);
CREATE INDEX idx_p_delivery_routes_hub_delivery_manager_id ON p_delivery_routes (hub_delivery_manager_id);

CREATE TABLE p_delivery_managers
(
    -- user-service의 user_id와 동일한 값(논리 참조)이므로 자동 생성하지 않는다.
    user_id    UUID        NOT NULL,
    -- HUB: 허브 간 이동 담당(hub_id NULL) / COMPANY: 최종 허브 -> 업체 담당(hub_id 필수)
    type       VARCHAR(20) NOT NULL,
    hub_id     UUID,
    -- 라운드로빈 배정 순번
    sequence   INTEGER     NOT NULL,
    created_at TIMESTAMP(6),
    created_by UUID,
    updated_at TIMESTAMP(6),
    updated_by UUID,
    deleted_at TIMESTAMP(6),
    deleted_by UUID,

    CONSTRAINT pk_p_delivery_managers PRIMARY KEY (user_id),
    CONSTRAINT ck_p_delivery_managers_type CHECK (type IN ('HUB', 'COMPANY'))
);

CREATE INDEX idx_p_delivery_managers_type_hub_id ON p_delivery_managers (type, hub_id);

CREATE TABLE p_delivery_manager_cursors
(
    id                       UUID         NOT NULL,
    type                     VARCHAR(255) NOT NULL,
    -- PostgreSQL은 UNIQUE 제약에서 NULL을 서로 다른 값으로 취급하므로,
    -- HUB 전역 그룹은 NULL 대신 sentinel UUID(00000000-...-0000)를 사용한다.
    hub_id                   UUID         NOT NULL,
    last_assigned_manager_id UUID,
    created_at               TIMESTAMP(6),
    created_by               UUID,
    updated_at               TIMESTAMP(6),
    updated_by               UUID,
    deleted_at               TIMESTAMP(6),
    deleted_by               UUID,

    CONSTRAINT pk_p_delivery_manager_cursors PRIMARY KEY (id),
    CONSTRAINT uk_cursor_type_hub_id UNIQUE (type, hub_id),
    CONSTRAINT ck_p_delivery_manager_cursors_type CHECK (type IN ('HUB', 'COMPANY'))
);
