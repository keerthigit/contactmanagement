# Contact fields (audit, mobile, home phone) implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `createdBy`, `updatedBy`, required `mobile`, optional `homePhone` to contacts; remove `phones`; secure all `/contacts` routes with HTTP Basic; populate audit fields via Spring Data JPA auditing; update search and tests.

**Architecture:** Spring Security 6 filter chain authenticates every request to `/contacts/**` with HTTP Basic and an in-memory user for local dev and tests. Spring Data JPA auditing (`AuditingEntityListener`, `AuditorAware`) fills `createdBy` / `updatedBy` from `SecurityContextHolder`. The `Contact` entity remains the REST JSON shape; audit fields are Jackson read-only on input. Phone search uses scalar `LIKE` on `mobile` and `homePhone`.

**Tech stack:** Java 17, Spring Boot 3.2, Spring Data JPA, PostgreSQL, Spring Security, JUnit 5, MockMvc, `spring-security-test`.

**Spec:** `docs/superpowers/specs/2026-05-12-contact-fields-design.md`

---

## File map

| File | Role |
|------|------|
| `backend/contact-service/pom.xml` | Add `spring-boot-starter-security`. |
| `backend/contact-service/src/main/java/com/contactmanagement/contactservice/config/SecurityConfig.java` | `SecurityFilterChain` (all requests authenticated, HTTP Basic, CSRF disabled for REST), `PasswordEncoder`, `UserDetailsService`. |
| `backend/contact-service/src/main/java/com/contactmanagement/contactservice/config/JpaAuditingConfig.java` | `@EnableJpaAuditing`. |
| `backend/contact-service/src/main/java/com/contactmanagement/contactservice/config/SpringSecurityAuditorAware.java` | `AuditorAware<String>` from current `Authentication`. |
| `backend/contact-service/src/main/java/com/contactmanagement/contactservice/model/Contact.java` | Remove `phones`; add `mobile`, `homePhone`, `createdBy`, `updatedBy`, listeners, validation, Jackson read-only on audit fields. |
| `backend/contact-service/src/main/java/com/contactmanagement/contactservice/service/ContactService.java` | `update(...)` copies `mobile` / `homePhone`; remove `setPhones`. |
| `backend/contact-service/src/main/resources/schema.sql` | Add four columns on `contacts`; drop `contact_phones` table and its index. |
| `backend/contact-service/src/main/java/com/contactmanagement/contactservice/repository/ContactSpecification.java` | Phone predicate: OR of `LIKE` on `mobile` and `homePhone` (no join). |
| `backend/contact-service/src/test/java/com/contactmanagement/contactservice/controller/ContactControllerTest.java` | `@Import(SecurityConfig.class)`, `@WithMockUser(username = "contactapi")` on class, adjust JSON expectations. |
| `backend/contact-service/src/test/java/com/contactmanagement/contactservice/controller/ContactControllerSecurityMvcTest.java` | Small `@WebMvcTest` slice: no `@WithMockUser`; assert `GET /contacts` returns **401** (avoids anonymous vs 403 ambiguity). |
| `backend/contact-service/src/test/java/com/contactmanagement/contactservice/controller/ContactControllerIntegrationTest.java` | HTTP Basic on all `mockMvc` calls; set `mobile` on entities; assertions for audit fields on create/update. |
| `backend/contact-service/src/test/java/com/contactmanagement/contactservice/controller/ContactControllerPersistentTest.java` | Same as integration for auth and `mobile`. |

**Credentials (locked for this plan):** HTTP Basic user `contactapi` / password `contactapi-secret` (document in `application.yml` comments or plan only; tests must match).

---

### Task 1: Add Spring Security dependency

**Files:**
- Modify: `backend/contact-service/pom.xml`

