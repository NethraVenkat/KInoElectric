package com.backend.javaleap.controller;

import com.backend.javaleap.entity.enums.OrderStatus;
import com.backend.javaleap.model.request.CreateInvoiceRequest;
import com.backend.javaleap.model.request.EnergySalesOrderRequest;
import com.backend.javaleap.model.request.SalesOrderRequest;
import com.backend.javaleap.model.response.InvoiceResponse;
import com.backend.javaleap.model.response.SalesOrderResponse;
import com.backend.javaleap.service.SalesOrderService;
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

/** Sales orders - monthly energy export to the regional grid operator. */
@RestController
@RequestMapping(AppConstants.API_BASE + "/sales/orders")
@RequiredArgsConstructor
@Tag(name = "Sales - Orders", description = "Energy export SO -> confirm -> customer invoice")
public class SalesOrderController {

    private final SalesOrderService service;

    @GetMapping
    @Operation(summary = "Search sales orders")
    public ApiResponse<PageResponse<SalesOrderResponse>> search(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(service.search(status, customerId, q, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Sales order with lines")
    public ApiResponse<SalesOrderResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping("/energy")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generate the energy export order for a period from telemetry (kWh per sector)")
    public ApiResponse<SalesOrderResponse> energy(@Valid @RequestBody EnergySalesOrderRequest request) {
        return ApiResponse.ok("Energy sales order created", service.createEnergyOrder(request));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a manual draft SO")
    public ApiResponse<SalesOrderResponse> create(@Valid @RequestBody SalesOrderRequest request) {
        return ApiResponse.ok("Sales order created", service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a manual draft SO")
    public ApiResponse<SalesOrderResponse> update(@PathVariable Long id, @Valid @RequestBody SalesOrderRequest request) {
        return ApiResponse.ok("Sales order updated", service.update(id, request));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm (assigns SO number)")
    public ApiResponse<SalesOrderResponse> confirm(@PathVariable Long id) {
        return ApiResponse.ok("Sales order confirmed", service.confirm(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a draft or confirmed SO")
    public ApiResponse<SalesOrderResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok("Sales order cancelled", service.cancel(id));
    }

    @PostMapping("/{id}/invoice")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create the customer invoice (draft) from a confirmed SO")
    public ApiResponse<InvoiceResponse> invoice(@PathVariable Long id,
                                                @Valid @RequestBody(required = false) CreateInvoiceRequest request) {
        return ApiResponse.ok("Customer invoice created", service.createInvoice(id, request));
    }
}
