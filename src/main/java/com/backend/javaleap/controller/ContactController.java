package com.backend.javaleap.controller;

import com.backend.javaleap.model.ApiResponse;
import com.backend.javaleap.model.AppConstants;
import com.backend.javaleap.entity.enums.ContactType;
import com.backend.javaleap.model.PageResponse;
import com.backend.javaleap.model.request.ContactRequest;
import com.backend.javaleap.model.response.ContactResponse;
import com.backend.javaleap.service.ContactService;
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

/** Contact master - customers (regional power grids) and vendors (tile manufacturers). */
@RestController
@RequestMapping(AppConstants.API_BASE + "/accounting/contacts")
@RequiredArgsConstructor
@Tag(name = "Accounting - Contacts", description = "Customers and vendors")
public class ContactController {

    private final ContactService contactService;

    @GetMapping
    @Operation(summary = "Search contacts (type=CUSTOMER also returns BOTH)")
    public ApiResponse<PageResponse<ContactResponse>> search(
            @RequestParam(required = false) ContactType type,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.ok(contactService.search(type, active, q, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "One contact")
    public ApiResponse<ContactResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(contactService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a customer or vendor (code generated automatically)")
    public ApiResponse<ContactResponse> create(@Valid @RequestBody ContactRequest request) {
        return ApiResponse.ok("Contact created", contactService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a contact")
    public ApiResponse<ContactResponse> update(@PathVariable Long id, @Valid @RequestBody ContactRequest request) {
        return ApiResponse.ok("Contact updated", contactService.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Activate / deactivate (contacts are never deleted)")
    public ApiResponse<ContactResponse> setActive(@PathVariable Long id, @RequestParam boolean value) {
        return ApiResponse.ok(value ? "Contact activated" : "Contact deactivated", contactService.setActive(id, value));
    }
}
