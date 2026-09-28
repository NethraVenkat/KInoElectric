package com.backend.javaleap.model.request;

import com.backend.javaleap.entity.enums.AccountSubType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Add an account under a group account; its type (asset, income...) is inherited from the parent. */
public record AccountCreateRequest(
        @NotBlank(message = "code is required")
        @Pattern(regexp = "^[0-9]{3,6}$", message = "code must be 3-6 digits, e.g. 5300")
        String code,
        @NotBlank(message = "name is required") @Size(max = 120) String name,
        @NotBlank(message = "parentCode is required") String parentCode,
        @NotNull(message = "subType is required") AccountSubType subType,
        Boolean groupAccount,
        Boolean reconcilable,
        @Size(max = 255) String description) {
}
