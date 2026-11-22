package com.mountainview.sms.expenditure;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByEntryDateBetweenOrderByEntryDateAscIdAsc(LocalDate from, LocalDate to);
}
