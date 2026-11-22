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

        /** SR-1: set a student's usual lunch method (canteen, PATH or tuckshop). */
    public LunchStudent setLunchMethod(Long studentId, LunchMethod method) {
        Student student = findStudent(studentId);
        student.setLunchMethod(method);
        return LunchStudent.from(students.save(student));
    }
     /** SR-2: record a student's lunch for a day. */
    public LunchOrderResponse createOrder(LunchOrderRequest request) {
        LunchOrder order = new LunchOrder();
        apply(request, order);
        return LunchOrderResponse.from(orders.save(order));
    }

    /** SR-4: change an order or its payment. */
    public LunchOrderResponse updateOrder(Long orderId, LunchOrderRequest request) {
        LunchOrder order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("No lunch order with id " + orderId));
        apply(request, order);
        return LunchOrderResponse.from(orders.save(order));
    }

    /** The orders for one day, optionally for one grade only. */
    @Transactional(readOnly = true)
    public List<LunchOrderResponse> listOrders(LocalDate date, Integer gradeLevel) {
        List<LunchOrder> found = (gradeLevel == null)
                ? orders.findByOrderDateOrderByIdAsc(date)
                : orders.findByOrderDateAndStudentGradeLevelOrderByIdAsc(date, gradeLevel);
        return found.stream().map(LunchOrderResponse::from).toList();
    }

    /** SR-5: order counts and payment totals for one day, for each of the three lunch channels. */
    @Transactional(readOnly = true)
    public DailyLunchReport dailyReport(LocalDate date) {
        List<LunchOrder> dayOrders = orders.findByOrderDateOrderByIdAsc(date);

        List<MethodTotal> byMethod = new ArrayList<>();
        int totalOrders = 0;
        BigDecimal totalPayments = BigDecimal.ZERO;

        for (LunchMethod method : LunchMethod.values()) {
            int count = 0;
            BigDecimal payments = BigDecimal.ZERO;
            for (LunchOrder order : dayOrders) {
                if (order.getMethod() == method && order.getStatus() != LunchStatus.CANCELLED) {
                    count++;
                    payments = payments.add(order.getAmountPaid());
                }
            }
            byMethod.add(new MethodTotal(method, count, payments));
            totalOrders += count;
            totalPayments = totalPayments.add(payments);
        }
        return new DailyLunchReport(date, totalOrders, totalPayments, byMethod);
    }

    private void apply(LunchOrderRequest request, LunchOrder order) {
        Student student = findStudent(request.studentId());
        LunchMethod method = (request.method() != null) ? request.method() : student.getLunchMethod();

        BigDecimal amount = (request.amountPaid() != null) ? request.amountPaid() : BigDecimal.ZERO;
        if (method == LunchMethod.PATH) {
            amount = BigDecimal.ZERO;   // PATH lunches are paid for by the programme, not the student
        }

        order.setStudent(student);
        order.setOrderDate(request.orderDate());
        order.setMethod(method);
        order.setStatus(request.status());
        order.setItemDescription(request.itemDescription());
        order.setAmountPaid(amount);
    }

    private Student findStudent(Long studentId) {
        return students.findById(studentId)
                .orElseThrow(() -> new NotFoundException("No student with id " + studentId));
    }
}


