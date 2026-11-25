package com.mountainview.sms.expenditure;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/* A single ledger entry that is entered into the expenditure table*/

@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
@NoArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerCategory category;

    /** Always positive. Whether it adds or subtracts is decided by type. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate entryDate;

    private String description;

    /** Who was paid, or who the money came from. */
    private String payee;

    /** Cheque number or bank transfer reference. */
    private String referenceNumber;
}
