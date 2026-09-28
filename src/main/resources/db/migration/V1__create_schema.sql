-- =====================================================================
--  PadaShakti - V1 : Core schema
--  Smart Urban Subway Kinetic Floor Energy Harvesting Network
--  MySQL 8.0+ / InnoDB / utf8mb4
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. USERS  - control-centre accounts (JWT login, role based access)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)  NOT NULL,
    email           VARCHAR(120) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at   DATETIME(3)  NULL,
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'OPERATOR', 'ENGINEER'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 2. STATION  - a metro station fitted with kinetic floor tiles
-- ---------------------------------------------------------------------
CREATE TABLE station (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    code                    VARCHAR(20)   NOT NULL,
    name                    VARCHAR(120)  NOT NULL,
    city                    VARCHAR(60)   NOT NULL,
    line_name               VARCHAR(80)   NULL,
    latitude                DECIMAL(9, 6) NULL,
    longitude               DECIMAL(9, 6) NULL,
    daily_footfall_estimate INT           NOT NULL DEFAULT 0,
    layout_width            INT           NOT NULL DEFAULT 1000 COMMENT 'Floor-map canvas width (SVG units)',
    layout_height           INT           NOT NULL DEFAULT 740  COMMENT 'Floor-map canvas height (SVG units)',
    status                  VARCHAR(20)   NOT NULL DEFAULT 'ONLINE',
    created_at              DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_station PRIMARY KEY (id),
    CONSTRAINT uk_station_code UNIQUE (code),
    CONSTRAINT ck_station_status CHECK (status IN ('ONLINE', 'OFFLINE', 'MAINTENANCE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 3. ZONE  - area inside a station (entry, stairs, platform ...)
--    x/y/width/height = rectangle on the floor-map canvas
--    grid_rows/grid_cols = tile grid inside the zone
-- ---------------------------------------------------------------------
CREATE TABLE zone (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    station_id      BIGINT        NOT NULL,
    code            VARCHAR(10)   NOT NULL,
    name            VARCHAR(80)   NOT NULL,
    zone_type       VARCHAR(20)   NOT NULL,
    traffic_weight  DECIMAL(4, 2) NOT NULL DEFAULT 1.00 COMMENT 'Relative footfall share used by simulator',
    max_capacity    INT           NOT NULL COMMENT 'Safe number of people in zone (crowd safety)',
    pos_x           INT           NOT NULL,
    pos_y           INT           NOT NULL,
    width           INT           NOT NULL,
    height          INT           NOT NULL,
    grid_rows       INT           NOT NULL,
    grid_cols       INT           NOT NULL,
    display_order   INT           NOT NULL DEFAULT 0,
    created_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_zone PRIMARY KEY (id),
    CONSTRAINT uk_zone_station_code UNIQUE (station_id, code),
    CONSTRAINT fk_zone_station FOREIGN KEY (station_id) REFERENCES station (id) ON DELETE CASCADE,
    CONSTRAINT ck_zone_type CHECK (zone_type IN ('ENTRY', 'STAIRS', 'CONCOURSE', 'PLATFORM', 'CORRIDOR', 'EXIT')),
    CONSTRAINT ck_zone_weight CHECK (traffic_weight > 0),
    CONSTRAINT ck_zone_grid CHECK (grid_rows > 0 AND grid_cols > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 4. TILE  - one kinetic energy harvesting floor tile (IoT node)
-- ---------------------------------------------------------------------
CREATE TABLE tile (
    id                    BIGINT         NOT NULL AUTO_INCREMENT,
    zone_id               BIGINT         NOT NULL,
    tile_code             VARCHAR(30)    NOT NULL,
    tile_type             VARCHAR(20)    NOT NULL,
    status                VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    grid_row              INT            NOT NULL,
    grid_col              INT            NOT NULL,
    pos_x                 DECIMAL(8, 2)  NOT NULL COMMENT 'Tile centre X on floor-map canvas',
    pos_y                 DECIMAL(8, 2)  NOT NULL COMMENT 'Tile centre Y on floor-map canvas',
    rated_joules_per_step DECIMAL(5, 2)  NOT NULL,
    health_pct            DECIMAL(5, 2)  NOT NULL DEFAULT 100.00,
    steps_today           BIGINT         NOT NULL DEFAULT 0,
    energy_today_j        DECIMAL(14, 3) NOT NULL DEFAULT 0,
    total_steps           BIGINT         NOT NULL DEFAULT 0,
    total_energy_j        DECIMAL(18, 3) NOT NULL DEFAULT 0,
    installed_on          DATE           NOT NULL,
    last_maintenance_on   DATE           NULL,
    last_step_at          DATETIME(3)    NULL,
    version               BIGINT         NOT NULL DEFAULT 0 COMMENT 'Optimistic locking',
    created_at            DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at            DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_tile PRIMARY KEY (id),
    CONSTRAINT uk_tile_code UNIQUE (tile_code),
    CONSTRAINT uk_tile_zone_grid UNIQUE (zone_id, grid_row, grid_col),
    CONSTRAINT fk_tile_zone FOREIGN KEY (zone_id) REFERENCES zone (id) ON DELETE CASCADE,
    CONSTRAINT ck_tile_type CHECK (tile_type IN ('PIEZO', 'ELECTROMAGNETIC')),
    CONSTRAINT ck_tile_status CHECK (status IN ('ACTIVE', 'IDLE', 'FAULTY', 'MAINTENANCE')),
    CONSTRAINT ck_tile_health CHECK (health_pct BETWEEN 0 AND 100)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_tile_status ON tile (status);

-- ---------------------------------------------------------------------
-- 5. BATTERY  - energy storage bank fed by the tiles
-- ---------------------------------------------------------------------
CREATE TABLE battery (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    station_id        BIGINT         NOT NULL,
    code              VARCHAR(20)    NOT NULL,
    name              VARCHAR(80)    NOT NULL,
    chemistry         VARCHAR(30)    NOT NULL DEFAULT 'LiFePO4',
    capacity_wh       DECIMAL(10, 2) NOT NULL,
    charge_wh         DECIMAL(10, 3) NOT NULL,
    max_charge_rate_w DECIMAL(10, 2) NOT NULL,
    temperature_c     DECIMAL(5, 2)  NOT NULL DEFAULT 28.00,
    cycle_count       INT            NOT NULL DEFAULT 0,
    health_pct        DECIMAL(5, 2)  NOT NULL DEFAULT 100.00,
    status            VARCHAR(20)    NOT NULL DEFAULT 'IDLE',
    version           BIGINT         NOT NULL DEFAULT 0,
    created_at        DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_battery PRIMARY KEY (id),
    CONSTRAINT uk_battery_code UNIQUE (code),
    CONSTRAINT fk_battery_station FOREIGN KEY (station_id) REFERENCES station (id) ON DELETE CASCADE,
    CONSTRAINT ck_battery_status CHECK (status IN ('CHARGING', 'DISCHARGING', 'IDLE', 'FULL', 'LOW')),
    CONSTRAINT ck_battery_charge CHECK (charge_wh >= 0 AND charge_wh <= capacity_wh)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 6. READING  - raw live snapshot per simulation tick (station level)
--    High-volume table: pruned to the last 24h by the backend.
-- ---------------------------------------------------------------------
CREATE TABLE reading (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    station_id    BIGINT         NOT NULL,
    recorded_at   DATETIME(3)    NOT NULL,
    steps         INT            NOT NULL COMMENT 'Steps in this tick',
    energy_j      DECIMAL(12, 3) NOT NULL COMMENT 'Energy harvested in this tick',
    power_w       DECIMAL(10, 3) NOT NULL COMMENT 'Average power during this tick',
    battery_pct   DECIMAL(5, 2)  NOT NULL,
    active_tiles  INT            NOT NULL,
    CONSTRAINT pk_reading PRIMARY KEY (id),
    CONSTRAINT fk_reading_station FOREIGN KEY (station_id) REFERENCES station (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_reading_station_time ON reading (station_id, recorded_at);

-- ---------------------------------------------------------------------
-- 7. ZONE_READING  - per-zone snapshot (crowd density & energy by zone)
-- ---------------------------------------------------------------------
CREATE TABLE zone_reading (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    zone_id         BIGINT         NOT NULL,
    recorded_at     DATETIME(3)    NOT NULL,
    steps           INT            NOT NULL,
    energy_j        DECIMAL(12, 3) NOT NULL,
    occupancy       INT            NOT NULL COMMENT 'Estimated people in zone',
    density_pct     DECIMAL(5, 2)  NOT NULL COMMENT 'occupancy / max_capacity * 100',
    CONSTRAINT pk_zone_reading PRIMARY KEY (id),
    CONSTRAINT fk_zone_reading_zone FOREIGN KEY (zone_id) REFERENCES zone (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_zone_reading_zone_time ON zone_reading (zone_id, recorded_at);

-- ---------------------------------------------------------------------
-- 8. ENERGY_SUMMARY_HOURLY  - rolled-up history for analytics & reports
-- ---------------------------------------------------------------------
CREATE TABLE energy_summary_hourly (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    station_id    BIGINT         NOT NULL,
    hour_start    DATETIME       NOT NULL,
    steps         BIGINT         NOT NULL,
    energy_j      DECIMAL(16, 3) NOT NULL,
    avg_power_w   DECIMAL(10, 3) NOT NULL,
    peak_power_w  DECIMAL(10, 3) NOT NULL,
    footfall      INT            NOT NULL COMMENT 'Estimated passengers in the hour',
    CONSTRAINT pk_energy_summary_hourly PRIMARY KEY (id),
    CONSTRAINT uk_energy_summary_station_hour UNIQUE (station_id, hour_start),
    CONSTRAINT fk_energy_summary_station FOREIGN KEY (station_id) REFERENCES station (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 9. ALERT  - faults, low battery, overcrowding ...
-- ---------------------------------------------------------------------
CREATE TABLE alert (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    station_id        BIGINT       NOT NULL,
    zone_id           BIGINT       NULL,
    tile_id           BIGINT       NULL,
    alert_type        VARCHAR(30)  NOT NULL,
    severity          VARCHAR(10)  NOT NULL,
    title             VARCHAR(150) NOT NULL,
    message           VARCHAR(500) NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    acknowledged_at   DATETIME(3)  NULL,
    acknowledged_by   BIGINT       NULL,
    resolved_at       DATETIME(3)  NULL,
    resolved_by       BIGINT       NULL,
    CONSTRAINT pk_alert PRIMARY KEY (id),
    CONSTRAINT fk_alert_station FOREIGN KEY (station_id) REFERENCES station (id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_zone FOREIGN KEY (zone_id) REFERENCES zone (id) ON DELETE SET NULL,
    CONSTRAINT fk_alert_tile FOREIGN KEY (tile_id) REFERENCES tile (id) ON DELETE SET NULL,
    CONSTRAINT fk_alert_ack_user FOREIGN KEY (acknowledged_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_alert_resolve_user FOREIGN KEY (resolved_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_alert_type CHECK (alert_type IN (
        'TILE_FAULT', 'TILE_DEGRADED', 'BATTERY_LOW', 'BATTERY_FULL', 'BATTERY_OVERHEAT',
        'ZONE_OVERCROWDED', 'GATEWAY_OFFLINE', 'SYSTEM')),
    CONSTRAINT ck_alert_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT ck_alert_status CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_alert_station_status_time ON alert (station_id, status, created_at);
