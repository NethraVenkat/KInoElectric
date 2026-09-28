package com.backend.javaleap.service;

import com.backend.javaleap.entity.Budget;
import com.backend.javaleap.entity.BudgetLine;
import com.backend.javaleap.entity.enums.ContactType;
import com.backend.javaleap.entity.enums.PaymentMethod;
import com.backend.javaleap.model.request.ContactRequest;
import com.backend.javaleap.model.request.CreateInvoiceRequest;
import com.backend.javaleap.model.request.EnergySalesOrderRequest;
import com.backend.javaleap.model.request.JournalEntryRequest;
import com.backend.javaleap.model.request.JournalLineRequest;
import com.backend.javaleap.model.request.OrderLineRequest;
import com.backend.javaleap.model.request.PaymentRequest;
import com.backend.javaleap.model.request.PurchaseOrderRequest;
import com.backend.javaleap.model.request.SalesOrderRequest;
import com.backend.javaleap.model.response.InvoiceResponse;
import com.backend.javaleap.repository.AccountRepository;
import com.backend.javaleap.repository.AnalyticAccountRepository;
import com.backend.javaleap.repository.BudgetRepository;
import com.backend.javaleap.repository.ContactRepository;
import com.backend.javaleap.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads six months of realistic demo activity (April - September 2026) through the real services, so every
 * screen has data: extra customers and vendors, monthly energy sales orders and invoices, receipts, tile repair
 * purchase orders and bills, vendor payments, subsidy journal entries and subsidy budget lines.
 * <p>
 * Runs once (only while no purchase order exists) and only when {@code app.demo-data.enabled=true}.
 * Everything happens in one transaction: if anything fails, nothing is saved and the app still starts.
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final int YEAR = 2026;
    /** Zone analytic account -> traffic weight (same weights as the V2 seed). */
    private static final Map<String, Double> ZONES = new LinkedHashMap<>();

    static {
        ZONES.put("AA-CHC-ENT", 1.00);
        ZONES.put("AA-CHC-STR", 0.85);
        ZONES.put("AA-CHC-CON", 0.70);
        ZONES.put("AA-CHC-PF1", 0.90);
        ZONES.put("AA-CHC-PF2", 0.80);
        ZONES.put("AA-CHC-EXT", 0.75);
    }

    private final PlatformTransactionManager transactionManager;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ContactRepository contactRepository;
    private final BudgetRepository budgetRepository;
    private final AnalyticAccountRepository analyticAccountRepository;
    private final AccountRepository accountRepository;
    private final ContactService contactService;
    private final PurchaseOrderService purchaseOrderService;
    private final SalesOrderService salesOrderService;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final JournalEntryService journalEntryService;

    @Override
    public void run(ApplicationArguments args) {
        if (purchaseOrderRepository.count() > 0) {
            log.info("Demo data: transactions already exist - skipping");
            return;
        }
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> seed());
            log.info("Demo data: loaded six months of kinetic energy transactions (Apr-Sep {})", YEAR);
        } catch (RuntimeException e) {
            log.warn("Demo data could not be loaded (nothing was saved): {}", e.getMessage());
        }
    }

    private void seed() {
        Long grid = contactRepository.findByCode("CUST-0001").orElseThrow().getId();
        Long kinetiFloor = contactRepository.findByCode("VEND-0001").orElseThrow().getId();
        Long voltEdge = contactRepository.findByCode("VEND-0002").orElseThrow().getId();

        Long exchange = contactService.create(new ContactRequest(ContactType.CUSTOMER,
                "Southern Regional Energy Exchange Ltd", "29AACCS4321E1Z3", "AACCS4321E", "settlements@srex.example.in",
                "+91-80-4123-5566", "Settlements Team", "88 MG Road", "Bengaluru", "Karnataka", "560001", "India",
                45, new BigDecimal("300000"), "1300", null, "Buys surplus kinetic energy through the inter-state exchange (IGST)")).id();
        Long greenCorp = contactService.create(new ContactRequest(ContactType.CUSTOMER,
                "Tamil Nadu Green Energy Corporation", "33AADCT7788G1Z9", "AADCT7788G", "accounts@tngec.example.in",
                "+91-44-2811-4400", "Renewables Desk", "144 Mount Road", "Chennai", "Tamil Nadu", "600006", "India",
                30, new BigDecimal("200000"), "1300", null, "Green-energy certificate buyer")).id();
        Long piezoGrid = contactService.create(new ContactRequest(ContactType.VENDOR,
                "PiezoGrid Components Pvt Ltd", "29AAHCP3344M1Z6", "AAHCP3344M", "orders@piezogrid.example.in",
                "+91-80-2266-7788", "Sales Desk", "12 Electronic City Phase 1", "Bengaluru", "Karnataka", "560100", "India",
                30, null, null, "2100", "Electromagnetic tile generator supplier (inter-state, IGST)")).id();

        // ---------------- sales: monthly energy export invoices to the regional grid ----------------
        int[] kwhPerMonth = {3100, 2950, 3250, 3400, 3350};                      // Apr..Aug network export
        for (int m = 0; m < kwhPerMonth.length; m++) {
            LocalDate monthEnd = LocalDate.of(YEAR, 4 + m, 1).plusMonths(1).minusDays(1);
            LocalDate billed = monthEnd.plusDays(2);
            String period = monthEnd.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH) + " " + YEAR;
            InvoiceResponse inv = sell(grid, billed, "Metered export " + period, energyLines(kwhPerMonth[m], period, null));
            if (m < 4) {
                pay(inv, inv.totalAmount(), billed.plusDays(24), PaymentMethod.NEFT, "UTR CRPGC" + (4 + m) + "2026");
            } else {
                pay(inv, inv.totalAmount().multiply(new BigDecimal("0.40")).setScale(2, RoundingMode.HALF_UP),
                        billed.plusDays(16), PaymentMethod.RTGS, "UTR CRPGC-PART-AUG");
            }
        }
        // inter-state exchange (IGST): June paid, August open
        InvoiceResponse ex1 = sell(exchange, LocalDate.of(YEAR, 6, 30), "Exchange block Jun 2026",
                List.of(line("PRD-KWH", "Surplus kinetic energy via exchange - June", 1200, "7.50", "AA-CHC-PF1")));
        pay(ex1, ex1.totalAmount(), LocalDate.of(YEAR, 8, 10), PaymentMethod.RTGS, "UTR SREX0810");
        sell(exchange, LocalDate.of(YEAR, 8, 31), "Exchange block Aug 2026",
                List.of(line("PRD-KWH", "Surplus kinetic energy via exchange - August", 1350, "7.50", "AA-CHC-PF2")));
        // green-energy buyer: July invoice still unpaid -> overdue
        sell(greenCorp, LocalDate.of(YEAR, 7, 5), "Green energy block Jul 2026",
                List.of(line("PRD-KWH", "Certified kinetic green energy - concourse", 900, "9.00", "AA-CHC-CON")));

        // September: energy order generated from telemetry, confirmed and invoiced as a draft
        try {
            Long so = salesOrderService.createEnergyOrder(new EnergySalesOrderRequest(grid, "CHC",
                    LocalDate.of(YEAR, 9, 1), LocalDate.of(YEAR, 9, 26), "PRD-KWH")).id();
            salesOrderService.confirm(so);
            salesOrderService.createInvoice(so, new CreateInvoiceRequest(LocalDate.of(YEAR, 9, 27), null));
        } catch (RuntimeException e) {
            log.info("Demo data: telemetry energy order skipped ({})", e.getMessage());
        }
        // a draft sales order waiting for confirmation
        salesOrderService.create(new SalesOrderRequest(greenCorp, LocalDate.of(YEAR, 9, 26), "GE-OCT-ADVANCE",
                "Advance booking for October green-energy block",
                List.of(line("PRD-KWH", "Certified kinetic green energy - platforms", 1000, "9.00", "AA-CHC-PF1"))));

        // ---------------- purchases: tile repairs and maintenance ----------------
        InvoiceResponse b1 = buy(kinetiFloor, LocalDate.of(YEAR, 4, 18), "AA-CHC-PF1", "Q1 transducer overhaul",
                List.of(line("SRV-OVH", "Transducer overhaul - platform 1 tiles", 4, null, "AA-CHC-PF1", "CHC-PF1-R2C07")), true);
        pay(b1, b1.totalAmount(), LocalDate.of(YEAR, 5, 15), PaymentMethod.NEFT, "NEFT KF-0515");

        InvoiceResponse b2 = buy(kinetiFloor, LocalDate.of(YEAR, 6, 10), "AA-CHC-CON", "Concourse tile replacement",
                List.of(line("PRD-TILE-PZ", "Replacement piezo tiles - concourse", 2, null, "AA-CHC-CON", "CHC-CON-R3C04")), true);
        pay(b2, b2.totalAmount(), LocalDate.of(YEAR, 7, 5), PaymentMethod.RTGS, "RTGS KF-0705");

        InvoiceResponse b3 = buy(voltEdge, LocalDate.of(YEAR, 7, 22), "AA-CHC-ENT", "Inverter service Q2",
                List.of(line("SRV-INV", "Power inverter maintenance - entry gates", 2, null, "AA-CHC-ENT"),
                        line("SRV-INV", "Power inverter maintenance - exit gates", 1, null, "AA-CHC-EXT")), true);
        pay(b3, new BigDecimal("8000.00"), LocalDate.of(YEAR, 8, 20), PaymentMethod.UPI, "UPI VOLT-0820");

        buy(piezoGrid, LocalDate.of(YEAR, 8, 28), "AA-CHC-PF1", "EM generator tile",
                List.of(line("PRD-TILE-EM", "Electromagnetic tile - platform 1", 1, null, "AA-CHC-PF1")), true);  // posted, unpaid

        buy(kinetiFloor, LocalDate.of(YEAR, 9, 15), "AA-CHC-STR", "Stairs overhaul",
                List.of(line("SRV-OVH", "Transducer overhaul - stairs & escalators", 2, null, "AA-CHC-STR", "CHC-STR-R1C05")), false); // bill draft

        Long confirmedPo = purchaseOrderService.create(new PurchaseOrderRequest(kinetiFloor, LocalDate.of(YEAR, 9, 22),
                LocalDate.of(YEAR, 10, 5), "AA-CHC-EXT", "Exit tile check", "Awaiting delivery",
                List.of(line("SRV-OVH", "Transducer overhaul - exit gate tile", 1, null, "AA-CHC-EXT", "CHC-EXT-R1C02")))).id();
        purchaseOrderService.confirm(confirmedPo);
        purchaseOrderService.create(new PurchaseOrderRequest(voltEdge, LocalDate.of(YEAR, 9, 25), LocalDate.of(YEAR, 10, 15),
                "AA-CHC-PF2", "Inverter service Q3", "Draft - quotation pending",
                List.of(line("SRV-INV", "Power inverter maintenance - platform 2", 2, null, "AA-CHC-PF2"))));

        // ---------------- journal entries: sustainability subsidy received each month ----------------
        for (int month = 4; month <= 9; month++) {
            List<JournalLineRequest> lines = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            for (Map.Entry<String, Double> z : ZONES.entrySet()) {
                BigDecimal amt = BigDecimal.valueOf(3000 * z.getValue()).setScale(2, RoundingMode.HALF_UP);
                total = total.add(amt);
                lines.add(new JournalLineRequest("4200", null, z.getKey(), "Smart-city subsidy share", null, amt));
            }
            lines.add(0, new JournalLineRequest("1500", null, null, "Subsidy received", total, null));
            Long id = journalEntryService.createDraft(new JournalEntryRequest("BNK", LocalDate.of(YEAR, month, 10),
                    "SUBSIDY-" + month, "Sustainability subsidy - Smart Cities Mission", lines)).id();
            journalEntryService.post(id);
        }
        // a draft accrual waiting for review
        journalEntryService.createDraft(new JournalEntryRequest("MISC", LocalDate.of(YEAR, 9, 30), "ACCR-SEP",
                "September inverter maintenance accrual (draft)",
                List.of(new JournalLineRequest("5200", null, "AA-CHC-PF2", "Accrued maintenance", new BigDecimal("4500"), null),
                        new JournalLineRequest("2200", voltEdge, null, "Station maintenance payable", null, new BigDecimal("4500")))));

        addSubsidyBudgetLines();
    }

    /** Monthly energy invoice lines: one per zone, quantity by traffic weight. */
    private List<OrderLineRequest> energyLines(int totalKwh, String period, String price) {
        double weightSum = ZONES.values().stream().mapToDouble(Double::doubleValue).sum();
        List<OrderLineRequest> lines = new ArrayList<>();
        ZONES.forEach((aa, w) -> lines.add(line("PRD-KWH", "Harvested kinetic electricity " + aa.substring(7) + " - " + period,
                Math.round(totalKwh * w / weightSum), price, aa)));
        return lines;
    }

    private static OrderLineRequest line(String product, String description, double qty, String price, String zone) {
        return line(product, description, qty, price, zone, null);
    }

    private static OrderLineRequest line(String product, String description, double qty, String price, String zone, String tile) {
        return new OrderLineRequest(product, description, BigDecimal.valueOf(qty), price == null ? null : new BigDecimal(price),
                null, zone, tile);
    }

    private InvoiceResponse sell(Long customerId, LocalDate date, String reference, List<OrderLineRequest> lines) {
        Long so = salesOrderService.create(new SalesOrderRequest(customerId, date, reference, null, lines)).id();
        salesOrderService.confirm(so);
        InvoiceResponse inv = salesOrderService.createInvoice(so, new CreateInvoiceRequest(date, null));
        return invoiceService.post(inv.id());
    }

    private InvoiceResponse buy(Long vendorId, LocalDate date, String zone, String reference,
                                List<OrderLineRequest> lines, boolean post) {
        Long po = purchaseOrderService.create(new PurchaseOrderRequest(vendorId, date, date.plusDays(7), zone, reference,
                null, lines)).id();
        purchaseOrderService.confirm(po);
        InvoiceResponse bill = purchaseOrderService.createBill(po, new CreateInvoiceRequest(date.plusDays(2), reference.toUpperCase()));
        return post ? invoiceService.post(bill.id()) : bill;
    }

    private void pay(InvoiceResponse invoice, BigDecimal amount, LocalDate date, PaymentMethod method, String reference) {
        paymentService.register(new PaymentRequest(invoice.id(), amount, date, method, "BNK", reference, null));
    }

    /** Adds planned subsidy income (4200) per zone to the FY budget so the Budget Report shows both income lines. */
    private void addSubsidyBudgetLines() {
        Budget budget = budgetRepository.findAll().stream()
                .filter(b -> "BUD-FY2026-27".equals(b.getCode())).findFirst().orElse(null);
        if (budget == null || budget.getLines().stream().anyMatch(l -> "4200".equals(l.getAccount().getCode()))) {
            return;
        }
        var subsidy = accountRepository.findByCode("4200").orElseThrow();
        ZONES.forEach((aa, w) -> analyticAccountRepository.findByCode(aa).ifPresent(a -> budget.addLine(BudgetLine.builder()
                .analyticAccount(a).account(subsidy)
                .plannedAmount(BigDecimal.valueOf(36000 * w).setScale(2, RoundingMode.HALF_UP)).build())));
        budgetRepository.save(budget);
    }
}
