package com.contactmanagement.contactservice.controller;

import com.contactmanagement.contactservice.config.SecurityConfig;
import com.contactmanagement.contactservice.model.Contact;
import com.contactmanagement.contactservice.model.ContactStatus;
import com.contactmanagement.contactservice.service.ContactService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ContactController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.security.users.read.username=reader_test",
        "app.security.users.read.password=reader_test_password",
        "app.security.users.write.username=writer_test",
        "app.security.users.write.password=writer_test_password",
        "app.security.users.admin.username=admin_test",
        "app.security.users.admin.password=admin_test_password"
})
@DisplayName("ContactController Security Tests")
class ContactControllerSecurityTest {

    @MockBean
    private ContactService contactService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should reject unauthenticated requests")
    void shouldRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/contacts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow read user to access GET endpoint")
    void shouldAllowReadUserForGet() throws Exception {
        when(contactService.readAll()).thenReturn(List.of());

        mockMvc.perform(get("/contacts")
                        .with(httpBasic("reader_test", "reader_test_password")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should forbid read user from write endpoint")
    void shouldForbidReadUserForPost() throws Exception {
        Contact contact = buildContact();

        mockMvc.perform(post("/contacts")
                        .with(httpBasic("reader_test", "reader_test_password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contact)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow write user to create contacts")
    void shouldAllowWriteUserForPost() throws Exception {
        Contact contact = buildContact();
        when(contactService.create(any(Contact.class))).thenReturn(contact);

        mockMvc.perform(post("/contacts")
                        .with(httpBasic("writer_test", "writer_test_password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contact)))
                .andExpect(status().isCreated());
    }

    private Contact buildContact() {
        Contact contact = new Contact();
        contact.setId(UUID.randomUUID());
        contact.setFirstName("Secure");
        contact.setLastName("User");
        contact.setStatus(ContactStatus.ACTIVE);
        return contact;
    }
}
