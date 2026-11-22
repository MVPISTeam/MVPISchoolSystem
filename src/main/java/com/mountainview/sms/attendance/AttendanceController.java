package com.mountainview.sms.attendance;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mountainview.sms.attendance.AttendanceDtos.AttendanceUpdateRequest;
import com.mountainview.sms.attendance.AttendanceDtos.ClassRegisterRequest;
import com.mountainview.sms.attendance.AttendanceDtos.DailySummary;
import com.mountainview.sms.attendance.AttendanceDtos.MonthlyReport;
import com.mountainview.sms.attendance.AttendanceDtos.RegisterRow;
import com.mountainview.sms.attendance.AttendanceDtos.StudentAttendanceHistory;
import com.mountainview.sms.security.Access;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/attendance")
@PreAuthorize(Access.TEACHING)
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    // GET /api/attendance/classes/1M?date=2026-11-18
    @GetMapping("/classes/{className}")
    public List<RegisterRow> register(@PathVariable String className, @RequestParam LocalDate date) {
        return service.registerFor(className, date);
    }

    // PUT because sending the same register twice must not create duplicates.
    @PutMapping("/classes/{className}")
    public List<RegisterRow> saveRegister(
            @PathVariable String className, @Valid @RequestBody ClassRegisterRequest request) {
        return service.saveRegister(className, request);
    }

    @PutMapping("/records/{id}")
    public RegisterRow updateRecord(@PathVariable Long id, @Valid @RequestBody AttendanceUpdateRequest request) {
        return service.updateRecord(id, request);
    }

    // GET /api/attendance/summary/daily?date=2026-11-18
    @GetMapping("/summary/daily")
    public DailySummary dailySummary(@RequestParam LocalDate date) {
        return service.dailySummary(date);
    }

    // GET /api/attendance/reports/monthly?year=2026&month=11
    @GetMapping("/reports/monthly")
    public MonthlyReport monthlyReport(@RequestParam int year, @RequestParam int month) {
        return service.monthlyReport(year, month);
    }

    // GET /api/attendance/students/4?from=2026-11-01&to=2026-11-30
    @GetMapping("/students/{studentId}")
    public StudentAttendanceHistory historyForStudent(
            @PathVariable Long studentId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.historyForStudent(studentId, from, to);
    }

    // GET /api/attendance/students?name=brown&from=2026-11-01&to=2026-11-30
    @GetMapping("/students")
    public List<StudentAttendanceHistory> historyByName(
            @RequestParam String name, @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return service.historyByName(name, from, to);
    }
}
