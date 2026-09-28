package com.backend.javaleap.service;

import com.backend.javaleap.entity.enums.AccountType;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.model.response.BalanceSheetResponse;
import com.backend.javaleap.model.response.ProfitLossResponse;
import com.backend.javaleap.model.response.StatementLine;
import com.backend.javaleap.model.response.StatementSection;
import com.backend.javaleap.repository.JournalItemRepository;
import com.backend.javaleap.repository.projection.AccountBalanceView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Financial statements built from posted journal lines:
 * Balance Sheet (as of a date) and Profit &amp; Loss (for a period).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialReportService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final JournalItemRepository itemRepository;

    /** Indian financial year starts on 1 April. */
    public static LocalDate financialYearStart(LocalDate date) {
        int year = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
        return LocalDate.of(year, 4, 1);
    }

    public BalanceSheetResponse balanceSheet(LocalDate asOf) {
        LocalDate date = asOf != null ? asOf : LocalDate.now();
        List<StatementLine> assets = new ArrayList<>();
        List<StatementLine> liabilities = new ArrayList<>();
        List<StatementLine> equity = new ArrayList<>();
        BigDecimal earnings = BigDecimal.ZERO;

        for (AccountBalanceView row : itemRepository.balancesAsOf(LedgerService.BOOKED, date)) {
            BigDecimal net = row.getDebit().subtract(row.getCredit()); // debit-positive
            switch (row.getAccountType()) {
                case ASSET -> add(assets, row, net);
                case LIABILITY -> add(liabilities, row, net.negate());
                case EQUITY -> add(equity, row, net.negate());
                case INCOME, EXPENSE -> earnings = earnings.add(net.negate());
            }
        }
        if (earnings.signum() != 0) {
            equity.add(new StatementLine(null, "Current Year Earnings", scale(earnings)));
        }
        StatementSection a = section("Assets", assets);
        StatementSection l = section("Liabilities", liabilities);
        StatementSection e = section("Equity", equity);
        BigDecimal totalLe = l.total().add(e.total());
        return new BalanceSheetResponse(date, a, l, e, scale(earnings), a.total(), totalLe,
                a.total().compareTo(totalLe) == 0);
    }

    public ProfitLossResponse profitLoss(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : financialYearStart(end);
        if (start.isAfter(end)) {
            throw new BadRequestException("from must be on or before to");
        }
        List<StatementLine> income = new ArrayList<>();
        List<StatementLine> expenses = new ArrayList<>();
        for (AccountBalanceView row : itemRepository.balancesBetween(LedgerService.BOOKED, start, end)) {
            if (row.getAccountType() == AccountType.INCOME) {
                add(income, row, row.getCredit().subtract(row.getDebit()));
            } else if (row.getAccountType() == AccountType.EXPENSE) {
                add(expenses, row, row.getDebit().subtract(row.getCredit()));
            }
        }
        StatementSection in = section("Income", income);
        StatementSection ex = section("Expenses", expenses);
        BigDecimal net = in.total().subtract(ex.total());
        BigDecimal margin = in.total().signum() > 0
                ? net.multiply(HUNDRED).divide(in.total(), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2);
        return new ProfitLossResponse(start, end, in, ex, net, margin);
    }

    private static void add(List<StatementLine> lines, AccountBalanceView row, BigDecimal amount) {
        if (amount.signum() != 0) {
            lines.add(new StatementLine(row.getCode(), row.getName(), scale(amount)));
        }
    }

    private static StatementSection section(String title, List<StatementLine> lines) {
        BigDecimal total = lines.stream().map(StatementLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new StatementSection(title, List.copyOf(lines), scale(total));
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
