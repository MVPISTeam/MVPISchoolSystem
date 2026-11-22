package com.mountainview.sms.academic;

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
import com.mountainview.sms.student.Student;
import com.mountainview.sms.student.StudentRepository;


/*This is for TS-1 to TS-6. A test for each test scenario */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AcademicApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    StudentRepository students;

    Long studentId;

    @BeforeEach
    void createAStudent() {
        studentId = students.save(new Student("Testa", "Gradewell", Gender.FEMALE, 3, "3T", Division.PRIMARY)).getId();
    }

    private String gradeJson(String subject, double classwork, double exam) {
        return """
                {
                  "studentId": %d,
                  "subject": "%s",
                  "term": "TERM_1",
                  "academicYear": "2026-2027",
                  "classworkGrade": %s,
                  "examGrade": %s
                }
                """.formatted(studentId, subject, classwork, exam);
    }

    private int addGrade(String subject, double classwork, double exam) throws Exception {
        String body = mvc.perform(post("/api/academics/grades")
                        .with(httpBasic("teacher", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gradeJson(subject, classwork, exam)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    @DisplayName("TS-1: a grade can be entered and is stored")
    void addGradeStoresIt() throws Exception {
        addGrade("MATHEMATICS", 80, 90);

        mvc.perform(get("/api/academics/students/" + studentId).with(httpBasic("teacher", "mvpi1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grades", hasSize(1)))
                .andExpect(jsonPath("$.grades[0].subject").value("MATHEMATICS"))
                .andExpect(jsonPath("$.grades[0].finalMark").value(85.0))
                .andExpect(jsonPath("$.grades[0].letter").value("B"));
    }

    @Test
    @DisplayName("TS-2: an existing grade can be edited")
    void editGrade() throws Exception {
        int gradeId = addGrade("MATHEMATICS", 50, 50);

        mvc.perform(put("/api/academics/grades/" + gradeId)
                        .with(httpBasic("teacher", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "classworkGrade": 92, "examGrade": 96 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalMark").value(94.0))
                .andExpect(jsonPath("$.letter").value("A"));
    }

    @Test
    @DisplayName("TS-3: a grade above 100 is rejected with 400")
    void rejectsGradeAbove100() throws Exception {
        mvc.perform(post("/api/academics/grades")
                        .with(httpBasic("teacher", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gradeJson("MATHEMATICS", 101, 90)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.classworkGrade").exists());
    }

    @Test
    @DisplayName("TS-3: a negative grade is rejected with 400")
    void rejectsNegativeGrade() throws Exception {
        mvc.perform(post("/api/academics/grades")
                        .with(httpBasic("teacher", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gradeJson("MATHEMATICS", 70, -5)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.examGrade").exists());
    }

    @Test
    @DisplayName("The same subject, term and year cannot be entered twice (409)")
    void rejectsDuplicateGrade() throws Exception {
        addGrade("READING", 70, 70);

        mvc.perform(post("/api/academics/grades")
                        .with(httpBasic("teacher", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gradeJson("READING", 75, 75)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("TS-4 + TS-5: searching by name returns the student's grades with the right GPA")
    void searchByNameReturnsGradesAndGpa() throws Exception {
        addGrade("MATHEMATICS", 90, 100);
        addGrade("LANGUAGE_ARTS", 80, 90);
        addGrade("READING", 60, 70);

        mvc.perform(get("/api/academics/students").param("name", "gradewell")
                        .with(httpBasic("principal", "mvpi1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].studentName").value("Testa Gradewell"))
                .andExpect(jsonPath("$[0].grades", hasSize(3)))
                .andExpect(jsonPath("$[0].gpa").value(2.67));
    }

    @Test
    @DisplayName("TS-6: asking for a student who does not exist gives 404")
    void unknownStudentIs404() throws Exception {
        mvc.perform(get("/api/academics/students").param("name", "nobody-has-this-name")
                        .with(httpBasic("teacher", "mvpi1234")))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/academics/students/999999").with(httpBasic("teacher", "mvpi1234")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("The tuckshop manager cannot see grades (403)")
    void tuckshopManagerIsForbidden() throws Exception {
        mvc.perform(get("/api/academics/students/" + studentId).with(httpBasic("tuckshop", "mvpi1234")))
                .andExpect(status().isForbidden());
    }
}