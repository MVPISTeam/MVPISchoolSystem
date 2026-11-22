 package com.mountainview.sms.attendance;

 import java.time.LocalDate;
 import java.util.List;

 import com.mountainview.sms.student.Division;
 import com.mountainview.sms.student.Gender;
 import com.mountainview.sms.student.Student;

 import jakarta.validation.Valid;
 import jakarta.validation.constraints.NotEmpty;
 import jakarta.validation.constraints.NotNull;
 import jakarta.validation.constraints.PastOrPresent;

 //Every request and response shape for attendance, kept together in one file.
 public final class AttendanceDtos {

     private AttendanceDtos() {
     }

     //What the client sends

     //One student's line in the register.
     public record AttendanceEntry(
             @NotNull Long studentId,
             @NotNull AttendanceStatus morning,
             @NotNull AttendanceStatus afternoon) {
     }

     // The whole register for a class on one day.
     public record ClassRegisterRequest(
             @NotNull @PastOrPresent LocalDate date,
             @NotEmpty @Valid List<AttendanceEntry> entries) {
     }

     // Body for correcting a single saved record.
     public record AttendanceUpdateRequest(
             @NotNull AttendanceStatus morning,
             @NotNull AttendanceStatus afternoon) {
     }

     //  What the API sends back

     // One row of the register. recordId, morning and afternoon are null until the day is marked.
     public record RegisterRow(
             Long recordId,
             Long studentId,
             String studentName,
             Gender gender,
             int gradeLevel,
             String className,
             LocalDate date,
             AttendanceStatus morning,
             AttendanceStatus afternoon) {

         public static RegisterRow from(Student student, LocalDate date, AttendanceRecord record) {
             return new RegisterRow(
                     record == null ? null : record.getId(),
                     student.getId(),
                     student.getFullName(),
                     student.getGender(),
                     student.getGradeLevel(),
                     student.getClassName(),
                     date,
                     record == null ? null : record.getMorningStatus(),
                     record == null ? null : record.getAfternoonStatus());
         }

         public static RegisterRow from(AttendanceRecord record) {
             return from(record.getStudent(), record.getAttendanceDate(), record);
         }
     }

     public record ClassTotal(String className, Division division, int boysPresent, int girlsPresent) {
     }

     public record DivisionTotal(Division division, int boysPresent, int girlsPresent) {
     }

     // SR-4: boys and girls present per class and per division for one day.
     public record DailySummary(LocalDate date, List<ClassTotal> classes, List<DivisionTotal> divisions) {
     }

     /**
      * One line of the Ministry report.
      * "Attendances" adds up every day a boy or girl was in school during the month.
      * "Marked" is how many register entries were recorded for them, present or absent.
      */
     public record MonthlyDivisionRow(
             Division division,
             int boysAttendances,
             int boysMarked,
             double boysPercent,
             int girlsAttendances,
             int girlsMarked,
             double girlsPercent) {
     }

     // SR-5: the monthly summary by division and gender.
     public record MonthlyReport(int year, int month, int schoolDaysRecorded, List<MonthlyDivisionRow> divisions) {
     }

     // SR-6: one student's attendance over a period.
     public record StudentAttendanceHistory(
             Long studentId,
             String studentName,
             String className,
             LocalDate from,
             LocalDate to,
             int daysPresent,
             int daysAbsent,
             int daysLate,
             List<RegisterRow> records) {
     }
 }
