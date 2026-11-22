package com.mountainview.sms.lunch;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.mountainview.sms.student.Division;
import com.mountainview.sms.student.Gender;
import com.mountainview.sms.student.LunchMethod;
import com.mountainview.sms.student.Student;
import com.mountainview.sms.student.StudentRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional


public class LunchApiTest {

        private static final String DAY = "2020-03-02";

@Autowired 
MockMvc mvc;

@Autowired
StudentRepository students;

Long grade6Id ;
Long otherGrade6Id;

@BeforeEach 
void createStudents(){

    grade6Id = students.save(new Student("Devon", "Lunchwell", Gender.MALE, 6, "6T", Division.PRIMARY)).getId();
    otherGrade6Id = students.save(new Student("Erica", "Lunchwell", Gender.FEMALE, 6, "6T", Division.PRIMARY)).getId();
    }

    private String orderJson(Long studentId, String method, String status, String amount){
        return """
                {
                  "studentId": %d,
                  "orderDate": "%s",
                  "method": %s,
                  "status": "%s",
                  "itemDescription": "Chicken and rice",
                  "amountPaid": %s
                 }
                """.formatted(studentId, DAY, method == null ? "null" : "\"" + method + "\"", status, amount);
             }


     private int createOrder(Long studentId, String method, String status, String amount) throws Exception {
        String body = mvc.perform(post("/api/lunch/orders")
                        .with(httpBasic("teacher", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(studentId, method, status, amount)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private void putOnPath(Long studentId) throws Exception {
        mvc.perform(put("/api/lunch/students/" + studentId + "/method")
                        .with(httpBasic("teacher", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "method": "PATH" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lunchMethod").value("PATH"));
    }

    @Test
    @DisplayName("TS-1: a lunch entry can be added and is stored")
    void addLunchOrder() throws Exception {
        createOrder(grade6Id, "CANTEEN", "ORDERED", "450.00");

        mvc.perform(get("/api/lunch/orders").param("date", DAY).param("gradeLevel", "6")
                        .with(httpBasic("tuckshop", "mvpi1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].studentName").value("Devon Lunchwell"))
                .andExpect(jsonPath("$[0].method").value("CANTEEN"))
                .andExpect(jsonPath("$[0].amountPaid").value(450.00));
    }

    @Test
    @DisplayName("TS-2: PATH students can be listed by grade")
    void pathStudentsByGrade() throws Exception {
        putOnPath(grade6Id);

        mvc.perform(get("/api/lunch/students").param("method", "PATH").param("gradeLevel", "6")
                        .with(httpBasic("teacher", "mvpi1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].studentName").value("Devon Lunchwell"))
                .andExpect(jsonPath("$[0].lunchMethod").value("PATH"));
    }

    @Test
    @DisplayName("TS-3: lunch orders can be listed by grade")
    void ordersByGrade() throws Exception {
        createOrder(grade6Id, "CANTEEN", "ORDERED", "450.00");
        createOrder(otherGrade6Id, "TUCKSHOP", "ORDERED", "200.00");

        mvc.perform(get("/api/lunch/orders").param("date", DAY).param("gradeLevel", "6")
                        .with(httpBasic("teacher", "mvpi1234")))
                .andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(get("/api/lunch/orders").param("date", DAY).param("gradeLevel", "5")
                        .with(httpBasic("teacher", "mvpi1234")))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("AC-4: an order and its payment can be edited")
    void editOrder() throws Exception {
        int orderId = createOrder(grade6Id, "CANTEEN", "ORDERED", "450.00");

        mvc.perform(put("/api/lunch/orders/" + orderId)
                        .with(httpBasic("teacher", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(grade6Id, "TUCKSHOP", "SERVED", "300.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("TUCKSHOP"))
                .andExpect(jsonPath("$.status").value("SERVED"))
                .andExpect(jsonPath("$.amountPaid").value(300.00));
    }

    @Test
    @DisplayName("A PATH lunch never records a payment, and leaving method out uses the student's usual one")
    void pathLunchIsFree() throws Exception {
        putOnPath(grade6Id);

        mvc.perform(post("/api/lunch/orders")
                        .with(httpBasic("teacher", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(grade6Id, null, "ORDERED", "450.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.method").value("PATH"))
                .andExpect(jsonPath("$.amountPaid").value(0));
    }

    @Test
    @DisplayName("SR-5: the daily report gives order counts and payment totals, ignoring cancelled orders")
    void dailyReport() throws Exception {
        createOrder(grade6Id, "CANTEEN", "SERVED", "450.00");
        createOrder(otherGrade6Id, "CANTEEN", "ORDERED", "500.50");
        createOrder(grade6Id, "TUCKSHOP", "SERVED", "200.00");
        createOrder(otherGrade6Id, "TUCKSHOP", "CANCELLED", "150.00");
        createOrder(grade6Id, "PATH", "SERVED", "0");

        mvc.perform(get("/api/lunch/reports/daily").param("date", DAY).with(httpBasic("tuckshop", "mvpi1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(4))
                .andExpect(jsonPath("$.totalPayments").value(1150.50))
                .andExpect(jsonPath("$.byMethod", hasSize(3)))
                .andExpect(jsonPath("$.byMethod[?(@.method == 'CANTEEN')].orderCount").value(2))
                .andExpect(jsonPath("$.byMethod[?(@.method == 'CANTEEN')].paymentTotal").value(950.50))
                .andExpect(jsonPath("$.byMethod[?(@.method == 'TUCKSHOP')].orderCount").value(1))
                .andExpect(jsonPath("$.byMethod[?(@.method == 'PATH')].orderCount").value(1));
    }

    @Test
    @DisplayName("A negative payment is rejected (400)")
    void negativePaymentIsRejected() throws Exception {
        mvc.perform(post("/api/lunch/orders")
                        .with(httpBasic("teacher", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(grade6Id, "CANTEEN", "ORDERED", "-50")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amountPaid").exists());
    }

    @Test
    @DisplayName("The bursar has no access to lunch orders (403)")
    void bursarIsForbidden() throws Exception {
        mvc.perform(get("/api/lunch/orders").param("date", DAY).with(httpBasic("bursar", "mvpi1234")))
                .andExpect(status().isForbidden());
    }
}




