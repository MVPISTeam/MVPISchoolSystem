package com.mountainview.sms.attendance;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AttendanceApiTest {

    // A date far in the past, so the demo data never has records on it
    private static final String DAY = "2020-03-02";

    @Autowired
    MockMvc mvc;

    @Autowired
    StudentRepository students;

    Long boyId;
    Long girlId;
    Long infantGirlId;

    @BeforeEach
    void createTwoTestClasses() {
        boyId = students.save(new Student("Andre", "Zephyr", Gender.MALE, 2, "T2", Division.PRIMARY)).getId();
        girlId = students.save(new Student("Bella", "Zephyr", Gender.FEMALE, 2, "T2", Division.PRIMARY)).getId();
        infantGirlId = students.save(new Student("Cara", "Zephyr", Gender.FEMALE, 0, "TK", Division.INFANT)).getId();
    }

    private String registerJson(String date, String boyMorning, String boyAfternoon) {
        return """
                {
                  "date": "%s",
                  "entries": [
                    { "studentId": %d, "morning": "%s", "afternoon": "%s" },
                    { "studentId": %d, "morning": "PRESENT", "afternoon": "LATE" }
                  ]
                }
                """.formatted(date, boyId, boyMorning, boyAfternoon, girlId);
    }

    private void saveT2Register(String date, String boyMorning, String boyAfternoon) throws Exception {
        mvc.perform(put("/api/attendance/classes/T2")
                        .with(httpBasic("teacher", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(date, boyMorning, boyAfternoon)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TS-1: morning and afternoon attendance is recorded for every student in a class")
    void recordAttendanceForAClass() throws Exception {
        // Before marking: the class list comes back with empty statuses
        mvc.perform(get("/api/attendance/classes/T2").param("date", DAY).with(httpBasic("teacher", "mvpi1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].studentName").value("Andre Zephyr"))
                .andExpect(jsonPath("$[0].gender").value("MALE"))
                .andExpect(jsonPath("$[0].morning").doesNotExist());

        saveT2Register(DAY, "PRESENT", "PRESENT");

        mvc.perform(get("/api/attendance/classes/T2").param("date", DAY).with(httpBasic("teacher", "mvpi1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].morning").value("PRESENT"))
                .andExpect(jsonPath("$[0].afternoon").value("PRESENT"))
                .andExpect(jsonPath("$[1].afternoon").value("LATE"));
    }

    @Test
    @DisplayName("TS-2: saving the same class and day again updates it instead of duplicating")
    void savingTwiceDoesNotDuplicate() throws Exception {
        saveT2Register(DAY, "PRESENT", "PRESENT");
        saveT2Register(DAY, "ABSENT", "ABSENT");

        mvc.perform(get("/api/attendance/students/" + boyId).param("from", DAY).param("to", DAY)
                        .with(httpBasic("teacher", "mvpi1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(1)))
                .andExpect(jsonPath("$.records[0].morning").value("ABSENT"));
    }

    @Test
    @DisplayName("TS-3: a single attendance entry can be edited and the change is saved")
    void editOneEntry() throws Exception {
        saveT2Register(DAY, "ABSENT", "ABSENT");
        String register = mvc.perform(get("/api/attendance/classes/T2").param("date", DAY)
                        .with(httpBasic("teacher", "mvpi1234")))
                .andReturn().getResponse().getContentAsString();
        int recordId = JsonPath.read(register, "$[0].recordId");

        mvc.perform(put("/api/attendance/records/" + recordId)
                        .with(httpBasic("teacher", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "morning": "LATE", "afternoon": "PRESENT" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.morning").value("LATE"));

        mvc.perform(get("/api/attendance/classes/T2").param("date", DAY).with(httpBasic("teacher", "mvpi1234")))
                .andExpect(jsonPath("$[0].morning").value("LATE"));
    }

    @Test
    @DisplayName("TS-4: daily totals by gender and division match the entries recorded")
    void dailyTotalsMatchEntries() throws Exception {
        saveT2Register(DAY, "ABSENT", "ABSENT");   // boy absent all day, girl present
        mvc.perform(put("/api/attendance/classes/TK")
                        .with(httpBasic("teacher", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "date": "%s", "entries": [ { "studentId": %d, "morning": "LATE", "afternoon": "PRESENT" } ] }
                                """.formatted(DAY, infantGirlId)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/attendance/summary/daily").param("date", DAY).with(httpBasic("principal", "mvpi1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classes", hasSize(2)))
                .andExpect(jsonPath("$.classes[?(@.className == 'T2')].boysPresent").value(0))
                .andExpect(jsonPath("$.classes[?(@.className == 'T2')].girlsPresent").value(1))
                .andExpect(jsonPath("$.classes[?(@.className == 'TK')].girlsPresent").value(1))
                .andExpect(jsonPath("$.divisions[?(@.division == 'PRIMARY')].girlsPresent").value(1))
                .andExpect(jsonPath("$.divisions[?(@.division == 'PRIMARY')].boysPresent").value(0))
                .andExpect(jsonPath("$.divisions[?(@.division == 'INFANT')].girlsPresent").value(1));
    }

    @Test
    @DisplayName("TS-5: the monthly report adds up attendance by division and gender")
    void monthlyReport() throws Exception {
        saveT2Register("2020-03-02", "PRESENT", "PRESENT");
        saveT2Register("2020-03-03", "ABSENT", "ABSENT");
        saveT2Register("2020-03-04", "LATE", "PRESENT");

        mvc.perform(get("/api/attendance/reports/monthly").param("year", "2020").param("month", "3")
                        .with(httpBasic("principal", "mvpi1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schoolDaysRecorded").value(3))
                .andExpect(jsonPath("$.divisions", hasSize(1)))
                .andExpect(jsonPath("$.divisions[0].division").value("PRIMARY"))
                .andExpect(jsonPath("$.divisions[0].boysAttendances").value(2))
                .andExpect(jsonPath("$.divisions[0].boysMarked").value(3))
                .andExpect(jsonPath("$.divisions[0].boysPercent").value(66.7))
                .andExpect(jsonPath("$.divisions[0].girlsAttendances").value(3))
                .andExpect(jsonPath("$.divisions[0].girlsPercent").value(100.0));
    }

    @Test
    @DisplayName("TS-6: a month with no attendance gives an empty report, not an error")
    void emptyMonthIsNotAnError() throws Exception {
        mvc.perform(get("/api/attendance/reports/monthly").param("year", "1999").param("month", "1")
                        .with(httpBasic("principal", "mvpi1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schoolDaysRecorded").value(0))
                .andExpect(jsonPath("$.divisions", hasSize(0)));
    }

    @Test
    @DisplayName("TS-7: attendance history for a student who does not exist gives 404")
    void unknownStudentIs404() throws Exception {
        mvc.perform(get("/api/attendance/students").param("name", "nobody-has-this-name")
                        .param("from", "2020-03-01").param("to", "2020-03-31")
                        .with(httpBasic("teacher", "mvpi1234")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("AC-5: a student's history counts present, absent and late days")
    void studentHistoryByName() throws Exception {
        saveT2Register("2020-03-02", "PRESENT", "PRESENT");
        saveT2Register("2020-03-03", "ABSENT", "ABSENT");
        saveT2Register("2020-03-04", "LATE", "PRESENT");

        mvc.perform(get("/api/attendance/students").param("name", "andre zephyr")
                        .param("from", "2020-03-01").param("to", "2020-03-31")
                        .with(httpBasic("teacher", "mvpi1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].daysPresent").value(2))
                .andExpect(jsonPath("$[0].daysAbsent").value(1))
                .andExpect(jsonPath("$[0].daysLate").value(1));
    }

    @Test
    @DisplayName("A value that is not Present, Absent or Late is rejected with 400")
    void rejectsUnknownStatus() throws Exception {
        mvc.perform(put("/api/attendance/classes/T2")
                        .with(httpBasic("teacher", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(DAY, "SLEEPING", "PRESENT")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A student from another class cannot be marked in this register (400)")
    void rejectsStudentFromAnotherClass() throws Exception {
        mvc.perform(put("/api/attendance/classes/T2")
                        .with(httpBasic("teacher", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "date": "%s", "entries": [ { "studentId": %d, "morning": "PRESENT", "afternoon": "PRESENT" } ] }
                                """.formatted(DAY, infantGirlId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Pre-condition: the tuckshop manager cannot take attendance (403)")
    void tuckshopManagerIsForbidden() throws Exception {
        mvc.perform(get("/api/attendance/classes/T2").param("date", DAY).with(httpBasic("tuckshop", "mvpi1234")))
                .andExpect(status().isForbidden());
    }
}
