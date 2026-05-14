package com.contactmanagement.contactservice.controller;

import com.contactmanagement.contactservice.config.SecurityConfig;
import com.contactmanagement.contactservice.service.ContactService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ContactController.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                EurekaClientAutoConfiguration.class
        })
@Import(SecurityConfig.class)
@DisplayName("ContactController security (slice)")
class ContactControllerSecurityMvcTest {

    @MockBean
    private ContactService contactService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /contacts without user returns 401")
    void getContacts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/contacts"))
                .andExpect(status().isUnauthorized());
    }
}
