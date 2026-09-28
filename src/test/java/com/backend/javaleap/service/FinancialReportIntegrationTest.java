package com.backend.javaleap.service;

import com.backend.javaleap.entity.AnalyticAccount;
import com.backend.javaleap.entity.Station;
import com.backend.javaleap.entity.enums.BudgetStatus;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.exception.ConflictException;
import com.backend.javaleap.model.request.BudgetLineRequest;
import com.backend.javaleap.model.request.BudgetRequest;
import com.backend.javaleap.model.request.JournalEntryRequest;
import com.backend.javaleap.model.request.JournalLineRequest;
import com.backend.javaleap.model.response.BalanceSheetResponse;
import com.backend.javaleap.model.response.BudgetLineResponse;
import com.backend.javaleap.model.response.BudgetResponse;
import com.backend.javaleap.model.response.ProfitLossResponse;
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

/** Phase E: Balance Sheet, Profit & Loss and zone budgets on H2. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FinancialReportIntegrationTest {

    @Autowired ApplicationContext ctx;
    @Autowired JournalEntryService entryService;
    @Autowired FinancialReportService reportService;
    @Autowired BudgetService budgetService;
    @Autowired StationRepository stationRepository;
    @Autowired AnalyticAccountRepository analyticAccountRepository;

    final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        new AccountingTestData(ctx, false);
        Station s = stationRepository.save(Station.builder().code("RPT").name("Report Station").city("Chennai").build());
        analyticAccountRepository.save(AnalyticAccount.builder().code("AA-RPT-PF1").name("Platform 1")
                .station(s).active(true).build());

        // capital 1000 into bank
        post(line("1500", null, "1000", null), line("3100", null, null, "1000"));
        // energy sale 100 + GST 18 received in bank, tagged to the zone
        post(line("1500", null, "118", null), line("4100", "AA-RPT-PF1", null, "100"), line("2300", null, null, "18"));
        // tile repair 40 paid from bank, tagged to the zone
        post(line("5100", "AA-RPT-PF1", "40", null), line("1500", null, null, "40"));
    }

    private static JournalLineRequest line(String acc, String aa, String dr, String cr) {
        return new JournalLineRequest(acc, null, aa, null,
                dr == null ? null : new BigDecimal(dr), cr == null ? null : new BigDecimal(cr));
    }

    private void post(JournalLineRequest... lines) {
        Long id = entryService.createDraft(new JournalEntryRequest("MISC", today, "RPT", "report test", List.of(lines))).id();
        entryService.post(id);
    }

    @Test
    void balanceSheetBalancesWithCurrentEarnings() {
        BalanceSheetResponse bs = reportService.balanceSheet(today);
        assertThat(bs.totalAssets()).isEqualByComparingTo("1078.00");       // bank 1000 - 40 + 118
        assertThat(bs.liabilities().total()).isEqualByComparingTo("18.00");  // GST output
        assertThat(bs.currentEarnings()).isEqualByComparingTo("60.00");      // 100 - 40
        assertThat(bs.equity().total()).isEqualByComparingTo("1060.00");
        assertThat(bs.balanced()).isTrue();
    }

    @Test
    void profitAndLossShowsNetProfitAndMargin() {
        ProfitLossResponse pl = reportService.profitLoss(today.minusDays(1), today.plusDays(1));
        assertThat(pl.income().total()).isEqualByComparingTo("100.00");
        assertThat(pl.expenses().total()).isEqualByComparingTo("40.00");
        assertThat(pl.netProfit()).isEqualByComparingTo("60.00");
        assertThat(pl.marginPct()).isEqualByComparingTo("60.00");
        assertThatThrownBy(() -> reportService.profitLoss(today, today.minusDays(3)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void budgetReportComparesPlannedWithActuals() {
        BudgetResponse b = budgetService.create(new BudgetRequest("bud-test", "Test budget", today.minusMonths(1),
                today.plusMonths(1), null, List.of(
                new BudgetLineRequest("AA-RPT-PF1", "4100", new BigDecimal("200")),
                new BudgetLineRequest("AA-RPT-PF1", "5100", new BigDecimal("50")))));

        assertThat(b.code()).isEqualTo("BUD-TEST");
        assertThat(b.status()).isEqualTo(BudgetStatus.DRAFT);
        assertThat(b.actualRevenue()).isEqualByComparingTo("100.00");
        assertThat(b.actualCost()).isEqualByComparingTo("40.00");
        assertThat(b.actualNet()).isEqualByComparingTo("60.00");

        BudgetLineResponse revenue = b.lines().get(0);
        assertThat(revenue.achievementPct()).isEqualByComparingTo("50.00");
        assertThat(revenue.variance()).isEqualByComparingTo("-100.00");   // below plan: unfavourable
        BudgetLineResponse cost = b.lines().get(1);
        assertThat(cost.variance()).isEqualByComparingTo("10.00");        // under budget: favourable

        assertThat(budgetService.confirm(b.id()).status()).isEqualTo(BudgetStatus.CONFIRMED);
        assertThatThrownBy(() -> budgetService.confirm(b.id())).isInstanceOf(BadRequestException.class);
    }

    @Test
    void budgetValidationRejectsBadInput() {
        assertThatThrownBy(() -> budgetService.create(new BudgetRequest("B1", "Bad", today, today, null,
                List.of(new BudgetLineRequest("AA-RPT-PF1", "1500", BigDecimal.TEN)))))
                .isInstanceOf(BadRequestException.class);   // balance-sheet account
        assertThatThrownBy(() -> budgetService.create(new BudgetRequest("B2", "Bad", today, today.minusDays(1), null,
                List.of(new BudgetLineRequest("AA-RPT-PF1", "4100", BigDecimal.TEN)))))
                .isInstanceOf(BadRequestException.class);   // dates reversed
        budgetService.create(new BudgetRequest("B3", "Ok", today, today, null,
                List.of(new BudgetLineRequest("AA-RPT-PF1", "4100", BigDecimal.TEN))));
        assertThatThrownBy(() -> budgetService.create(new BudgetRequest("b3", "Dup", today, today, null,
                List.of(new BudgetLineRequest("AA-RPT-PF1", "4100", BigDecimal.TEN)))))
                .isInstanceOf(ConflictException.class);
    }
}
