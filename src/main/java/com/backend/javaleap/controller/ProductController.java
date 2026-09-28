package com.backend.javaleap.controller;

import com.backend.javaleap.model.ApiResponse;
import com.backend.javaleap.model.AppConstants;
import com.backend.javaleap.entity.enums.ProductType;
import com.backend.javaleap.model.PageResponse;
import com.backend.javaleap.model.request.ProductRequest;
import com.backend.javaleap.model.response.ProductResponse;
import com.backend.javaleap.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Product master - harvested kWh, kinetic tiles, overhaul services. */
@RestController
@RequestMapping(AppConstants.API_BASE + "/accounting/products")
@RequiredArgsConstructor
@Tag(name = "Accounting - Products", description = "Goods and services bought or sold")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Search products")
    public ApiResponse<PageResponse<ProductResponse>> search(
            @RequestParam(required = false) ProductType type,
            @RequestParam(required = false) Boolean canBeSold,
            @RequestParam(required = false) Boolean canBePurchased,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "code", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.ok(productService.search(type, canBeSold, canBePurchased, active, q, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "One product")
    public ApiResponse<ProductResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(productService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a product")
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok("Product created", productService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product")
    public ApiResponse<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok("Product updated", productService.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Activate / deactivate")
    public ApiResponse<ProductResponse> setActive(@PathVariable Long id, @RequestParam boolean value) {
        return ApiResponse.ok(value ? "Product activated" : "Product deactivated", productService.setActive(id, value));
    }
}
