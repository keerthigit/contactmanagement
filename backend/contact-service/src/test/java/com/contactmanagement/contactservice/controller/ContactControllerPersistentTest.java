package com.contactmanagement.contactservice.controller;

import com.contactmanagement.contactservice.model.Contact;
import com.contactmanagement.contactservice.model.ContactStatus;
import com.contactmanagement.contactservice.repository.ContactRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests that persist data to the database.
 * 
 * NOTE: These tests will actually save data to the database and NOT rollback.
 * Use with caution - data will persist after tests run.
 * 
 * To see the data in the database:
 * 1. Run this test class
 * 2. Check the database: docker exec -it contact-postgres psql -U contactuser -d contactdb -c "SELECT * FROM contacts;"
 */
@SpringBootTest(classes = com.contactmanagement.contactservice.ContactServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
// NOTE: NO @Transactional - data will persist!
// NOTE: This test uses the full Spring Boot context with real database
@DisplayName("ContactController Persistent Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContactControllerPersistentTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static java.util.List<UUID> createdContactIds = new java.util.ArrayList<>();

    @BeforeAll
    static void setUpOnce() {
        System.out.println("=========================================");
        System.out.println("Running Persistent Integration Tests");
        System.out.println("Creating 10 contacts - data will be saved to the database!");
        System.out.println("=========================================");
    }

    @BeforeEach
    void setUp() {
        // Optional: Clear only if you want fresh data each time
        // contactRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Should create 10 contacts and persist to database (data will remain)")
    void testCreate10Contacts_Persistent() throws Exception {
        String[][] contactData = {
            {"John", "Doe", "ACTIVE", "john.doe@example.com", "+1234567890", "123 Main St, City, State 12345", "friend,work"},
            {"Jane", "Smith", "ACTIVE", "jane.smith@example.com", "+1987654321", "456 Oak Ave, Town, State 54321", "family,personal"},
            {"Bob", "Johnson", "INACTIVE", "bob.johnson@example.com", "+1555123456", "789 Pine Rd, Village, State 67890", "work,colleague"},
            {"Alice", "Williams", "ACTIVE", "alice.williams@example.com", "+1444555666", "321 Elm St, City, State 11111", "friend,personal"},
            {"Charlie", "Brown", "ARCHIVED", "charlie.brown@example.com", "+1777888999", "654 Maple Dr, Town, State 22222", "family"},
            {"Diana", "Davis", "ACTIVE", "diana.davis@example.com", "+1222333444", "987 Cedar Ln, Village, State 33333", "work,client"},
            {"Edward", "Miller", "INACTIVE", "edward.miller@example.com", "+1666777888", "147 Birch Way, City, State 44444", "colleague"},
            {"Fiona", "Wilson", "ACTIVE", "fiona.wilson@example.com", "+1999888777", "258 Spruce Ct, Town, State 55555", "friend,personal"},
            {"George", "Moore", "ACTIVE", "george.moore@example.com", "+1111222333", "369 Willow Pl, Village, State 66666", "work,partner"},
            {"Helen", "Taylor", "INACTIVE", "helen.taylor@example.com", "+1444555777", "741 Ash Blvd, City, State 77777", "family,friend"}
        };

        for (int i = 0; i < contactData.length; i++) {
            String[] data = contactData[i];
            Contact testContact = new Contact();
            testContact.setFirstName(data[0]);
            testContact.setLastName(data[1]);
            testContact.setStatus(ContactStatus.valueOf(data[2]));
            testContact.setEmails(Arrays.asList(data[3]));
            testContact.setPhones(Collections.singletonList(data[4]));
            testContact.setAddresses(Collections.singletonList(data[5]));
            testContact.setTags(Arrays.asList(data[6].split(",")));

            String response = mockMvc.perform(post("/contacts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testContact)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.firstName").value(data[0]))
                    .andExpect(jsonPath("$.lastName").value(data[1]))
                    .andExpect(jsonPath("$.status").value(data[2]))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Extract ID from response
            String idString = response.substring(response.indexOf("\"id\":\"") + 6, 
                    response.indexOf("\"", response.indexOf("\"id\":\"") + 6));
            UUID contactId = UUID.fromString(idString);
            createdContactIds.add(contactId);
            
            System.out.println("Created contact #" + (i + 1) + ": " + data[0] + " " + data[1] + " (ID: " + contactId + ")");
        }
        
        System.out.println("\nSuccessfully created " + createdContactIds.size() + " contacts!");
        System.out.println("You can verify in database with:");
        System.out.println("docker exec contact-postgres psql -U contactuser -d contactdb -c \"SELECT id, first_name, last_name, status FROM contacts ORDER BY created_at DESC LIMIT 10;\"");
    }

    @Test
    @Order(2)
    @DisplayName("Should retrieve all contacts including persisted ones")
    void testGetAllContacts_Persistent() throws Exception {
        mockMvc.perform(get("/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(10))))
                .andExpect(jsonPath("$[*].firstName").exists());
    }

    @AfterAll
    static void tearDown() {
        System.out.println("=========================================");
        System.out.println("Tests completed. Data is still in database.");
        System.out.println("To view contacts:");
        System.out.println("docker exec -it contact-postgres psql -U contactuser -d contactdb -c \"SELECT * FROM contacts;\"");
        System.out.println("=========================================");
    }
}
