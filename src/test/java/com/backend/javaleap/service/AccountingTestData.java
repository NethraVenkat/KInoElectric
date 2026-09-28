package com.backend.javaleap.service;

import com.backend.javaleap.entity.Account;
import com.backend.javaleap.entity.Journal;
import com.backend.javaleap.entity.User;
import com.backend.javaleap.entity.enums.AccountSubType;
import com.backend.javaleap.entity.enums.AccountType;
import com.backend.javaleap.entity.enums.JournalType;
import com.backend.javaleap.entity.enums.Role;
import com.backend.javaleap.repository.AccountRepository;
import com.backend.javaleap.repository.JournalRepository;
import com.backend.javaleap.repository.UserRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** Test fixture: a compact chart of accounts (same codes as V9) and the SAL/PUR/BNK/MISC journals. */
public class AccountingTestData {

    public final Map<String, Account> accounts = new HashMap<>();

    public AccountingTestData(ApplicationContext ctx, boolean withUsers) {
        AccountRepository accountRepo = ctx.getBean(AccountRepository.class);
        JournalRepository journalRepo = ctx.getBean(JournalRepository.class);

        group(accountRepo, "1000", "Assets", AccountType.ASSET);
        group(accountRepo, "2000", "Liabilities", AccountType.LIABILITY);
        group(accountRepo, "3000", "Equity", AccountType.EQUITY);
        group(accountRepo, "4000", "Income", AccountType.INCOME);
        group(accountRepo, "5000", "Expenses", AccountType.EXPENSE);
        post(accountRepo, "1100", "Kinetic Floor Grid Assets", "1000", AccountSubType.FIXED_ASSET);
        post(accountRepo, "1300", "Accounts Receivable - Power Grid", "1000", AccountSubType.RECEIVABLE);
        post(accountRepo, "1400", "GST Input Credit", "1000", AccountSubType.TAX_INPUT);
        post(accountRepo, "1500", "Bank - Current Account", "1000", AccountSubType.BANK);
        post(accountRepo, "2100", "Tile Vendor Creditors", "2000", AccountSubType.PAYABLE);
        post(accountRepo, "2300", "GST Output Payable", "2000", AccountSubType.TAX_OUTPUT);
        post(accountRepo, "3100", "Capital - Transit Authority Fund", "3000", AccountSubType.EQUITY);
        post(accountRepo, "4100", "Harvested Power Sales Revenue", "4000", AccountSubType.OPERATING_REVENUE);
        post(accountRepo, "4200", "Sustainability Subsidies", "4000", AccountSubType.OTHER_INCOME);
        post(accountRepo, "5100", "Piezoelectric Transducer Repairs", "5000", AccountSubType.OPERATING_EXPENSE);

        int year = LocalDate.now().getYear();
        journal(journalRepo, "SAL", "Sales Journal", JournalType.SALE, "INV", accounts.get("4100"), year);
        journal(journalRepo, "PUR", "Purchase Journal", JournalType.PURCHASE, "BILL", accounts.get("5100"), year);
        journal(journalRepo, "BNK", "Bank Journal", JournalType.BANK, "BNK", accounts.get("1500"), year);
        journal(journalRepo, "MISC", "Miscellaneous Journal", JournalType.GENERAL, "JE", null, year);

        if (withUsers) {
            UserRepository users = ctx.getBean(UserRepository.class);
            PasswordEncoder encoder = ctx.getBean(PasswordEncoder.class);
            for (String[] u : new String[][]{{"admin", "ADMIN"}, {"accountant", "ACCOUNTANT"}}) {
                if (users.findByUsername(u[0]).isEmpty()) {
                    users.save(User.builder().username(u[0]).email(u[0] + "@test.in")
                            .passwordHash(encoder.encode(KineticTestData.PASSWORD)).fullName(u[0])
                            .role(Role.valueOf(u[1])).enabled(true).build());
                }
            }
        }
    }

    public Account get(String code) {
        return accounts.get(code);
    }

    private void group(AccountRepository repo, String code, String name, AccountType type) {
        accounts.put(code, repo.save(Account.builder().code(code).name(name).accountType(type)
                .subType(AccountSubType.GROUP).groupAccount(true).build()));
    }

    private void post(AccountRepository repo, String code, String name, String parent, AccountSubType sub) {
        Account p = accounts.get(parent);
        accounts.put(code, repo.save(Account.builder().code(code).name(name).accountType(p.getAccountType())
                .subType(sub).parent(p)
                .reconcilable(sub == AccountSubType.RECEIVABLE || sub == AccountSubType.PAYABLE)
                .build()));
    }

    private static void journal(JournalRepository repo, String code, String name, JournalType type, String prefix,
                                Account defaultAccount, int year) {
        repo.save(Journal.builder().code(code).name(name).journalType(type).sequencePrefix(prefix)
                .defaultAccount(defaultAccount).sequenceYear(year).nextNumber(1).build());
    }
}
