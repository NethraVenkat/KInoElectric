package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.ContactType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Contact master record. */
public record ContactResponse(
        Long id,
        String code,
        ContactType contactType,
        String name,
        String gstin,
        String pan,
        String email,
        String phone,
        String contactPerson,
        String addressLine,
        String city,
        String state,
        String pincode,
        String country,
        Integer paymentTermsDays,
        BigDecimal creditLimit,
        String receivableAccountCode,
        String payableAccountCode,
        boolean active,
        String notes,
        LocalDateTime createdAt) {
}
