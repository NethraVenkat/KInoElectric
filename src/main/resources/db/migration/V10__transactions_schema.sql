-- =====================================================================
--  PadaShakti - V10 : Transaction flows
--  Purchase Orders -> Vendor Bills -> Vendor Payments
--  Sales Orders (energy export) -> Customer Invoices -> Customer Receipts
-- =====================================================================

-- ---------------------------------------------------------------------
-- DOCUMENT_SEQUENCE - numbering for documents that are not journal entries (PO, SO)
-- ---------------------------------------------------------------------
CREATE TABLE document_sequence (
    code         VARCHAR(10) NOT NULL,
    prefix       VARCHAR(10) NOT NULL,
    seq_year     INT         NOT NULL,
    next_number  INT         NOT NULL DEFAULT 1,
    CONSTRAINT pk_document_sequence PRIMARY KEY (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO document_sequence (code, prefix, seq_year, next_number) VALUES
('PO', 'PO', YEAR(CURDATE()), 1),
('SO', 'SO', YEAR(CURDATE()), 1);

-- ---------------------------------------------------------------------
-- PURCHASE_ORDER - replacement tiles / transducer overhauls from tile vendors
-- ---------------------------------------------------------------------
CREATE TABLE purchase_order (
    id                   BIGINT         NOT NULL AUTO_INCREMENT,
    po_number            VARCHAR(30)    NULL COMMENT 'Assigned on confirmation, e.g. PO/2026/0001',
    vendor_id            BIGINT         NOT NULL,
    order_date           DATE           NOT NULL,
    expected_date        DATE           NULL,
    status               VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    analytic_account_id  BIGINT         NULL COMMENT 'Default station zone for all lines',
    reference            VARCHAR(100)   NULL COMMENT 'e.g. tile code or alert being fixed',
    notes                VARCHAR(500)   NULL,
    untaxed_amount       DECIMAL(16, 2) NOT NULL DEFAULT 0,
    tax_amount           DECIMAL(16, 2) NOT NULL DEFAULT 0,
    total_amount         DECIMAL(16, 2) NOT NULL DEFAULT 0,
    created_by           BIGINT         NULL,
    confirmed_at         DATETIME(3)    NULL,
    confirmed_by         BIGINT         NULL,
    version              BIGINT         NOT NULL DEFAULT 0,
    created_at           DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at           DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_purchase_order PRIMARY KEY (id),
    CONSTRAINT uk_purchase_order_number UNIQUE (po_number),
    CONSTRAINT fk_po_vendor FOREIGN KEY (vendor_id) REFERENCES contact (id),
    CONSTRAINT fk_po_analytic FOREIGN KEY (analytic_account_id) REFERENCES analytic_account (id),
    CONSTRAINT fk_po_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_po_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_po_status CHECK (status IN ('DRAFT', 'CONFIRMED', 'INVOICED', 'CANCELLED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_po_vendor_status ON purchase_order (vendor_id, status);

CREATE TABLE purchase_order_line (
    id                   BIGINT         NOT NULL AUTO_INCREMENT,
    purchase_order_id    BIGINT         NOT NULL,
    line_no              INT            NOT NULL,
    product_id           BIGINT         NOT NULL,
    description          VARCHAR(255)   NOT NULL,
    tile_id              BIGINT         NULL COMMENT 'Tile being replaced / overhauled',
    analytic_account_id  BIGINT         NULL,
    quantity             DECIMAL(12, 3) NOT NULL,
    unit_price           DECIMAL(14, 4) NOT NULL,
    gst_rate             DECIMAL(5, 2)  NOT NULL,
    subtotal             DECIMAL(16, 2) NOT NULL,
    tax_amount           DECIMAL(16, 2) NOT NULL,
    total                DECIMAL(16, 2) NOT NULL,
    CONSTRAINT pk_purchase_order_line PRIMARY KEY (id),
    CONSTRAINT fk_pol_order FOREIGN KEY (purchase_order_id) REFERENCES purchase_order (id) ON DELETE CASCADE,
    CONSTRAINT fk_pol_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_pol_tile FOREIGN KEY (tile_id) REFERENCES tile (id) ON DELETE SET NULL,
    CONSTRAINT fk_pol_analytic FOREIGN KEY (analytic_account_id) REFERENCES analytic_account (id),
    CONSTRAINT ck_pol_amounts CHECK (quantity > 0 AND unit_price >= 0 AND gst_rate >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- SALES_ORDER - monthly energy export to the regional grid operator
-- ---------------------------------------------------------------------
CREATE TABLE sales_order (
    id                   BIGINT         NOT NULL AUTO_INCREMENT,
    so_number            VARCHAR(30)    NULL COMMENT 'Assigned on confirmation, e.g. SO/2026/0001',
    customer_id          BIGINT         NOT NULL,
    order_date           DATE           NOT NULL,
    station_id           BIGINT         NULL COMMENT 'Set for energy export orders',
    period_start         DATE           NULL COMMENT 'Billing period of exported energy',
    period_end           DATE           NULL,
    status               VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    reference            VARCHAR(100)   NULL,
    notes                VARCHAR(500)   NULL,
    untaxed_amount       DECIMAL(16, 2) NOT NULL DEFAULT 0,
    tax_amount           DECIMAL(16, 2) NOT NULL DEFAULT 0,
    total_amount         DECIMAL(16, 2) NOT NULL DEFAULT 0,
    created_by           BIGINT         NULL,
    confirmed_at         DATETIME(3)    NULL,
    confirmed_by         BIGINT         NULL,
    version              BIGINT         NOT NULL DEFAULT 0,
    created_at           DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at           DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_sales_order PRIMARY KEY (id),
    CONSTRAINT uk_sales_order_number UNIQUE (so_number),
    CONSTRAINT fk_so_customer FOREIGN KEY (customer_id) REFERENCES contact (id),
    CONSTRAINT fk_so_station FOREIGN KEY (station_id) REFERENCES station (id),
    CONSTRAINT fk_so_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_so_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_so_status CHECK (status IN ('DRAFT', 'CONFIRMED', 'INVOICED', 'CANCELLED')),
    CONSTRAINT ck_so_period CHECK (period_start IS NULL OR period_end IS NULL OR period_start <= period_end)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_so_customer_status ON sales_order (customer_id, status);
CREATE INDEX idx_so_station_period ON sales_order (station_id, period_start, period_end);

CREATE TABLE sales_order_line (
    id                   BIGINT         NOT NULL AUTO_INCREMENT,
    sales_order_id       BIGINT         NOT NULL,
    line_no              INT            NOT NULL,
    product_id           BIGINT         NOT NULL,
    description          VARCHAR(255)   NOT NULL,
    analytic_account_id  BIGINT         NULL COMMENT 'Floor sector that generated the energy',
    quantity             DECIMAL(12, 3) NOT NULL,
    unit_price           DECIMAL(14, 4) NOT NULL,
    gst_rate             DECIMAL(5, 2)  NOT NULL,
    subtotal             DECIMAL(16, 2) NOT NULL,
    tax_amount           DECIMAL(16, 2) NOT NULL,
    total                DECIMAL(16, 2) NOT NULL,
    CONSTRAINT pk_sales_order_line PRIMARY KEY (id),
    CONSTRAINT fk_sol_order FOREIGN KEY (sales_order_id) REFERENCES sales_order (id) ON DELETE CASCADE,
    CONSTRAINT fk_sol_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_sol_analytic FOREIGN KEY (analytic_account_id) REFERENCES analytic_account (id),
    CONSTRAINT ck_sol_amounts CHECK (quantity > 0 AND unit_price >= 0 AND gst_rate >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- INVOICE - customer invoices and vendor bills
-- ---------------------------------------------------------------------
CREATE TABLE invoice (
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    invoice_type        VARCHAR(20)    NOT NULL,
    invoice_number      VARCHAR(30)    NULL COMMENT 'Journal entry number once posted (INV/..., BILL/...)',
    contact_id          BIGINT         NOT NULL,
    invoice_date        DATE           NOT NULL,
    due_date            DATE           NOT NULL,
    status              VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    payment_state       VARCHAR(20)    NOT NULL DEFAULT 'NOT_PAID',
    sales_order_id      BIGINT         NULL,
    purchase_order_id   BIGINT         NULL,
    vendor_reference    VARCHAR(60)    NULL COMMENT 'Supplier bill number',
    place_of_supply     VARCHAR(2)     NULL COMMENT 'GST state code of the contact',
    untaxed_amount      DECIMAL(16, 2) NOT NULL DEFAULT 0,
    cgst_amount         DECIMAL(16, 2) NOT NULL DEFAULT 0,
    sgst_amount         DECIMAL(16, 2) NOT NULL DEFAULT 0,
    igst_amount         DECIMAL(16, 2) NOT NULL DEFAULT 0,
    tax_amount          DECIMAL(16, 2) NOT NULL DEFAULT 0,
    total_amount        DECIMAL(16, 2) NOT NULL DEFAULT 0,
    amount_paid         DECIMAL(16, 2) NOT NULL DEFAULT 0,
    amount_due          DECIMAL(16, 2) NOT NULL DEFAULT 0,
    journal_entry_id    BIGINT         NULL,
    notes               VARCHAR(500)   NULL,
    created_by          BIGINT         NULL,
    posted_at           DATETIME(3)    NULL,
    posted_by           BIGINT         NULL,
    version             BIGINT         NOT NULL DEFAULT 0,
    created_at          DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_invoice PRIMARY KEY (id),
    CONSTRAINT uk_invoice_number UNIQUE (invoice_number),
    CONSTRAINT fk_invoice_contact FOREIGN KEY (contact_id) REFERENCES contact (id),
    CONSTRAINT fk_invoice_so FOREIGN KEY (sales_order_id) REFERENCES sales_order (id),
    CONSTRAINT fk_invoice_po FOREIGN KEY (purchase_order_id) REFERENCES purchase_order (id),
    CONSTRAINT fk_invoice_entry FOREIGN KEY (journal_entry_id) REFERENCES journal_entry (id),
    CONSTRAINT fk_invoice_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_invoice_posted_by FOREIGN KEY (posted_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_invoice_type CHECK (invoice_type IN ('CUSTOMER_INVOICE', 'VENDOR_BILL')),
    CONSTRAINT ck_invoice_status CHECK (status IN ('DRAFT', 'POSTED', 'CANCELLED')),
    CONSTRAINT ck_invoice_payment_state CHECK (payment_state IN ('NOT_PAID', 'PARTIAL', 'PAID')),
    CONSTRAINT ck_invoice_dates CHECK (due_date >= invoice_date),
    CONSTRAINT ck_invoice_paid CHECK (amount_paid >= 0 AND amount_paid <= total_amount)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_invoice_type_status ON invoice (invoice_type, status, payment_state);
CREATE INDEX idx_invoice_contact ON invoice (contact_id);
CREATE INDEX idx_invoice_due ON invoice (due_date);

CREATE TABLE invoice_line (
    id                   BIGINT         NOT NULL AUTO_INCREMENT,
    invoice_id           BIGINT         NOT NULL,
    line_no              INT            NOT NULL,
    product_id           BIGINT         NULL,
    description          VARCHAR(255)   NOT NULL,
    account_id           BIGINT         NOT NULL COMMENT 'Income (invoice) or expense/asset (bill) account',
    analytic_account_id  BIGINT         NULL,
    quantity             DECIMAL(12, 3) NOT NULL,
    unit_price           DECIMAL(14, 4) NOT NULL,
    gst_rate             DECIMAL(5, 2)  NOT NULL,
    subtotal             DECIMAL(16, 2) NOT NULL,
    tax_amount           DECIMAL(16, 2) NOT NULL,
    total                DECIMAL(16, 2) NOT NULL,
    CONSTRAINT pk_invoice_line PRIMARY KEY (id),
    CONSTRAINT fk_il_invoice FOREIGN KEY (invoice_id) REFERENCES invoice (id) ON DELETE CASCADE,
    CONSTRAINT fk_il_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_il_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT fk_il_analytic FOREIGN KEY (analytic_account_id) REFERENCES analytic_account (id),
    CONSTRAINT ck_il_amounts CHECK (quantity > 0 AND unit_price >= 0 AND gst_rate >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- PAYMENT - customer receipts (INBOUND) and vendor payments (OUTBOUND)
-- ---------------------------------------------------------------------
CREATE TABLE payment (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    payment_number    VARCHAR(30)    NULL COMMENT 'Bank/cash journal entry number',
    payment_type      VARCHAR(10)    NOT NULL,
    contact_id        BIGINT         NOT NULL,
    invoice_id        BIGINT         NOT NULL,
    payment_date      DATE           NOT NULL,
    amount            DECIMAL(16, 2) NOT NULL,
    method            VARCHAR(20)    NOT NULL,
    journal_id        BIGINT         NOT NULL,
    reference         VARCHAR(60)    NULL COMMENT 'UTR / cheque number',
    status            VARCHAR(20)    NOT NULL DEFAULT 'POSTED',
    journal_entry_id  BIGINT         NULL,
    notes             VARCHAR(255)   NULL,
    created_by        BIGINT         NULL,
    cancelled_at      DATETIME(3)    NULL,
    cancelled_by      BIGINT         NULL,
    created_at        DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3)    NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_payment PRIMARY KEY (id),
    CONSTRAINT uk_payment_number UNIQUE (payment_number),
    CONSTRAINT fk_payment_contact FOREIGN KEY (contact_id) REFERENCES contact (id),
    CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id) REFERENCES invoice (id),
    CONSTRAINT fk_payment_journal FOREIGN KEY (journal_id) REFERENCES journal (id),
    CONSTRAINT fk_payment_entry FOREIGN KEY (journal_entry_id) REFERENCES journal_entry (id),
    CONSTRAINT fk_payment_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_payment_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_payment_type CHECK (payment_type IN ('INBOUND', 'OUTBOUND')),
    CONSTRAINT ck_payment_method CHECK (method IN ('BANK_TRANSFER', 'NEFT', 'RTGS', 'UPI', 'CHEQUE', 'CASH')),
    CONSTRAINT ck_payment_status CHECK (status IN ('POSTED', 'CANCELLED')),
    CONSTRAINT ck_payment_amount CHECK (amount > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_payment_invoice ON payment (invoice_id);
CREATE INDEX idx_payment_contact_date ON payment (contact_id, payment_date);
