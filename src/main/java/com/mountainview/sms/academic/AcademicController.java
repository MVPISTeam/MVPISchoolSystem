package com.mountainview.sms.academic;

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

/*This is for AC-1 and AC-2 */

@RestController
@RequestMapping("/api/academics")
@PreAuthorize(Access.TEACHING)
public class AcademicController {

    private final AcademicService service;

    public AcademicController(AcademicService service) {
        this.service = service;
    }

    @PostMapping("/grades")
    @ResponseStatus(HttpStatus.CREATED)
    public GradeResponse addGrade(@Valid @RequestBody GradeRequest request) {
        return service.addGrade(request);
    }

    @PutMapping("/grades/{id}")
    public GradeResponse updateGrade(@PathVariable Long id, @Valid @RequestBody GradeUpdateRequest request) {
        return service.updateGrade(id, request);
    }

    @GetMapping("/students/{studentId}")
    public AcademicReport reportForStudent(
            @PathVariable Long studentId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Term term) {
        return service.reportForStudent(studentId, academicYear, term);
    }

    @GetMapping("/students")
    public List<AcademicReport> reportsByName(
            @RequestParam String name,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Term term) {
        return service.reportsByName(name, academicYear, term);
    }
}