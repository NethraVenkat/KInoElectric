package com.backend.javaleap.model.response;

import com.backend.javaleap.entity.enums.AccountSubType;
import com.backend.javaleap.entity.enums.AccountType;

/** Chart of Accounts row. */
public record AccountResponse(
        Long id,
        String code,
        String name,
        AccountType accountType,
        AccountSubType subType,
        String parentCode,
        int level,
        boolean groupAccount,
        boolean reconcilable,
        boolean active,
        String description) {
}
