package com.mountainview.sms.academic;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record GradeRequest(
    @NotNull Long studentId,
    @NotNull Subject subject,
    @NotNull Term term,
    @NotBlank @Pattern(regexp = "\\d{4}-\\d{4}", message = "must look like 2026-2027") String academicYear,
    @NotNull @Min(0) @Max(100) Double classworkGrade,
    @NotNull @Min(0) @Max(100) Double examGrade) {

}