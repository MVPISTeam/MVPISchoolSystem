package com.mountainview.sms.academic;

import java.util.List;

/*Thius contains information teachers will recieve for a student which is grades and GPA */

public record AcademicReport(
    Long studentId,
    String studentName,
    int gradelevel,
    String className,
    double gpa,
    List<GradeResponse> grades){

}