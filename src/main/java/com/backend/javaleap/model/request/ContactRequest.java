package com.backend.javaleap.model.request;

import com.backend.javaleap.entity.enums.ContactType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Create or fully update a contact (regional power grid customer, tile vendor...). */
public record ContactRequest(
        @NotNull(message = "contactType is required") ContactType contactType,
        @NotBlank(message = "name is required") @Size(max = 150) String name,
        @Pattern(regexp = "^$|^[0-9]{2}[A-Za-z]{5}[0-9]{4}[A-Za-z][1-9A-Za-z][Zz][0-9A-Za-z]$",
                message = "gstin must be a valid 15-character GSTIN, e.g. 33AABCC1234D1Z5")
        String gstin,
        @Pattern(regexp = "^$|^[A-Za-z]{5}[0-9]{4}[A-Za-z]$", message = "pan must be like AABCC1234D")
        String pan,
        @Email(message = "email must be valid") @Size(max = 120) String email,
        @Pattern(regexp = "^$|^[+0-9 ()-]{6,20}$", message = "phone may contain digits, spaces, + - ( )")
        String phone,
        @Size(max = 100) String contactPerson,
        @Size(max = 255) String addressLine,
        @Size(max = 60) String city,
        @Size(max = 60) String state,
        @Pattern(regexp = "^$|^[0-9]{6}$", message = "pincode must be 6 digits") String pincode,
        @Size(max = 60) String country,
        @Min(value = 0, message = "paymentTermsDays must be 0-365")
        @Max(value = 365, message = "paymentTermsDays must be 0-365") Integer paymentTermsDays,
        @DecimalMin(value = "0.00", message = "creditLimit cannot be negative") BigDecimal creditLimit,
        @Size(max = 20) String receivableAccountCode,
        @Size(max = 20) String payableAccountCode,
        @Size(max = 500) String notes) {
}
