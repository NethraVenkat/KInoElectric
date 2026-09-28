package com.backend.javaleap.service;

import com.backend.javaleap.entity.AnalyticAccount;
import com.backend.javaleap.entity.Station;
import com.backend.javaleap.entity.enums.AccountSubType;
import com.backend.javaleap.entity.enums.ContactType;
import com.backend.javaleap.entity.enums.EntryStatus;
import com.backend.javaleap.entity.enums.ProductType;
import com.backend.javaleap.entity.enums.UnitOfMeasure;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.exception.ConflictException;
import com.backend.javaleap.model.request.AccountCreateRequest;
import com.backend.javaleap.model.request.ContactRequest;
import com.backend.javaleap.model.request.JournalEntryRequest;
import com.backend.javaleap.model.request.JournalLineRequest;
import com.backend.javaleap.model.request.ProductRequest;
import com.backend.javaleap.model.request.ReverseEntryRequest;
import com.backend.javaleap.model.response.ContactResponse;
import com.backend.javaleap.model.response.JournalEntryResponse;
import com.backend.javaleap.model.response.TrialBalanceResponse;
import com.backend.javaleap.repository.AnalyticAccountRepository;
import com.backend.javaleap.repository.StationRepository;
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

/** Double-entry engine, trial balance, ledger and master data rules on H2. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountingIntegrationTest {

    @Autowired ApplicationContext ctx;
    @Autowired JournalEntryService entryService;
    @Autowired LedgerService ledgerService;
    @Autowired ContactService contactService;
    @Autowired ProductService productService;
    @Autowired AccountService accountService;
    @Autowired StationRepository stationRepository;
    @Autowired AnalyticAccountRepository analyticAccountRepository;

    AccountingTestData data;
    final int year = LocalDate.now().getYear();

    @BeforeEach
    void setUp() {
        data = new AccountingTestData(ctx, false);
    }

    private static JournalLineRequest dr(String acc, String amount) {
        return new JournalLineRequest(acc, null, null, null, new BigDecimal(amount), null);
    }

    private static JournalLineRequest cr(String acc, String amount) {
        return new JournalLineRequest(acc, null, null, null, null, new BigDecimal(amount));
    }

    private JournalEntryResponse draft(JournalLineRequest... lines) {
        return entryService.createDraft(new JournalEntryRequest("MISC", LocalDate.now(), "TEST", "test entry",
                List.of(lines)));
    }

    // ------------------------------------------------------------------ journal engine

    @Test
    void balancedEntryPostsWithSequentialNumbers() {
        JournalEntryResponse d = draft(dr("1500", "25000"), cr("4200", "25000"));
        assertThat(d.status()).isEqualTo(EntryStatus.DRAFT);
        assertThat(d.entryNumber()).isNull();

        JournalEntryResponse p1 = entryService.post(d.id());
        JournalEntryResponse p2 = entryService.post(draft(dr("1500", "10"), cr("4200", "10")).id());

        assertThat(p1.status()).isEqualTo(EntryStatus.POSTED);
        assertThat(p1.entryNumber()).isEqualTo("JE/" + year + "/0001");
        assertThat(p2.entryNumber()).isEqualTo("JE/" + year + "/0002");
        assertThat(p1.totalDebit()).isEqualByComparingTo("25000.00");
    }

    @Test
    void invalidEntriesAreRejected() {
        assertThatThrownBy(() -> draft(dr("1500", "100"), cr("4200", "90")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("not balanced");
        assertThatThrownBy(() -> draft(new JournalLineRequest("1500", null, null, null,
                new BigDecimal("5"), new BigDecimal("5")), cr("4200", "0")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("either a debit or a credit");
        assertThatThrownBy(() -> draft(dr("1000", "100"), cr("4200", "100")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Line 1");
        assertThatThrownBy(() -> draft(dr("1300", "100"), cr("4100", "100")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("requires a contact");
        assertThatThrownBy(() -> draft(dr("9999", "100"), cr("4100", "100")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Unknown account");
    }

    @Test
    void postedEntriesAreImmutable() {
        JournalEntryResponse p = entryService.post(draft(dr("1500", "500"), cr("4200", "500")).id());
        JournalEntryRequest change = new JournalEntryRequest("MISC", LocalDate.now(), null, null,
                List.of(dr("1500", "1"), cr("4200", "1")));
        assertThatThrownBy(() -> entryService.updateDraft(p.id(), change)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> entryService.deleteDraft(p.id())).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> entryService.post(p.id())).isInstanceOf(BadRequestException.class);
    }

    @Test
    void reversalNetsAccountsToZero() {
        JournalEntryResponse p = entryService.post(draft(dr("1500", "7500"), cr("4200", "7500")).id());
        JournalEntryResponse rev = entryService.reverse(p.id(), new ReverseEntryRequest(null, "posted twice"));

        assertThat(rev.status()).isEqualTo(EntryStatus.POSTED);
        assertThat(rev.reversalOfNumber()).isEqualTo(p.entryNumber());
        assertThat(rev.lines().get(0).credit()).isEqualByComparingTo("7500");
        assertThat(entryService.get(p.id()).status()).isEqualTo(EntryStatus.REVERSED);

        TrialBalanceResponse tb = ledgerService.trialBalance(LocalDate.now());
        assertThat(tb.balanced()).isTrue();
        assertThat(tb.rows()).allMatch(r -> r.debitBalance().signum() == 0 && r.creditBalance().signum() == 0);

        assertThatThrownBy(() -> entryService.reverse(p.id(), new ReverseEntryRequest(null, "again")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void trialBalanceAndLedgerRunningBalance() {
        entryService.post(draft(dr("1500", "1000"), cr("3100", "1000")).id());
        entryService.post(draft(dr("5100", "300"), cr("1500", "300")).id());

        TrialBalanceResponse tb = ledgerService.trialBalance(null);
        assertThat(tb.balanced()).isTrue();
        assertThat(tb.totalDebitBalance()).isEqualByComparingTo("1000.00");

        var ledger = ledgerService.ledger(data.get("1500").getId(), LocalDate.now().minusDays(1), LocalDate.now());
        assertThat(ledger.lines()).hasSize(2);
        assertThat(ledger.lines().get(0).runningBalance()).isEqualByComparingTo("1000");
        assertThat(ledger.closingBalance()).isEqualByComparingTo("700");
    }

    @Test
    void linesCanBeTaggedWithAnalyticAccountAndContact() {
        Station s = stationRepository.save(Station.builder().code("AAS").name("Analytic Station").city("Chennai").build());
        analyticAccountRepository.save(AnalyticAccount.builder().code("AA-AAS-CON").name("Concourse")
                .station(s).active(true).build());
        ContactResponse vendor = contactService.create(contact(ContactType.VENDOR, "Tile Co", null, null));

        JournalEntryResponse e = entryService.createDraft(new JournalEntryRequest("PUR", LocalDate.now(), "BILL-1", null,
                List.of(new JournalLineRequest("5100", null, "AA-AAS-CON", "overhaul", new BigDecimal("3500"), null),
                        new JournalLineRequest("2100", vendor.id(), null, "vendor", null, new BigDecimal("3500")))));
        assertThat(e.lines().get(0).analyticAccountCode()).isEqualTo("AA-AAS-CON");
        assertThat(e.lines().get(1).contactName()).isEqualTo("Tile Co");
        assertThat(entryService.post(e.id()).entryNumber()).startsWith("BILL/");
    }

    // ------------------------------------------------------------------ contacts

    private static ContactRequest contact(ContactType type, String name, String gstin, String pan) {
        return new ContactRequest(type, name, gstin, pan, null, null, null, null, "Chennai", "Tamil Nadu", null, null,
                30, null, null, null, null);
    }

    @Test
    void contactsGetCodesDefaultAccountsAndValidatedGstin() {
        ContactResponse grid = contactService.create(contact(ContactType.CUSTOMER, "Grid Corp", "33aabcc1234d1z5", null));
        ContactResponse vendor = contactService.create(contact(ContactType.VENDOR, "Tile Co", null, null));

        assertThat(grid.code()).isEqualTo("CUST-0001");
        assertThat(grid.gstin()).isEqualTo("33AABCC1234D1Z5");
        assertThat(grid.pan()).isEqualTo("AABCC1234D");
        assertThat(grid.receivableAccountCode()).isEqualTo("1300");
        assertThat(grid.payableAccountCode()).isNull();
        assertThat(vendor.code()).isEqualTo("VEND-0001");
        assertThat(vendor.payableAccountCode()).isEqualTo("2100");

        assertThatThrownBy(() -> contactService.create(contact(ContactType.VENDOR, "Dup", "33AABCC1234D1Z5", null)))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> contactService.create(contact(ContactType.CUSTOMER, "Bad PAN", "33AAGFV9012K1Z8",
                "ZZZZZ9999Z"))).isInstanceOf(BadRequestException.class);
        ContactRequest wrongAccount = new ContactRequest(ContactType.CUSTOMER, "X", null, null, null, null, null, null,
                null, null, null, null, 30, null, "2100", null, null);
        assertThatThrownBy(() -> contactService.create(wrongAccount)).isInstanceOf(BadRequestException.class);
    }

    // ------------------------------------------------------------------ products

    private static ProductRequest product(String code, ProductType type, UnitOfMeasure uom, boolean sold,
                                          boolean purchased, String income, String expense) {
        return new ProductRequest(code, code + " name", type, uom, null, new BigDecimal("8"), new BigDecimal("3500"),
                new BigDecimal("18"), sold, purchased, income, expense, null);
    }

    @Test
    void productsGetDefaultAccountsAndRules() {
        var kwh = productService.create(product("PRD-KWH", ProductType.GOODS, UnitOfMeasure.KWH, true, false, null, null));
        var overhaul = productService.create(product("SRV-OVH", ProductType.SERVICE, UnitOfMeasure.JOB, false, true,
                null, null));
        var tile = productService.create(product("PRD-TILE", ProductType.GOODS, UnitOfMeasure.UNIT, false, true,
                null, null));

        assertThat(kwh.incomeAccountCode()).isEqualTo("4100");
        assertThat(kwh.salePrice()).isEqualByComparingTo("8.0000");
        assertThat(overhaul.expenseAccountCode()).isEqualTo("5100");
        assertThat(tile.expenseAccountCode()).isEqualTo("1100");

        assertThatThrownBy(() -> productService.create(product("NONE", ProductType.GOODS, UnitOfMeasure.UNIT,
                false, false, null, null))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> productService.create(product("BADINC", ProductType.GOODS, UnitOfMeasure.UNIT,
                true, false, "5100", null))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> productService.create(product("PRD-KWH", ProductType.GOODS, UnitOfMeasure.KWH,
                true, false, null, null))).isInstanceOf(ConflictException.class);
    }

    // ------------------------------------------------------------------ chart of accounts

    @Test
    void chartOfAccountsRules() {
        var created = accountService.create(new AccountCreateRequest("5300", "Station Cleaning", "5000",
                AccountSubType.OPERATING_EXPENSE, false, false, null));
        assertThat(created.accountType().name()).isEqualTo("EXPENSE");
        assertThat(created.parentCode()).isEqualTo("5000");

        assertThatThrownBy(() -> accountService.create(new AccountCreateRequest("5400", "Wrong", "5000",
                AccountSubType.RECEIVABLE, false, false, null))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> accountService.create(new AccountCreateRequest("5500", "Under posting", "5100",
                AccountSubType.OPERATING_EXPENSE, false, false, null))).isInstanceOf(BadRequestException.class);

        entryService.post(draft(dr("1500", "50"), cr("4200", "50")).id());
        assertThatThrownBy(() -> accountService.delete(data.get("4200").getId()))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("postings");
        accountService.delete(created.id());
        assertThat(accountService.list()).noneMatch(a -> a.code().equals("5300"));
    }
}
