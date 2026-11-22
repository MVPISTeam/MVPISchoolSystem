package com.mountainview.sms.attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findByStudentIdAndAttendanceDate(Long studentId, LocalDate attendanceDate);

    List<AttendanceRecord> findByAttendanceDate(LocalDate attendanceDate);

    List<AttendanceRecord> findByAttendanceDateBetween(LocalDate from, LocalDate to);

    List<AttendanceRecord> findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            Long studentId, LocalDate from, LocalDate to);
}
