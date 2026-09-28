-- =====================================================================
--  PadaShakti - V8 : Accounting schema
--  Chart of Accounts, Journals, Journal Entries (double entry),
--  Contacts (customers / vendors), Products
-- =====================================================================

-- ---------------------------------------------------------------------
-- ACCOUNT - Chart of Accounts (tree via parent_id; group accounts cannot be posted to)
-- ---------------------------------------------------------------------
CREATE TABLE account (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    code          VARCHAR(20)  NOT NULL,
    name          VARCHAR(120) NOT NULL,
    account_type  VARCHAR(20)  NOT NULL,
    sub_type      VARCHAR(30)  NOT NULL,
    parent_id     BIGINT       NULL,
    is_group      BOOLEAN      NOT NULL DEFAULT FALSE,
    reconcilable  BOOLEAN      NOT NULL DEFAULT FALSE,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    description   VARCHAR(255) NULL,
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_account PRIMARY KEY (id),
    CONSTRAINT uk_account_code UNIQUE (code),
    CONSTRAINT fk_account_parent FOREIGN KEY (parent_id) REFERENCES account (id),
    CONSTRAINT ck_account_type CHECK (account_type IN ('ASSET', 'LIABILITY', 'EQUITY', 'INCOME', 'EXPENSE')),
    CONSTRAINT ck_account_sub_type CHECK (sub_type IN (
        'GROUP', 'FIXED_ASSET', 'CURRENT_ASSET', 'RECEIVABLE', 'BANK', 'CASH', 'TAX_INPUT',
        'PAYABLE', 'CURRENT_LIABILITY', 'TAX_OUTPUT', 'EQUITY', 'RETAINED_EARNINGS',
        'OPERATING_REVENUE', 'OTHER_INCOME', 'OPERATING_EXPENSE', 'OTHER_EXPENSE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- JOURNAL - Sales, Purchase, Cash, Bank, Miscellaneous (with numbering)
-- ---------------------------------------------------------------------
CREATE TABLE journal (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    code                VARCHAR(10)  NOT NULL,
    name                VARCHAR(80)  NOT NULL,
    journal_type        VARCHAR(20)  NOT NULL,
    default_account_id  BIGINT       NULL,
    sequence_prefix     VARCHAR(10)  NOT NULL,
    sequence_year       INT          NOT NULL,
    next_number         INT          NOT NULL DEFAULT 1,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_journal PRIMARY KEY (id),
    CONSTRAINT uk_journal_code UNIQUE (code),
    CONSTRAINT uk_journal_prefix UNIQUE (sequence_prefix),
    CONSTRAINT fk_journal_default_account FOREIGN KEY (default_account_id) REFERENCES account (id),
    CONSTRAINT ck_journal_type CHECK (journal_type IN ('SALE', 'PURCHASE', 'CASH', 'BANK', 'GENERAL'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- CONTACT - Contact master (Regional Power Grid = customer, Tile Manufacturer = vendor)
-- ---------------------------------------------------------------------
CREATE TABLE contact (
    id                     BIGINT         NOT NULL AUTO_INCREMENT,
    code                   VARCHAR(20)    NOT NULL,
    contact_type           VARCHAR(10)    NOT NULL,
    name                   VARCHAR(150)   NOT NULL,
    gstin                  VARCHAR(15)    NULL,
    pan                    VARCHAR(10)    NULL,
    email                  VARCHAR(120)   NULL,
    phone                  VARCHAR(20)    NULL,
    contact_person         VARCHAR(100)   NULL,
    address_line           VARCHAR(255)   NULL,
    city                   VARCHAR(60)    NULL,
    state                  VARCHAR(60)    NULL,
    pincode                VARCHAR(10)    NULL,
    country                VARCHAR(60)    NOT NULL DEFAULT 'India',
    payment_terms_days     INT            NOT NULL DEFAULT 30,
    credit_limit           DECIMAL(16, 2) NULL,
    receivable_account_id  BIGINT         NULL,
    payable_account_id     BIGINT         NULL,
    active                 BOOLEAN        NOT NULL DEFAULT TRUE,
    notes                  VARCHAR(500)   NULL,
    created_at             DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at             DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_contact PRIMARY KEY (id),
    CONSTRAINT uk_contact_code UNIQUE (code),
    CONSTRAINT uk_contact_gstin UNIQUE (gstin),
    CONSTRAINT fk_contact_receivable FOREIGN KEY (receivable_account_id) REFERENCES account (id),
    CONSTRAINT fk_contact_payable FOREIGN KEY (payable_account_id) REFERENCES account (id),
    CONSTRAINT ck_contact_type CHECK (contact_type IN ('CUSTOMER', 'VENDOR', 'BOTH')),
    CONSTRAINT ck_contact_terms CHECK (payment_terms_days BETWEEN 0 AND 365)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_contact_type_name ON contact (contact_type, name);

-- ---------------------------------------------------------------------
-- PRODUCT - Product master (kWh goods, tiles, overhaul services)
-- ---------------------------------------------------------------------
CREATE TABLE product (
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    code                VARCHAR(30)    NOT NULL,
    name                VARCHAR(150)   NOT NULL,
    product_type        VARCHAR(10)    NOT NULL,
    uom                 VARCHAR(10)    NOT NULL,
    hsn_sac             VARCHAR(10)    NULL,
    sale_price          DECIMAL(14, 4) NOT NULL DEFAULT 0,
    purchase_price      DECIMAL(14, 4) NOT NULL DEFAULT 0,
    gst_rate            DECIMAL(5, 2)  NOT NULL DEFAULT 0,
    can_be_sold         BOOLEAN        NOT NULL DEFAULT FALSE,
    can_be_purchased    BOOLEAN        NOT NULL DEFAULT FALSE,
    income_account_id   BIGINT         NULL,
    expense_account_id  BIGINT         NULL,
    description         VARCHAR(500)   NULL,
    active              BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at          DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_product PRIMARY KEY (id),
    CONSTRAINT uk_product_code UNIQUE (code),
    CONSTRAINT fk_product_income FOREIGN KEY (income_account_id) REFERENCES account (id),
    CONSTRAINT fk_product_expense FOREIGN KEY (expense_account_id) REFERENCES account (id),
    CONSTRAINT ck_product_type CHECK (product_type IN ('GOODS', 'SERVICE')),
    CONSTRAINT ck_product_uom CHECK (uom IN ('KWH', 'UNIT', 'JOB', 'HOUR')),
    CONSTRAINT ck_product_prices CHECK (sale_price >= 0 AND purchase_price >= 0),
    CONSTRAINT ck_product_gst CHECK (gst_rate >= 0 AND gst_rate <= 40)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- JOURNAL_ENTRY - header of a double-entry voucher
-- ---------------------------------------------------------------------
CREATE TABLE journal_entry (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    entry_number    VARCHAR(30)    NULL COMMENT 'Assigned when posted, e.g. INV/2026/0001',
    journal_id      BIGINT         NOT NULL,
    entry_date      DATE           NOT NULL,
    reference       VARCHAR(100)   NULL,
    narration       VARCHAR(500)   NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    source_type     VARCHAR(30)    NOT NULL DEFAULT 'MANUAL',
    source_id       BIGINT         NULL,
    total_debit     DECIMAL(16, 2) NOT NULL DEFAULT 0,
    total_credit    DECIMAL(16, 2) NOT NULL DEFAULT 0,
    reversal_of_id  BIGINT         NULL,
    created_by      BIGINT         NULL,
    posted_at       DATETIME(3)    NULL,
    posted_by       BIGINT         NULL,
    cancelled_at    DATETIME(3)    NULL,
    cancelled_by    BIGINT         NULL,
    version         BIGINT         NOT NULL DEFAULT 0,
    created_at      DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_journal_entry PRIMARY KEY (id),
    CONSTRAINT uk_journal_entry_number UNIQUE (entry_number),
    CONSTRAINT fk_entry_journal FOREIGN KEY (journal_id) REFERENCES journal (id),
    CONSTRAINT fk_entry_reversal FOREIGN KEY (reversal_of_id) REFERENCES journal_entry (id),
    CONSTRAINT fk_entry_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_entry_posted_by FOREIGN KEY (posted_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_entry_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_entry_status CHECK (status IN ('DRAFT', 'POSTED', 'REVERSED', 'CANCELLED')),
    CONSTRAINT ck_entry_source CHECK (source_type IN (
        'MANUAL', 'OPENING', 'CUSTOMER_INVOICE', 'VENDOR_BILL', 'PAYMENT', 'REVERSAL', 'SYSTEM')),
    CONSTRAINT ck_entry_balanced CHECK (status <> 'POSTED' OR total_debit = total_credit)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_entry_journal_date ON journal_entry (journal_id, entry_date);
CREATE INDEX idx_entry_status_date ON journal_entry (status, entry_date);
CREATE INDEX idx_entry_source ON journal_entry (source_type, source_id);

-- ---------------------------------------------------------------------
-- JOURNAL_ITEM - one debit or credit line
-- ---------------------------------------------------------------------
CREATE TABLE journal_item (
    id                   BIGINT         NOT NULL AUTO_INCREMENT,
    entry_id             BIGINT         NOT NULL,
    line_no              INT            NOT NULL,
    account_id           BIGINT         NOT NULL,
    contact_id           BIGINT         NULL,
    analytic_account_id  BIGINT         NULL,
    label                VARCHAR(255)   NULL,
    debit                DECIMAL(16, 2) NOT NULL DEFAULT 0,
    credit               DECIMAL(16, 2) NOT NULL DEFAULT 0,
    CONSTRAINT pk_journal_item PRIMARY KEY (id),
    CONSTRAINT fk_item_entry FOREIGN KEY (entry_id) REFERENCES journal_entry (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT fk_item_contact FOREIGN KEY (contact_id) REFERENCES contact (id),
    CONSTRAINT fk_item_analytic FOREIGN KEY (analytic_account_id) REFERENCES analytic_account (id),
    CONSTRAINT ck_item_amounts CHECK (debit >= 0 AND credit >= 0 AND (debit = 0 OR credit = 0) AND (debit + credit) > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_item_account ON journal_item (account_id);
CREATE INDEX idx_item_contact ON journal_item (contact_id);
CREATE INDEX idx_item_analytic ON journal_item (analytic_account_id);