- [ ] **Step 1: Add dependency after `spring-boot-starter-validation`**

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
```

- [ ] **Step 2: Run tests (expect widespread failures until later tasks)**

Run:

```bash
cd /Users/keerthikanth/1_Keerthi/cursor_practice/learing_code_base_project/contactmanagement/backend/contact-service && mvn -q test
```

Expected: many failures (401, validation, removed `phones`).

- [ ] **Step 3: Commit**

```bash
git add backend/contact-service/pom.xml && git commit -m "build(contact-service): add spring-boot-starter-security"
```

---

### Task 2: Auditor and JPA auditing configuration

**Files:**
- Create: `backend/contact-service/src/main/java/com/contactmanagement/contactservice/config/SpringSecurityAuditorAware.java`
- Create: `backend/contact-service/src/main/java/com/contactmanagement/contactservice/config/JpaAuditingConfig.java`

- [ ] **Step 1: Create `SpringSecurityAuditorAware.java`**

```java
package com.contactmanagement.contactservice.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    @NonNull
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName);
    }
}
```

- [ ] **Step 2: Create `JpaAuditingConfig.java`**

```java
package com.contactmanagement.contactservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/contact-service/src/main/java/com/contactmanagement/contactservice/config/SpringSecurityAuditorAware.java backend/contact-service/src/main/java/com/contactmanagement/contactservice/config/JpaAuditingConfig.java && git commit -m "feat(contact-service): enable JPA auditing with security-backed auditor"
```

---

### Task 3: Security filter chain and dev user

**Files:**
- Create: `backend/contact-service/src/main/java/com/contactmanagement/contactservice/config/SecurityConfig.java`

- [ ] **Step 1: Create `SecurityConfig.java`**

```java
package com.contactmanagement.contactservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    public static final String DEV_USERNAME = "contactapi";
    public static final String DEV_PASSWORD = "contactapi-secret";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.builder()
                .username(DEV_USERNAME)
                .password(passwordEncoder.encode(DEV_PASSWORD))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 2: Run tests**

Run: `mvn -q test` from `backend/contact-service`.

Expected: still failing on integration/WebMvc until tests send credentials and entity changes land.

- [ ] **Step 3: Commit**

```bash
git add backend/contact-service/src/main/java/com/contactmanagement/contactservice/config/SecurityConfig.java && git commit -m "feat(contact-service): secure all endpoints with HTTP Basic dev user"
```

---

### Task 4: `Contact` entity — phones out, mobile, home phone, audit

**Files:**
- Modify: `backend/contact-service/src/main/java/com/contactmanagement/contactservice/model/Contact.java`

- [ ] **Step 1: Replace entire file contents with**

```java
package com.contactmanagement.contactservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "contacts")
@EntityListeners(AuditingEntityListener.class)
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "First name is required")
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @ElementCollection
    @CollectionTable(name = "contact_emails", joinColumns = @JoinColumn(name = "contact_id"))
    @Column(name = "email")
    private List<String> emails = new ArrayList<>();

    @NotBlank(message = "Mobile is required")
    @Column(name = "mobile", nullable = false, length = 50)
    private String mobile;

    @Column(name = "home_phone", length = 50)
    private String homePhone;

    @ElementCollection
    @CollectionTable(name = "contact_addresses", joinColumns = @JoinColumn(name = "contact_id"))
    @Column(name = "address")
    private List<String> addresses = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "contact_tags", joinColumns = @JoinColumn(name = "contact_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ContactStatus status;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false, length = 255)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Contact() {
    }

    public Contact(String firstName, String lastName, ContactStatus status, String mobile) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = status;
        this.mobile = mobile;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getHomePhone() {
        return homePhone;
    }

    public void setHomePhone(String homePhone) {
        this.homePhone = homePhone;
    }

    public List<String> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<String> addresses) {
        this.addresses = addresses;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public ContactStatus getStatus() {
        return status;
    }

    public void setStatus(ContactStatus status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/contact-service/src/main/java/com/contactmanagement/contactservice/model/Contact.java && git commit -m "feat(contact-service): replace phones with mobile, homePhone, audit fields"
```

---

### Task 5: `ContactService.update` copies phone scalars

**Files:**
- Modify: `backend/contact-service/src/main/java/com/contactmanagement/contactservice/service/ContactService.java`

- [ ] **Step 1: In `update`, after `setLastName`, add mobile/home and remove phones line**

