package com.backend.javaleap.controller;

import com.backend.javaleap.model.ApiResponse;
import com.backend.javaleap.model.AppConstants;
import com.backend.javaleap.model.response.BalanceSheetResponse;
import com.backend.javaleap.model.response.BudgetResponse;
import com.backend.javaleap.model.response.ProfitLossResponse;
import com.backend.javaleap.service.BudgetService;
import com.backend.javaleap.service.FinancialReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Financial reports required by the use case: Balance Sheet, Profit &amp; Loss and Budget Report. */
@RestController
@RequestMapping(AppConstants.API_BASE + "/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Balance Sheet, Kinetic Energy P&L and Station Budget Report")
public class ReportController {

    private final FinancialReportService reportService;
    private final BudgetService budgetService;

    @GetMapping("/balance-sheet")
    @Operation(summary = "Balance Sheet as of a date (default today)")
    public ApiResponse<BalanceSheetResponse> balanceSheet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ApiResponse.ok(reportService.balanceSheet(asOf));
    }

    @GetMapping("/profit-loss")
    @Operation(summary = "Profit & Loss for a period (default: financial year to date)")
    public ApiResponse<ProfitLossResponse> profitLoss(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.profitLoss(from, to));
    }

    @GetMapping("/budget/{id}")
    @Operation(summary = "Station Budget Report: planned vs actual per zone")
    public ApiResponse<BudgetResponse> budget(@PathVariable Long id) {
        return ApiResponse.ok(budgetService.get(id));
    }
}
