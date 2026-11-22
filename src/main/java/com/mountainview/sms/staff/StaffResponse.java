package com.mountainview.sms.staff;

import java.time.LocalDate;

// The JSON body sent back for a staff member.
public record StaffResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        LocalDate dateOfBirth,
        String address,
        String phone,
        String email,
        String className,
        StaffPosition position,
        LocalDate hireDate,
        EmploymentType employmentType,
        EmploymentStatus employmentStatus) {

    public static StaffResponse from(StaffMember s) {
        return new StaffResponse(
                s.getId(),
                s.getFirstName(),
                s.getLastName(),
                s.getFullName(),
                s.getDateOfBirth(),
                s.getAddress(),
                s.getPhone(),
                s.getEmail(),
                s.getClassName(),
                s.getPosition(),
                s.getHireDate(),
                s.getEmploymentType(),
                s.getEmploymentStatus());
    }
}