package com.contactmanagement.contactservice.controller;

import com.contactmanagement.contactservice.config.SecurityConfig;
import com.contactmanagement.contactservice.model.Contact;
import com.contactmanagement.contactservice.model.ContactStatus;
import com.contactmanagement.contactservice.repository.ContactRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@Transactional
@DisplayName("ContactController Integration Tests")
class ContactControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Contact testContact;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        SecurityConfig.DEV_USERNAME,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))));
        contactRepository.deleteAll();

        testContact = new Contact();
        testContact.setFirstName("John");
        testContact.setLastName("Doe");
        testContact.setStatus(ContactStatus.ACTIVE);
        testContact.setEmail("john.doe@example.com");
        testContact.setMobile("+1234567890");
        testContact.setHomePhone("+1987654321");
        testContact.setAddresses(Collections.singletonList("123 Main St, City, State 12345"));
        testContact.setTags(Arrays.asList("friend", "work"));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should create contact and persist to database")
    void testCreateContact_Integration() throws Exception {
        assertEquals(0, contactRepository.count());

        mockMvc.perform(post("/contacts")
                        .with(httpBasic(SecurityConfig.DEV_USERNAME, SecurityConfig.DEV_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testContact)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.mobile").value("+1234567890"))
                .andExpect(jsonPath("$.homePhone").value("+1987654321"))
                .andExpect(jsonPath("$.createdBy").value(SecurityConfig.DEV_USERNAME))
                .andExpect(jsonPath("$.updatedBy").value(SecurityConfig.DEV_USERNAME));

        assertEquals(1, contactRepository.count());
        List<Contact> contacts = contactRepository.findAll();
        assertEquals(1, contacts.size());
        assertEquals("John", contacts.get(0).getFirstName());
        assertEquals("Doe", contacts.get(0).getLastName());
        assertEquals(ContactStatus.ACTIVE, contacts.get(0).getStatus());
        assertEquals("john.doe@example.com", contacts.get(0).getEmail());
        assertEquals(SecurityConfig.DEV_USERNAME, contacts.get(0).getCreatedBy());
        assertEquals(SecurityConfig.DEV_USERNAME, contacts.get(0).getUpdatedBy());
    }

    @Test
    @DisplayName("Should retrieve all contacts from database")
    void testGetAllContacts_Integration() throws Exception {
        Contact contact1 = new Contact();
        contact1.setFirstName("John");
        contact1.setLastName("Doe");
        contact1.setStatus(ContactStatus.ACTIVE);
        contact1.setMobile("+1111111111");
        contact1.setEmail("john.doe@example.com");
        contact1.setAddresses(new ArrayList<>());
        contact1.setTags(new ArrayList<>());
        contactRepository.save(contact1);

        Contact contact2 = new Contact();
        contact2.setFirstName("Jane");
        contact2.setLastName("Smith");
        contact2.setStatus(ContactStatus.INACTIVE);
        contact2.setMobile("+2222222222");
        contact2.setEmail("jane.smith@example.com");
        contact2.setAddresses(new ArrayList<>());
        contact2.setTags(new ArrayList<>());
        contactRepository.save(contact2);

        mockMvc.perform(get("/contacts")
                        .with(httpBasic(SecurityConfig.DEV_USERNAME, SecurityConfig.DEV_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].firstName").exists())
                .andExpect(jsonPath("$[1].firstName").exists());
    }

    @Test
    @DisplayName("Should retrieve contact by ID from database")
    void testGetContactById_Integration() throws Exception {
        Contact savedContact = contactRepository.save(testContact);
        UUID contactId = savedContact.getId();

        mockMvc.perform(get("/contacts/{id}", contactId)
                        .with(httpBasic(SecurityConfig.DEV_USERNAME, SecurityConfig.DEV_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contactId.toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    @DisplayName("Should update contact in database")
    void testUpdateContact_Integration() throws Exception {
        Contact savedContact = contactRepository.save(testContact);
        UUID contactId = savedContact.getId();
        String originalCreatedBy = savedContact.getCreatedBy();

        Contact updatedContact = new Contact();
        updatedContact.setFirstName("Johnny");
        updatedContact.setLastName("Doe Jr.");
        updatedContact.setStatus(ContactStatus.INACTIVE);
        updatedContact.setMobile("+1234567890");
        updatedContact.setHomePhone(null);
        updatedContact.setEmail(savedContact.getEmail());
        updatedContact.setAddresses(savedContact.getAddresses());
        updatedContact.setTags(savedContact.getTags());

        mockMvc.perform(put("/contacts/{id}", contactId)
                        .with(httpBasic(SecurityConfig.DEV_USERNAME, SecurityConfig.DEV_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedContact)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.lastName").value("Doe Jr."))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        Contact dbContact = contactRepository.findById(contactId).orElseThrow();
        assertEquals("Johnny", dbContact.getFirstName());
        assertEquals("Doe Jr.", dbContact.getLastName());
        assertEquals(ContactStatus.INACTIVE, dbContact.getStatus());
        assertEquals(originalCreatedBy, dbContact.getCreatedBy());
        assertEquals(SecurityConfig.DEV_USERNAME, dbContact.getUpdatedBy());
    }

    @Test
    @DisplayName("Should delete contact from database")
    void testDeleteContact_Integration() throws Exception {
        Contact savedContact = contactRepository.save(testContact);
        UUID contactId = savedContact.getId();
        assertEquals(1, contactRepository.count());

        mockMvc.perform(delete("/contacts/{id}", contactId)
                        .with(httpBasic(SecurityConfig.DEV_USERNAME, SecurityConfig.DEV_PASSWORD)))
                .andExpect(status().isNoContent());

        assertEquals(0, contactRepository.count());
        assertFalse(contactRepository.existsById(contactId));
    }
}
