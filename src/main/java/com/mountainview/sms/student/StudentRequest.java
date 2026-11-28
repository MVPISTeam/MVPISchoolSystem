package com.mountainview.sms.student;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

// What the client sends to add or update a student.
public record StudentRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull Gender gender,
        @NotNull @Min(0) @Max(6) Integer gradeLevel,
        @NotBlank String className,
        @NotNull Division division,
        @NotNull @Past LocalDate dateOfBirth,
        @NotBlank String address,
        String guardianName,
        String guardianPhone,
        @Size(max = 1000) String healthInfo,
        Boolean immunizationUpToDate,
        LocalDate registrationDate) {
}
