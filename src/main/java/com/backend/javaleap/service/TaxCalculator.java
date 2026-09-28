package com.backend.javaleap.service;

import com.backend.javaleap.entity.Contact;
import com.backend.javaleap.model.CompanyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Line amounts and Indian GST split.
 * <ul>
 *   <li>subtotal = quantity x unit price (2 dp), tax = subtotal x GST% (2 dp)</li>
 *   <li>Place of supply = first 2 digits of the contact's GSTIN (or company state if unregistered)</li>
 *   <li>Same state as the company: CGST + SGST (half each); other state: IGST</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class TaxCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final CompanyProperties company;

    public record LineAmounts(BigDecimal subtotal, BigDecimal tax, BigDecimal total) { }

    public record GstSplit(String placeOfSupply, BigDecimal cgst, BigDecimal sgst, BigDecimal igst) { }

    public LineAmounts line(BigDecimal quantity, BigDecimal unitPrice, BigDecimal gstRate) {
        BigDecimal subtotal = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = subtotal.multiply(gstRate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        return new LineAmounts(subtotal, tax, subtotal.add(tax));
    }

    public GstSplit split(BigDecimal totalTax, Contact contact) {
        String pos = placeOfSupply(contact);
        if (pos == null || pos.equals(company.stateCode())) {
            BigDecimal cgst = totalTax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            return new GstSplit(pos == null ? company.stateCode() : pos, cgst, totalTax.subtract(cgst), BigDecimal.ZERO);
        }
        return new GstSplit(pos, BigDecimal.ZERO, BigDecimal.ZERO, totalTax);
    }

    public String placeOfSupply(Contact contact) {
        if (contact.getGstin() != null && contact.getGstin().length() >= 2) {
            return contact.getGstin().substring(0, 2);
        }
        if (contact.getState() == null || contact.getState().equalsIgnoreCase(company.state())) {
            return company.stateCode();
        }
        return null;
    }
}
