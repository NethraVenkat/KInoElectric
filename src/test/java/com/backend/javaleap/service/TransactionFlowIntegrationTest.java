package com.backend.javaleap.service;

import com.backend.javaleap.entity.AnalyticAccount;
import com.backend.javaleap.entity.EnergyGenerationDaily;
import com.backend.javaleap.entity.Station;
import com.backend.javaleap.entity.Zone;
import com.backend.javaleap.entity.enums.ContactType;
import com.backend.javaleap.entity.enums.InvoiceStatus;
import com.backend.javaleap.entity.enums.OrderStatus;
import com.backend.javaleap.entity.enums.PaymentMethod;
import com.backend.javaleap.entity.enums.PaymentState;
import com.backend.javaleap.entity.enums.ProductType;
import com.backend.javaleap.entity.enums.UnitOfMeasure;
import com.backend.javaleap.entity.enums.ZoneType;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.exception.ConflictException;
import com.backend.javaleap.model.request.ContactRequest;
import com.backend.javaleap.model.request.CreateInvoiceRequest;
import com.backend.javaleap.model.request.EnergySalesOrderRequest;
import com.backend.javaleap.model.request.OrderLineRequest;
import com.backend.javaleap.model.request.PaymentRequest;
import com.backend.javaleap.model.request.ProductRequest;
import com.backend.javaleap.model.request.PurchaseOrderRequest;
import com.backend.javaleap.model.request.SalesOrderRequest;
import com.backend.javaleap.model.response.InvoiceResponse;
import com.backend.javaleap.model.response.JournalEntryResponse;
import com.backend.javaleap.model.response.PurchaseOrderResponse;
import com.backend.javaleap.model.response.SalesOrderResponse;
import com.backend.javaleap.repository.AnalyticAccountRepository;
import com.backend.javaleap.repository.EnergyGenerationDailyRepository;
import com.backend.javaleap.repository.StationRepository;
import com.backend.javaleap.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Purchase -> bill -> payment and energy SO -> invoice -> receipt, end to end through the ledger. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionFlowIntegrationTest {

    @Autowired ApplicationContext ctx;
    @Autowired ContactService contactService;
    @Autowired ProductService productService;
    @Autowired PurchaseOrderService purchaseOrderService;
    @Autowired SalesOrderService salesOrderService;
    @Autowired InvoiceService invoiceService;
    @Autowired PaymentService paymentService;
    @Autowired JournalEntryService journalEntryService;
    @Autowired LedgerService ledgerService;
    @Autowired StationRepository stationRepository;
    @Autowired ZoneRepository zoneRepository;
    @Autowired AnalyticAccountRepository analyticAccountRepository;
    @Autowired EnergyGenerationDailyRepository generationRepository;

    final int year = LocalDate.now().getYear();
    Long vendorId;
    Long customerId;

    @BeforeEach
    void setUp() {
        new AccountingTestData(ctx, false);
        vendorId = contactService.create(contact(ContactType.VENDOR, "KinetiFloor Technologies", "33AAFCK5678P1Z2")).id();
        customerId = contactService.create(contact(ContactType.CUSTOMER, "Chennai Regional Power Grid",
                "33AABCC1234D1Z5")).id();
        productService.create(product("SRV-OVH", ProductType.SERVICE, UnitOfMeasure.JOB, false, true, "0", "3500", "18"));
        productService.create(product("PRD-TILE-PZ", ProductType.GOODS, UnitOfMeasure.UNIT, false, true, "0", "22000",
                "18"));
        productService.create(product("PRD-KWH", ProductType.GOODS, UnitOfMeasure.KWH, true, false, "8", "0", "0"));
    }

    // ------------------------------------------------------------------ purchase flow

    @Test
    void purchaseOrderToBillToPaymentBalancesTheBooks() {
        PurchaseOrderResponse po = purchaseOrderService.create(tilePo(vendorId));
        assertThat(po.status()).isEqualTo(OrderStatus.DRAFT);
        assertThat(po.untaxedAmount()).isEqualByComparingTo("29000.00");
        assertThat(po.taxAmount()).isEqualByComparingTo("5220.00");
        assertThat(po.totalAmount()).isEqualByComparingTo("34220.00");

        po = purchaseOrderService.confirm(po.id());
        assertThat(po.poNumber()).isEqualTo("PO/" + year + "/0001");

        InvoiceResponse bill = purchaseOrderService.createBill(po.id(), new CreateInvoiceRequest(null, "KF-INV-778"));
        assertThat(bill.status()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(bill.lines()).extracting("accountCode").containsExactly("5100", "1100");
        assertThat(bill.placeOfSupply()).isEqualTo("33");
        assertThat(bill.cgstAmount()).isEqualByComparingTo("2610.00");
        assertThat(bill.sgstAmount()).isEqualByComparingTo("2610.00");
        assertThat(bill.igstAmount()).isEqualByComparingTo("0");
        assertThat(bill.dueDate()).isEqualTo(LocalDate.now().plusDays(30));
        assertThat(purchaseOrderService.get(po.id()).status()).isEqualTo(OrderStatus.INVOICED);

        bill = invoiceService.post(bill.id());
        assertThat(bill.invoiceNumber()).isEqualTo("BILL/" + year + "/0001");
        JournalEntryResponse je = journalEntryService.get(bill.journalEntryId());
        assertThat(je.lines()).hasSize(4);
        assertThat(je.lines().get(3).accountCode()).isEqualTo("2100");
        assertThat(je.lines().get(3).credit()).isEqualByComparingTo("34220.00");
        assertThat(je.lines().get(3).contactName()).isEqualTo("KinetiFloor Technologies");

        var p1 = paymentService.register(pay(bill.id(), "20000"));
        assertThat(p1.paymentNumber()).isEqualTo("BNK/" + year + "/0001");
        InvoiceResponse after = invoiceService.get(bill.id());
        assertThat(after.paymentState()).isEqualTo(PaymentState.PARTIAL);
        assertThat(after.amountDue()).isEqualByComparingTo("14220.00");

        final Long billId = bill.id();
        paymentService.register(pay(billId, "14220"));
        assertThat(invoiceService.get(billId).paymentState()).isEqualTo(PaymentState.PAID);
        assertThatThrownBy(() -> paymentService.register(pay(billId, "1"))).isInstanceOf(BadRequestException.class);

        var tb = ledgerService.trialBalance(null);
        assertThat(tb.balanced()).isTrue();
        var payable = tb.rows().stream().filter(r -> r.accountCode().equals("2100")).findFirst().orElseThrow();
        assertThat(payable.creditBalance()).isEqualByComparingTo("0");
    }

    @Test
    void interStateVendorBillUsesIgst() {
        Long outOfState = contactService.create(contact(ContactType.VENDOR, "Mumbai Tiles", "27AABCM1234F1Z5")).id();
        PurchaseOrderResponse po = purchaseOrderService.confirm(purchaseOrderService.create(tilePo(outOfState)).id());
        InvoiceResponse bill = purchaseOrderService.createBill(po.id(), null);
        assertThat(bill.placeOfSupply()).isEqualTo("27");
        assertThat(bill.igstAmount()).isEqualByComparingTo("5220.00");
        assertThat(bill.cgstAmount()).isEqualByComparingTo("0");
    }

    // ------------------------------------------------------------------ energy sales flow

    @Test
    void energyExportIsBilledPerSectorAndCollected() {
        LocalDate start = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        seedExport(start);

        SalesOrderResponse so = salesOrderService.createEnergyOrder(
                new EnergySalesOrderRequest(customerId, "TST", start, end, null));
        assertThat(so.lines()).hasSize(2);
        assertThat(so.lines().get(0).quantity()).isEqualByComparingTo("1.500");
        assertThat(so.lines().get(0).analyticAccountCode()).isEqualTo("AA-TST-ENT");
        assertThat(so.lines().get(1).quantity()).isEqualByComparingTo("2.000");
        assertThat(so.totalAmount()).isEqualByComparingTo("28.00");

        assertThatThrownBy(() -> salesOrderService.createEnergyOrder(
                new EnergySalesOrderRequest(customerId, "TST", start, end, null)))
                .isInstanceOf(ConflictException.class);

        so = salesOrderService.confirm(so.id());
        assertThat(so.soNumber()).isEqualTo("SO/" + year + "/0001");
        InvoiceResponse inv = invoiceService.post(salesOrderService.createInvoice(so.id(), null).id());
        assertThat(inv.invoiceNumber()).isEqualTo("INV/" + year + "/0001");

        JournalEntryResponse je = journalEntryService.get(inv.journalEntryId());
        assertThat(je.lines().get(0).accountCode()).isEqualTo("1300");
        assertThat(je.lines().get(0).debit()).isEqualByComparingTo("28.00");
        assertThat(je.lines().get(1).analyticAccountCode()).isEqualTo("AA-TST-ENT");
        assertThat(je.lines().get(1).credit()).isEqualByComparingTo("12.00");

        paymentService.register(new PaymentRequest(inv.id(), new BigDecimal("28"), null, PaymentMethod.NEFT, null,
                "UTR123", null));
        assertThat(invoiceService.get(inv.id()).paymentState()).isEqualTo(PaymentState.PAID);
        assertThat(ledgerService.trialBalance(null).balanced()).isTrue();
    }

    @Test
    void energyOrderRejectsFuturePeriodsAndEmptyExports() {
        seedExport(LocalDate.now().minusMonths(1).withDayOfMonth(1));
        assertThatThrownBy(() -> salesOrderService.createEnergyOrder(new EnergySalesOrderRequest(customerId, "TST",
                LocalDate.now(), LocalDate.now().plusDays(3), null))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> salesOrderService.createEnergyOrder(new EnergySalesOrderRequest(customerId, "TST",
                LocalDate.now().minusYears(1), LocalDate.now().minusYears(1).plusDays(5), null)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("No energy");
    }

    // ------------------------------------------------------------------ cancellation rules

    @Test
    void cancellationReversesEntriesInTheRightOrder() {
        PurchaseOrderResponse po = purchaseOrderService.confirm(purchaseOrderService.create(tilePo(vendorId)).id());
        InvoiceResponse bill = invoiceService.post(purchaseOrderService.createBill(po.id(), null).id());
        var payment = paymentService.register(pay(bill.id(), "1000"));

        assertThatThrownBy(() -> invoiceService.cancel(bill.id()))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("payments");

        paymentService.cancel(payment.id());
        InvoiceResponse reopened = invoiceService.get(bill.id());
        assertThat(reopened.paymentState()).isEqualTo(PaymentState.NOT_PAID);
        assertThat(reopened.amountDue()).isEqualByComparingTo("34220.00");

        assertThat(invoiceService.cancel(bill.id()).status()).isEqualTo(InvoiceStatus.CANCELLED);
        assertThat(purchaseOrderService.get(po.id()).status()).isEqualTo(OrderStatus.CONFIRMED);

        var tb = ledgerService.trialBalance(null);
        assertThat(tb.balanced()).isTrue();
        assertThat(tb.rows()).allMatch(r -> r.debitBalance().signum() == 0 && r.creditBalance().signum() == 0);

        // the PO can be billed again
        assertThat(purchaseOrderService.createBill(po.id(), null).status()).isEqualTo(InvoiceStatus.DRAFT);
    }

    // ------------------------------------------------------------------ validation

    @Test
    void businessRulesAreEnforced() {
        assertThatThrownBy(() -> purchaseOrderService.create(tilePo(customerId)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("not a vendor");

        SalesOrderRequest badSo = new SalesOrderRequest(customerId, LocalDate.now(), null, null,
                List.of(new OrderLineRequest("SRV-OVH", null, BigDecimal.ONE, null, null, null, null)));
        assertThatThrownBy(() -> salesOrderService.create(badSo))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("cannot be sold");

        PurchaseOrderResponse po = purchaseOrderService.confirm(purchaseOrderService.create(tilePo(vendorId)).id());
        assertThatThrownBy(() -> purchaseOrderService.confirm(po.id())).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> paymentService.register(pay(purchaseOrderService.createBill(po.id(), null).id(), "10")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("posted");
    }

    // ------------------------------------------------------------------ helpers

    private static ContactRequest contact(ContactType type, String name, String gstin) {
        return new ContactRequest(type, name, gstin, null, null, null, null, null, null, null, null, null, 30, null,
                null, null, null);
    }

    private static ProductRequest product(String code, ProductType type, UnitOfMeasure uom, boolean sold,
                                          boolean purchased, String sale, String purchase, String gst) {
        return new ProductRequest(code, code, type, uom, null, new BigDecimal(sale), new BigDecimal(purchase),
                new BigDecimal(gst), sold, purchased, null, null, null);
    }

    private static PurchaseOrderRequest tilePo(Long vendor) {
        return new PurchaseOrderRequest(vendor, LocalDate.now(), null, null, "Repair CHC-PF1-R2C07", null, List.of(
                new OrderLineRequest("SRV-OVH", null, new BigDecimal("2"), null, null, null, null),
                new OrderLineRequest("PRD-TILE-PZ", null, BigDecimal.ONE, null, null, null, null)));
    }

    private static PaymentRequest pay(Long invoiceId, String amount) {
        return new PaymentRequest(invoiceId, new BigDecimal(amount), null, PaymentMethod.BANK_TRANSFER, null, null, null);
    }

    /** Station TST with ENT (1.5 kWh exported) and PF1 (2 kWh exported) in the month starting at {@code start}. */
    private void seedExport(LocalDate start) {
        Station st = stationRepository.save(Station.builder().code("TST").name("Test Station").city("Chennai").build());
        Zone ent = zoneRepository.save(zone(st, "ENT", ZoneType.ENTRY, 1));
        Zone pf1 = zoneRepository.save(zone(st, "PF1", ZoneType.PLATFORM, 2));
        analyticAccountRepository.save(AnalyticAccount.builder().code("AA-TST-ENT").name("Entry").station(st)
                .zone(ent).active(true).build());
        analyticAccountRepository.save(AnalyticAccount.builder().code("AA-TST-PF1").name("Platform").station(st)
                .zone(pf1).active(true).build());
        generationRepository.save(day(st, ent, start, "3600000"));
        generationRepository.save(day(st, ent, start.plusDays(1), "1800000"));
        generationRepository.save(day(st, pf1, start.plusDays(2), "7200000"));
    }

    private static Zone zone(Station st, String code, ZoneType type, int order) {
        return Zone.builder().station(st).code(code).name(code + " sector").zoneType(type).maxCapacity(100)
                .posX(0).posY(0).width(100).height(100).gridRows(1).gridCols(1).displayOrder(order).build();
    }

    private static EnergyGenerationDaily day(Station st, Zone z, LocalDate date, String exportedJ) {
        return EnergyGenerationDaily.builder().station(st).zone(z).genDate(date)
                .energyJ(new BigDecimal(exportedJ)).exportedJ(new BigDecimal(exportedJ)).build();
    }
}
