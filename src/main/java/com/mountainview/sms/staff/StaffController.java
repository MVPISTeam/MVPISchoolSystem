package com.mountainview.sms.staff;

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

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

// Staff records. Only the Principal and Vice Principal may use anything in here.
@RestController
@RequestMapping("/api/staff")
@PreAuthorize(Access.LEADERSHIP)
@Tag(name = "Staff")
public class StaffController {

    private final StaffService service;

    public StaffController(StaffService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse add(@Valid @RequestBody StaffRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public StaffResponse update(@PathVariable Long id, @Valid @RequestBody StaffRequest request) {
        return service.update(id, request);
    }

    @GetMapping("/{id}")
    public StaffResponse view(@PathVariable Long id) {
        return service.get(id);
    }

    // e.g. /api/staff?name=smi&position=TEACHER&className=3A&status=ACTIVE  (all filters optional). 404 "No records found" if nothing matches.
    @GetMapping
    public List<StaffResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) StaffPosition position,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) EmploymentStatus status) {
        return service.search(name, position, className, status);
    }
}