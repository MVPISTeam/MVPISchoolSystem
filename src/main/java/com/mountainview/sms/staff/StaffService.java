package com.mountainview.sms.staff;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mountainview.sms.common.ConflictException;
import com.mountainview.sms.common.NotFoundException;

@Service
public class StaffService {

    private final StaffMemberRepository staff;

    public StaffService(StaffMemberRepository staff) {
        this.staff = staff;
    }

    // Each method is one transaction, so a record is committed to the database before the response is sent.
    @Transactional
    public StaffResponse create(StaffRequest request) {
        checkDates(request);
        String email = blankToNull(request.email());
        if (email != null && staff.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A staff member with the email " + email + " already exists");
        }
        StaffMember member = new StaffMember();
        copy(request, email, member);
        return StaffResponse.from(staff.save(member));
    }

    @Transactional
    public StaffResponse update(Long id, StaffRequest request) {
        checkDates(request);
        StaffMember member = find(id);
        String email = blankToNull(request.email());
        if (email != null && staff.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("A staff member with the email " + email + " already exists");
        }
        copy(request, email, member);
        return StaffResponse.from(staff.save(member));
    }

    @Transactional(readOnly = true)
    public StaffResponse get(Long id) {
        return StaffResponse.from(find(id));
    }

    // Every filter is optional. With none, this returns everyone.
    @Transactional(readOnly = true)
    public List<StaffResponse> search(String name, StaffPosition position, String className, EmploymentStatus status) {
        return staff.search(clean(name), clean(className)).stream()
                .filter(s -> position == null || s.getPosition() == position)
                .filter(s -> status == null || s.getEmploymentStatus() == status)
                .map(StaffResponse::from)
                .toList();
    }

    private void checkDates(StaffRequest request) {
        if (request.hireDate().isBefore(request.dateOfBirth())) {
            throw new IllegalArgumentException("Hire date cannot be before the date of birth");
        }
    }

    private StaffMember find(Long id) {
        return staff.findById(id)
                .orElseThrow(() -> new NotFoundException("No staff member with id " + id));
    }

    private void copy(StaffRequest request, String email, StaffMember member) {
        member.setFirstName(request.firstName().trim());
        member.setLastName(request.lastName().trim());
        member.setDateOfBirth(request.dateOfBirth());
        member.setAddress(request.address().trim());
        member.setPhone(request.phone().trim());
        member.setEmail(email);
        member.setClassName(blankToNull(request.className()));
        member.setPosition(request.position());
        member.setHireDate(request.hireDate());
        member.setEmploymentType(request.employmentType());
        if (request.employmentStatus() != null) {
            member.setEmploymentStatus(request.employmentStatus());
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToNull(String value) {
        String trimmed = clean(value);
        return trimmed.isEmpty() ? null : trimmed;
    }
}