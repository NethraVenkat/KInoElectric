package com.backend.javaleap.controller;

import com.backend.javaleap.entity.enums.PaymentStatus;
import com.backend.javaleap.entity.enums.PaymentType;
import com.backend.javaleap.model.request.PaymentRequest;
import com.backend.javaleap.model.response.PaymentResponse;
import com.backend.javaleap.service.PaymentService;
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
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;

/** Customer receipts and vendor payments via the Bank / Cash journal. */
@RestController
@RequestMapping(AppConstants.API_BASE + "/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Receipts from grid operators, payments to tile vendors")
public class PaymentController {

    private final PaymentService service;

    @GetMapping
    @Operation(summary = "Search payments")
    public ApiResponse<PageResponse<PaymentResponse>> search(
            @RequestParam(required = false) PaymentType type,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) Long contactId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(service.search(type, status, contactId, from, to, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "One payment")
    public ApiResponse<PaymentResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a payment against a posted invoice (bank transfer by default)")
    public ApiResponse<PaymentResponse> register(@Valid @RequestBody PaymentRequest request) {
        return ApiResponse.ok("Payment registered", service.register(request));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a payment (reverses its journal entry)")
    public ApiResponse<PaymentResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok("Payment cancelled", service.cancel(id));
    }
}
