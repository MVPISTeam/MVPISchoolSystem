package com.mountainview.sms.student;

/** The class-list view of a student. Safe for teachers: no personal data in here. */
public record StudentSummary(
        Long id,
        String firstName,
        String lastName,
        Gender gender,
        int gradeLevel,
        String className,
        Division division) {

    public static StudentSummary from(Student student) {
        return new StudentSummary(
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getGender(),
                student.getGradeLevel(),
                student.getClassName(),
                student.getDivision());
    }
}