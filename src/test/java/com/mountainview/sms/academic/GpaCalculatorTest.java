package com.mountainview.sms.academic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GpaCalculatorTest {
    private static GradeRecord record(double classwork, double exam) {
        GradeRecord record = new GradeRecord();
        record.setClassworkGrade(classwork);
        record.setExamGrade(exam);
        return record;
    }

    @Test
    @DisplayName("TS-4: final mark is the average of classwork and exam")
    void finalMarkIsTheAverage() {
        assertEquals(85.0, GpaCalculator.finalMark(80,90));
        assertEquals(72.5, GpaCalculator.finalMark(70,75));
    }

    @Test
    @DisplayName("TS-4: grade points follow the A-F scale at the boundaries")
    void gradePointsAtBoundaries() {
        assertEquals(4.0, GpaCalculator.gradePoints(90));
        assertEquals(3.0, GpaCalculator.gradePoints(89.99));
        assertEquals(3.0, GpaCalculator.gradePoints(80));
        assertEquals(2.0, GpaCalculator.gradePoints(70));
        assertEquals(1.0, GpaCalculator.gradePoints(60));
        assertEquals(0.0, GpaCalculator.gradePoints(59.99));
    }

    @Test
    @DisplayName("TS-4: GPA is the average of the grade points")
    void gpaIsAverageOfGradePoints() {
        List<GradeRecord> records = List.of(record(90, 100), record(80, 90), record(60, 70));
        assertEquals(2.67, GpaCalculator.gpa(records));
    }

    @Test
    @DisplayName("TS-4: a student with no grades has a GPA of 0.0")
    void gpaOfNothingIsZero() {
        assertEquals(0.0, GpaCalculator.gpa(List.of()));
    }
}

