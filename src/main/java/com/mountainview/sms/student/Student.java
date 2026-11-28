package com.mountainview.sms.student;

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

/**
 * One student. Grades, attendance and lunch orders all point back to a row in this table.
 */
@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    /** 0 = infant department, 1 to 6 = primary grades. */
    @Column(nullable = false)
    private int gradeLevel;

    /** The class the student sits in, e.g. "1M" or "4A". */
    @Column(nullable = false)
    private String className;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Division division;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LunchMethod lunchMethod = LunchMethod.CANTEEN;

    //Personal data (Feature 2). Only Principal, Vice Principal and admin staff can edit this

    private LocalDate dateOfBirth;

    private String address;

    private String guardianName;

    private String guardianPhone;

    @Column(length = 1000)
    private String healthInfo;

    private Boolean immunizationUpToDate;

    private LocalDate registrationDate;

    public Student(String firstName, String lastName, Gender gender, int gradeLevel, String className, Division division) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
        this.gradeLevel = gradeLevel;
        this.className = className;
        this.division = division;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}