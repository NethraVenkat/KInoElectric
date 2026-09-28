package com.backend.javaleap.repository.projection;

import com.backend.javaleap.entity.enums.AlertSeverity;

/** Number of unresolved alerts per severity. */
public interface AlertCountView {
    AlertSeverity getSeverity();
    Long getTotal();
}
