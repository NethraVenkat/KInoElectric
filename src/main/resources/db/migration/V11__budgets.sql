-- =====================================================================
--  PadaShakti - V11 : Budgets per transit station zone (analytic account)
--  A budget plans revenue (income accounts) and cost (expense accounts)
--  for each zone over a period; actuals come from posted journal lines.
-- =====================================================================

CREATE TABLE budget (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(30)  NOT NULL,
    name       VARCHAR(150) NOT NULL,
    date_from  DATE         NOT NULL,
    date_to    DATE         NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    notes      VARCHAR(500) NULL,
    created_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    CONSTRAINT uk_budget_code UNIQUE (code),
    CONSTRAINT chk_budget_status CHECK (status IN ('DRAFT', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT chk_budget_dates CHECK (date_to >= date_from)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE budget_line (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    budget_id           BIGINT        NOT NULL,
    analytic_account_id BIGINT        NOT NULL,
    account_id          BIGINT        NOT NULL,
    planned_amount      DECIMAL(16,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_budget_line UNIQUE (budget_id, analytic_account_id, account_id),
    CONSTRAINT chk_budget_line_amount CHECK (planned_amount >= 0),
    CONSTRAINT fk_budget_line_budget FOREIGN KEY (budget_id) REFERENCES budget (id) ON DELETE CASCADE,
    CONSTRAINT fk_budget_line_analytic FOREIGN KEY (analytic_account_id) REFERENCES analytic_account (id),
    CONSTRAINT fk_budget_line_account FOREIGN KEY (account_id) REFERENCES account (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_journal_item_analytic ON journal_item (analytic_account_id, account_id);

-- ---------- Demo budget: FY 2026-27 for every Chennai Central zone ----------
INSERT INTO budget (code, name, date_from, date_to, status, notes)
VALUES ('BUD-FY2026-27', 'Chennai Central FY 2026-27 Kinetic Budget', '2026-04-01', '2027-03-31', 'CONFIRMED',
        'Planned green-energy revenue vs tile maintenance cost per station zone.');

SET @budget_id = LAST_INSERT_ID();

-- revenue: harvested power sales (4100), weighted by zone traffic
INSERT INTO budget_line (budget_id, analytic_account_id, account_id, planned_amount)
SELECT @budget_id, aa.id, (SELECT id FROM account WHERE code = '4100'), ROUND(60000 * z.traffic_weight, 2)
FROM analytic_account aa JOIN zone z ON z.id = aa.zone_id
WHERE aa.code LIKE 'AA-CHC-%';

-- cost: piezoelectric transducer repairs (5100)
INSERT INTO budget_line (budget_id, analytic_account_id, account_id, planned_amount)
SELECT @budget_id, aa.id, (SELECT id FROM account WHERE code = '5100'), ROUND(25000 * z.traffic_weight, 2)
FROM analytic_account aa JOIN zone z ON z.id = aa.zone_id
WHERE aa.code LIKE 'AA-CHC-%';
