# Phase 2 Implementation Plan: Search Endpoints for Contact Management System

## Overview
Implement search functionality with filtering (name, email, phone, zip), pagination, and sorting capabilities for the Contact Management System backend.

---

## Architecture Overview

The search implementation will follow a layered architecture:
1. **Controller Layer**: Handle HTTP requests with query parameters
2. **Service Layer**: Business logic for search operations
3. **Repository Layer**: Database queries with JPA Specifications for dynamic filtering
4. **DTO Layer**: Request/Response objects for search operations

---

## Task Breakdown

### Task 1: Create Search Request DTO
**File to Create**: `backend/contact-service/src/main/java/com/contactmanagement/contactservice/dto/ContactSearchRequest.java`

**Purpose**: 
- Encapsulate search parameters (filters, pagination, sorting)
- Validate input parameters
- Support query parameter binding

**Fields**:
- `name` (String, optional): Search in first name and last name
- `email` (String, optional): Search in email list
- `phone` (String, optional): Search in phone list
- `zip` (String, optional): Search for zip code in addresses
- `page` (Integer, default: 0): Page number (0-indexed)
- `size` (Integer, default: 20): Page size
- `sortBy` (String, default: "createdAt"): Field to sort by
- `sortDirection` (String, default: "DESC"): Sort direction (ASC/DESC)

**Why**: Provides a clean, validated way to receive search parameters from the API.

---

### Task 2: Create Paginated Response DTO
**File to Create**: `backend/contact-service/src/main/java/com/contactmanagement/contactservice/dto/PaginatedResponse.java`

**Purpose**:
- Standardized pagination response structure
- Reusable across different search endpoints

**Fields**:
- `content` (List<T>): List of results
- `page` (Integer): Current page number
- `size` (Integer): Page size
- `totalElements` (Long): Total number of elements
- `totalPages` (Integer): Total number of pages
- `hasNext` (Boolean): Whether there's a next page
- `hasPrevious` (Boolean): Whether there's a previous page

**Why**: Provides consistent pagination metadata to clients.

---

### Task 3: Create JPA Specification for Dynamic Filtering
**File to Create**: `backend/contact-service/src/main/java/com/contactmanagement/contactservice/repository/ContactSpecification.java`

**Purpose**:
- Build dynamic JPA queries based on search criteria
- Handle complex filtering logic (name, email, phone, zip)
- Support case-insensitive partial matching

**Methods**:
- `hasName(String name)`: Search in firstName and lastName (case-insensitive, partial match)
- `hasEmail(String email)`: Search in emails collection (case-insensitive, partial match)
- `hasPhone(String phone)`: Search in phones collection (partial match)
- `hasZip(String zip)`: Search for zip code pattern in addresses (regex or contains)
- `buildSpecification(ContactSearchRequest request)`: Combine all filters

**Why**: JPA Specifications provide type-safe, dynamic query building without writing native SQL.

---

### Task 4: Extend ContactRepository with Specification Support
**File to Modify**: `backend/contact-service/src/main/java/com/contactmanagement/contactservice/repository/ContactRepository.java`

**Changes**:
- Extend `JpaSpecificationExecutor<Contact>` in addition to `JpaRepository`
- This enables `findAll(Specification, Pageable)` method

**Why**: Enables pagination and sorting with dynamic specifications.

---

### Task 5: Add Search Method to ContactService
**File to Modify**: `backend/contact-service/src/main/java/com/contactmanagement/contactservice/service/ContactService.java`

**New Method**:
- `searchContacts(ContactSearchRequest request)`: Returns `PaginatedResponse<Contact>`
  - Builds specification from request
  - Creates Pageable with sorting
  - Executes repository query
  - Converts Page to PaginatedResponse

**Why**: Encapsulates search business logic and provides clean interface to controller.

---

### Task 6: Add Search Endpoint to ContactController
**File to Modify**: `backend/contact-service/src/main/java/com/contactmanagement/contactservice/controller/ContactController.java`

**New Endpoint**:
- `GET /contacts/search`: Search contacts with filters, pagination, and sorting
  - Accepts `ContactSearchRequest` as query parameters
  - Returns `PaginatedResponse<Contact>`
  - Handles validation errors

**Modification**:
- Optionally deprecate or keep existing `GET /contacts` endpoint (for backward compatibility)

**Why**: Exposes search functionality via REST API.

---

### Task 7: Add Database Indexes for Performance
**File to Modify**: `backend/contact-service/src/main/resources/schema.sql`

**New Indexes**:
- `idx_contacts_first_name`: Index on first_name for name searches
- `idx_contacts_last_name`: Index on last_name for name searches
- `idx_contact_emails_email`: Index on email column for email searches
- `idx_contact_phones_phone`: Index on phone column for phone searches
- `idx_contact_addresses_address`: Index on address column (for zip searches)

**Why**: Improves query performance, especially for large datasets.

