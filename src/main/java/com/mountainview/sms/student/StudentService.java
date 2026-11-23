package com.mountainview.sms.student;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mountainview.sms.common.NotFoundException;

@Service
@Transactional
public class StudentService {

    private final StudentRepository students;

    public StudentService(StudentRepository students) {
        this.students = students;
    }

    /** SR-1, SR-2: add a new student. */
    public StudentDetails create(StudentRequest request) {
        Student student = new Student();
        copy(request, student);
        if (student.getRegistrationDate() == null) {
            student.setRegistrationDate(LocalDate.now());
        }
        return StudentDetails.from(students.save(student));
    }

    /** SR-2: update a student who is already in the system. */
    public StudentDetails update(Long id, StudentRequest request) {
        Student student = find(id);
        copy(request, student);
        return StudentDetails.from(students.save(student));
    }

    @Transactional(readOnly = true)
    public StudentDetails get(Long id) {
        return StudentDetails.from(find(id));
    }

    /** The class list. Uses the first filter that was supplied; with none, returns everyone. */
    @Transactional(readOnly = true)
    public List<StudentSummary> list(String name, String className, Integer gradeLevel) {
        List<Student> found;
        if (className != null && !className.isBlank()) {
            found = students.findByClassNameIgnoreCaseOrderByLastNameAscFirstNameAsc(className);
        } else if (gradeLevel != null) {
            found = students.findByGradeLevelOrderByLastNameAscFirstNameAsc(gradeLevel);
        } else if (name != null && !name.isBlank()) {
            found = students.searchByName(name.trim());
        } else {
            found = students.findAll(Sort.by("lastName", "firstName"));
        }
        return found.stream().map(StudentSummary::from).toList();
    }

    private Student find(Long id) {
        return students.findById(id)
                .orElseThrow(() -> new NotFoundException("No student with id " + id));
    }

    private void copy(StudentRequest request, Student student) {
        student.setFirstName(request.firstName().trim());
        student.setLastName(request.lastName().trim());
        student.setGender(request.gender());
        student.setGradeLevel(request.gradeLevel());
        student.setClassName(request.className().trim());
        student.setDivision(request.division());
        student.setDateOfBirth(request.dateOfBirth());
        student.setAddress(request.address());
        student.setGuardianName(request.guardianName());
        student.setGuardianPhone(request.guardianPhone());
        student.setHealthInfo(request.healthInfo());
        student.setImmunizationUpToDate(request.immunizationUpToDate());
        if (request.registrationDate() != null) {
            student.setRegistrationDate(request.registrationDate());
        }
    }
}
