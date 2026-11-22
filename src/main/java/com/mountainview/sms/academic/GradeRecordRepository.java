package com.mountainview.sms.academic;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeRecordRepository extends JpaRepository<GradeRecord, Long> {
    boolean existsByStudentIdAndSubjectAndTermAndAcademicYear(
        Long studentId, Subject subject, Term term, String academicYear
    );

    List<GradeRecord> findByStudentIdOrderByAcademicYearAscTermAscSubjectAsc(Long studentId);
}



