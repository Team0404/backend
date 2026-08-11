CREATE TABLE p_users
(
    user_id          UUID         NOT NULL,
    username         VARCHAR(100) NOT NULL,
    nickname         VARCHAR(100) NOT NULL,
    slack_id         VARCHAR(100) NOT NULL,
    password         VARCHAR(255) NOT NULL,
    role             VARCHAR(30)  NOT NULL,
    approval_status  VARCHAR(20)  NOT NULL,
    rejection_reason VARCHAR(255),
    hub_id           UUID,
    company_id       UUID,
    created_at       TIMESTAMP(6),
    created_by       UUID,
    updated_at       TIMESTAMP(6),
    updated_by       UUID,
    deleted_at       TIMESTAMP(6),
    deleted_by       UUID,

    CONSTRAINT pk_p_users PRIMARY KEY (user_id),
    CONSTRAINT uk_p_users_username UNIQUE (username),
    CONSTRAINT uk_p_users_slack_id UNIQUE (slack_id),
    CONSTRAINT ck_p_users_role CHECK (
        role IN ('MASTER', 'HUB_MANAGER', 'DELIVERY_MANAGER', 'SUPPLIER_MANAGER')
    ),
    CONSTRAINT ck_p_users_approval_status CHECK (
        approval_status IN ('PENDING', 'APPROVED', 'REJECTED')
    )
);
