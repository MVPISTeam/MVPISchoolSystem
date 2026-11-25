package com.mountainview.sms.expenditure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mountainview.sms.common.NotFoundException;
import com.mountainview.sms.expenditure.LedgerDtos.CategoryTotal;
import com.mountainview.sms.expenditure.LedgerDtos.LedgerEntryRequest;
import com.mountainview.sms.expenditure.LedgerDtos.LedgerEntryResponse;
import com.mountainview.sms.expenditure.LedgerDtos.LedgerSummary;

/* This class acts on the database */

@Service
@Transactional
public class LedgerService {

    private final LedgerEntryRepository entries;

    public LedgerService(LedgerEntryRepository entries) {
        this.entries = entries;
    }

    
    public LedgerEntryResponse create(LedgerEntryRequest request) {
        LedgerEntry entry = new LedgerEntry();
        copy(request, entry);
        return LedgerEntryResponse.from(entries.save(entry));
    }

    
    public LedgerEntryResponse update(Long id, LedgerEntryRequest request) {
        LedgerEntry entry = entries.findById(id)
                .orElseThrow(() -> new NotFoundException("No ledger entry with id " + id));
        copy(request, entry);
        return LedgerEntryResponse.from(entries.save(entry));
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> list(LocalDate from, LocalDate to, EntryType type, LedgerCategory category) {
        checkPeriod(from, to);
        List<LedgerEntryResponse> results = new ArrayList<>();
        for (LedgerEntry entry : entries.findByEntryDateBetweenOrderByEntryDateAscIdAsc(from, to)) {
            boolean typeMatches = type == null || type == entry.getType();
            boolean categoryMatches = category == null || category == entry.getCategory();
            if (typeMatches && categoryMatches) {
                results.add(LedgerEntryResponse.from(entry));
            }
        }
        return results;
    }

    
    @Transactional(readOnly = true)
    public LedgerSummary summary(LocalDate from, LocalDate to) {
        checkPeriod(from, to);
        List<LedgerEntry> period = entries.findByEntryDateBetweenOrderByEntryDateAscIdAsc(from, to);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        Map<LedgerCategory, BigDecimal> incomeByCategory = new EnumMap<>(LedgerCategory.class);
        Map<LedgerCategory, BigDecimal> expensesByCategory = new EnumMap<>(LedgerCategory.class);

        for (LedgerEntry entry : period) {
            if (entry.getType() == EntryType.INCOME) {
                totalIncome = totalIncome.add(entry.getAmount());
                incomeByCategory.merge(entry.getCategory(), entry.getAmount(), BigDecimal::add);
            } else {
                totalExpenses = totalExpenses.add(entry.getAmount());
                expensesByCategory.merge(entry.getCategory(), entry.getAmount(), BigDecimal::add);
            }
        }

        return new LedgerSummary(
                from,
                to,
                totalIncome,
                totalExpenses,
                totalIncome.subtract(totalExpenses),
                toTotals(incomeByCategory),
                toTotals(expensesByCategory),
                period.stream().map(LedgerEntryResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public LedgerSummary monthlyReport(int year, int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }
        YearMonth yearMonth = YearMonth.of(year, month);
        return summary(yearMonth.atDay(1), yearMonth.atEndOfMonth());
    }

    private static List<CategoryTotal> toTotals(Map<LedgerCategory, BigDecimal> totals) {
        List<CategoryTotal> list = new ArrayList<>();
        totals.forEach((category, total) -> list.add(new CategoryTotal(category, total)));
        return list;
    }

    private static void checkPeriod(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must be on or before 'to'");
        }
    }

    private void copy(LedgerEntryRequest request, LedgerEntry entry) {
        entry.setType(request.type());
        entry.setCategory(request.category());
        entry.setAmount(request.amount());
        entry.setEntryDate(request.entryDate());
        entry.setDescription(request.description());
        entry.setPayee(request.payee());
        entry.setReferenceNumber(request.referenceNumber());
    }
}
