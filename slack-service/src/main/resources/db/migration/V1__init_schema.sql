-- slack-service 초기 스키마
-- AI 요청/응답 로그(p_ai_messages) / 슬랙 메시지 이력(p_slack_messages)

CREATE TABLE p_ai_messages
(
    ai_message_id           UUID         NOT NULL,
    -- 관련 주문 ID (주문서비스 논리 참조)
    order_id                UUID,
    -- 관련 배송 ID (출발/도착 허브 참조용, 배송서비스 논리 참조)
    delivery_id             UUID         NOT NULL,
    -- 알림을 받을 발송 허브 담당자의 슬랙 ID.
    -- 배송의 recipient_slack_id(수령 고객)와는 다른 대상이며, AI 호출이 실패해
    -- 슬랙 이력이 생기지 않은 건도 재생성(A4) 후 재발송할 수 있도록 여기에 보관한다.
    manager_slack_id        VARCHAR(100) NOT NULL,
    -- AI 요청 프롬프트 원문(상품/수량, 납기, 발송지/경유지/도착지, 근무시간 등)
    request_prompt          TEXT         NOT NULL,
    response_content        TEXT,
    -- AI가 산출한 최종 발송 시한
    final_dispatch_deadline TIMESTAMP(6),
    model                   VARCHAR(50),
    fail_reason             TEXT,
    -- A4(재생성 API) 호출 횟수. GeminiClient의 즉시 재시도는 여기에 반영하지 않는다.
    retry_count             INTEGER      NOT NULL,
    max_retry_count         INTEGER      NOT NULL,
    status                  VARCHAR(20)  NOT NULL,
    created_at              TIMESTAMP(6),
    created_by              UUID,
    updated_at              TIMESTAMP(6),
    updated_by              UUID,
    deleted_at              TIMESTAMP(6),
    deleted_by              UUID,

    CONSTRAINT pk_p_ai_messages PRIMARY KEY (ai_message_id),
    -- CANCELLED: 주문 취소로 무효화된 상태. Soft Delete와 구분되며 조회 대상에는 계속 포함된다.
    CONSTRAINT ck_p_ai_messages_status CHECK (
        status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED')
        )
);

CREATE INDEX idx_p_ai_messages_order_id ON p_ai_messages (order_id);

CREATE TABLE p_slack_messages
(
    slack_message_id  UUID         NOT NULL,
    receiver_slack_id VARCHAR(100) NOT NULL,
    message           TEXT         NOT NULL,
    message_type      VARCHAR(30)  NOT NULL,
    -- AI가 생성한 메시지인 경우에만 연결된다(스키마 내부 물리 FK).
    ai_message_id     UUID,
    -- 관련 주문/배송 ID (타 서비스 논리 참조)
    reference_id      UUID,
    status            VARCHAR(20)  NOT NULL,
    -- M2(재발송 API) 호출 횟수. SlackWebhookClient의 즉시 재시도는 여기에 반영하지 않는다.
    retry_count       INTEGER      NOT NULL,
    max_retry_count   INTEGER      NOT NULL,
    fail_reason       TEXT,
    sent_at           TIMESTAMP(6),
    created_at        TIMESTAMP(6),
    created_by        UUID,
    updated_at        TIMESTAMP(6),
    updated_by        UUID,
    deleted_at        TIMESTAMP(6),
    deleted_by        UUID,

    CONSTRAINT pk_p_slack_messages PRIMARY KEY (slack_message_id),
    CONSTRAINT fk_p_slack_messages_ai_message_id
        FOREIGN KEY (ai_message_id) REFERENCES p_ai_messages (ai_message_id),
    CONSTRAINT ck_p_slack_messages_message_type CHECK (
        message_type IN ('GENERAL', 'DISPATCH_DEADLINE', 'DAILY_ROUTE')
        ),
    CONSTRAINT ck_p_slack_messages_status CHECK (
        status IN ('PENDING', 'SUCCESS', 'FAILED')
        )
);

CREATE INDEX idx_p_slack_messages_receiver_slack_id ON p_slack_messages (receiver_slack_id);
CREATE INDEX idx_p_slack_messages_ai_message_id ON p_slack_messages (ai_message_id);
