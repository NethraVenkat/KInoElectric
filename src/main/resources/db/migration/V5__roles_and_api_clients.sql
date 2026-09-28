-- =====================================================================
--  PadaShakti - V5 : Use-case roles + API clients for the System actor
--  ADMIN      = Transit Energy Director
--  ACCOUNTANT = Invoicing User (Transit Accountant)
--  SYSTEM     = Kinetic telemetry system (devices / gateways / simulator)
-- =====================================================================

ALTER TABLE users DROP CHECK ck_users_role;

UPDATE users
SET username      = 'accountant',
    email         = 'accountant@padashakti.in',
    full_name     = 'Transit Accountant',
    role          = 'ACCOUNTANT',
    password_hash = '$2a$10$EA5cfyGdgewRyydXsJGM7em47034gaTnpRoEwfQ7V2DZxODzFHA7m'   -- Accountant@123
WHERE username = 'operator';

UPDATE users
SET username      = 'system',
    email         = 'system@padashakti.in',
    full_name     = 'Kinetic Telemetry System',
    role          = 'SYSTEM',
    password_hash = '$2a$10$1.xd.avlfkz.e82AEC8xP.MxImGooe3IggtCgO5KmJy1Z078TcuH.'   -- System@123
WHERE username = 'engineer';

UPDATE users SET full_name = 'Transit Energy Director' WHERE username = 'admin';

-- Safety net for any other legacy accounts
UPDATE users SET role = 'ACCOUNTANT' WHERE role IN ('OPERATOR', 'ENGINEER');

ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'ACCOUNTANT', 'SYSTEM'));

-- ---------------------------------------------------------------------
-- API_CLIENT - machine credentials (X-API-KEY header) for telemetry
-- Only the SHA-256 hash of the key is stored. The key acts as its user.
-- ---------------------------------------------------------------------
CREATE TABLE api_client (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(80)  NOT NULL,
    key_prefix    VARCHAR(12)  NOT NULL COMMENT 'First characters of the key, for identification',
    key_hash      VARCHAR(64)  NOT NULL COMMENT 'SHA-256 hex of the full key',
    user_id       BIGINT       NOT NULL COMMENT 'Account the key acts as (role SYSTEM)',
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    expires_at    DATETIME(3)  NULL,
    last_used_at  DATETIME(3)  NULL,
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_api_client PRIMARY KEY (id),
    CONSTRAINT uk_api_client_hash UNIQUE (key_hash),
    CONSTRAINT fk_api_client_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Demo gateway key (dev only):  pdk_sys_ChcKineticTelemetry2026Demo
INSERT INTO api_client (name, key_prefix, key_hash, user_id, enabled)
SELECT 'Chennai Central Tile Gateway', 'pdk_sys_',
       '5eef71897b05f383638bc0532a07a8a2cf3168fe065dc1647ee3d138f3df5091', id, TRUE
FROM users WHERE username = 'system';
