package com.mountainview.sms.attendance;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mountainview.sms.attendance.AttendanceDtos.AttendanceEntry;
import com.mountainview.sms.attendance.AttendanceDtos.AttendanceUpdateRequest;
import com.mountainview.sms.attendance.AttendanceDtos.ClassRegisterRequest;
import com.mountainview.sms.attendance.AttendanceDtos.ClassTotal;
import com.mountainview.sms.attendance.AttendanceDtos.DailySummary;
import com.mountainview.sms.attendance.AttendanceDtos.DivisionTotal;
import com.mountainview.sms.attendance.AttendanceDtos.MonthlyDivisionRow;
import com.mountainview.sms.attendance.AttendanceDtos.MonthlyReport;
import com.mountainview.sms.attendance.AttendanceDtos.RegisterRow;
import com.mountainview.sms.attendance.AttendanceDtos.StudentAttendanceHistory;
import com.mountainview.sms.common.NotFoundException;
import com.mountainview.sms.student.Division;
import com.mountainview.sms.student.Gender;
import com.mountainview.sms.student.Student;
import com.mountainview.sms.student.StudentRepository;

@Service
@Transactional
public class AttendanceService {

    private final AttendanceRecordRepository records;
    private final StudentRepository students;

    public AttendanceService(AttendanceRecordRepository records, StudentRepository students) {
        this.records = records;
        this.students = students;
    }

    // SR-1: the class list for a day, with whatever has been marked so far.
    @Transactional(readOnly = true)
    public List<RegisterRow> registerFor(String className, LocalDate date) {
        List<RegisterRow> rows = new ArrayList<>();
        for (Student student : studentsIn(className)) {
            AttendanceRecord record = records.findByStudentIdAndAttendanceDate(student.getId(), date).orElse(null);
            rows.add(RegisterRow.from(student, date, record));
        }
        return rows;
    }

    /**
     * SR-2, SR-3, SR-7: save the register for a class.
     * A student who was already marked for that day is updated; otherwise a new record is created.
     */
    public List<RegisterRow> saveRegister(String className, ClassRegisterRequest request) {
        List<Student> classList = studentsIn(className);

        for (AttendanceEntry entry : request.entries()) {
            Student student = findInClass(classList, entry.studentId(), className);

            AttendanceRecord record = records
                    .findByStudentIdAndAttendanceDate(student.getId(), request.date())
                    .orElseGet(AttendanceRecord::new);
            record.setStudent(student);
            record.setAttendanceDate(request.date());
            record.setMorningStatus(entry.morning());
            record.setAfternoonStatus(entry.afternoon());
            records.save(record);
        }
        return registerFor(className, request.date());
    }

    // SR-3: correct one saved record.
    public RegisterRow updateRecord(Long recordId, AttendanceUpdateRequest request) {
        AttendanceRecord record = records.findById(recordId)
                .orElseThrow(() -> new NotFoundException("No attendance record with id " + recordId));
        record.setMorningStatus(request.morning());
        record.setAfternoonStatus(request.afternoon());
        return RegisterRow.from(records.save(record));
    }

    // SR-4: boys and girls present today, per class and per division.
    @Transactional(readOnly = true)
    public DailySummary dailySummary(LocalDate date) {
        Map<String, Tally> perClass = new TreeMap<>();
        Map<Division, Tally> perDivision = new TreeMap<>();
        Map<String, Division> divisionOfClass = new TreeMap<>();

        for (AttendanceRecord record : records.findByAttendanceDate(date)) {
            Student student = record.getStudent();
            divisionOfClass.put(student.getClassName(), student.getDivision());
            Tally classTally = perClass.computeIfAbsent(student.getClassName(), key -> new Tally());
            Tally divisionTally = perDivision.computeIfAbsent(student.getDivision(), key -> new Tally());
            if (record.countsAsPresent()) {
                classTally.addPresent(student.getGender());
                divisionTally.addPresent(student.getGender());
            }
        }

        List<ClassTotal> classes = new ArrayList<>();
        perClass.forEach((className, tally) ->
                classes.add(new ClassTotal(className, divisionOfClass.get(className), tally.boysPresent, tally.girlsPresent)));

        List<DivisionTotal> divisions = new ArrayList<>();
        perDivision.forEach((division, tally) ->
                divisions.add(new DivisionTotal(division, tally.boysPresent, tally.girlsPresent)));

        return new DailySummary(date, classes, divisions);
    }

