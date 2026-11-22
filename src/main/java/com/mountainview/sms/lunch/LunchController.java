package com.mountainview.sms.lunch;

import java.time.LocalDate;
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

import com.mountainview.sms.lunch.LunchDtos.DailyLunchReport;
import com.mountainview.sms.lunch.LunchDtos.LunchMethodRequest;
import com.mountainview.sms.lunch.LunchDtos.LunchOrderRequest;
import com.mountainview.sms.lunch.LunchDtos.LunchOrderResponse;
import com.mountainview.sms.lunch.LunchDtos.LunchStudent;
import com.mountainview.sms.security.Access;
import com.mountainview.sms.student.LunchMethod;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/lunch")
@PreAuthorize(Access.LUNCH)

public class LunchController {

    private final LunchService service;

    public LunchController(LunchService service){
        this.service = service;

    }
@GetMapping("/students")
public List<LunchStudent> listStudents(
        @RequestParam(required = false) LunchMethod method,
        @RequestParam(required = false) Integer gradeLevel) {
    return service.listStudents(method, gradeLevel);
}

@PutMapping("/students/{studentId}/method")
public LunchStudent updateLunchMethod(
        @PathVariable Long studentId,
        @Valid @RequestBody LunchMethodRequest request) {
    return service.setLunchMethod(studentId, request.method());
}

@PutMapping("/orders/{id}")
public LunchOrderResponse updateOrder(
        @PathVariable Long id,
        @Valid @RequestBody LunchOrderRequest request) {
    return service.updateOrder(id, request);
}

@GetMapping("/orders")
public List<LunchOrderResponse> listOrders(
        @RequestParam LocalDate date,
        @RequestParam(required = false) Integer gradeLevel) {
    return service.listOrders(date, gradeLevel);
}

@GetMapping("/reports/daily")
public DailyLunchReport dailyReport(@RequestParam LocalDate date) {
    return service.dailyReport(date);
}

@PostMapping("/orders")
@ResponseStatus(HttpStatus.CREATED)
public LunchOrderResponse createOrder(
        @Valid @RequestBody LunchOrderRequest request) {
    return service.createOrder(request);
}
}