Replace the body assignments block with:

```java
        contact.setFirstName(contactDetails.getFirstName());
        contact.setLastName(contactDetails.getLastName());
        contact.setMobile(contactDetails.getMobile());
        contact.setHomePhone(contactDetails.getHomePhone());
        contact.setEmails(contactDetails.getEmails());
        contact.setAddresses(contactDetails.getAddresses());
        contact.setTags(contactDetails.getTags());
        contact.setStatus(contactDetails.getStatus());
```

(remove any `contact.setPhones(...)` line.)

- [ ] **Step 2: Commit**

```bash
git add backend/contact-service/src/main/java/com/contactmanagement/contactservice/service/ContactService.java && git commit -m "fix(contact-service): update mobile and home phone on contact update"
```

---

### Task 6: `schema.sql` aligned with entity

**Files:**
- Modify: `backend/contact-service/src/main/resources/schema.sql`

- [ ] **Step 1: Replace `contacts` table definition and remove `contact_phones`**

Use this block for the top of the file (through tags), replacing the old `contacts` create and entire `contact_phones` section and its index:

```sql
CREATE TABLE IF NOT EXISTS contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    mobile VARCHAR(50) NOT NULL,
    home_phone VARCHAR(50),
    status VARCHAR(50) NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Delete the old:

```sql
CREATE TABLE IF NOT EXISTS contact_phones (
    ...
);
```

and delete:

```sql
CREATE INDEX IF NOT EXISTS idx_contact_phones_contact_id ON contact_phones(contact_id);
CREATE INDEX IF NOT EXISTS idx_contact_phones_phone ON contact_phones(phone);
```

Keep other tables (`contact_emails`, `contact_addresses`, `contact_tags`) and remaining indexes except any `contact_phones` references.

- [ ] **Step 2: Commit**

```bash
git add backend/contact-service/src/main/resources/schema.sql && git commit -m "chore(contact-service): schema for mobile, home_phone, audit; drop contact_phones"
```

---

### Task 7: Search specification — phone on scalars

**Files:**
- Modify: `backend/contact-service/src/main/java/com/contactmanagement/contactservice/repository/ContactSpecification.java`

- [ ] **Step 1: Replace the phone filter block (the `if (request.getPhone()...)` section) with**

```java
            // Phone filter: match mobile OR home phone (partial match)
            if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
                String phonePattern = "%" + request.getPhone().trim() + "%";
                Predicate mobilePredicate = criteriaBuilder.like(root.get("mobile"), phonePattern);
                Predicate homePredicate = criteriaBuilder.like(root.get("homePhone"), phonePattern);
                predicates.add(criteriaBuilder.or(mobilePredicate, homePredicate));
            }
```

Remove the old `Join<Contact, String> phoneJoin = root.join("phones", ...)` block for this filter. Do not set `needsDistinct = true` solely for this phone branch (no collection join).

- [ ] **Step 2: Commit**

```bash
git add backend/contact-service/src/main/java/com/contactmanagement/contactservice/repository/ContactSpecification.java && git commit -m "feat(contact-service): search phone against mobile and homePhone"
```

---

### Task 8: `ContactControllerTest` — security, JSON, 401

**Files:**
- Modify: `backend/contact-service/src/test/java/com/contactmanagement/contactservice/controller/ContactControllerTest.java`

- [ ] **Step 1: Add imports**

```java
import com.contactmanagement.contactservice.config.SecurityConfig;
import org.springframework.security.test.context.support.WithMockUser;
```

- [ ] **Step 2: Add `@Import` next to existing `@Import(TestConfig.class)`**

```java
@Import({TestConfig.class, SecurityConfig.class})
```

- [ ] **Step 3: Add class-level mock user (username must match auditing expectation in slice)**

```java
@WithMockUser(username = "contactapi")
```

Place it on the class `ContactControllerTest` (same level as `@WebMvcTest`).

- [ ] **Step 4: Update `createTestContact` helper** — replace `setPhones` with:

```java
        contact.setMobile("+1234567890");
        contact.setHomePhone(null);
