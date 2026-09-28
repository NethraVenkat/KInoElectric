package com.backend.javaleap.controller;

import com.backend.javaleap.entity.enums.InvoiceStatus;
import com.backend.javaleap.entity.enums.InvoiceType;
import com.backend.javaleap.entity.enums.PaymentState;
import com.backend.javaleap.model.request.InvoiceUpdateRequest;
import com.backend.javaleap.model.response.InvoiceResponse;
import com.backend.javaleap.service.InvoiceService;
import com.backend.javaleap.model.ApiResponse;
import com.backend.javaleap.model.AppConstants;
import com.backend.javaleap.model.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Customer invoices and vendor bills. */
@RestController
@RequestMapping(AppConstants.API_BASE + "/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Customer invoices and vendor bills: post, cancel")
public class InvoiceController {

    private final InvoiceService service;

    @GetMapping
    @Operation(summary = "Search invoices (overdueOnly=true lists unpaid invoices past due date)")
    public ApiResponse<PageResponse<InvoiceResponse>> search(
            @RequestParam(required = false) InvoiceType type,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) PaymentState paymentState,
            @RequestParam(required = false) Long contactId,
            @RequestParam(defaultValue = "false") boolean overdueOnly,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(service.search(type, status, paymentState, contactId, overdueOnly, q, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Invoice with lines, GST split and payments")
    public ApiResponse<InvoiceResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit dates / vendor reference / notes of a draft")
    public ApiResponse<InvoiceResponse> update(@PathVariable Long id, @Valid @RequestBody InvoiceUpdateRequest request) {
        return ApiResponse.ok("Invoice updated", service.updateDraft(id, request));
    }

    @PostMapping("/{id}/post")
    @Operation(summary = "Post: creates the journal entry and invoice number")
    public ApiResponse<InvoiceResponse> post(@PathVariable Long id) {
        return ApiResponse.ok("Invoice posted", service.post(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel (posted invoices are reversed in the books)")
    public ApiResponse<InvoiceResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok("Invoice cancelled", service.cancel(id));
    }
}
