package com.backend.javaleap.service;

import com.backend.javaleap.entity.Account;
import com.backend.javaleap.entity.Contact;
import com.backend.javaleap.entity.enums.AccountSubType;
import com.backend.javaleap.entity.enums.ContactType;
import com.backend.javaleap.exception.BadRequestException;
import com.backend.javaleap.exception.ConflictException;
import com.backend.javaleap.exception.ResourceNotFoundException;
import com.backend.javaleap.model.PageResponse;
import com.backend.javaleap.model.request.ContactRequest;
import com.backend.javaleap.model.response.ContactResponse;
import com.backend.javaleap.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Contact master. Codes are generated per type (CUST-0001, VEND-0001, CONT-0001);
 * GSTIN is validated, normalised and unique; receivable/payable accounts default from the chart.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    /** 2-digit state code + 10-char PAN + entity number + 'Z' + checksum character. */
    private static final String GSTIN_REGEX = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$";

    private final ContactRepository contactRepository;
    private final AccountService accountService;
    private final AccountingMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<ContactResponse> search(ContactType type, Boolean active, String q, Pageable pageable) {
        Set<ContactType> types = type == null ? EnumSet.allOf(ContactType.class)
                : type == ContactType.BOTH ? EnumSet.of(ContactType.BOTH) : EnumSet.of(type, ContactType.BOTH);
        return PageResponse.from(contactRepository.search(types, active, blankToNull(q), pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ContactResponse get(Long id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public ContactResponse create(ContactRequest req) {
        Contact c = new Contact();
        c.setCode(nextCode(req.contactType()));
        apply(c, req);
        Contact saved = contactRepository.save(c);
        log.info("Contact {} '{}' created ({})", saved.getCode(), saved.getName(), saved.getContactType());
        return mapper.toResponse(saved);
    }

    @Transactional
    public ContactResponse update(Long id, ContactRequest req) {
        Contact c = find(id);
        apply(c, req);
        return mapper.toResponse(c);
    }

    /** Contacts are never hard-deleted (they appear on invoices and ledgers) - they are deactivated. */
    @Transactional
    public ContactResponse setActive(Long id, boolean active) {
        Contact c = find(id);
        c.setActive(active);
        return mapper.toResponse(c);
    }

    /** Active contact by id (used by journal lines and documents). */
    @Transactional(readOnly = true)
    public Contact requireActive(Long id) {
        Contact c = contactRepository.findById(id).orElseThrow(() -> new BadRequestException("Unknown contact " + id));
        if (!Boolean.TRUE.equals(c.getActive())) {
            throw new BadRequestException("Contact " + c.getCode() + " is inactive");
        }
        return c;
    }

    // ------------------------------------------------------------------ helpers

    private void apply(Contact c, ContactRequest req) {
        String gstin = blankToNull(req.gstin());
        if (gstin != null) {
            gstin = gstin.toUpperCase(Locale.ROOT);
            if (!gstin.matches(GSTIN_REGEX)) {
                throw new BadRequestException("Invalid GSTIN " + gstin);
            }
            final Long selfId = c.getId();
            contactRepository.findByGstin(gstin).filter(o -> !o.getId().equals(selfId)).ifPresent(o -> {
                throw new ConflictException("GSTIN already registered to contact " + o.getCode());
            });
        }
        String pan = blankToNull(req.pan());
        if (pan != null) {
            pan = pan.toUpperCase(Locale.ROOT);
        }
        if (gstin != null && pan != null && !gstin.substring(2, 12).equals(pan)) {
            throw new BadRequestException("PAN does not match characters 3-12 of the GSTIN");
        }
        if (gstin != null && pan == null) {
            pan = gstin.substring(2, 12);
        }

        c.setContactType(req.contactType());
        c.setName(req.name().trim());
        c.setGstin(gstin);
        c.setPan(pan);
        c.setEmail(blankToNull(req.email()));
        c.setPhone(blankToNull(req.phone()));
        c.setContactPerson(blankToNull(req.contactPerson()));
        c.setAddressLine(blankToNull(req.addressLine()));
        c.setCity(blankToNull(req.city()));
        c.setState(blankToNull(req.state()));
        c.setPincode(blankToNull(req.pincode()));
        c.setCountry(req.country() == null || req.country().isBlank() ? "India" : req.country().trim());
        c.setPaymentTermsDays(req.paymentTermsDays() == null ? 30 : req.paymentTermsDays());
        c.setCreditLimit(req.creditLimit());
        c.setNotes(blankToNull(req.notes()));
        if (c.getActive() == null) {
            c.setActive(true);
        }

        c.setReceivableAccount(c.isCustomer()
                ? controlAccount(req.receivableAccountCode(), AccountSubType.RECEIVABLE) : null);
        c.setPayableAccount(c.isVendor()
                ? controlAccount(req.payableAccountCode(), AccountSubType.PAYABLE) : null);
    }

    private Account controlAccount(String code, AccountSubType expected) {
        if (code == null || code.isBlank()) {
            return accountService.defaultFor(expected);
        }
        Account a = accountService.requirePostable(code);
        if (a.getSubType() != expected) {
            throw new BadRequestException("Account " + a.getCode() + " is not a " + expected + " account");
        }
        return a;
    }

    private String nextCode(ContactType type) {
        String prefix = switch (type) {
            case CUSTOMER -> "CUST-";
            case VENDOR -> "VEND-";
            case BOTH -> "CONT-";
        };
        String max = contactRepository.findMaxCode(prefix);
        int next = 1;
        if (max != null) {
            try {
                next = Integer.parseInt(max.substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
                next = (int) contactRepository.count() + 1;
            }
        }
        return String.format("%s%04d", prefix, next);
    }

    private Contact find(Long id) {
        return contactRepository.findWithAccountsById(id).orElseThrow(() -> new ResourceNotFoundException("Contact", id));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
