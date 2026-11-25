package com.mountainview.sms.lunch;

import com.mountainview.sms.student.LunchMethod;
import com.mountainview.sms.student.Student;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.*;
import java.util.List;

 
public final class LunchDtos {
   
    private LunchDtos(){
    
    }

    
    public record LunchMethodRequest ( @NotNull LunchMethod method){
    }

    

    public record LunchOrderRequest(@NotNull Long studentId, @NotNull LocalDate orderDate, LunchMethod method, @NotNull LunchStatus status, @Size(max = 255) String itemDescription, @PositiveOrZero BigDecimal amountPaid){

    }

    
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

    
    public record DailyLunchReport(LocalDate date, int totalOrders, BigDecimal totalPayments, List<MethodTotal> byMethod) {
    }
}
