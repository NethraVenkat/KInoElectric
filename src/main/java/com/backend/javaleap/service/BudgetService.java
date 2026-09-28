package com.backend.javaleap.service;

import com.backend.javaleap.entity.Account;
import com.backend.javaleap.entity.AnalyticAccount;
import com.backend.javaleap.entity.Budget;
import com.backend.javaleap.entity.BudgetLine;
import com.backend.javaleap.entity.enums.AccountType;
import com.backend.javaleap.entity.enums.BudgetStatus;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.exception.ConflictException;
import com.backend.javaleap.exception.ResourceNotFoundException;
import com.backend.javaleap.model.request.BudgetLineRequest;
import com.backend.javaleap.model.request.BudgetRequest;
import com.backend.javaleap.model.response.BudgetLineResponse;
import com.backend.javaleap.model.response.BudgetResponse;
import com.backend.javaleap.repository.AccountRepository;
import com.backend.javaleap.repository.AnalyticAccountRepository;
import com.backend.javaleap.repository.BudgetRepository;
import com.backend.javaleap.repository.JournalItemRepository;
import com.backend.javaleap.repository.projection.AnalyticActualView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Zone budgets (analytic account = transit station zone) and the Budget Report:
 * planned green-energy revenue and tile maintenance cost vs posted actuals.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BudgetService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BudgetRepository budgetRepository;
    private final AnalyticAccountRepository analyticAccountRepository;
    private final AccountRepository accountRepository;
    private final JournalItemRepository itemRepository;

    public List<BudgetResponse> list() {
        return budgetRepository.findAllByOrderByDateFromDescIdDesc().stream().map(this::toResponse).toList();
    }

    public BudgetResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public BudgetResponse create(BudgetRequest request) {
        String code = request.code().trim().toUpperCase();
        if (budgetRepository.existsByCode(code)) {
            throw new ConflictException("Budget code already exists: " + code);
        }
        if (request.dateTo().isBefore(request.dateFrom())) {
            throw new BadRequestException("dateTo must be on or after dateFrom");
        }
        Budget budget = Budget.builder().code(code).name(request.name().trim())
                .dateFrom(request.dateFrom()).dateTo(request.dateTo()).notes(request.notes()).build();

        Set<String> seen = new HashSet<>();
        for (BudgetLineRequest line : request.lines()) {
            AnalyticAccount aa = analyticAccountRepository.findByCode(line.analyticAccountCode().trim())
                    .orElseThrow(() -> new BadRequestException("Unknown zone (analytic account): " + line.analyticAccountCode()));
            Account account = accountRepository.findByCode(line.accountCode().trim())
                    .orElseThrow(() -> new BadRequestException("Unknown account: " + line.accountCode()));
            if (Boolean.TRUE.equals(account.getGroupAccount())
                    || (account.getAccountType() != AccountType.INCOME && account.getAccountType() != AccountType.EXPENSE)) {
                throw new BadRequestException("Budget lines must use a postable income or expense account: " + account.getCode());
            }
            if (!seen.add(aa.getCode() + "|" + account.getCode())) {
                throw new BadRequestException("Duplicate budget line for " + aa.getCode() + " / " + account.getCode());
            }
            budget.addLine(BudgetLine.builder().analyticAccount(aa).account(account)
                    .plannedAmount(line.plannedAmount().setScale(2, RoundingMode.HALF_UP)).build());
        }
        return toResponse(budgetRepository.save(budget));
    }

    @Transactional
    public BudgetResponse confirm(Long id) {
        Budget budget = find(id);
        if (budget.getStatus() != BudgetStatus.DRAFT) {
            throw new BadRequestException("Only draft budgets can be confirmed");
        }
        budget.setStatus(BudgetStatus.CONFIRMED);
        return toResponse(budget);
    }

    @Transactional
    public BudgetResponse cancel(Long id) {
        Budget budget = find(id);
        if (budget.getStatus() == BudgetStatus.CANCELLED) {
            throw new BadRequestException("Budget is already cancelled");
        }
        budget.setStatus(BudgetStatus.CANCELLED);
        return toResponse(budget);
    }

    private Budget find(Long id) {
        return budgetRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Budget", id));
    }

    private BudgetResponse toResponse(Budget budget) {
        Map<String, AnalyticActualView> actuals = new HashMap<>();
        for (AnalyticActualView v : itemRepository.analyticActuals(LedgerService.BOOKED, budget.getDateFrom(), budget.getDateTo())) {
            actuals.put(v.getAnalyticAccountId() + "|" + v.getAccountId(), v);
        }
        BigDecimal plannedRevenue = BigDecimal.ZERO, actualRevenue = BigDecimal.ZERO;
        BigDecimal plannedCost = BigDecimal.ZERO, actualCost = BigDecimal.ZERO;
        List<BudgetLineResponse> lines = new ArrayList<>();

        for (BudgetLine line : budget.getLines()) {
            Account account = line.getAccount();
            AnalyticAccount aa = line.getAnalyticAccount();
            AnalyticActualView v = actuals.get(aa.getId() + "|" + account.getId());
            BigDecimal debit = v == null ? BigDecimal.ZERO : v.getDebit();
            BigDecimal credit = v == null ? BigDecimal.ZERO : v.getCredit();
            boolean revenue = account.getAccountType() == AccountType.INCOME;
            BigDecimal actual = (revenue ? credit.subtract(debit) : debit.subtract(credit)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal planned = line.getPlannedAmount();
            BigDecimal variance = revenue ? actual.subtract(planned) : planned.subtract(actual);
            BigDecimal pct = planned.signum() > 0
                    ? actual.multiply(HUNDRED).divide(planned, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2);
            if (revenue) {
                plannedRevenue = plannedRevenue.add(planned);
                actualRevenue = actualRevenue.add(actual);
            } else {
                plannedCost = plannedCost.add(planned);
                actualCost = actualCost.add(actual);
            }
            lines.add(new BudgetLineResponse(line.getId(), aa.getCode(), aa.getName(), account.getCode(), account.getName(),
                    account.getAccountType(), planned, actual, variance, pct));
        }
        return new BudgetResponse(budget.getId(), budget.getCode(), budget.getName(), budget.getDateFrom(), budget.getDateTo(),
                budget.getStatus(), budget.getNotes(), plannedRevenue, actualRevenue, plannedCost, actualCost,
                plannedRevenue.subtract(plannedCost), actualRevenue.subtract(actualCost), lines);
    }
}
