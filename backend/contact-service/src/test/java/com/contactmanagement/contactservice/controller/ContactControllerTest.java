package com.contactmanagement.contactservice.controller;

import com.contactmanagement.contactservice.TestConfig;
import com.contactmanagement.contactservice.dto.ContactSearchRequest;
import com.contactmanagement.contactservice.dto.PaginatedResponse;
import com.contactmanagement.contactservice.model.Contact;
import com.contactmanagement.contactservice.model.ContactStatus;
import com.contactmanagement.contactservice.service.ContactService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ContactController.class, 
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                EurekaClientAutoConfiguration.class
        })
@Import(TestConfig.class)
@DisplayName("ContactController Unit Tests")
class ContactControllerTest {

    @MockBean
    private ContactService contactService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Contact testContact;
    private UUID testContactId;

    @BeforeEach
    void setUp() {
        testContactId = UUID.randomUUID();
        testContact = createTestContact(testContactId);
    }

    // ========== POST /contacts - Create Contact ==========

    @Test
    @DisplayName("Should create contact successfully")
    void testCreateContact_Success() throws Exception {
        Contact newContact = createTestContact(null);
        Contact savedContact = createTestContact(testContactId);

        when(contactService.create(any(Contact.class))).thenReturn(savedContact);

        mockMvc.perform(post("/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newContact)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testContactId.toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.emails", hasSize(2)))
                .andExpect(jsonPath("$.phones", hasSize(1)))
                .andExpect(jsonPath("$.addresses", hasSize(1)))
                .andExpect(jsonPath("$.tags", hasSize(2)));

        verify(contactService, times(1)).create(any(Contact.class));
    }

