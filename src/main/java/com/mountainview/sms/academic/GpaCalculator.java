package com.mountainview.sms.academic;

import java.util.List;

/* Using this purely for the math for grading. */

public final class GpaCalculator{
    private GpaCalculator() {
    }

    public static double finalMark(double classworkGrade, double examGrade){
        return round((classworkGrade + examGrade)/2.0);
    }

    public static String letter(double finalMark) {
        if (finalMark >= 90) return "A";
        if (finalMark >= 80) return "B";
        if (finalMark >= 70) return "C";
        if (finalMark >= 60) return "D";
        return "F";
    }

    public static double gradePoints(double finalMark) {
        if (finalMark >= 90) return 4.0;
        if (finalMark >= 80) return 3.0;
        if (finalMark >= 70) return 2.0;
        if (finalMark >= 60) return 1.0;
        return 0.0;
    }

    public static double gpa(List<GradeRecord> records){
        if (records.isEmpty()) {
            return 0.0;
        }
        double totalPoints = 0.0;
        for (GradeRecord record : records) {
            totalPoints += gradePoints(finalMark(record.getClassworkGrade(), record.getExamGrade()));
        }
        return round(totalPoints/records.size());
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}