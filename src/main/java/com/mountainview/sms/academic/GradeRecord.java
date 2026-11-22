package com.mountainview.sms.academic;

import com.mountainview.sms.student.Student;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "grade_records",
        // The database itself refuses a second row for the same student + subject + term + year
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "subject", "term", "academic_year"}))

@Getter
@Setter
@NoArgsConstructor
public class GradeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**Many grade records belong to one student. Stored as student id coluymn */
    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Subject subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Term term;

    @Column(nullable = false)
    private String academicYear;

    @Column(nullable = false)
    private double classworkGrade;

    @Column(nullable = false)
    private double examGrade;
}