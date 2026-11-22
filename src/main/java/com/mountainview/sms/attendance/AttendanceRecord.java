package com.mountainview.sms.attendance;

import java.time.LocalDate;

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
@Table(name = "attendance_records",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "attendance_date"}))
@Getter
@Setter
@NoArgsConstructor
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus morningStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus afternoonStatus;

    public boolean countsAsPresent() {
        return morningStatus != AttendanceStatus.ABSENT || afternoonStatus != AttendanceStatus.ABSENT;
    }

    public boolean wasLate() {
        return morningStatus == AttendanceStatus.LATE || afternoonStatus == AttendanceStatus.LATE;
    }
}
