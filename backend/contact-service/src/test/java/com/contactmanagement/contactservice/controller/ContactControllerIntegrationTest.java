package com.contactmanagement.contactservice.controller;

import com.contactmanagement.contactservice.model.Contact;
import com.contactmanagement.contactservice.model.ContactStatus;
import com.contactmanagement.contactservice.repository.ContactRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
        // Clear the database before each test
        contactRepository.deleteAll();
        
        // Create a test contact
        testContact = new Contact();
        testContact.setFirstName("John");
        testContact.setLastName("Doe");
        testContact.setStatus(ContactStatus.ACTIVE);
        testContact.setEmails(Arrays.asList("john.doe@example.com", "j.doe@example.com"));
        testContact.setPhones(Collections.singletonList("+1234567890"));
        testContact.setAddresses(Collections.singletonList("123 Main St, City, State 12345"));
        testContact.setTags(Arrays.asList("friend", "work"));
    }

    @Test
    @DisplayName("Should create contact and persist to database")
    void testCreateContact_Integration() throws Exception {
        // Verify database is empty initially
        assertEquals(0, contactRepository.count());

        // Create contact via API
        mockMvc.perform(post("/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testContact)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Verify contact was saved to database
        assertEquals(1, contactRepository.count());
        List<Contact> contacts = contactRepository.findAll();
        assertEquals(1, contacts.size());
        assertEquals("John", contacts.get(0).getFirstName());
        assertEquals("Doe", contacts.get(0).getLastName());
        assertEquals(ContactStatus.ACTIVE, contacts.get(0).getStatus());
        assertEquals(2, contacts.get(0).getEmails().size());
    }

    @Test
    @DisplayName("Should retrieve all contacts from database")
    void testGetAllContacts_Integration() throws Exception {
        // Save contacts directly to database
        Contact contact1 = new Contact();
        contact1.setFirstName("John");
        contact1.setLastName("Doe");
        contact1.setStatus(ContactStatus.ACTIVE);
        contactRepository.save(contact1);

        Contact contact2 = new Contact();
        contact2.setFirstName("Jane");
        contact2.setLastName("Smith");
        contact2.setStatus(ContactStatus.INACTIVE);
        contactRepository.save(contact2);

        // Retrieve via API
        mockMvc.perform(get("/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].firstName").exists())
                .andExpect(jsonPath("$[1].firstName").exists());
    }

    @Test
    @DisplayName("Should retrieve contact by ID from database")
    void testGetContactById_Integration() throws Exception {
        // Save contact to database
        Contact savedContact = contactRepository.save(testContact);
        UUID contactId = savedContact.getId();

        // Retrieve via API
        mockMvc.perform(get("/contacts/{id}", contactId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(contactId.toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    @DisplayName("Should update contact in database")
    void testUpdateContact_Integration() throws Exception {
        // Save contact to database
        Contact savedContact = contactRepository.save(testContact);
        UUID contactId = savedContact.getId();

        // Update contact via API
        Contact updatedContact = new Contact();
        updatedContact.setFirstName("Johnny");
        updatedContact.setLastName("Doe Jr.");
        updatedContact.setStatus(ContactStatus.INACTIVE);

        mockMvc.perform(put("/contacts/{id}", contactId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedContact)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.lastName").value("Doe Jr."))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        // Verify update in database
        Contact dbContact = contactRepository.findById(contactId).orElseThrow();
        assertEquals("Johnny", dbContact.getFirstName());
        assertEquals("Doe Jr.", dbContact.getLastName());
        assertEquals(ContactStatus.INACTIVE, dbContact.getStatus());
    }

    @Test
    @DisplayName("Should delete contact from database")
    void testDeleteContact_Integration() throws Exception {
        // Save contact to database
        Contact savedContact = contactRepository.save(testContact);
        UUID contactId = savedContact.getId();
        assertEquals(1, contactRepository.count());

        // Delete via API
        mockMvc.perform(delete("/contacts/{id}", contactId))
                .andExpect(status().isNoContent());

        // Verify deletion from database
        assertEquals(0, contactRepository.count());
        assertFalse(contactRepository.existsById(contactId));
    }
}
