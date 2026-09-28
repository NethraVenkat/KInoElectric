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

/** API-level tests: X-API-KEY for the System actor and role rules on kinetic endpoints. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class KineticSecurityIntegrationTest {

    private static final String TELEMETRY = """
            {"stationCode":"TST","source":"GW-TEST","intervalSeconds":60,
             "sectors":[{"sectorCode":"ENT","steps":120}]}""";

    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        new KineticTestData(context);
    }

    @Test
    void systemApiKeyCanPostTelemetry() throws Exception {
        mvc.perform(post("/api/kinetic/telemetry").header("X-API-KEY", KineticTestData.API_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content(TELEMETRY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSteps").value(120))
                .andExpect(jsonPath("$.data.duplicate").value(false));
    }

    @Test
    void wrongApiKeyIsRejected() throws Exception {
        mvc.perform(post("/api/kinetic/telemetry").header("X-API-KEY", "pdk_sys_wrong")
                        .contentType(MediaType.APPLICATION_JSON).content(TELEMETRY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accountantCannotPostTelemetryButAdminCan() throws Exception {
        mvc.perform(post("/api/kinetic/telemetry").header("Authorization", bearer("accountant"))
                        .contentType(MediaType.APPLICATION_JSON).content(TELEMETRY))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/kinetic/telemetry").header("Authorization", bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(TELEMETRY))
                .andExpect(status().isOk());
    }

    @Test
    void systemKeyCannotReadBusinessData() throws Exception {
        mvc.perform(get("/api/kinetic/sectors").header("X-API-KEY", KineticTestData.API_KEY))
                .andExpect(status().isForbidden());
    }

    @Test
    void accountantCanReadSectorsButNotCreateThem() throws Exception {
        String token = bearer("accountant");
        mvc.perform(get("/api/kinetic/sectors").param("stationCode", "TST").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
        mvc.perform(post("/api/kinetic/sectors").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stationCode":"TST","code":"CON","name":"Concourse","zoneType":"CONCOURSE",
                                 "maxCapacity":300,"gridRows":2,"gridCols":3}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreatesSectorAndNewApiKeyWorks() throws Exception {
        String token = bearer("admin");
        mvc.perform(post("/api/kinetic/sectors").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stationCode":"TST","code":"CON","name":"Concourse","zoneType":"CONCOURSE",
                                 "maxCapacity":300,"gridRows":2,"gridCols":3}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sector.analyticAccountCode").value("AA-TST-CON"))
                .andExpect(jsonPath("$.data.tiles.length()").value(6));

        String json = mvc.perform(post("/api/admin/api-keys").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Gateway 2\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String newKey = JsonPath.read(json, "$.data.apiKey");

        mvc.perform(post("/api/kinetic/telemetry").header("X-API-KEY", newKey)
                        .contentType(MediaType.APPLICATION_JSON).content(TELEMETRY))
                .andExpect(status().isOk());
    }

    @Test
    void emptyTelemetryFailsValidation() throws Exception {
        mvc.perform(post("/api/kinetic/telemetry").header("X-API-KEY", KineticTestData.API_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stationCode\":\"TST\",\"sectors\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.sectors").exists());
    }

    private String bearer(String username) throws Exception {
        String json = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + KineticTestData.PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.data.accessToken");
    }
}
