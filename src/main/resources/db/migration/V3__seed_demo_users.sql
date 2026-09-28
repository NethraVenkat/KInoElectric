-- =====================================================================
--  PadaShakti - V3 : Demo users (BCrypt hashed passwords)
--  admin    / Admin@123     -> ADMIN     (full access)
--  operator / Operator@123  -> OPERATOR  (station control, simulation)
--  engineer / Engineer@123  -> ENGINEER  (tiles, maintenance)
--  Change these passwords before any real deployment.
-- =====================================================================
INSERT INTO users (username, email, password_hash, full_name, role, enabled)
VALUES ('admin',    'admin@padashakti.in',
        '$2a$10$k.W0X5fREIKfZPPZmxBR3uGNC8HzX1ulQmRrl2h.VKMqCdMDW6Yj2', 'System Administrator', 'ADMIN',    TRUE),
       ('operator', 'operator@padashakti.in',
        '$2a$10$/Uo69kcZ4Q3xBnUGpnERzeSCEoAeLbu57iGMa4vCjYH6rBucBl/wm', 'Station Operator',     'OPERATOR', TRUE),
       ('engineer', 'engineer@padashakti.in',
        '$2a$10$B6Gj/Rvv/Rm14Q.b5hBch.yjy/Ed04nXoqpZyL4AYScSvx5I27zb.', 'Maintenance Engineer', 'ENGINEER', TRUE);
