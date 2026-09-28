package com.backend.javaleap.controller;

import com.backend.javaleap.entity.enums.OrderStatus;
import com.backend.javaleap.model.request.CreateInvoiceRequest;
import com.backend.javaleap.model.request.PurchaseOrderRequest;
import com.backend.javaleap.model.response.InvoiceResponse;
import com.backend.javaleap.model.response.PurchaseOrderResponse;
import com.backend.javaleap.service.PurchaseOrderService;
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

/** Purchase orders for replacement tiles and transducer overhauls. */
@RestController
@RequestMapping(AppConstants.API_BASE + "/purchase/orders")
@RequiredArgsConstructor
@Tag(name = "Purchase - Orders", description = "PO -> confirm -> vendor bill")
public class PurchaseOrderController {

    private final PurchaseOrderService service;

    @GetMapping
    @Operation(summary = "Search purchase orders")
    public ApiResponse<PageResponse<PurchaseOrderResponse>> search(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(service.search(status, vendorId, q, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Purchase order with lines")
    public ApiResponse<PurchaseOrderResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a draft PO (e.g. replacement piezo tiles)")
    public ApiResponse<PurchaseOrderResponse> create(@Valid @RequestBody PurchaseOrderRequest request) {
        return ApiResponse.ok("Purchase order created", service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a draft PO")
    public ApiResponse<PurchaseOrderResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody PurchaseOrderRequest request) {
        return ApiResponse.ok("Purchase order updated", service.update(id, request));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm (assigns PO number)")
    public ApiResponse<PurchaseOrderResponse> confirm(@PathVariable Long id) {
        return ApiResponse.ok("Purchase order confirmed", service.confirm(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a draft or confirmed PO")
    public ApiResponse<PurchaseOrderResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok("Purchase order cancelled", service.cancel(id));
    }

    @PostMapping("/{id}/bill")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create the vendor bill (draft) from a confirmed PO")
    public ApiResponse<InvoiceResponse> bill(@PathVariable Long id,
                                             @Valid @RequestBody(required = false) CreateInvoiceRequest request) {
        return ApiResponse.ok("Vendor bill created", service.createBill(id, request));
    }
}
