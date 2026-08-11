CREATE TABLE p_hubs
(
    hub_id     UUID          NOT NULL,
    name       VARCHAR(100)  NOT NULL,
    address    VARCHAR(255)  NOT NULL,
    latitude   DECIMAL(10,7) NOT NULL,
    longitude  DECIMAL(10,7) NOT NULL,
    created_at TIMESTAMP(6),
    created_by UUID,
    updated_at TIMESTAMP(6),
    updated_by UUID,
    deleted_at TIMESTAMP(6),
    deleted_by UUID,

    CONSTRAINT pk_p_hubs
        PRIMARY KEY (hub_id),

    CONSTRAINT ck_p_hubs_latitude
        CHECK (latitude BETWEEN -90 AND 90),

    CONSTRAINT ck_p_hubs_longitude
        CHECK (longitude BETWEEN -180 AND 180)
);


CREATE TABLE p_hub_routes
(
    hub_route_id     UUID          NOT NULL,
    departure_hub_id UUID          NOT NULL,
    arrival_hub_id   UUID          NOT NULL,
    duration_minutes INTEGER       NOT NULL,
    distance_km      DECIMAL(10,2) NOT NULL,
    created_at       TIMESTAMP(6),
    created_by       UUID,
    updated_at       TIMESTAMP(6),
    updated_by       UUID,
    deleted_at       TIMESTAMP(6),
    deleted_by       UUID,

    CONSTRAINT pk_p_hub_routes
        PRIMARY KEY (hub_route_id),

    CONSTRAINT fk_p_hub_routes_departure
        FOREIGN KEY (departure_hub_id)
            REFERENCES p_hubs (hub_id),

    CONSTRAINT fk_p_hub_routes_arrival
        FOREIGN KEY (arrival_hub_id)
            REFERENCES p_hubs (hub_id),

    CONSTRAINT ck_p_hub_routes_different_hubs
        CHECK (departure_hub_id <> arrival_hub_id),

    CONSTRAINT ck_p_hub_routes_duration
        CHECK (duration_minutes > 0),

    CONSTRAINT ck_p_hub_routes_distance
        CHECK (distance_km > 0)
);


CREATE INDEX idx_p_hub_routes_departure_hub
    ON p_hub_routes (departure_hub_id);

CREATE INDEX idx_p_hub_routes_arrival_hub
    ON p_hub_routes (arrival_hub_id);


CREATE UNIQUE INDEX uk_p_hub_routes_active_route
    ON p_hub_routes (
                     departure_hub_id,
                     arrival_hub_id
        )
    WHERE deleted_at IS NULL;