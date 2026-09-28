package com.backend.javaleap.controller;

import com.backend.javaleap.model.ApiResponse;
import com.backend.javaleap.model.AppConstants;
import com.backend.javaleap.model.request.AccountCreateRequest;
import com.backend.javaleap.model.request.AccountUpdateRequest;
import com.backend.javaleap.model.response.AccountResponse;
import com.backend.javaleap.model.response.LedgerResponse;
import com.backend.javaleap.service.AccountService;
import com.backend.javaleap.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Chart of Accounts (read: Admin + Accountant, write: Admin) and account ledgers. */
@RestController
@RequestMapping(AppConstants.API_BASE + "/accounting/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounting - Chart of Accounts", description = "Accounts tree and general ledger")
public class AccountController {

    private final AccountService accountService;
    private final LedgerService ledgerService;

    @GetMapping
    @Operation(summary = "Full chart of accounts")
    public ApiResponse<List<AccountResponse>> list() {
        return ApiResponse.ok(accountService.list());
    }

    @GetMapping("/{id}")
    @Operation(summary = "One account")
    public ApiResponse<AccountResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(accountService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add an account under a group")
    public ApiResponse<AccountResponse> create(@Valid @RequestBody AccountCreateRequest request) {
        return ApiResponse.ok("Account created", accountService.create(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update name, sub-type (if no postings), reconcilable, active")
    public ApiResponse<AccountResponse> update(@PathVariable Long id, @Valid @RequestBody AccountUpdateRequest request) {
        return ApiResponse.ok("Account updated", accountService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an unused account")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return ApiResponse.ok("Account deleted", null);
    }

    @GetMapping("/{id}/ledger")
    @Operation(summary = "General ledger of an account with running balance")
    public ApiResponse<LedgerResponse> ledger(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(ledgerService.ledger(id, from, to));
    }
}
