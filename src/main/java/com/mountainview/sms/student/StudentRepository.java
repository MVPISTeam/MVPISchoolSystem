package com.mountainview.sms.student;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Every feature finds students through this one interface.
 * Spring writes the SQL for us from the method names.
 */
public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findByClassNameIgnoreCaseOrderByLastNameAscFirstNameAsc(String className);

    List<Student> findByGradeLevelOrderByLastNameAscFirstNameAsc(int gradeLevel);

    List<Student> findByLunchMethodOrderByGradeLevelAscLastNameAsc(LunchMethod lunchMethod);

    List<Student> findByLunchMethodAndGradeLevelOrderByLastNameAscFirstNameAsc(LunchMethod lunchMethod, int gradeLevel);

    @Query("""
            select s from Student s
            where lower(concat(s.firstName, ' ', s.lastName)) like lower(concat('%', :name, '%'))
            order by s.lastName, s.firstName
            """)
    List<Student> searchByName(@Param("name") String name);
}
