package com.backend.javaleap.service;

import com.backend.javaleap.entity.Account;
import com.backend.javaleap.entity.enums.AccountSubType;
import com.backend.javaleap.entity.enums.AccountType;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.exception.ConflictException;
import com.backend.javaleap.exception.ResourceNotFoundException;
import com.backend.javaleap.model.request.AccountCreateRequest;
import com.backend.javaleap.model.request.AccountUpdateRequest;
import com.backend.javaleap.model.response.AccountResponse;
import com.backend.javaleap.repository.AccountRepository;
import com.backend.javaleap.repository.JournalItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Chart of Accounts management with accounting safety rules. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    /** Which sub-types are valid under each account type. */
    private static final Map<AccountType, Set<AccountSubType>> ALLOWED = Map.of(
            AccountType.ASSET, EnumSet.of(AccountSubType.GROUP, AccountSubType.FIXED_ASSET, AccountSubType.CURRENT_ASSET,
                    AccountSubType.RECEIVABLE, AccountSubType.BANK, AccountSubType.CASH, AccountSubType.TAX_INPUT),
            AccountType.LIABILITY, EnumSet.of(AccountSubType.GROUP, AccountSubType.PAYABLE,
                    AccountSubType.CURRENT_LIABILITY, AccountSubType.TAX_OUTPUT),
            AccountType.EQUITY, EnumSet.of(AccountSubType.GROUP, AccountSubType.EQUITY, AccountSubType.RETAINED_EARNINGS),
            AccountType.INCOME, EnumSet.of(AccountSubType.GROUP, AccountSubType.OPERATING_REVENUE,
                    AccountSubType.OTHER_INCOME),
            AccountType.EXPENSE, EnumSet.of(AccountSubType.GROUP, AccountSubType.OPERATING_EXPENSE,
                    AccountSubType.OTHER_EXPENSE));

    private final AccountRepository accountRepository;
    private final JournalItemRepository journalItemRepository;
    private final AccountingMapper mapper;

    /** Whole chart in code order, with tree level. */
    @Transactional(readOnly = true)
    public List<AccountResponse> list() {
        List<Account> all = accountRepository.findAllByOrderByCodeAsc();
        Map<Long, Account> byId = new HashMap<>();
        all.forEach(a -> byId.put(a.getId(), a));
        return all.stream().map(a -> mapper.toResponse(a, level(a, byId))).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(Long id) {
        Account a = find(id);
        return mapper.toResponse(a, a.getParent() == null ? 0 : 1);
    }

    @Transactional
    public AccountResponse create(AccountCreateRequest req) {
        if (accountRepository.existsByCode(req.code())) {
            throw new ConflictException("Account code " + req.code() + " already exists");
        }
        Account parent = accountRepository.findByCode(req.parentCode().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Parent account not found: " + req.parentCode()));
        if (!Boolean.TRUE.equals(parent.getGroupAccount())) {
            throw new BadRequestException("Parent " + parent.getCode() + " is not a group account");
        }
        AccountSubType subType = Boolean.TRUE.equals(req.groupAccount()) ? AccountSubType.GROUP : req.subType();
        if (!Boolean.TRUE.equals(req.groupAccount()) && subType == AccountSubType.GROUP) {
            throw new BadRequestException("A posting account cannot have sub-type GROUP");
        }
        checkSubType(parent.getAccountType(), subType);

        Account saved = accountRepository.save(Account.builder()
                .code(req.code())
                .name(req.name().trim())
                .accountType(parent.getAccountType())
                .subType(subType)
                .parent(parent)
                .groupAccount(Boolean.TRUE.equals(req.groupAccount()))
                .reconcilable(Boolean.TRUE.equals(req.reconcilable()))
                .active(true)
                .description(req.description())
                .build());
        log.info("Account {} {} created under {}", saved.getCode(), saved.getName(), parent.getCode());
        return mapper.toResponse(saved, 1);
    }

    @Transactional
    public AccountResponse update(Long id, AccountUpdateRequest req) {
        Account a = find(id);
        boolean hasPostings = journalItemRepository.existsByAccountId(id);
        if (req.name() != null && !req.name().isBlank()) {
            a.setName(req.name().trim());
        }
        if (req.subType() != null && req.subType() != a.getSubType()) {
            if (hasPostings) {
                throw new BadRequestException("Sub-type cannot change: account " + a.getCode() + " has postings");
            }
            if (Boolean.TRUE.equals(a.getGroupAccount()) != (req.subType() == AccountSubType.GROUP)) {
                throw new BadRequestException("Group accounts must keep sub-type GROUP and posting accounts cannot use it");
            }
            checkSubType(a.getAccountType(), req.subType());
            a.setSubType(req.subType());
        }
        if (req.reconcilable() != null) {
            a.setReconcilable(req.reconcilable());
        }
        if (req.active() != null) {
            if (!req.active() && Boolean.TRUE.equals(a.getGroupAccount()) && accountRepository.existsByParentId(id)) {
                throw new BadRequestException("Deactivate the child accounts first");
            }
            a.setActive(req.active());
        }
        if (req.description() != null) {
            a.setDescription(req.description());
        }
        return mapper.toResponse(a, a.getParent() == null ? 0 : 1);
    }

    /** Only accounts without postings and without children can be deleted. */
    @Transactional
    public void delete(Long id) {
        Account a = find(id);
        if (journalItemRepository.existsByAccountId(id)) {
            throw new BadRequestException("Account " + a.getCode() + " has postings - deactivate it instead");
        }
        if (accountRepository.existsByParentId(id)) {
            throw new BadRequestException("Account " + a.getCode() + " has child accounts");
        }
        accountRepository.delete(a);
        log.info("Account {} deleted", a.getCode());
    }

    /** Resolves an active posting account by code (used by journal entries, contacts, products). */
    @Transactional(readOnly = true)
    public Account requirePostable(String code) {
        Account a = accountRepository.findByCode(code.trim())
                .orElseThrow(() -> new BadRequestException("Unknown account " + code));
        if (!a.isPostable()) {
            throw new BadRequestException("Account " + a.getCode() + " is a group or inactive account and cannot be used");
        }
        return a;
    }

    /** Default posting account of a sub-type, e.g. the receivable account. */
    @Transactional(readOnly = true)
    public Account defaultFor(AccountSubType subType) {
        return accountRepository.findFirstBySubTypeAndActiveTrueAndGroupAccountFalseOrderByCodeAsc(subType)
                .orElseThrow(() -> new BadRequestException("No active " + subType + " account in the chart of accounts"));
    }

    private static void checkSubType(AccountType type, AccountSubType subType) {
        if (!ALLOWED.get(type).contains(subType)) {
            throw new BadRequestException("Sub-type " + subType + " is not valid for a " + type + " account");
        }
    }

    private static int level(Account a, Map<Long, Account> byId) {
        int level = 0;
        Account cur = a;
        while (cur.getParent() != null && level < 10) {
            cur = byId.getOrDefault(cur.getParent().getId(), null);
            level++;
            if (cur == null) {
                break;
            }
        }
        return level;
    }

    private Account find(Long id) {
        return accountRepository.findWithParentById(id).orElseThrow(() -> new ResourceNotFoundException("Account", id));
    }
}
