-- =====================================================================
--  PadaShakti - V2 : Reference data
--  1 station, 6 zones, 190 kinetic tiles, 1 battery bank
-- =====================================================================

-- ---------- Station ----------
INSERT INTO station (code, name, city, line_name, latitude, longitude,
                     daily_footfall_estimate, layout_width, layout_height, status)
VALUES ('CHC', 'Chennai Central Metro', 'Chennai', 'Blue Line / Green Line (Interchange)',
        13.081800, 80.275600, 110000, 1000, 740, 'ONLINE');

SET @station_id = LAST_INSERT_ID();

-- ---------- Zones (floor-map layout on a 1000 x 740 canvas) ----------
--  code  name                     type        weight cap   x    y    w    h   rows cols order
INSERT INTO zone (station_id, code, name, zone_type, traffic_weight, max_capacity,
                  pos_x, pos_y, width, height, grid_rows, grid_cols, display_order)
VALUES (@station_id, 'ENT', 'Entry Gates',          'ENTRY',     1.00, 350,  20,  20, 300, 150, 4,  8, 1),
       (@station_id, 'STR', 'Stairs & Escalators',  'STAIRS',    0.85, 200, 340,  20, 200, 150, 3,  6, 2),
       (@station_id, 'CON', 'Concourse',            'CONCOURSE', 0.70, 600, 560,  20, 420, 150, 4, 10, 3),
       (@station_id, 'PF1', 'Platform 1 (Blue Line)',  'PLATFORM', 0.90, 900,  20, 200, 960, 160, 3, 14, 4),
       (@station_id, 'PF2', 'Platform 2 (Green Line)', 'PLATFORM', 0.80, 900,  20, 390, 960, 160, 3, 14, 5),
       (@station_id, 'EXT', 'Exit Gates',           'EXIT',      0.75, 300,  20, 580, 400, 140, 2,  8, 6);

-- ---------- Tiles ----------
-- One tile per (row, col) cell of every zone grid.
-- Tile centre = zone origin + padding + (index + 0.5) * cell size.
-- Every 3rd cell (diagonal pattern) is an ELECTROMAGNETIC tile (higher output),
-- the rest are PIEZO tiles.
INSERT INTO tile (zone_id, tile_code, tile_type, status, grid_row, grid_col, pos_x, pos_y,
                  rated_joules_per_step, health_pct, installed_on, last_maintenance_on)
SELECT z.id,
       CONCAT('CHC-', z.code, '-R', r.n, 'C', LPAD(c.n, 2, '0')),
       CASE WHEN MOD(r.n + c.n, 3) = 0 THEN 'ELECTROMAGNETIC' ELSE 'PIEZO' END,
       'ACTIVE',
       r.n,
       c.n,
       ROUND(z.pos_x + 10 + (c.n - 0.5) * ((z.width - 20) / z.grid_cols), 2),
       ROUND(z.pos_y + 30 + (r.n - 0.5) * ((z.height - 40) / z.grid_rows), 2),
       CASE WHEN MOD(r.n + c.n, 3) = 0 THEN 4.00 ELSE 2.50 END,
       100 - MOD(r.n * 7 + c.n * 13, 9),
       '2026-06-15',
       '2026-09-01'
FROM zone z
         JOIN (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) r
              ON r.n <= z.grid_rows
         JOIN (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
               UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
               UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
               UNION ALL SELECT 13 UNION ALL SELECT 14) c
              ON c.n <= z.grid_cols
WHERE z.station_id = @station_id;

-- ---------- Realistic starting conditions ----------
-- 2 faulty tiles, 3 degraded tiles, 1 under maintenance
UPDATE tile SET status = 'FAULTY', health_pct = 18.00
WHERE tile_code IN ('CHC-PF1-R2C07', 'CHC-CON-R3C04');

UPDATE tile SET health_pct = 64.00
WHERE tile_code IN ('CHC-ENT-R2C03', 'CHC-STR-R1C05', 'CHC-PF2-R3C11');

UPDATE tile SET status = 'MAINTENANCE', health_pct = 55.00, last_maintenance_on = '2026-09-25'
WHERE tile_code = 'CHC-EXT-R1C02';

-- ---------- Battery bank ----------
INSERT INTO battery (station_id, code, name, chemistry, capacity_wh, charge_wh,
                     max_charge_rate_w, temperature_c, cycle_count, health_pct, status)
VALUES (@station_id, 'CHC-BAT-01', 'Main Storage Bank', 'LiFePO4',
        2000.00, 1200.000, 500.00, 29.50, 42, 98.50, 'IDLE');