```

Remove any `setPhones` / `phones` lines.

- [ ] **Step 5: Update assertions** — replace `jsonPath("$.phones", hasSize(1))` with:

```java
                .andExpect(jsonPath("$.mobile").value("+1234567890"))
```

In `testGetContactById_Success`, replace `jsonPath("$.phones[0]")` with `jsonPath("$.mobile").value("+1234567890")`.

- [ ] **Step 6: `testCreateContact_MinimalFields`** — set `minimalContact.setMobile("+10000000000");` and on `savedContact` set the same mobile; remove `setPhones` calls.

- [ ] **Step 7: Create `ContactControllerSecurityMvcTest.java`** (new file; keeps 401 behavior unambiguous)

```java
package com.contactmanagement.contactservice.controller;

import com.contactmanagement.contactservice.TestConfig;
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
@Import({TestConfig.class, SecurityConfig.class})
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
```

- [ ] **Step 8: Run slice tests**

Run:

```bash
cd /Users/keerthikanth/1_Keerthi/cursor_practice/learing_code_base_project/contactmanagement/backend/contact-service && mvn -q test -Dtest=ContactControllerTest,ContactControllerSecurityMvcTest
```

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add backend/contact-service/src/test/java/com/contactmanagement/contactservice/controller/ContactControllerTest.java backend/contact-service/src/test/java/com/contactmanagement/contactservice/controller/ContactControllerSecurityMvcTest.java && git commit -m "test(contact-service): WebMvcTest security and mobile JSON"
```

---

### Task 9: `ContactControllerIntegrationTest` — Basic auth, mobile, audit assertions

**Files:**
- Modify: `backend/contact-service/src/test/java/com/contactmanagement/contactservice/controller/ContactControllerIntegrationTest.java`

