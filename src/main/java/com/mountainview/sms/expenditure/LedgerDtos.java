package com.mountainview.sms.expenditure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Every request and response shape for the expenditure feature. */
public final class LedgerDtos {

    private LedgerDtos() {
    }

    
    public record LedgerEntryRequest(
            @NotNull EntryType type,
            @NotNull LedgerCategory category,
            @NotNull @Positive BigDecimal amount,
            @NotNull LocalDate entryDate,
            @Size(max = 255) String description,
            @Size(max = 255) String payee,
            @Size(max = 100) String referenceNumber) {
    }

    public record LedgerEntryResponse(
            Long id,
            EntryType type,
            LedgerCategory category,
            BigDecimal amount,
            LocalDate entryDate,
            String description,
            String payee,
            String referenceNumber) {

        public static LedgerEntryResponse from(LedgerEntry entry) {
            return new LedgerEntryResponse(entry.getId(), entry.getType(), entry.getCategory(), entry.getAmount(),
                    entry.getEntryDate(), entry.getDescription(), entry.getPayee(), entry.getReferenceNumber());
        }
    }

    public record CategoryTotal(LedgerCategory category, BigDecimal total) {
    }

    
    public record LedgerSummary(
            LocalDate from,
            LocalDate to,
            BigDecimal totalIncome,
            BigDecimal totalExpenses,
            BigDecimal balance,
            List<CategoryTotal> incomeByCategory,
            List<CategoryTotal> expensesByCategory,
            List<LedgerEntryResponse> entries) {
    }
}
