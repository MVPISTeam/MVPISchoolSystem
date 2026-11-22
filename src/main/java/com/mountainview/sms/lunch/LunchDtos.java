package com.mountainview.sms.lunch;

import com.mountainview.sms.student.LunchMethod;
import com.mountainview.sms.student.Student;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.*;
import java.util.List;

 /** Every request and response shape for the lunch feature. */
public final class LunchDtos {
   
    private LunchDtos(){
    
    }

    /** Body for setting a student's usual lunch method (SR-1). */
    public record LunchMethodRequest ( @NotNull LunchMethod method){
    }

     /**
     * Body for recording or editing an order (SR-2, SR-4).
     * method may be left out: the student's usual lunch method is used.
     * amountPaid may be left out: it counts as 0. PATH lunches are always 0.
     */

    public record LunchOrderRequest(@NotNull Long studentId, @NotNull LocalDate orderDate, LunchMethod method, @NotNull LunchStatus status, @Size(max = 255) String itemDescription, @PositiveOrZero BigDecimal amountPaid){

    }

     // ---------- What the API sends back ----------

    /** A student as the lunch feature sees them: name, grade and lunch method only. */
    public record LunchStudent(Long studentId, String studentName, int gradeLevel, String className, LunchMethod lunchMethod) {

        public static LunchStudent from(Student student) {
            return new LunchStudent(student.getId(), student.getFullName(), student.getGradeLevel(),
                    student.getClassName(), student.getLunchMethod());
        }
    }
    public record LunchOrderResponse(
            Long id,
            Long studentId,
            String studentName,
            int gradeLevel,
            String className,
            LocalDate orderDate,
            LunchMethod method,
            LunchStatus status,
            String itemDescription,
            BigDecimal amountPaid) {

        public static LunchOrderResponse from(LunchOrder order) {
            Student student = order.getStudent();
            return new LunchOrderResponse(order.getId(), student.getId(), student.getFullName(),
                    student.getGradeLevel(), student.getClassName(), order.getOrderDate(), order.getMethod(),
                    order.getStatus(), order.getItemDescription(), order.getAmountPaid());
        }
    }

    public record MethodTotal(LunchMethod method, int orderCount, BigDecimal paymentTotal) {
    }

    /** SR-5: the daily report with order counts and payment totals. Cancelled orders are not counted. */
    public record DailyLunchReport(LocalDate date, int totalOrders, BigDecimal totalPayments, List<MethodTotal> byMethod) {
    }
}