- [ ] **Step 1: Add static import**

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import com.contactmanagement.contactservice.config.SecurityConfig;
```

- [ ] **Step 2: Every `mockMvc.perform(...)`** wrap with `.with(httpBasic(SecurityConfig.DEV_USERNAME, SecurityConfig.DEV_PASSWORD))` immediately after `perform(`:

Example:

```java
        mockMvc.perform(post("/contacts")
                        .with(httpBasic(SecurityConfig.DEV_USERNAME, SecurityConfig.DEV_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testContact)))
```

Apply the same `.with(httpBasic(...))` to `get`, `put`, and `delete` calls in this class.

- [ ] **Step 3: In `setUp`, after `setStatus`, add**

```java
        testContact.setMobile("+1234567890");
        testContact.setHomePhone("+1987654321");
```

Remove `setPhones`.

- [ ] **Step 4: In `testCreateContact_Integration`, extend JSON assertions**

```java
                .andExpect(jsonPath("$.mobile").value("+1234567890"))
                .andExpect(jsonPath("$.homePhone").value("+1987654321"))
                .andExpect(jsonPath("$.createdBy").value(SecurityConfig.DEV_USERNAME))
                .andExpect(jsonPath("$.updatedBy").value(SecurityConfig.DEV_USERNAME))
```

- [ ] **Step 5: For `contact1` / `contact2` in `testGetAllContacts_Integration`, set `setMobile` on each** (distinct values, e.g. `+1111111111` and `+2222222222`) so validation passes when saved through repository (no HTTP on that path).

- [ ] **Step 6: In `testUpdateContact_Integration`, set on `updatedContact`**

```java
        updatedContact.setMobile("+1234567890");
        updatedContact.setHomePhone(null);
```

After successful update, load from repository and assert:

```java
        assertEquals(SecurityConfig.DEV_USERNAME, dbContact.getCreatedBy());
        assertEquals(SecurityConfig.DEV_USERNAME, dbContact.getUpdatedBy());
```

For a stronger update assertion, perform two sequential updates under two different HTTP Basic users only if you add a second test user to `SecurityConfig`; otherwise assert `updatedBy` still equals `contactapi` after one update from same user.

- [ ] **Step 7: Add unauthorized integration test**

```java
    @Test
    @DisplayName("Should return 401 when GET /contacts without credentials")
    void testGetAllContacts_Unauthorized() throws Exception {
        mockMvc.perform(get("/contacts"))
                .andExpect(status().isUnauthorized());
    }
```

- [ ] **Step 8: Run integration tests**

```bash
mvn -q test -Dtest=ContactControllerIntegrationTest
```

Expected: PASS (requires test DB from `application-test.yml`; if PostgreSQL not running, start stack or use embedded H2 only if you change test config — this plan assumes existing project test setup).

- [ ] **Step 9: Commit**

```bash
git add backend/contact-service/src/test/java/com/contactmanagement/contactservice/controller/ContactControllerIntegrationTest.java && git commit -m "test(contact-service): integration tests with Basic auth and audit fields"
```

---

### Task 10: `ContactControllerPersistentTest` — auth and mobile

**Files:**
- Modify: `backend/contact-service/src/test/java/com/contactmanagement/contactservice/controller/ContactControllerPersistentTest.java`

- [ ] **Step 1: Add imports and static import** same pattern as Task 9 for `SecurityConfig` and `httpBasic`.

- [ ] **Step 2: Replace `setPhones` with `setMobile` using the CSV phone column** (`data[4]`):

```java
            testContact.setMobile(data[4]);
            testContact.setHomePhone(null);
```

- [ ] **Step 3: Add `.with(httpBasic(SecurityConfig.DEV_USERNAME, SecurityConfig.DEV_PASSWORD))` to `post` and `get` in this class.**

- [ ] **Step 4: Run**

```bash
mvn -q test -Dtest=ContactControllerPersistentTest
```

Expected: PASS against live DB if configured; otherwise fix environment per project README.

- [ ] **Step 5: Commit**

```bash
git add backend/contact-service/src/test/java/com/contactmanagement/contactservice/controller/ContactControllerPersistentTest.java && git commit -m "test(contact-service): persistent tests use Basic auth and mobile"
```

---

### Task 11: Full suite and manual smoke

- [ ] **Step 1: Run full Maven test**

```bash
cd /Users/keerthikanth/1_Keerthi/cursor_practice/learing_code_base_project/contactmanagement/backend/contact-service && mvn -q test
```

Expected: all tests PASS.

- [ ] **Step 2: Manual curl (optional)**

```bash
curl -i -u contactapi:contactapi-secret -X POST http://localhost:8081/contacts \
  -H "Content-Type: application/json" \
  -d '{"firstName":"A","lastName":"B","status":"ACTIVE","mobile":"555","emails":[],"addresses":[],"tags":[]}'
```

Expected: `201` and JSON includes `"createdBy":"contactapi"`.

- [ ] **Step 3: Commit** (only if you changed anything in Step 2 documentation files; otherwise no commit).

---

## Plan self-review (spec coverage)

| Spec item | Task |
|-----------|------|
| Columns `mobile`, `home_phone`, `created_by`, `updated_by` | Task 4, 6 |
| Drop `phones` / `contact_phones` | Task 4, 6 |
| Audit from principal, not client-writable | Task 2, 4 (`@JsonProperty` READ_ONLY), Task 3 |
| JPA auditing | Task 2, 4 (`EntityListeners`, `@CreatedBy`, `@LastModifiedBy`) |
| All `/contacts` require auth, 401 otherwise | Task 3, 8 (`ContactControllerSecurityMvcTest`), Task 9 (unauthorized GET) |
| `mobile` required validation | Task 4, 8 minimal create |
| Search `phone` on `mobile` OR `homePhone` | Task 7 |
| `sortBy` whitelist unchanged | No code change (do not edit `ContactSearchRequest`) |
| Tests updated | Tasks 8–10 |

**Placeholder scan:** None intentional; credentials are explicit constants in `SecurityConfig`.

**Type consistency:** JSON property `homePhone` matches Java `homePhone`; DB column `home_phone`.

---

**Plan complete and saved to `docs/superpowers/plans/2026-05-12-contact-fields.md`. Two execution options:**

**1. Subagent-Driven (recommended)** — Dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints.

**Which approach?**
