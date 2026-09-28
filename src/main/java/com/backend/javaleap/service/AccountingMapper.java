package com.backend.javaleap.service;

import com.backend.javaleap.entity.Account;
import com.backend.javaleap.entity.Contact;
import com.backend.javaleap.entity.Journal;
import com.backend.javaleap.entity.JournalEntry;
import com.backend.javaleap.entity.JournalItem;
import com.backend.javaleap.entity.Product;
import com.backend.javaleap.entity.User;
import com.backend.javaleap.model.response.AccountResponse;
import com.backend.javaleap.model.response.ContactResponse;
import com.backend.javaleap.model.response.JournalEntryResponse;
import com.backend.javaleap.model.response.JournalLineResponse;
import com.backend.javaleap.model.response.JournalResponse;
import com.backend.javaleap.model.response.ProductResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/** Accounting entities -> DTOs. */
@Component
public class AccountingMapper {

    public AccountResponse toResponse(Account a, int level) {
        return new AccountResponse(a.getId(), a.getCode(), a.getName(), a.getAccountType(), a.getSubType(),
                a.getParent() == null ? null : a.getParent().getCode(), level,
                Boolean.TRUE.equals(a.getGroupAccount()), Boolean.TRUE.equals(a.getReconcilable()),
                Boolean.TRUE.equals(a.getActive()), a.getDescription());
    }

    public JournalResponse toResponse(Journal j) {
        Account d = j.getDefaultAccount();
        return new JournalResponse(j.getId(), j.getCode(), j.getName(), j.getJournalType(),
                d == null ? null : d.getCode(), d == null ? null : d.getName(),
                j.getSequencePrefix(), j.previewNextNumber(), Boolean.TRUE.equals(j.getActive()));
    }

    public ContactResponse toResponse(Contact c) {
        return new ContactResponse(c.getId(), c.getCode(), c.getContactType(), c.getName(), c.getGstin(), c.getPan(),
                c.getEmail(), c.getPhone(), c.getContactPerson(), c.getAddressLine(), c.getCity(), c.getState(),
                c.getPincode(), c.getCountry(), c.getPaymentTermsDays(), c.getCreditLimit(),
                c.getReceivableAccount() == null ? null : c.getReceivableAccount().getCode(),
                c.getPayableAccount() == null ? null : c.getPayableAccount().getCode(),
                Boolean.TRUE.equals(c.getActive()), c.getNotes(), c.getCreatedAt());
    }

    public ProductResponse toResponse(Product p) {
        return new ProductResponse(p.getId(), p.getCode(), p.getName(), p.getProductType(), p.getUom(), p.getHsnSac(),
                p.getSalePrice(), p.getPurchasePrice(), p.getGstRate(),
                Boolean.TRUE.equals(p.getCanBeSold()), Boolean.TRUE.equals(p.getCanBePurchased()),
                p.getIncomeAccount() == null ? null : p.getIncomeAccount().getCode(),
                p.getExpenseAccount() == null ? null : p.getExpenseAccount().getCode(),
                p.getDescription(), Boolean.TRUE.equals(p.getActive()));
    }

    /** Full entry with lines (items/accounts must be loaded). */
    public JournalEntryResponse toDetail(JournalEntry e) {
        return build(e, e.getItems().stream().map(this::toLine).toList());
    }

    /** List view without lines. */
    public JournalEntryResponse toSummary(JournalEntry e) {
        return build(e, List.of());
    }

    private JournalEntryResponse build(JournalEntry e, List<JournalLineResponse> lines) {
        return new JournalEntryResponse(e.getId(), e.getEntryNumber(), e.getJournal().getCode(), e.getEntryDate(),
                e.getReference(), e.getNarration(), e.getStatus(), e.getSourceType(), e.getSourceId(),
                e.getTotalDebit(), e.getTotalCredit(),
                e.getReversalOf() == null ? null : e.getReversalOf().getId(),
                e.getReversalOf() == null ? null : e.getReversalOf().getEntryNumber(),
                username(e.getCreatedBy()), e.getPostedAt(), username(e.getPostedBy()), lines);
    }

    public JournalLineResponse toLine(JournalItem i) {
        return new JournalLineResponse(i.getLineNo(), i.getAccount().getCode(), i.getAccount().getName(),
                i.getContact() == null ? null : i.getContact().getId(),
                i.getContact() == null ? null : i.getContact().getName(),
                i.getAnalyticAccount() == null ? null : i.getAnalyticAccount().getCode(),
                i.getLabel(), i.getDebit(), i.getCredit());
    }

    private static String username(User u) {
        return u == null ? null : u.getUsername();
    }
}
