package com.mountainview.sms.lunch;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDate;


public interface LunchOrderRepository extends JpaRepository<LunchOrder, Long> {

List<LunchOrder> findByOrderDateOrderByIdAsc (LocalDate orderDate);
List<LunchOrder> findByOrderDateAndStudentGradeLevelOrderByIdAsc(LocalDate orderDate, int gradeLevel); 
    
}