package com.mountainview.sms.student;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mountainview.sms.security.Access;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService service;

    public StudentController(StudentService service) {
        this.service = service;
    }

    /** Class list without personal data. Teachers may call this one. */
    @GetMapping
    @PreAuthorize(Access.CLASS_LIST)
    public List<StudentSummary> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) Integer gradeLevel) {
        return service.list(name, className, gradeLevel);
    }

    /** The full personal record. SR-3: authorised roles only. */
    @GetMapping("/{id}")
    @PreAuthorize(Access.STUDENT_RECORDS)
    public StudentDetails get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.STUDENT_RECORDS)
    public StudentDetails create(@Valid @RequestBody StudentRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Access.STUDENT_RECORDS)
    public StudentDetails update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return service.update(id, request);
    }
}
