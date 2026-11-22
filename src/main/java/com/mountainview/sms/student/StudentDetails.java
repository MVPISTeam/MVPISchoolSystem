package com.mountainview.sms.student;

import java.time.LocalDate;
import java.time.Period;

/** The full personal record. Only returned to roles in Access.STUDENT_RECORDS. */
public record StudentDetails(
        Long id,
        String firstName,
        String lastName,
        Gender gender,
        int gradeLevel,
        String className,
        Division division,
        LocalDate dateOfBirth,
        Integer age,
        String address,
        String guardianName,
        String guardianPhone,
        String healthInfo,
        Boolean immunizationUpToDate,
        LocalDate registrationDate) {

    public static StudentDetails from(Student student) {
        return new StudentDetails(
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getGender(),
                student.getGradeLevel(),
                student.getClassName(),
                student.getDivision(),
                student.getDateOfBirth(),
                ageOf(student.getDateOfBirth()),
                student.getAddress(),
                student.getGuardianName(),
                student.getGuardianPhone(),
                student.getHealthInfo(),
                student.getImmunizationUpToDate(),
                student.getRegistrationDate());
    }

    /** We store the date of birth and work the age out, so it is never out of date. */
    private static Integer ageOf(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return null;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}