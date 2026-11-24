 package com.mountainview.sms.report;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.mountainview.sms.student.Division;
import com.mountainview.sms.student.Gender;
import com.mountainview.sms.student.Student;
import com.mountainview.sms.student.StudentRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportCardApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    StudentRepository students;

    @Test
    @DisplayName("A report card combines grades and attendance for one student")
    void reportCard() throws Exception {
        Long id = students.save(new Student("Rita", "Reportly", Gender.FEMALE, 5, "5T", Division.PRIMARY)).getId();

        mvc.perform(get("/api/reports/students/" + id + "/report-card")
                        .param("academicYear", "2026-2027").param("term", "TERM_1")
                        .param("from", "2026-09-07").param("to", "2026-12-18")
                        .with(httpBasic("teacher", "mvpi1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.academics.studentName").value("Rita Reportly"))
                .andExpect(jsonPath("$.academics.gpa").value(0.0))
                .andExpect(jsonPath("$.attendance.daysPresent").value(0));
    }
}
