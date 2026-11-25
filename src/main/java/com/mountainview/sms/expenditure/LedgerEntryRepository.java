package com.mountainview.sms.expenditure;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    /* A list of ledger entries| AKA the table*/
    
    List<LedgerEntry> findByEntryDateBetweenOrderByEntryDateAscIdAsc(LocalDate from, LocalDate to);
}
