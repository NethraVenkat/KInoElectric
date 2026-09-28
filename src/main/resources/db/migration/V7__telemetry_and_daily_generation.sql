-- =====================================================================
--  PadaShakti - V7 : Telemetry batches + daily generation log per sector
-- =====================================================================

-- ---------------------------------------------------------------------
-- TELEMETRY_BATCH - one row per POST /api/kinetic/telemetry call
-- batch_ref gives idempotency (a gateway can safely retry).
-- ---------------------------------------------------------------------
CREATE TABLE telemetry_batch (
    id                   BIGINT         NOT NULL AUTO_INCREMENT,
    station_id           BIGINT         NOT NULL,
    batch_ref            VARCHAR(64)    NULL,
    source               VARCHAR(60)    NOT NULL,
    recorded_at          DATETIME(3)    NOT NULL,
    received_at          DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    interval_seconds     INT            NOT NULL,
    sector_count         INT            NOT NULL,
    steps                BIGINT         NOT NULL,
    energy_j             DECIMAL(14, 3) NOT NULL,
    stored_j             DECIMAL(14, 3) NOT NULL COMMENT 'Routed into station batteries',
    exported_j           DECIMAL(14, 3) NOT NULL COMMENT 'Surplus exported to the regional grid',
    submitted_by         BIGINT         NULL,
    CONSTRAINT pk_telemetry_batch PRIMARY KEY (id),
    CONSTRAINT uk_telemetry_batch_ref UNIQUE (batch_ref),
    CONSTRAINT fk_telemetry_batch_station FOREIGN KEY (station_id) REFERENCES station (id) ON DELETE CASCADE,
    CONSTRAINT fk_telemetry_batch_user FOREIGN KEY (submitted_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_telemetry_batch_station_time ON telemetry_batch (station_id, recorded_at);

-- ---------------------------------------------------------------------
-- ENERGY_GENERATION_DAILY - total daily generation per floor sector
-- Source for monthly grid export invoices and energy reports.
-- ---------------------------------------------------------------------
CREATE TABLE energy_generation_daily (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    station_id       BIGINT         NOT NULL,
    zone_id          BIGINT         NOT NULL,
    gen_date         DATE           NOT NULL,
    steps            BIGINT         NOT NULL DEFAULT 0,
    energy_j         DECIMAL(16, 3) NOT NULL DEFAULT 0,
    stored_j         DECIMAL(16, 3) NOT NULL DEFAULT 0,
    exported_j       DECIMAL(16, 3) NOT NULL DEFAULT 0,
    peak_power_w     DECIMAL(10, 3) NOT NULL DEFAULT 0,
    telemetry_count  INT            NOT NULL DEFAULT 0,
    version          BIGINT         NOT NULL DEFAULT 0,
    created_at       DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_energy_generation_daily PRIMARY KEY (id),
    CONSTRAINT uk_generation_zone_date UNIQUE (zone_id, gen_date),
    CONSTRAINT fk_generation_station FOREIGN KEY (station_id) REFERENCES station (id) ON DELETE CASCADE,
    CONSTRAINT fk_generation_zone FOREIGN KEY (zone_id) REFERENCES zone (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_generation_station_date ON energy_generation_daily (station_id, gen_date);

-- ---------- 35 days of history per sector (for monthly invoicing demo) ----------
-- Station: ~110k passengers/day x ~6 tile steps x ~2.8 J effective = ~0.5 kWh/day.
-- Weekends at 70%. Battery stays near the export threshold, so ~70% is exported.
INSERT INTO energy_generation_daily (station_id, zone_id, gen_date, steps, energy_j,
                                     stored_j, exported_j, peak_power_w, telemetry_count)
WITH RECURSIVE days (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM days WHERE n < 35
),
base AS (
    SELECT d.n,
           CURDATE() - INTERVAL d.n DAY AS gen_date,
           z.id AS zone_id,
           z.station_id,
           z.traffic_weight / (SELECT SUM(z2.traffic_weight) FROM zone z2 WHERE z2.station_id = z.station_id) AS share,
           CASE WHEN DAYOFWEEK(CURDATE() - INTERVAL d.n DAY) IN (1, 7) THEN 0.70 ELSE 1.00 END
               * (0.92 + MOD(d.n * 7 + z.id * 3, 17) / 100) AS factor
    FROM days d
             CROSS JOIN zone z
)
SELECT station_id,
       zone_id,
       gen_date,
       ROUND(660000 * share * factor),
       ROUND(660000 * share * factor * 2.8, 3),
       ROUND(660000 * share * factor * 2.8 * 0.30, 3),
       ROUND(660000 * share * factor * 2.8 * 0.70, 3),
       ROUND(660000 * share * factor * 2.8 / 3600 / 3, 3),
       960
FROM base;