---

### Task 8: Add Validation and Error Handling
**Files to Consider**:
- `backend/contact-service/src/main/java/com/contactmanagement/contactservice/dto/ContactSearchRequest.java` (add validation annotations)
- `backend/contact-service/src/main/java/com/contactmanagement/contactservice/controller/ContactController.java` (add exception handling)

**Validations**:
- Page number >= 0
- Page size between 1 and 100
- Sort direction is ASC or DESC
- Sort field is valid (firstName, lastName, createdAt, updatedAt)

**Why**: Ensures data integrity and provides meaningful error messages.

---

## Implementation Details

### Search Logic

1. **Name Search**: 
   - Searches both `firstName` and `lastName` fields
   - Case-insensitive partial match (LIKE '%name%')
   - OR condition between firstName and lastName

2. **Email Search**:
   - Searches in `emails` collection table
   - Case-insensitive partial match
   - Uses JOIN with `contact_emails` table

3. **Phone Search**:
   - Searches in `phones` collection table
   - Partial match (removes formatting for better matching)
   - Uses JOIN with `contact_phones` table

4. **Zip Code Search**:
   - Searches in `addresses` collection table
   - Uses regex pattern to find zip codes (5-digit or 5+4 format)
   - Case-insensitive search
   - Uses JOIN with `contact_addresses` table

5. **Combined Filters**:
   - All filters are combined with AND logic
   - If multiple filters are provided, all must match

6. **Pagination**:
   - Default page: 0
   - Default size: 20
   - Maximum size: 100 (configurable)

7. **Sorting**:
   - Default: Sort by `createdAt` descending
   - Supported fields: `firstName`, `lastName`, `createdAt`, `updatedAt`
   - Default direction: DESC

---

## API Endpoint Specification

### GET /contacts/search

**Query Parameters**:
- `name` (optional, String): Search term for name
- `email` (optional, String): Search term for email
- `phone` (optional, String): Search term for phone
- `zip` (optional, String): Zip code to search
- `page` (optional, Integer, default: 0): Page number
- `size` (optional, Integer, default: 20): Page size
- `sortBy` (optional, String, default: "createdAt"): Field to sort by
- `sortDirection` (optional, String, default: "DESC"): Sort direction (ASC/DESC)

**Response**:
```json
{
  "content": [
    {
      "id": "uuid",
      "firstName": "John",
      "lastName": "Doe",
      "emails": ["john@example.com"],
      "phones": ["123-456-7890"],
      "addresses": ["123 Main St, City, State 12345"],
      "tags": ["friend"],
      "status": "ACTIVE",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-01-01T00:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "hasNext": true,
  "hasPrevious": false
}
```

**Example Requests**:
- `GET /contacts/search?name=John&page=0&size=10`
- `GET /contacts/search?email=example.com&sortBy=firstName&sortDirection=ASC`
- `GET /contacts/search?phone=123&zip=12345&page=1`

---

## Testing Considerations

### Unit Tests
- Test `ContactSpecification` with various filter combinations
- Test `ContactService.searchContacts()` with different requests
- Test pagination and sorting logic

### Integration Tests
- Test search endpoint with various query parameters
- Test edge cases (empty results, invalid parameters)
- Test performance with large datasets

---

## Dependencies

No new dependencies required. The implementation uses:
- Spring Data JPA (already in pom.xml)
- Jakarta Validation (already in pom.xml)
- Spring Boot Web (already in pom.xml)

---

## File Summary

### Files to Create:
1. `backend/contact-service/src/main/java/com/contactmanagement/contactservice/dto/ContactSearchRequest.java`
2. `backend/contact-service/src/main/java/com/contactmanagement/contactservice/dto/PaginatedResponse.java`
3. `backend/contact-service/src/main/java/com/contactmanagement/contactservice/repository/ContactSpecification.java`

### Files to Modify:
1. `backend/contact-service/src/main/java/com/contactmanagement/contactservice/repository/ContactRepository.java`
2. `backend/contact-service/src/main/java/com/contactmanagement/contactservice/service/ContactService.java`
3. `backend/contact-service/src/main/java/com/contactmanagement/contactservice/controller/ContactController.java`
4. `backend/contact-service/src/main/resources/schema.sql`

---

## Implementation Order

1. Create DTOs (Tasks 1-2)
2. Create Specification class (Task 3)
3. Extend Repository (Task 4)
4. Add Service method (Task 5)
5. Add Controller endpoint (Task 6)
6. Add database indexes (Task 7)
7. Add validation (Task 8)

---

## Notes

- The zip code search will use pattern matching in address strings. For production, consider extracting zip codes into a separate field if more precise matching is needed.
- The current implementation assumes addresses are stored as free-form text. Zip code extraction uses regex patterns.
- All string searches are case-insensitive for better user experience.
- Pagination defaults can be configured via application properties if needed.
