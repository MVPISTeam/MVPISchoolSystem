package com.mountainview.sms.staff;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Covers STA-TS-1 to STA-TS-4. @Transactional rolls the database back after every test,
 * so the tests never leave data behind for each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StaffControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private StaffMemberRepository repository;

    private static final RequestPostProcessor PRINCIPAL = user("principal").roles("PRINCIPAL");
    private static final RequestPostProcessor VICE_PRINCIPAL = user("viceprincipal").roles("VICE_PRINCIPAL");
    private static final RequestPostProcessor TEACHER = user("teacher").roles("TEACHER");
    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMIN_STAFF");
    private static final RequestPostProcessor BURSAR = user("bursar").roles("BURSAR");

    // The demo seeder adds staff on startup. Clear them (rolled back after each test) so counts are exact.
    @BeforeEach
    void clearDemoStaff() {
        repository.deleteAll();
    }

    private static String staffJson(String first, String last, String email, String position, String className) {
        return """
                {
                  "firstName": "%s",
                  "lastName": "%s",
                  "dateOfBirth": "1990-05-15",
                  "address": "12 Hope Road, Kingston",
                  "phone": "876-555-0101",
                  "email": %s,
                  "className": %s,
                  "position": "%s",
                  "hireDate": "2020-09-01",
                  "employmentType": "FULL_TIME"
                }
                """.formatted(first, last,
                email == null ? "null" : "\"" + email + "\"",
                className == null ? "null" : "\"" + className + "\"",
                position);
    }

    private long createAs(RequestPostProcessor who, String body) throws Exception {
        String response = mvc.perform(post("/api/staff").with(who)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        // Pull "id":<number> out of the JSON without needing a JSON library.
        var matcher = java.util.regex.Pattern.compile("\"id\":(\\d+)").matcher(response);
        matcher.find();
        return Long.parseLong(matcher.group(1));
    }

    // ---- STA-TS-1: add and update ----

    @Test
    void principalCanAddStaffMember() throws Exception {
        mvc.perform(post("/api/staff").with(PRINCIPAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(staffJson("Mary", "Smith", "mary@mvpi.edu.jm", "TEACHER", "3A")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.fullName").value("Mary Smith"))
                .andExpect(jsonPath("$.dateOfBirth").value("1990-05-15"))
                .andExpect(jsonPath("$.className").value("3A"))
                .andExpect(jsonPath("$.employmentStatus").value("ACTIVE"));
    }

    @Test
    void principalCanUpdateStaffMember() throws Exception {
        long id = createAs(PRINCIPAL, staffJson("Mary", "Smith", "mary@mvpi.edu.jm", "TEACHER", "3A"));

        mvc.perform(put("/api/staff/" + id).with(PRINCIPAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(staffJson("Mary", "Smith-Brown", "mary@mvpi.edu.jm", "GUIDANCE_COUNSELLOR", "4B")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Smith-Brown"))
                .andExpect(jsonPath("$.position").value("GUIDANCE_COUNSELLOR"))
                .andExpect(jsonPath("$.className").value("4B"));

        mvc.perform(get("/api/staff/" + id).with(PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value("GUIDANCE_COUNSELLOR"));
    }

    @Test
    void updateWithoutStatusKeepsCurrentStatus() throws Exception {
        long id = createAs(PRINCIPAL, staffJson("Mary", "Smith", null, "TEACHER", "3A"));
        String onLeave = staffJson("Mary", "Smith", null, "TEACHER", "3A")
                .replace("\"employmentType\": \"FULL_TIME\"",
                        "\"employmentType\": \"FULL_TIME\", \"employmentStatus\": \"ON_LEAVE\"");
        mvc.perform(put("/api/staff/" + id).with(PRINCIPAL)
                        .contentType(MediaType.APPLICATION_JSON).content(onLeave))
                .andExpect(jsonPath("$.employmentStatus").value("ON_LEAVE"));

        // Same update again, this time with no status in the body.
        mvc.perform(put("/api/staff/" + id).with(PRINCIPAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(staffJson("Mary", "Smith", null, "TEACHER", "3A")))
                .andExpect(jsonPath("$.employmentStatus").value("ON_LEAVE"));
    }

    @Test
    void missingRequiredFieldsAreRejected() throws Exception {
        mvc.perform(post("/api/staff").with(PRINCIPAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \"\", \"position\": \"TEACHER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.lastName").exists())
                .andExpect(jsonPath("$.fieldErrors.dateOfBirth").exists())
                .andExpect(jsonPath("$.fieldErrors.phone").exists())
                .andExpect(jsonPath("$.fieldErrors.hireDate").exists());
    }

    @Test
    void positionMustBeOneOfTheAllowedValues() throws Exception {
        mvc.perform(post("/api/staff").with(PRINCIPAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(staffJson("Mary", "Smith", null, "WIZARD", "3A")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dateOfBirthMustBeInThePast() throws Exception {
        String body = staffJson("Mary", "Smith", null, "TEACHER", "3A")
                .replace("1990-05-15", "2999-01-01");
        mvc.perform(post("/api/staff").with(PRINCIPAL)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.dateOfBirth").exists());
    }

    @Test
    void hireDateCannotBeBeforeDateOfBirth() throws Exception {
        String body = staffJson("Mary", "Smith", null, "TEACHER", "3A")
                .replace("1990-05-15", "2021-01-01"); // born after the 2020-09-01 hire date
        mvc.perform(post("/api/staff").with(PRINCIPAL)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validPhoneFormatsAreAccepted() throws Exception {
        for (String phone : new String[] { "876-555-0123", "(876) 555-0123", "+1 876 555 0123", "8765550123", "555-0123" }) {
            mvc.perform(post("/api/staff").with(PRINCIPAL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(staffJson("Mary", "Smith", null, "TEACHER", "3A")
                                    .replace("876-555-0101", phone)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void scrambledPhoneNumbersAreRejected() throws Exception {
        for (String phone : new String[] { ")3)(  )28-18-3 39 62()+38)0", "(02)7)9)9417+-+8", "abc", "123", "876--555-0123" }) {
            mvc.perform(post("/api/staff").with(PRINCIPAL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(staffJson("Mary", "Smith", null, "TEACHER", "3A")
                                    .replace("876-555-0101", phone)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.phone").exists());
        }
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        createAs(PRINCIPAL, staffJson("Mary", "Smith", "same@mvpi.edu.jm", "TEACHER", "3A"));

        mvc.perform(post("/api/staff").with(PRINCIPAL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(staffJson("John", "Brown", "SAME@mvpi.edu.jm", "TEACHER", "2B")))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownIdReturnsNotFound() throws Exception {
        mvc.perform(get("/api/staff/999999").with(PRINCIPAL))
                .andExpect(status().isNotFound());
    }

    // ---- STA-TS-2: only authorized users ----

    @Test
    void vicePrincipalIsAuthorized() throws Exception {
        mvc.perform(get("/api/staff").with(VICE_PRINCIPAL)).andExpect(status().isOk());
    }

    @Test
    void otherRolesAreForbiddenFromEverything() throws Exception {
        String body = staffJson("Mary", "Smith", null, "TEACHER", "3A");
        for (RequestPostProcessor who : new RequestPostProcessor[] { TEACHER, ADMIN, BURSAR }) {
            mvc.perform(get("/api/staff").with(who)).andExpect(status().isForbidden());
            mvc.perform(get("/api/staff/1").with(who)).andExpect(status().isForbidden());
            mvc.perform(post("/api/staff").with(who)
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
            mvc.perform(put("/api/staff/1").with(who)
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        }
    }

    @Test
    void anonymousUsersAreUnauthorized() throws Exception {
        mvc.perform(get("/api/staff")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/staff")
                .contentType(MediaType.APPLICATION_JSON)
                .content(staffJson("Mary", "Smith", null, "TEACHER", "3A")))
                .andExpect(status().isUnauthorized());
    }

    // ---- STA-TS-3: data is stored ----

    @Test
    void addedStaffMemberIsStoredInTheDatabase() throws Exception {
        long id = createAs(PRINCIPAL, staffJson("Mary", "Smith", "mary@mvpi.edu.jm", "TEACHER", "3A"));

        StaffMember saved = repository.findById(id).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("Mary", saved.getFirstName());
        org.junit.jupiter.api.Assertions.assertEquals("3A", saved.getClassName());
    }

    // ---- STA-TS-4: search and view ----

    @Test
    void searchFindsStaffByNamePositionClassAndStatus() throws Exception {
        createAs(PRINCIPAL, staffJson("Mary", "Smith", "mary@mvpi.edu.jm", "TEACHER", "3A"));
        createAs(PRINCIPAL, staffJson("John", "Brown", "john@mvpi.edu.jm", "TEACHER", "4B"));
        createAs(PRINCIPAL, staffJson("Carla", "Reid", null, "BURSAR", null));

        mvc.perform(get("/api/staff").with(PRINCIPAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].lastName").value("Brown")); // sorted by last name

        mvc.perform(get("/api/staff").param("name", "mar").with(PRINCIPAL))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fullName").value("Mary Smith"));

        mvc.perform(get("/api/staff").param("position", "TEACHER").with(PRINCIPAL))
                .andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(get("/api/staff").param("className", "4b").with(PRINCIPAL))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName").value("John"));

        mvc.perform(get("/api/staff").param("status", "FORMER").with(PRINCIPAL))
                .andExpect(jsonPath("$", hasSize(0)));
    }
}