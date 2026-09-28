package com.backend.javaleap.model.response;

import java.math.BigDecimal;
import java.util.List;

/** A titled block of a financial statement with its total. */
public record StatementSection(String title, List<StatementLine> lines, BigDecimal total) {
}
