package com.backend.javaleap.service;

import com.backend.javaleap.entity.enums.ContactType;
import com.backend.javaleap.entity.enums.ProductType;
import com.backend.javaleap.entity.enums.UnitOfMeasure;
import com.backend.javaleap.model.request.ContactRequest;
import com.backend.javaleap.model.request.ProductRequest;
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

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Transaction APIs over HTTP: the accountant runs the purchase flow; the System key is locked out. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionSecurityIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired ContactService contactService;
    @Autowired ProductService productService;
    MockMvc mvc;
    Long vendorId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        new KineticTestData(context);
        new AccountingTestData(context, false);
        vendorId = contactService.create(new ContactRequest(ContactType.VENDOR, "KinetiFloor", null, null, null, null,
                null, null, null, null, null, null, 30, null, null, null, null)).id();
        productService.create(new ProductRequest("SRV-OVH", "Overhaul", ProductType.SERVICE, UnitOfMeasure.JOB, null,
                BigDecimal.ZERO, new BigDecimal("3500"), new BigDecimal("18"), false, true, null, null, null));
    }

    @Test
    void accountantRunsPurchaseFlowOverHttp() throws Exception {
        String token = bearer("accountant");
        String json = mvc.perform(post("/api/purchase/orders").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"vendorId":%d,"orderDate":"%s","reference":"Overhaul CHC-PF1-R2C07",
                                 "lines":[{"productCode":"SRV-OVH","quantity":1}]}"""
                                .formatted(vendorId, java.time.LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount").value(4130.0))
                .andReturn().getResponse().getContentAsString();
        Integer poId = JsonPath.read(json, "$.data.id");

        mvc.perform(post("/api/purchase/orders/" + poId + "/confirm").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.poNumber").value(org.hamcrest.Matchers.startsWith("PO/")));

        String billJson = mvc.perform(post("/api/purchase/orders/" + poId + "/bill").header("Authorization", token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer billId = JsonPath.read(billJson, "$.data.id");

        mvc.perform(post("/api/invoices/" + billId + "/post").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoiceNumber").value(org.hamcrest.Matchers.startsWith("BILL/")));

        mvc.perform(post("/api/payments").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoiceId\":" + billId + ",\"amount\":4130,\"method\":\"NEFT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.paymentType").value("OUTBOUND"));
    }

    @Test
    void invalidOrderReturns400() throws Exception {
        mvc.perform(post("/api/purchase/orders").header("Authorization", bearer("accountant"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vendorId\":" + vendorId + ",\"orderDate\":\"2026-09-27\",\"lines\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.lines").exists());
    }

    @Test
    void systemKeyCannotSeeInvoices() throws Exception {
        mvc.perform(get("/api/invoices").header("X-API-KEY", KineticTestData.API_KEY))
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
