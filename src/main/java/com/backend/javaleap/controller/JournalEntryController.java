package com.backend.javaleap.controller;

import com.backend.javaleap.model.ApiResponse;
import com.backend.javaleap.model.AppConstants;
import com.backend.javaleap.entity.enums.EntrySourceType;
import com.backend.javaleap.entity.enums.EntryStatus;
import com.backend.javaleap.model.PageResponse;
import com.backend.javaleap.model.request.JournalEntryRequest;
import com.backend.javaleap.model.request.ReverseEntryRequest;
import com.backend.javaleap.model.response.JournalEntryResponse;
import com.backend.javaleap.service.JournalEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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

/** Journal entries: draft -> post -> (reverse). */
@RestController
@RequestMapping(AppConstants.API_BASE + "/accounting/journal-entries")
@RequiredArgsConstructor
@Tag(name = "Accounting - Journal entries", description = "Double-entry vouchers")
public class JournalEntryController {

    private final JournalEntryService journalEntryService;

    @GetMapping
    @Operation(summary = "Search entries")
    public ApiResponse<PageResponse<JournalEntryResponse>> search(
            @RequestParam(required = false) String journalCode,
            @RequestParam(required = false) EntryStatus status,
            @RequestParam(required = false) EntrySourceType sourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = {"entryDate", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(journalEntryService.search(journalCode, status, sourceType, from, to, q, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Entry with all lines")
    public ApiResponse<JournalEntryResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(journalEntryService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a balanced draft entry")
    public ApiResponse<JournalEntryResponse> create(@Valid @RequestBody JournalEntryRequest request) {
        return ApiResponse.ok("Draft entry created", journalEntryService.createDraft(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a draft entry")
    public ApiResponse<JournalEntryResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody JournalEntryRequest request) {
        return ApiResponse.ok("Draft entry updated", journalEntryService.updateDraft(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a draft entry")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        journalEntryService.deleteDraft(id);
        return ApiResponse.ok("Draft entry deleted", null);
    }

    @PostMapping("/{id}/post")
    @Operation(summary = "Post a draft (assigns the document number, makes it immutable)")
    public ApiResponse<JournalEntryResponse> post(@PathVariable Long id) {
        return ApiResponse.ok("Entry posted", journalEntryService.post(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a draft (kept for audit)")
    public ApiResponse<JournalEntryResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok("Entry cancelled", journalEntryService.cancelDraft(id));
    }

    @PostMapping("/{id}/reverse")
    @Operation(summary = "Reverse a posted entry with a mirror entry")
    public ApiResponse<JournalEntryResponse> reverse(@PathVariable Long id,
                                                     @Valid @RequestBody ReverseEntryRequest request) {
        return ApiResponse.ok("Entry reversed", journalEntryService.reverse(id, request));
    }
}
