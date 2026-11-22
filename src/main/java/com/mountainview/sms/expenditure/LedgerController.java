 package com.mountainview.sms.expenditure;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mountainview.sms.expenditure.LedgerDtos.LedgerEntryRequest;
import com.mountainview.sms.expenditure.LedgerDtos.LedgerEntryResponse;
import com.mountainview.sms.expenditure.LedgerDtos.LedgerSummary;
import com.mountainview.sms.security.Access;

import jakarta.validation.Valid;

/** AC-1: only Principal, Vice Principal and Bursar reach these endpoints. */
@RestController
@RequestMapping("/api/expenditure")
@PreAuthorize(Access.FINANCE)
public class LedgerController {

    private final LedgerService service;

    public LedgerController(LedgerService service) {
        this.service = service;
    }

    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerEntryResponse create(@Valid @RequestBody LedgerEntryRequest request) {
        return service.create(request);
    }

    @PutMapping("/entries/{id}")
    public LedgerEntryResponse update(@PathVariable Long id, @Valid @RequestBody LedgerEntryRequest request) {
        return service.update(id, request);
    }

    /** GET /api/expenditure/entries?from=2026-11-01&to=2026-11-30&type=EXPENSE&category=UTILITIES */
    @GetMapping("/entries")
    public List<LedgerEntryResponse> list(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) EntryType type,
            @RequestParam(required = false) LedgerCategory category) {
        return service.list(from, to, type, category);
    }

    /** GET /api/expenditure/summary?from=2026-09-01&to=2026-12-18 */
    @GetMapping("/summary")
    public LedgerSummary summary(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.summary(from, to);
    }

    /** GET /api/expenditure/reports/monthly?year=2026&month=11 */
    @GetMapping("/reports/monthly")
    public LedgerSummary monthlyReport(@RequestParam int year, @RequestParam int month) {
        return service.monthlyReport(year, month);
    }
}
