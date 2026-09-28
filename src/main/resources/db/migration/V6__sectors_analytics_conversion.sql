-- =====================================================================
--  PadaShakti - V6 : Analytic accounts (Transit Station Zones)
--                    + kinetic conversion rates per tile technology
-- =====================================================================

-- ---------------------------------------------------------------------
-- ANALYTIC_ACCOUNT - cost/revenue centre per floor sector (budget flow)
-- ---------------------------------------------------------------------
CREATE TABLE analytic_account (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    code         VARCHAR(30)  NOT NULL,
    name         VARCHAR(150) NOT NULL,
    station_id   BIGINT       NOT NULL,
    zone_id      BIGINT       NULL COMMENT 'Floor sector this account tracks (1:1)',
    description  VARCHAR(255) NULL,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_analytic_account PRIMARY KEY (id),
    CONSTRAINT uk_analytic_account_code UNIQUE (code),
    CONSTRAINT uk_analytic_account_zone UNIQUE (zone_id),
    CONSTRAINT fk_analytic_account_station FOREIGN KEY (station_id) REFERENCES station (id) ON DELETE CASCADE,
    CONSTRAINT fk_analytic_account_zone FOREIGN KEY (zone_id) REFERENCES zone (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO analytic_account (code, name, station_id, zone_id, description)
SELECT CONCAT('AA-', s.code, '-', z.code),
       CONCAT(s.name, ' - ', z.name),
       s.id, z.id,
       CONCAT('Transit Station Zone: ', z.name)
FROM zone z
         JOIN station s ON s.id = z.station_id;

-- ---------------------------------------------------------------------
-- KINETIC_CONVERSION_RATE - configured by the Transit Energy Director
-- energy per step (J) = joules_per_step x conversion_efficiency x tile health
-- ---------------------------------------------------------------------
CREATE TABLE kinetic_conversion_rate (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    tile_type             VARCHAR(20)   NOT NULL,
    joules_per_step       DECIMAL(5, 2) NOT NULL COMMENT 'Rated mechanical-to-electrical output per step',
    conversion_efficiency DECIMAL(4, 3) NOT NULL COMMENT 'Power electronics efficiency 0-1',
    description           VARCHAR(255)  NULL,
    updated_by            BIGINT        NULL,
    version               BIGINT        NOT NULL DEFAULT 0,
    created_at            DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at            DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_kinetic_conversion_rate PRIMARY KEY (id),
    CONSTRAINT uk_conversion_rate_type UNIQUE (tile_type),
    CONSTRAINT fk_conversion_rate_user FOREIGN KEY (updated_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_conversion_rate_type CHECK (tile_type IN ('PIEZO', 'ELECTROMAGNETIC')),
    CONSTRAINT ck_conversion_rate_joules CHECK (joules_per_step > 0 AND joules_per_step <= 10),
    CONSTRAINT ck_conversion_rate_eff CHECK (conversion_efficiency > 0 AND conversion_efficiency <= 1)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO kinetic_conversion_rate (tile_type, joules_per_step, conversion_efficiency, description)
VALUES ('PIEZO',           2.50, 0.900, 'Piezoelectric crystal stack - durable, lower output'),
       ('ELECTROMAGNETIC', 4.00, 0.920, 'Electromagnetic micro-generator - higher output per step');
