package com.backend.javaleap.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The transit energy organisation issuing invoices ({@code app.company.*}).
 * stateCode decides CGST+SGST (same state) vs IGST (other state).
 */
@ConfigurationProperties(prefix = "app.company")
public record CompanyProperties(String name, String gstin, String stateCode, String state, String address) {
}