    // SR-5: the monthly summary for the Ministry, by division and gender.
    @Transactional(readOnly = true)
    public MonthlyReport monthlyReport(int year, int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }
        YearMonth yearMonth = YearMonth.of(year, month);
        List<AttendanceRecord> monthRecords =
                records.findByAttendanceDateBetween(yearMonth.atDay(1), yearMonth.atEndOfMonth());

        Map<Division, Tally> perDivision = new TreeMap<>();
        Set<LocalDate> schoolDays = new HashSet<>();

        for (AttendanceRecord record : monthRecords) {
            Student student = record.getStudent();
            schoolDays.add(record.getAttendanceDate());
            Tally tally = perDivision.computeIfAbsent(student.getDivision(), key -> new Tally());
            tally.addMarked(student.getGender());
            if (record.countsAsPresent()) {
                tally.addPresent(student.getGender());
            }
        }

        // A month with no records gives an empty list and 0 school days, not an error (TS-6)
        List<MonthlyDivisionRow> rows = new ArrayList<>();
        perDivision.forEach((division, tally) -> rows.add(new MonthlyDivisionRow(
                division,
                tally.boysPresent, tally.boysMarked, percent(tally.boysPresent, tally.boysMarked),
                tally.girlsPresent, tally.girlsMarked, percent(tally.girlsPresent, tally.girlsMarked))));

        return new MonthlyReport(year, month, schoolDays.size(), rows);
    }

    // SR-6: one student's attendance between two dates.
    @Transactional(readOnly = true)
    public StudentAttendanceHistory historyForStudent(Long studentId, LocalDate from, LocalDate to) {
        Student student = students.findById(studentId)
                .orElseThrow(() -> new NotFoundException("No student with id " + studentId));
        return buildHistory(student, from, to);
    }

    // AC-5: the same, but found by typing the student's name.
    @Transactional(readOnly = true)
    public List<StudentAttendanceHistory> historyByName(String name, LocalDate from, LocalDate to) {
        List<Student> matches = students.searchByName(name.trim());
        if (matches.isEmpty()) {
            throw new NotFoundException("No student found with a name like '" + name + "'");
        }
        List<StudentAttendanceHistory> histories = new ArrayList<>();
        for (Student student : matches) {
            histories.add(buildHistory(student, from, to));
        }
        return histories;
    }

    private StudentAttendanceHistory buildHistory(Student student, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must be on or before 'to'");
        }
        int present = 0;
        int absent = 0;
        int late = 0;
        List<RegisterRow> rows = new ArrayList<>();
        for (AttendanceRecord record : records
                .findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(student.getId(), from, to)) {
            if (record.countsAsPresent()) {
                present++;
            } else {
                absent++;
            }
            if (record.wasLate()) {
                late++;
            }
            rows.add(RegisterRow.from(record));
        }
        return new StudentAttendanceHistory(
                student.getId(), student.getFullName(), student.getClassName(), from, to, present, absent, late, rows);
    }

    private List<Student> studentsIn(String className) {
        List<Student> classList = students.findByClassNameIgnoreCaseOrderByLastNameAscFirstNameAsc(className);
        if (classList.isEmpty()) {
            throw new NotFoundException("No students found in class " + className);
        }
        return classList;
    }

    private Student findInClass(List<Student> classList, Long studentId, String className) {
        for (Student student : classList) {
            if (student.getId().equals(studentId)) {
                return student;
            }
        }
        throw new IllegalArgumentException("Student " + studentId + " is not in class " + className);
    }

    private static double percent(int part, int whole) {
        if (whole == 0) {
            return 0.0;
        }
        return Math.round(part * 1000.0 / whole) / 10.0;
    }

    private static class Tally {
        int boysPresent;
        int girlsPresent;
        int boysMarked;
        int girlsMarked;

        void addPresent(Gender gender) {
            if (gender == Gender.MALE) {
                boysPresent++;
            } else {
                girlsPresent++;
            }
        }

        void addMarked(Gender gender) {
            if (gender == Gender.MALE) {
                boysMarked++;
            } else {
                girlsMarked++;
            }
        }
    }
}
