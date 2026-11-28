package com.mountainview.sms.student;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional 
class StudentApiTest {

    private static final String NEW_STUDENT = """
            {
              "firstName": "Zara",
              "lastName": "Whyte",
              "gender": "FEMALE",
              "gradeLevel": 2,
              "className": "2B",
              "division": "PRIMARY",
              "dateOfBirth": "2019-03-14",
              "address": "12 Test Avenue, Kingston 2",
              "guardianName": "Marcia Whyte",
              "guardianPhone": "876-555-0101",
              "healthInfo": "Asthma. Carries an inhaler.",
              "immunizationUpToDate": true
            }
            """;

    @Autowired
    MockMvc mvc;

    private int createStudent() throws Exception {
        String body = mvc.perform(post("/api/students")
                        .with(httpBasic("principal", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_STUDENT))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    @DisplayName("TS-1 + TS-4: a new student can be added and is still there when read back")
    void addStudentAndReadItBack() throws Exception {
        int id = createStudent();

        mvc.perform(get("/api/students/" + id).with(httpBasic("admin", "mvpi1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Zara"))
                .andExpect(jsonPath("$.address").value("12 Test Avenue, Kingston 2"))
                .andExpect(jsonPath("$.healthInfo").value("Asthma. Carries an inhaler."))
                .andExpect(jsonPath("$.age").isNumber());
    }

    @Test
    @DisplayName("TS-2: an existing student's information can be updated")
    void updateStudent() throws Exception {
        int id = createStudent();
        String changed = NEW_STUDENT.replace("12 Test Avenue, Kingston 2", "7 New Road, Kingston 3");

        mvc.perform(put("/api/students/" + id)
                        .with(httpBasic("viceprincipal", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changed))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("7 New Road, Kingston 3"));
    }

    @Test
    @DisplayName("TS-3: a teacher cannot read or add personal data (403)")
    void teacherCannotAccessPersonalData() throws Exception {
        int id = createStudent();

        mvc.perform(get("/api/students/" + id).with(httpBasic("teacher", "mvpi1234")))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/students")
                        .with(httpBasic("teacher", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_STUDENT))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TS-3: a teacher can see the class list, and it contains no personal fields")
    void teacherSeesClassListWithoutPersonalData() throws Exception {
        createStudent();

        mvc.perform(get("/api/students").param("className", "2B").with(httpBasic("teacher", "mvpi1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].lastName").value("Whyte"))
                .andExpect(jsonPath("$[0].address").doesNotExist())
                .andExpect(jsonPath("$[0].healthInfo").doesNotExist());
    }

    @Test
    @DisplayName("TS-5: missing required fields are rejected with 400 and a message per field")
    void rejectsInvalidStudent() throws Exception {
        mvc.perform(post("/api/students")
                        .with(httpBasic("principal", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "firstName": "", "lastName": "Whyte", "gradeLevel": 9 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.gradeLevel").exists());
    }

    @Test
    @DisplayName("TS-6: Asking for a student who does not exist gives 404")
    void unknownStudentIs404() throws Exception {
        mvc.perform(get("/api/students/999999").with(httpBasic("principal", "mvpi1234")))
                .andExpect(status().isNotFound());
    }
}
