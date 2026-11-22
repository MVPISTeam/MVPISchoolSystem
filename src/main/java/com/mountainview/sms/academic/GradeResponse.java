package com.mountainview.sms.academic;

public record GradeResponse (
    Long id,
    Long studentId,
    String studentName,
    Subject subject,
    Term term,
    String academicYear,
    double classworkGrade,
    double examGrade,
    double finalMark,
    String letter){
    
    public static GradeResponse from(GradeRecord record){
        double finalMark = GpaCalculator.finalMark(record.getClassworkGrade(), record.getExamGrade());
        return new GradeResponse(
            record.getId(),
            record.getStudent().getId(),
            record.getStudent().getFullName(),
            record.getSubject(),
            record.getTerm(),
            record.getAcademicYear(),
            record.getClassworkGrade(),
            record.getExamGrade(),
            finalMark,
            GpaCalculator.letter(finalMark));
    }
}