package com.backend.javaleap.service;

import com.backend.javaleap.entity.Journal;
import com.backend.javaleap.exception.ResourceNotFoundException;
import com.backend.javaleap.model.request.JournalUpdateRequest;
import com.backend.javaleap.model.response.JournalResponse;
import com.backend.javaleap.repository.JournalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Sales / Purchase / Cash / Bank / Misc journals. */
@Service
@RequiredArgsConstructor
public class JournalService {

    private final JournalRepository journalRepository;
    private final AccountService accountService;
    private final AccountingMapper mapper;

    @Transactional(readOnly = true)
    public List<JournalResponse> list() {
        return journalRepository.findAllByOrderByCodeAsc().stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public JournalResponse update(Long id, JournalUpdateRequest req) {
        Journal j = journalRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Journal", id));
        if (req.name() != null && !req.name().isBlank()) {
            j.setName(req.name().trim());
        }
        if (req.defaultAccountCode() != null) {
            j.setDefaultAccount(req.defaultAccountCode().isBlank() ? null
                    : accountService.requirePostable(req.defaultAccountCode()));
        }
        if (req.active() != null) {
            j.setActive(req.active());
        }
        return mapper.toResponse(j);
    }

    /** Active journal by code. */
    @Transactional(readOnly = true)
    public Journal require(String code) {
        Journal j = journalRepository.findByCode(code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Journal not found: " + code));
        if (!Boolean.TRUE.equals(j.getActive())) {
            throw new com.backend.javaleap.exception.BadRequestException("Journal " + j.getCode() + " is inactive");
        }
        return j;
    }
}
