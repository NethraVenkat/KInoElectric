package com.backend.javaleap.service;

import com.backend.javaleap.entity.User;
import com.backend.javaleap.entity.enums.Role;
import com.backend.javaleap.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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

/** End-to-end security tests: login, JWT filter, 401/403 handling and role rules. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthSecurityIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        save("admin", Role.ADMIN, true);
        save("accountant", Role.ACCOUNTANT, true);
        save("system", Role.SYSTEM, true);
        save("ghost", Role.ACCOUNTANT, false);
    }

    @Test
    void publicEndpointsNeedNoToken() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointWithoutTokenReturns401Json() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void loginReturnsTokenAndTokenOpensProtectedEndpoint() throws Exception {
        String token = login("accountant", "Secret@123");

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("accountant"))
                .andExpect(jsonPath("$.data.role").value("ACCOUNTANT"));
    }

    @Test
    void loginWithEmailWorks() throws Exception {
        login("accountant@test.in", "Secret@123");
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"system\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void disabledUserCannotLogIn() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ghost\",\"password\":\"Secret@123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void blankLoginFailsValidation() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.username").exists());
    }

    @Test
    void invalidTokenIsTreatedAsAnonymous() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userManagementIsAdminOnly() throws Exception {
        String accountantToken = login("accountant", "Secret@123");
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isForbidden());

        String adminToken = login("admin", "Secret@123");
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(4));
    }

    @Test
    void adminCanCreateUserAndDuplicateIsRejected() throws Exception {
        String adminToken = login("admin", "Secret@123");
        String body = """
                {"username":"newop","email":"newop@test.in","password":"Welcome123",
                 "fullName":"New Operator","role":"ACCOUNTANT"}""";

        mvc.perform(post("/api/users").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("newop"));

        mvc.perform(post("/api/users").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ helpers

    private String login(String username, String password) throws Exception {
        String json = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.data.accessToken");
    }

    private void save(String username, Role role, boolean enabled) {
        userRepository.save(User.builder()
                .username(username)
                .email(username + "@test.in")
                .passwordHash(passwordEncoder.encode("Secret@123"))
                .fullName(username)
                .role(role)
                .enabled(enabled)
                .build());
    }
}