    @Test
    @DisplayName("Should return 400 when creating contact with invalid data")
    void testCreateContact_ValidationFailure() throws Exception {
        Contact invalidContact = new Contact();
        // Missing required fields: firstName, lastName, status

        mockMvc.perform(post("/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidContact)))
                .andExpect(status().isBadRequest());

        verify(contactService, never()).create(any(Contact.class));
    }

    @Test
    @DisplayName("Should create contact with minimal required fields")
    void testCreateContact_MinimalFields() throws Exception {
        Contact minimalContact = new Contact();
        minimalContact.setFirstName("Jane");
        minimalContact.setLastName("Smith");
        minimalContact.setStatus(ContactStatus.ACTIVE);

        Contact savedContact = new Contact();
        savedContact.setId(testContactId);
        savedContact.setFirstName("Jane");
        savedContact.setLastName("Smith");
        savedContact.setStatus(ContactStatus.ACTIVE);
        savedContact.setEmails(new ArrayList<>());
        savedContact.setPhones(new ArrayList<>());
        savedContact.setAddresses(new ArrayList<>());
        savedContact.setTags(new ArrayList<>());

        when(contactService.create(any(Contact.class))).thenReturn(savedContact);

        mockMvc.perform(post("/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minimalContact)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testContactId.toString()))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(contactService, times(1)).create(any(Contact.class));
    }

    // ========== GET /contacts - Get All Contacts ==========

    @Test
    @DisplayName("Should return all contacts successfully")
    void testGetAllContacts_Success() throws Exception {
        List<Contact> contacts = Arrays.asList(
                createTestContact(testContactId),
                createTestContact(UUID.randomUUID(), "Jane", "Smith", ContactStatus.INACTIVE)
        );

        when(contactService.readAll()).thenReturn(contacts);

        mockMvc.perform(get("/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(testContactId.toString()))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[1].firstName").value("Jane"));

        verify(contactService, times(1)).readAll();
    }

    @Test
    @DisplayName("Should return empty list when no contacts exist")
    void testGetAllContacts_EmptyList() throws Exception {
        when(contactService.readAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(contactService, times(1)).readAll();
    }

    // ========== GET /contacts/{id} - Get Contact By ID ==========

    @Test
    @DisplayName("Should return contact by id successfully")
    void testGetContactById_Success() throws Exception {
        when(contactService.read(testContactId)).thenReturn(Optional.of(testContact));

        mockMvc.perform(get("/contacts/{id}", testContactId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testContactId.toString()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.emails[0]").value("john.doe@example.com"))
                .andExpect(jsonPath("$.phones[0]").value("+1234567890"));

        verify(contactService, times(1)).read(testContactId);
    }

    @Test
    @DisplayName("Should return 404 when contact not found")
    void testGetContactById_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(contactService.read(nonExistentId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/contacts/{id}", nonExistentId))
                .andExpect(status().isNotFound());

        verify(contactService, times(1)).read(nonExistentId);
    }

    @Test
    @DisplayName("Should return 400 when id format is invalid")
    void testGetContactById_InvalidIdFormat() throws Exception {
        mockMvc.perform(get("/contacts/{id}", "invalid-uuid"))
                .andExpect(status().isBadRequest());

        verify(contactService, never()).read(any(UUID.class));
    }

    // ========== PUT /contacts/{id} - Update Contact ==========

    @Test
    @DisplayName("Should update contact successfully")
    void testUpdateContact_Success() throws Exception {
        Contact updatedContact = createTestContact(testContactId);
        updatedContact.setFirstName("Johnny");
        updatedContact.setLastName("Doe Jr.");
        updatedContact.setStatus(ContactStatus.INACTIVE);

        when(contactService.update(eq(testContactId), any(Contact.class))).thenReturn(updatedContact);

        mockMvc.perform(put("/contacts/{id}", testContactId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedContact)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testContactId.toString()))
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.lastName").value("Doe Jr."))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(contactService, times(1)).update(eq(testContactId), any(Contact.class));
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent contact")
    void testUpdateContact_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        Contact contactToUpdate = createTestContact(nonExistentId);

        when(contactService.update(eq(nonExistentId), any(Contact.class)))
                .thenThrow(new RuntimeException("Contact not found with id: " + nonExistentId));

        mockMvc.perform(put("/contacts/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contactToUpdate)))
                .andExpect(status().isNotFound());

        verify(contactService, times(1)).update(eq(nonExistentId), any(Contact.class));
    }

    @Test
    @DisplayName("Should return 400 when updating contact with invalid data")
    void testUpdateContact_ValidationFailure() throws Exception {
        Contact invalidContact = new Contact();
        // Missing required fields

        mockMvc.perform(put("/contacts/{id}", testContactId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidContact)))
                .andExpect(status().isBadRequest());

        verify(contactService, never()).update(any(UUID.class), any(Contact.class));
    }

    // ========== DELETE /contacts/{id} - Delete Contact ==========

    @Test
    @DisplayName("Should delete contact successfully")
    void testDeleteContact_Success() throws Exception {
        doNothing().when(contactService).delete(testContactId);

        mockMvc.perform(delete("/contacts/{id}", testContactId))
                .andExpect(status().isNoContent());

        verify(contactService, times(1)).delete(testContactId);
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent contact")
    void testDeleteContact_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        doThrow(new RuntimeException("Contact not found with id: " + nonExistentId))
                .when(contactService).delete(nonExistentId);

        mockMvc.perform(delete("/contacts/{id}", nonExistentId))
                .andExpect(status().isNotFound());

        verify(contactService, times(1)).delete(nonExistentId);
    }

    // ========== GET /contacts/search - Search Contacts ==========

    @Test
    @DisplayName("Should search contacts successfully with name filter")
    void testSearchContacts_ByName() throws Exception {
        List<Contact> searchResults = Arrays.asList(
                createTestContact(testContactId),
                createTestContact(UUID.randomUUID(), "John", "Smith", ContactStatus.ACTIVE)
        );

        PaginatedResponse<Contact> paginatedResponse = new PaginatedResponse<>(
                searchResults, 0, 20, 2L
        );

        when(contactService.searchContacts(any(ContactSearchRequest.class)))
                .thenReturn(paginatedResponse);

        mockMvc.perform(get("/contacts/search")
                        .param("name", "John")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].firstName").value("John"));

        verify(contactService, times(1)).searchContacts(any(ContactSearchRequest.class));
    }

    @Test
    @DisplayName("Should search contacts successfully with email filter")
    void testSearchContacts_ByEmail() throws Exception {
        List<Contact> searchResults = Collections.singletonList(testContact);
        PaginatedResponse<Contact> paginatedResponse = new PaginatedResponse<>(
                searchResults, 0, 20, 1L
        );

        when(contactService.searchContacts(any(ContactSearchRequest.class)))
                .thenReturn(paginatedResponse);

        mockMvc.perform(get("/contacts/search")
                        .param("email", "john.doe@example.com")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].emails[0]").value("john.doe@example.com"));

        verify(contactService, times(1)).searchContacts(any(ContactSearchRequest.class));
    }

    @Test
    @DisplayName("Should search contacts successfully with phone filter")
    void testSearchContacts_ByPhone() throws Exception {
        List<Contact> searchResults = Collections.singletonList(testContact);
        PaginatedResponse<Contact> paginatedResponse = new PaginatedResponse<>(
                searchResults, 0, 20, 1L
        );

        when(contactService.searchContacts(any(ContactSearchRequest.class)))
                .thenReturn(paginatedResponse);

        mockMvc.perform(get("/contacts/search")
                        .param("phone", "1234567890")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(contactService, times(1)).searchContacts(any(ContactSearchRequest.class));
    }

    @Test
    @DisplayName("Should search contacts with multiple filters")
    void testSearchContacts_MultipleFilters() throws Exception {
        List<Contact> searchResults = Collections.singletonList(testContact);
        PaginatedResponse<Contact> paginatedResponse = new PaginatedResponse<>(
                searchResults, 0, 20, 1L
        );

        when(contactService.searchContacts(any(ContactSearchRequest.class)))
                .thenReturn(paginatedResponse);

        mockMvc.perform(get("/contacts/search")
                        .param("name", "John")
                        .param("email", "john.doe@example.com")
                        .param("phone", "1234567890")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "firstName")
                        .param("sortDirection", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(contactService, times(1)).searchContacts(any(ContactSearchRequest.class));
    }

    @Test
    @DisplayName("Should search contacts with pagination")
    void testSearchContacts_WithPagination() throws Exception {
        List<Contact> searchResults = Collections.singletonList(testContact);
        // Page 0, size 10, total 15 elements = 2 pages (0 and 1)
        // On page 0, hasNext should be true (page 1 exists), hasPrevious should be false
        PaginatedResponse<Contact> paginatedResponse = new PaginatedResponse<>(
                searchResults, 0, 10, 15L
        );

        when(contactService.searchContacts(any(ContactSearchRequest.class)))
                .thenReturn(paginatedResponse);

        mockMvc.perform(get("/contacts/search")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrevious").value(false));

        verify(contactService, times(1)).searchContacts(any(ContactSearchRequest.class));
    }

    @Test
    @DisplayName("Should return empty search results")
    void testSearchContacts_EmptyResults() throws Exception {
        PaginatedResponse<Contact> paginatedResponse = new PaginatedResponse<>(
                Collections.emptyList(), 0, 20, 0L
        );

        when(contactService.searchContacts(any(ContactSearchRequest.class)))
                .thenReturn(paginatedResponse);

        mockMvc.perform(get("/contacts/search")
                        .param("name", "NonExistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(contactService, times(1)).searchContacts(any(ContactSearchRequest.class));
    }

    @Test
    @DisplayName("Should search contacts with default pagination and sorting")
    void testSearchContacts_DefaultParameters() throws Exception {
        List<Contact> searchResults = Collections.singletonList(testContact);
        PaginatedResponse<Contact> paginatedResponse = new PaginatedResponse<>(
                searchResults, 0, 20, 1L
        );

        when(contactService.searchContacts(any(ContactSearchRequest.class)))
                .thenReturn(paginatedResponse);

        mockMvc.perform(get("/contacts/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(contactService, times(1)).searchContacts(any(ContactSearchRequest.class));
    }

    // ========== Helper Methods ==========

    private Contact createTestContact(UUID id) {
        return createTestContact(id, "John", "Doe", ContactStatus.ACTIVE);
    }

    private Contact createTestContact(UUID id, String firstName, String lastName, ContactStatus status) {
        Contact contact = new Contact();
        contact.setId(id);
        contact.setFirstName(firstName);
        contact.setLastName(lastName);
        contact.setStatus(status);
        contact.setEmails(Arrays.asList("john.doe@example.com", "j.doe@example.com"));
        contact.setPhones(Collections.singletonList("+1234567890"));
        contact.setAddresses(Collections.singletonList("123 Main St, City, State 12345"));
        contact.setTags(Arrays.asList("friend", "work"));
        contact.setCreatedAt(LocalDateTime.now());
        contact.setUpdatedAt(LocalDateTime.now());
        return contact;
    }
}
