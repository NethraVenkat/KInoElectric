package com.backend.javaleap.controller;

import com.backend.javaleap.model.ApiResponse;
import com.backend.javaleap.model.AppConstants;
import com.backend.javaleap.model.request.BudgetRequest;
import com.backend.javaleap.model.response.BudgetResponse;
import com.backend.javaleap.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Zone budgets: read by Admin + Accountant, created and confirmed by the Transit Energy Director. */
@RestController
@RequestMapping(AppConstants.API_BASE + "/budgets")
@RequiredArgsConstructor
@Tag(name = "Budgets", description = "Planned revenue and maintenance cost per transit station zone")
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    @Operation(summary = "All budgets with planned vs actual totals")
    public ApiResponse<List<BudgetResponse>> list() {
        return ApiResponse.ok(budgetService.list());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Budget report: planned vs actual per zone and account")
    public ApiResponse<BudgetResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(budgetService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a draft budget")
    public ApiResponse<BudgetResponse> create(@Valid @RequestBody BudgetRequest request) {
        return ApiResponse.ok("Budget created", budgetService.create(request));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Confirm a draft budget")
    public ApiResponse<BudgetResponse> confirm(@PathVariable Long id) {
        return ApiResponse.ok("Budget confirmed", budgetService.confirm(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel a budget")
    public ApiResponse<BudgetResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok("Budget cancelled", budgetService.cancel(id));
    }
}
