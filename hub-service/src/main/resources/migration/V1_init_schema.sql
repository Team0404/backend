CREATE TABLE p_hubs
(
    hub_id      UUID          NOT NULL,
    name        VARCHAR(100)  NOT NULL,
    address     VARCHAR(255)  NOT NULL,
    latitude    DECIMAL(10,7) NOT NULL,
    longitude   DECIMAL(10,7) NOT NULL,

    created_at  TIMESTAMP(6),
    created_by  UUID,
    updated_at  TIMESTAMP(6),
    updated_by  UUID,
    deleted_at  TIMESTAMP(6),
    deleted_by  UUID,

    CONSTRAINT pk_p_hubs
        PRIMARY KEY (hub_id),

    CONSTRAINT ck_p_hubs_latitude
        CHECK (latitude BETWEEN -90 AND 90),

    CONSTRAINT ck_p_hubs_longitude
        CHECK (longitude BETWEEN -180 AND 180)
);