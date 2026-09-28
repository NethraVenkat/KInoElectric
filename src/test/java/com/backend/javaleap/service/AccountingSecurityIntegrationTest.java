package com.backend.javaleap.service;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Accounting API: role rules, validation and the draft -> post flow over HTTP. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountingSecurityIntegrationTest {

    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        new KineticTestData(context);                 // users admin/accountant/system + API key
        new AccountingTestData(context, false);       // chart of accounts + journals
    }

    @Test
    void accountantRegistersContacts() throws Exception {
        mvc.perform(post("/api/accounting/contacts").header("Authorization", bearer("accountant"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contactType":"CUSTOMER","name":"Chennai Regional Power Grid",
                                 "gstin":"33AABCC1234D1Z5","paymentTermsDays":30}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("CUST-0001"))
                .andExpect(jsonPath("$.data.receivableAccountCode").value("1300"));
    }

    @Test
    void invalidGstinFailsValidation() throws Exception {
        mvc.perform(post("/api/accounting/contacts").header("Authorization", bearer("accountant"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactType\":\"VENDOR\",\"name\":\"X\",\"gstin\":\"12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.gstin").exists());
    }

    @Test
    void onlyAdminChangesChartOfAccounts() throws Exception {
        String body = """
                {"code":"5300","name":"Station Cleaning","parentCode":"5000","subType":"OPERATING_EXPENSE"}""";
        mvc.perform(post("/api/accounting/accounts").header("Authorization", bearer("accountant"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/accounting/accounts").header("Authorization", bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accountType").value("EXPENSE"));
    }

    @Test
    void accountantDraftsAndPostsJournalEntry() throws Exception {
        String token = bearer("accountant");
        String json = mvc.perform(post("/api/accounting/journal-entries").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"journalCode":"MISC","entryDate":"%s","reference":"SUBSIDY-Q2",
                                 "lines":[{"accountCode":"1500","debit":25000},{"accountCode":"4200","credit":25000}]}"""
                                .formatted(java.time.LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(json, "$.data.id");

        mvc.perform(post("/api/accounting/journal-entries/" + id + "/post").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("POSTED"))
                .andExpect(jsonPath("$.data.entryNumber").value(org.hamcrest.Matchers.startsWith("JE/")));

        mvc.perform(get("/api/accounting/trial-balance").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balanced").value(true));
    }

    @Test
    void unbalancedEntryReturns400() throws Exception {
        mvc.perform(post("/api/accounting/journal-entries").header("Authorization", bearer("accountant"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"journalCode":"MISC","entryDate":"%s",
                                 "lines":[{"accountCode":"1500","debit":100},{"accountCode":"4200","credit":90}]}"""
                                .formatted(java.time.LocalDate.now())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not balanced")));
    }

    @Test
    void systemKeyCannotTouchAccounting() throws Exception {
        mvc.perform(get("/api/accounting/contacts").header("X-API-KEY", KineticTestData.API_KEY))
                .andExpect(status().isForbidden());
    }

    private String bearer(String username) throws Exception {
        String json = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + KineticTestData.PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.data.accessToken");
    }
}
