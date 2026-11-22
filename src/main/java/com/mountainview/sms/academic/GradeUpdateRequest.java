package com.mountainview.sms.academic;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GradeUpdateRequest(
    @NotNull @Min(0) @Max(100) Double classworkGrade,
    @NotNull @Min(0) @Max(100) Double examGrade) {

    }
