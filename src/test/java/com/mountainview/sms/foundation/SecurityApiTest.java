package com.mountainview.sms.foundation;

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

@SpringBootTest
@AutoConfigureMockMvc
class SecurityApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("No username and password: 401")
    void rejectsAnonymousRequests() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Wrong password: 401")
    void rejectsWrongPassword() throws Exception {
        mvc.perform(get("/api/auth/me").with(httpBasic("teacher", "not-the-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Seeded teacher can log in and sees their role")
    void seededTeacherCanLogIn() throws Exception {
        mvc.perform(get("/api/auth/me").with(httpBasic("teacher", "mvpi1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("teacher"))
                .andExpect(jsonPath("$.role").value("TEACHER"));
    }
}
