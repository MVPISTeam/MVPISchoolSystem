package com.mountainview.sms.lunch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mountainview.sms.common.NotFoundException;
import com.mountainview.sms.lunch.LunchDtos.DailyLunchReport;
import com.mountainview.sms.lunch.LunchDtos.LunchOrderRequest;
import com.mountainview.sms.lunch.LunchDtos.LunchOrderResponse;
import com.mountainview.sms.lunch.LunchDtos.LunchStudent;
import com.mountainview.sms.lunch.LunchDtos.MethodTotal;
import com.mountainview.sms.student.LunchMethod;
import com.mountainview.sms.student.Student;
import com.mountainview.sms.student.StudentRepository;

@Service
@Transactional 

public class LunchService {

    private final LunchOrderRepository orders;

    private final StudentRepository students;


    public LunchService( LunchOrderRepository orders, StudentRepository students ){
        this.orders = orders;
        this.students =  students;

    }
    @Transactional(readOnly = true)
    public List<LunchStudent> listStudents(LunchMethod method, Integer gradeLevel){
        List<Student> found;
        if (method != null && gradeLevel != null) {
            found = students.findByLunchMethodAndGradeLevelOrderByLastNameAscFirstNameAsc(method, gradeLevel);
        } else if (method != null) {
            found = students.findByLunchMethodOrderByGradeLevelAscLastNameAsc(method);
        } else if (gradeLevel != null) {
            found = students.findByGradeLevelOrderByLastNameAscFirstNameAsc(gradeLevel);
        } else {
            found = students.findAll(Sort.by("gradeLevel", "lastName","firstName"));
        }
        return found.stream().map(LunchStudent::from).toList();
    }
}


