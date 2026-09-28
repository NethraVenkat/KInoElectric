-- =====================================================================
--  PadaShakti - V4 : 7 days of hourly history + starting alerts
--  Gives the analytics charts real data from the first run.
-- =====================================================================

SET @station_id = (SELECT id FROM station WHERE code = 'CHC');

-- ---------- Hourly energy history (last 7 days = 168 hours) ----------
-- Footfall follows a metro day curve: night low, rush 8-10 AM and 5-8 PM.
-- Weekends run at 70%.  ~6 tile-steps per passenger, ~3.1 J per step.
INSERT INTO energy_summary_hourly (station_id, hour_start, steps, energy_j,
                                   avg_power_w, peak_power_w, footfall)
WITH RECURSIVE hours (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM hours WHERE n < 168
),
base AS (
    SELECT n,
           DATE_FORMAT(NOW() - INTERVAL n HOUR, '%Y-%m-%d %H:00:00') AS hour_start,
           HOUR(NOW() - INTERVAL n HOUR) AS h,
           DAYOFWEEK(NOW() - INTERVAL n HOUR) AS dow
    FROM hours
),
curve AS (
    SELECT n,
           hour_start,
           ELT(h + 1,
               0.02, 0.01, 0.01, 0.01, 0.03, 0.10, 0.35, 0.75,
               1.00, 0.90, 0.55, 0.45, 0.50, 0.45, 0.40, 0.45,
               0.60, 0.85, 1.00, 0.80, 0.50, 0.30, 0.15, 0.05)
               * CASE WHEN dow IN (1, 7) THEN 0.70 ELSE 1.00 END
               * (0.90 + MOD(n * 7, 20) / 100) AS factor
    FROM base
)
SELECT @station_id,
       hour_start,
       ROUND(12000 * factor * 6),                         -- tile steps
       ROUND(12000 * factor * 6 * 3.1, 3),                -- joules
       ROUND(12000 * factor * 6 * 3.1 / 3600, 3),         -- avg W over the hour
       ROUND(12000 * factor * 6 * 3.1 / 3600 * 1.6, 3),   -- peak W
       ROUND(12000 * factor)                              -- passengers
FROM curve;

-- ---------- Starting alerts ----------
INSERT INTO alert (station_id, zone_id, tile_id, alert_type, severity, title, message, status, created_at)
SELECT @station_id, t.zone_id, t.id, 'TILE_FAULT', 'CRITICAL',
       CONCAT('Tile ', t.tile_code, ' not generating'),
       CONCAT('Tile ', t.tile_code, ' output dropped to ', t.health_pct,
              '% of expected. Inspect piezo stack and wiring.'),
       'OPEN', NOW(3) - INTERVAL 40 MINUTE
FROM tile t
WHERE t.tile_code IN ('CHC-PF1-R2C07', 'CHC-CON-R3C04');

INSERT INTO alert (station_id, zone_id, tile_id, alert_type, severity, title, message, status, created_at)
SELECT @station_id, t.zone_id, t.id, 'TILE_DEGRADED', 'WARNING',
       CONCAT('Tile ', t.tile_code, ' degrading'),
       CONCAT('Tile ', t.tile_code, ' health at ', t.health_pct, '%. Schedule maintenance.'),
       'OPEN', NOW(3) - INTERVAL 2 HOUR
FROM tile t
WHERE t.tile_code IN ('CHC-ENT-R2C03', 'CHC-PF2-R3C11');

INSERT INTO alert (station_id, zone_id, alert_type, severity, title, message, status,
                   created_at, acknowledged_at, acknowledged_by, resolved_at, resolved_by)
SELECT @station_id, z.id, 'ZONE_OVERCROWDED', 'WARNING',
       'Platform 1 crowd density high',
       'Platform 1 density reached 92% during morning rush. Crowd cleared after train arrival.',
       'RESOLVED',
       NOW(3) - INTERVAL 1 DAY,
       NOW(3) - INTERVAL 1 DAY + INTERVAL 3 MINUTE, u.id,
       NOW(3) - INTERVAL 1 DAY + INTERVAL 12 MINUTE, u.id
FROM zone z
         JOIN users u ON u.username = 'operator'
WHERE z.station_id = @station_id AND z.code = 'PF1';

INSERT INTO alert (station_id, alert_type, severity, title, message, status, created_at)
VALUES (@station_id, 'SYSTEM', 'INFO', 'PadaShakti network online',
        'All zone gateways connected. 190 tiles registered.', 'OPEN', NOW(3) - INTERVAL 3 HOUR);
