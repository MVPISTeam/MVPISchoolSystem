package com.mountainview.sms.report;

import java.time.LocalDate;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mountainview.sms.academic.AcademicReport;
import com.mountainview.sms.academic.AcademicService;
import com.mountainview.sms.academic.Term;
import com.mountainview.sms.attendance.AttendanceDtos.StudentAttendanceHistory;
import com.mountainview.sms.attendance.AttendanceService;
import com.mountainview.sms.security.Access;

/**
 * A student's report card: their grades for a term plus their attendance for the same period.
 * It writes no new logic. It asks the academic and attendance services, which already know how.
 */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize(Access.TEACHING)
public class ReportCardController {

    public record ReportCard(AcademicReport academics, StudentAttendanceHistory attendance) {
    }

    private final AcademicService academicService;
    private final AttendanceService attendanceService;

    public ReportCardController(AcademicService academicService, AttendanceService attendanceService) {
        this.academicService = academicService;
        this.attendanceService = attendanceService;
    }

    /** GET /api/reports/students/4/report-card?academicYear=2026-2027&term=TERM_1&from=2026-09-07&to=2026-12-18 */
    @GetMapping("/students/{studentId}/report-card")
    public ReportCard reportCard(
            @PathVariable Long studentId,
            @RequestParam String academicYear,
            @RequestParam Term term,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return new ReportCard(
                academicService.reportForStudent(studentId, academicYear, term),
                attendanceService.historyForStudent(studentId, from, to));
    }
}
