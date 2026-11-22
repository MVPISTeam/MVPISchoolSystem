package com.mountainview.sms.expenditure;

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
class LedgerApiTest {

    @Autowired
    MockMvc mvc;

    private String entryJson(String type, String category, String amount, String date) {
        return """
                {
                  "type": "%s",
                  "category": "%s",
                  "amount": %s,
                  "entryDate": "%s",
                  "description": "Test entry",
                  "payee": "Test Supplier Ltd",
                  "referenceNumber": "CHQ-0042"
                }
                """.formatted(type, category, amount, date);
    }

    private int createEntry(String type, String category, String amount, String date) throws Exception {
        String body = mvc.perform(post("/api/expenditure/entries")
                        .with(httpBasic("bursar", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryJson(type, category, amount, date)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    @DisplayName("TS-1: only authorised users can reach the expenditure records")
    void onlyFinanceRolesHaveAccess() throws Exception {
        for (String username : new String[] {"teacher", "admin", "tuckshop"}) {
            mvc.perform(get("/api/expenditure/entries").param("from", "2020-03-01").param("to", "2020-03-31")
                            .with(httpBasic(username, "mvpi1234")))
                    .andExpect(status().isForbidden());
        }
        for (String username : new String[] {"principal", "viceprincipal", "bursar"}) {
            mvc.perform(get("/api/expenditure/entries").param("from", "2020-03-01").param("to", "2020-03-31")
                            .with(httpBasic(username, "mvpi1234")))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("TS-2: a record can be added and then edited")
    void addThenEdit() throws Exception {
        int id = createEntry("EXPENSE", "UTILITIES", "18500.00", "2020-03-05");

        mvc.perform(put("/api/expenditure/entries/" + id)
                        .with(httpBasic("bursar", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryJson("EXPENSE", "UTILITIES", "19250.75", "2020-03-06")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(19250.75))
                .andExpect(jsonPath("$.entryDate").value("2020-03-06"))
                .andExpect(jsonPath("$.referenceNumber").value("CHQ-0042"));
    }

    @Test
    @DisplayName("TS-2: an amount of zero or less is rejected (400)")
    void rejectsNonPositiveAmount() throws Exception {
        mvc.perform(post("/api/expenditure/entries")
                        .with(httpBasic("bursar", "mvpi1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryJson("EXPENSE", "UTILITIES", "0", "2020-03-05")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    @DisplayName("TS-3: the dashboard totals income and expenses by category for a period")
    void summaryByCategory() throws Exception {
        createEntry("INCOME", "MINISTRY_GRANT", "250000.00", "2020-03-02");
        createEntry("INCOME", "FUNDRAISER", "40000.00", "2020-03-10");
        createEntry("EXPENSE", "UTILITIES", "18500.00", "2020-03-05");
        createEntry("EXPENSE", "UTILITIES", "6500.50", "2020-03-20");
        createEntry("EXPENSE", "MAINTENANCE", "30000.00", "2020-03-25");
        createEntry("EXPENSE", "SUPPLIES", "9999.00", "2020-04-01");   // outside the period

        mvc.perform(get("/api/expenditure/summary").param("from", "2020-03-01").param("to", "2020-03-31")
                        .with(httpBasic("principal", "mvpi1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(290000.00))
                .andExpect(jsonPath("$.totalExpenses").value(55000.50))
                .andExpect(jsonPath("$.balance").value(234999.50))
                .andExpect(jsonPath("$.incomeByCategory", hasSize(2)))
                .andExpect(jsonPath("$.expensesByCategory", hasSize(2)))
                .andExpect(jsonPath("$.expensesByCategory[?(@.category == 'UTILITIES')].total").value(25000.50))
                .andExpect(jsonPath("$.entries", hasSize(5)));
    }

    @Test
    @DisplayName("SR-4/SR-5: entries can be filtered to income only or expenses only")
    void filterByType() throws Exception {
        createEntry("INCOME", "MINISTRY_GRANT", "250000.00", "2020-03-02");
        createEntry("EXPENSE", "UTILITIES", "18500.00", "2020-03-05");

        mvc.perform(get("/api/expenditure/entries").param("from", "2020-03-01").param("to", "2020-03-31")
                        .param("type", "INCOME").with(httpBasic("bursar", "mvpi1234")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].category").value("MINISTRY_GRANT"));
    }

    @Test
    @DisplayName("TS-4: the monthly expenditure report covers exactly that month")
    void monthlyReport() throws Exception {
        createEntry("EXPENSE", "UTILITIES", "18500.00", "2020-02-29");
        createEntry("EXPENSE", "SALARIES", "120000.00", "2020-03-01");
        createEntry("EXPENSE", "SUPPLIES", "5000.00", "2020-03-31");
        createEntry("EXPENSE", "SUPPLIES", "7000.00", "2020-04-01");

        mvc.perform(get("/api/expenditure/reports/monthly").param("year", "2020").param("month", "3")
                        .with(httpBasic("bursar", "mvpi1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2020-03-01"))
                .andExpect(jsonPath("$.to").value("2020-03-31"))
                .andExpect(jsonPath("$.totalExpenses").value(125000.00))
                .andExpect(jsonPath("$.entries", hasSize(2)));
    }

    @Test
    @DisplayName("TS-4: a month with nothing recorded gives zero totals, and month 13 gives 400")
    void emptyAndInvalidMonths() throws Exception {
        mvc.perform(get("/api/expenditure/reports/monthly").param("year", "1999").param("month", "1")
                        .with(httpBasic("bursar", "mvpi1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpenses").value(0))
                .andExpect(jsonPath("$.entries", hasSize(0)));

        mvc.perform(get("/api/expenditure/reports/monthly").param("year", "2020").param("month", "13")
                        .with(httpBasic("bursar", "mvpi1234")))
                .andExpect(status().isBadRequest());
    }
}