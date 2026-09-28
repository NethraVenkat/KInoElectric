package com.backend.javaleap.controller;

import com.backend.javaleap.model.ApiResponse;
import com.backend.javaleap.model.AppConstants;
import com.backend.javaleap.model.request.JournalUpdateRequest;
import com.backend.javaleap.model.response.JournalResponse;
import com.backend.javaleap.model.response.TrialBalanceResponse;
import com.backend.javaleap.service.JournalService;
import com.backend.javaleap.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Journals and the trial balance. */
@RestController
@RequestMapping(AppConstants.API_BASE + "/accounting")
@RequiredArgsConstructor
@Tag(name = "Accounting - Journals", description = "Sales, Purchase, Cash, Bank and Misc journals; trial balance")
public class JournalController {

    private final JournalService journalService;
    private final LedgerService ledgerService;

    @GetMapping("/journals")
    @Operation(summary = "All journals with next document number")
    public ApiResponse<List<JournalResponse>> list() {
        return ApiResponse.ok(journalService.list());
    }

    @PatchMapping("/journals/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update journal name, default account or active flag")
    public ApiResponse<JournalResponse> update(@PathVariable Long id, @Valid @RequestBody JournalUpdateRequest request) {
        return ApiResponse.ok("Journal updated", journalService.update(id, request));
    }

    @GetMapping("/trial-balance")
    @Operation(summary = "Trial balance as of a date (default today)")
    public ApiResponse<TrialBalanceResponse> trialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ApiResponse.ok(ledgerService.trialBalance(asOf));
    }
}
