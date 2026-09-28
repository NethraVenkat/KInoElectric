-- =====================================================================
--  PadaShakti - V9 : Chart of Accounts, journals, contacts, products,
--                    opening balance entry (FY 2026-27 starts 1 April 2026)
--  Contacts and GSTINs are fictional demo data.
-- =====================================================================

-- ---------- Group accounts ----------
INSERT INTO account (code, name, account_type, sub_type, is_group, description) VALUES
('1000', 'Assets',      'ASSET',     'GROUP', TRUE, 'Everything the transit energy network owns'),
('2000', 'Liabilities', 'LIABILITY', 'GROUP', TRUE, 'Amounts owed to vendors and tax authorities'),
('3000', 'Equity',      'EQUITY',    'GROUP', TRUE, 'Transit authority funding and retained results'),
('4000', 'Income',      'INCOME',    'GROUP', TRUE, 'Energy sales and subsidies'),
('5000', 'Expenses',    'EXPENSE',   'GROUP', TRUE, 'Tile and inverter maintenance costs');

SET @a = (SELECT id FROM account WHERE code = '1000');
SET @l = (SELECT id FROM account WHERE code = '2000');
SET @e = (SELECT id FROM account WHERE code = '3000');
SET @i = (SELECT id FROM account WHERE code = '4000');
SET @x = (SELECT id FROM account WHERE code = '5000');

-- ---------- Posting accounts (use case CoA + required control accounts) ----------
INSERT INTO account (code, name, account_type, sub_type, parent_id, reconcilable, description) VALUES
('1100', 'Kinetic Floor Grid Assets',          'ASSET',     'FIXED_ASSET',       @a, FALSE, 'Installed kinetic floor tiles'),
('1200', 'Battery Storage Banks',              'ASSET',     'FIXED_ASSET',       @a, FALSE, 'Station energy storage banks'),
('1300', 'Accounts Receivable - Power Grid',   'ASSET',     'RECEIVABLE',        @a, TRUE,  'Amounts due from regional grid operators'),
('1400', 'GST Input Credit',                   'ASSET',     'TAX_INPUT',         @a, FALSE, 'GST paid on purchases'),
('1500', 'Bank - Current Account',             'ASSET',     'BANK',              @a, TRUE,  'Transit authority current account'),
('1510', 'Cash in Hand',                       'ASSET',     'CASH',              @a, FALSE, 'Petty cash'),
('2100', 'Tile Vendor Creditors',              'LIABILITY', 'PAYABLE',           @l, TRUE,  'Amounts owed to kinetic tile manufacturers'),
('2200', 'Station Maintenance Payables',       'LIABILITY', 'PAYABLE',           @l, TRUE,  'Amounts owed for inverter / station maintenance'),
('2300', 'GST Output Payable',                 'LIABILITY', 'TAX_OUTPUT',        @l, FALSE, 'GST collected on sales'),
('3100', 'Capital - Transit Authority Fund',   'EQUITY',    'EQUITY',            @e, FALSE, 'Funding for the kinetic floor network'),
('3200', 'Retained Earnings',                  'EQUITY',    'RETAINED_EARNINGS', @e, FALSE, 'Accumulated results of previous years'),
('4100', 'Harvested Power Sales Revenue',      'INCOME',    'OPERATING_REVENUE', @i, FALSE, 'Energy exported to the regional grid'),
('4200', 'Sustainability Subsidies',           'INCOME',    'OTHER_INCOME',      @i, FALSE, 'Green energy grants and subsidies'),
('5100', 'Piezoelectric Transducer Repairs',   'EXPENSE',   'OPERATING_EXPENSE', @x, FALSE, 'Tile transducer overhauls and repairs'),
('5200', 'Power Inverter Maintenance',         'EXPENSE',   'OPERATING_EXPENSE', @x, FALSE, 'Inverter and power electronics upkeep');

-- ---------- Journals ----------
INSERT INTO journal (code, name, journal_type, default_account_id, sequence_prefix, sequence_year) VALUES
('SAL',  'Sales Journal',         'SALE',     (SELECT id FROM account WHERE code = '4100'), 'INV',  YEAR(CURDATE())),
('PUR',  'Purchase Journal',      'PURCHASE', (SELECT id FROM account WHERE code = '5100'), 'BILL', YEAR(CURDATE())),
('CSH',  'Cash Journal',          'CASH',     (SELECT id FROM account WHERE code = '1510'), 'CSH',  YEAR(CURDATE())),
('BNK',  'Bank Journal',          'BANK',     (SELECT id FROM account WHERE code = '1500'), 'BNK',  YEAR(CURDATE())),
('MISC', 'Miscellaneous Journal', 'GENERAL',  NULL,                                         'JE',   YEAR(CURDATE()));

