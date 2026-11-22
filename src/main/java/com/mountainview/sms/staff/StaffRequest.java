package com.mountainview.sms.staff;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

// The JSON body for adding a staff member (POST) or updating one (PUT).
public record StaffRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be 100 characters or fewer")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be 100 characters or fewer")
        String lastName,

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @NotBlank(message = "Address is required")
        @Size(max = 255, message = "Address must be 255 characters or fewer")
        String address,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^(\\+?1[\\s-]?)?(\\(\\d{3}\\)|\\d{3})?[\\s-]?\\d{3}[\\s-]?\\d{4}$",
                 message = "Phone number must look like 876-555-0123, (876) 555-0123 or +1 876 555 0123")
        String phone,

        @Email(message = "Email address is not valid")
        @Size(max = 150, message = "Email must be 150 characters or fewer")
        String email,

        @Size(max = 50, message = "Class must be 50 characters or fewer")
        String className,

        @NotNull(message = "Position is required")
        StaffPosition position,

        @NotNull(message = "Hire date is required")
        @PastOrPresent(message = "Hire date cannot be in the future")
        LocalDate hireDate,

        @NotNull(message = "Employment type is required")
        EmploymentType employmentType,

        // A new record starts as ACTIVE; an update that leaves this out keeps the current status.
        EmploymentStatus employmentStatus) {
}