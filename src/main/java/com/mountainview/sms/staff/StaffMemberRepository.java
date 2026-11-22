package com.mountainview.sms.staff;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffMemberRepository extends JpaRepository<StaffMember, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /**
     * Name is a "contains" match. className must match exactly (ignoring case).
     * Pass an empty string for any filter not wanting to be use.
     */
    @Query("""
            select s from StaffMember s
            where lower(concat(s.firstName, ' ', s.lastName)) like lower(concat('%', :name, '%'))
              and (:className = '' or lower(s.className) = lower(:className))
            order by s.lastName, s.firstName
            """)
    List<StaffMember> search(@Param("name") String name,
                             @Param("className") String className);
}