-- ---------- Contacts (fictional) ----------
INSERT INTO contact (code, contact_type, name, gstin, pan, email, phone, contact_person, address_line, city, state,
                     pincode, payment_terms_days, credit_limit, receivable_account_id, payable_account_id, notes) VALUES
('CUST-0001', 'CUSTOMER', 'Chennai Regional Power Grid Corporation', '33AABCC1234D1Z5', 'AABCC1234D',
 'billing@crpgc.example.in', '+91-44-2345-6789', 'Grid Settlements Desk', '12 Anna Salai', 'Chennai', 'Tamil Nadu',
 '600002', 30, 500000.00, (SELECT id FROM account WHERE code = '1300'), NULL,
 'Buys surplus kinetic energy exported from station batteries'),
('VEND-0001', 'VENDOR', 'KinetiFloor Technologies Pvt Ltd', '33AAFCK5678P1Z2', 'AAFCK5678P',
 'sales@kinetifloor.example.in', '+91-422-456-7890', 'Accounts Receivable', '45 Avinashi Road', 'Coimbatore',
 'Tamil Nadu', '641018', 30, NULL, NULL, (SELECT id FROM account WHERE code = '2100'),
 'Kinetic tile manufacturer - replacement piezo tiles and transducer overhauls'),
('VEND-0002', 'VENDOR', 'VoltEdge Inverter Services', '33AAGFV9012K1Z8', 'AAGFV9012K',
 'service@voltedge.example.in', '+91-44-2876-5432', 'Service Manager', '8 Guindy Industrial Estate', 'Chennai',
 'Tamil Nadu', '600032', 15, NULL, NULL, (SELECT id FROM account WHERE code = '2200'),
 'Power inverter maintenance contractor');

-- ---------- Products ----------
INSERT INTO product (code, name, product_type, uom, hsn_sac, sale_price, purchase_price, gst_rate,
                     can_be_sold, can_be_purchased, income_account_id, expense_account_id, description) VALUES
('PRD-KWH', 'Harvested Kinetic Electricity', 'GOODS', 'KWH', '27160000', 8.0000, 0, 0, TRUE, FALSE,
 (SELECT id FROM account WHERE code = '4100'), NULL, 'Energy exported to the grid, billed per kWh (GST exempt)'),
('PRD-TILE-PZ', 'Kinetic Floor Tile - Piezo', 'GOODS', 'UNIT', '85414300', 0, 22000.0000, 18, FALSE, TRUE,
 NULL, (SELECT id FROM account WHERE code = '1100'), 'Replacement piezoelectric floor tile (capitalised)'),
('PRD-TILE-EM', 'Kinetic Floor Tile - Electromagnetic', 'GOODS', 'UNIT', '85016100', 0, 32000.0000, 18, FALSE, TRUE,
 NULL, (SELECT id FROM account WHERE code = '1100'), 'Replacement electromagnetic floor tile (capitalised)'),
('SRV-OVH', 'Tile Transducer Overhaul', 'SERVICE', 'JOB', '998719', 0, 3500.0000, 18, FALSE, TRUE,
 NULL, (SELECT id FROM account WHERE code = '5100'), 'Overhaul of a tile transducer stack'),
('SRV-INV', 'Power Inverter Maintenance', 'SERVICE', 'JOB', '998717', 0, 6000.0000, 18, FALSE, TRUE,
 NULL, (SELECT id FROM account WHERE code = '5200'), 'Preventive maintenance of station power inverters');

-- ---------- Opening balance entry (1 April 2026) ----------
-- 190 tiles installed (~Rs 25,000 each), 2 kWh battery bank, bank balance, funded by transit authority capital.
INSERT INTO journal_entry (entry_number, journal_id, entry_date, reference, narration, status, source_type,
                           total_debit, total_credit, created_by, posted_at, posted_by)
VALUES (CONCAT('JE/', YEAR(CURDATE()), '/0001'), (SELECT id FROM journal WHERE code = 'MISC'), '2026-04-01',
        'OPENING-FY26', 'Opening balances - kinetic floor network commissioning', 'POSTED', 'OPENING',
        5430000.00, 5430000.00, (SELECT id FROM users WHERE username = 'admin'), NOW(3),
        (SELECT id FROM users WHERE username = 'admin'));

SET @je = LAST_INSERT_ID();

INSERT INTO journal_item (entry_id, line_no, account_id, label, debit, credit) VALUES
(@je, 1, (SELECT id FROM account WHERE code = '1100'), '190 kinetic floor tiles installed',  4750000.00, 0),
(@je, 2, (SELECT id FROM account WHERE code = '1200'), 'Main battery storage bank',           180000.00, 0),
(@je, 3, (SELECT id FROM account WHERE code = '1500'), 'Opening bank balance',                500000.00, 0),
(@je, 4, (SELECT id FROM account WHERE code = '3100'), 'Transit authority capital fund',           0, 5430000.00);

UPDATE journal SET next_number = 2 WHERE code = 'MISC';
