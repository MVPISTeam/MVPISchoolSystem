package com.mountainview.sms.academic;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mountainview.sms.academic.GradeRecord;
import com.mountainview.sms.academic.GradeRecordRepository;
import com.mountainview.sms.academic.GradeRequest;
import com.mountainview.sms.academic.GradeUpdateRequest;
import com.mountainview.sms.academic.Term;
import com.mountainview.sms.common.ConflictException;
import com.mountainview.sms.common.NotFoundException;
import com.mountainview.sms.student.Student;
import com.mountainview.sms.student.StudentRepository;

@Service
@Transactional
public class AcademicService {
    private final GradeRecordRepository grades;
    private final StudentRepository students;

    public AcademicService(GradeRecordRepository grades, StudentRepository students) {
        this.grades = grades;
        this.students = students;
    }

    /* This is for SR-1 and SR-5 To record a classwork and exam grade and save it. */
    public GradeResponse addGrade(GradeRequest request) {
        Student student = findStudent(request.studentId());

        boolean alreadyRecorded = grades.existsByStudentIdAndSubjectAndTermAndAcademicYear(
            student.getId(), request.subject(), request.term(), request.academicYear());

        if(alreadyRecorded){
            throw new ConflictException(student.getFullName() + " already has a " + request.subject() + " grade for " + request.term() + " " + request.academicYear() + ". Edit that record instead." );
        }

        GradeRecord record = new GradeRecord();
        record.setStudent(student);
        record.setSubject(request.subject());
        record.setTerm(request.term());
        record.setAcademicYear(request.academicYear());
        record.setClassworkGrade(request.classworkGrade());
        record.setExamGrade(request.examGrade());
        return GradeResponse.from(grades.save(record));
    }

    /* For SR-2: To edit a grade that was prebiously recorded. */

    public GradeResponse updateGrade(Long gradeId, GradeUpdateRequest request){
        GradeRecord record = grades.findById(gradeId)
                .orElseThrow(() -> new NotFoundException("No grade record with id " + gradeId));
        record.setClassworkGrade(request.classworkGrade());
        record.setExamGrade(request.examGrade());
        return GradeResponse.from(grades.save(record));
    }

    /* For SR-3,SR-4: To check one student grades with their GPA. */
    @Transactional(readOnly = true)
    public AcademicReport reportForStudent(Long studentId, String academicYear, Term term) {
        return buildReport(findStudent(studentId), academicYear, term);
    }

    /* For AC-2: To enter a  students name and retrieve their academic data. */
    @Transactional(readOnly = true)
    public List<AcademicReport> reportsByName(String name, String academicYear, Term term) {
        List<Student> matches = students.searchByName(name.trim());
        if (matches.isEmpty()) {
            throw new NotFoundException("No student found with a name like '" + name + "'");
        }
        List<AcademicReport> reports = new ArrayList<>();
        for (Student student : matches) {
            reports.add(buildReport(student, academicYear, term));
        }
        return reports;
    }

    private AcademicReport buildReport(Student student, String academicYear, Term term) {
        List<GradeRecord> records = new ArrayList<>();
        for (GradeRecord record : grades.findByStudentIdOrderByAcademicYearAscTermAscSubjectAsc(student.getId())) {
            boolean yearMatches = academicYear == null || academicYear.equals(record.getAcademicYear());
            boolean termMatches = term == null || term == record.getTerm();
            if (yearMatches && termMatches) {
                records.add(record);
            }
        }
        return new AcademicReport(
                student.getId(),
                student.getFullName(),
                student.getGradeLevel(),
                student.getClassName(),
                GpaCalculator.gpa(records),
                records.stream().map(GradeResponse::from).toList());
    }

    private Student findStudent(Long studentId) {
        return students.findById(studentId)
                .orElseThrow(() -> new NotFoundException("No student with id " + studentId));
    }
}
