# Security Audit Report

**Repository:** Contact Management System (React frontend, Spring Boot microservices, PostgreSQL)  
**Audit type:** Static review of source, configuration, and orchestration in this workspace (no code changes, no dynamic penetration testing).  
**Scope:** All tracked paths under the repository root, including `backend/`, `frontend/`, `docs/`, and `docker-compose.yml`.

---

## Findings

### 1. No authentication or authorization on the contact API

| Field | Detail |
|--------|--------|
| **File(s) / line(s)** | `backend/contact-service/src/main/java/com/contactmanagement/contactservice/controller/ContactController.java` (lines 16-71); `backend/contact-service/pom.xml` (lines 20-51, dependency list contains no security starter) |
| **Name / type** | Missing access control / broken authentication (OWASP A01:2021 Broken Access Control) |
| **Severity** | **Critical** |
| **Description** | All REST endpoints under `/contacts` are publicly reachable. There is no Spring Security (or other) filter chain, JWT, API keys, or mutual TLS. Any client who can reach the service can create, read, update, delete, and search contacts. |
| **Potential attack scenario** | An anonymous user on the same network (or the public internet if the port is exposed) calls `GET /contacts` to exfiltrate the full contact directory (names, emails, phones, addresses). They then `DELETE /contacts/{id}` to wipe records or `POST /contacts` to inject fraudulent data. |

---

### 2. Insecure direct object references (IDOR) on all CRUD operations

| Field | Detail |
|--------|--------|
| **File(s) / line(s)** | `backend/contact-service/src/main/java/com/contactmanagement/contactservice/controller/ContactController.java` (lines 39-44, 46-53, 56-63) |
| **Name / type** | Insecure direct object reference (IDOR) / missing authorization |
| **Severity** | **Critical** |
| **Description** | Operations use only a path `UUID` with no notion of tenant, role, or resource owner. There is no check that the caller is allowed to act on that identifier. |
| **Potential attack scenario** | After observing one legitimate `id` (e.g., from a shared link, log, or prior response), an attacker iterates or targets `PUT /contacts/{id}` and `DELETE /contacts/{id}` against other UUIDs to modify or remove arbitrary contacts. |

---

### 3. Hardcoded database credentials in application configuration

| Field | Detail |
|--------|--------|
| **File(s) / line(s)** | `backend/contact-service/src/main/resources/application.yml` (lines 8-10) |
| **Name / type** | Hardcoded secrets / credential exposure |
| **Severity** | **High** |
| **Description** | PostgreSQL username and password are stored in plaintext in a file that is intended for version control. Anyone with repo access gains database credentials; scanners and leaked clones expose them. |
| **Potential attack scenario** | A compromised developer laptop or a public fork of the repository exposes `contactuser` / `contactpass`. An attacker connects directly to PostgreSQL (especially if the port is published, see finding 5) and reads or tampers with all contact tables, bypassing application logic. |

---

### 4. Hardcoded credentials and overly exposed services in Docker Compose

| Field | Detail |
|--------|--------|
| **File(s) / line(s)** | `docker-compose.yml` (lines 7-10, 21-23, 49-54); published ports 11-12, 25-26, 38-39, 49-50, 66-67 |
| **Name / type** | Weak default credentials; excessive network exposure; defense-in-depth failure |
| **Severity** | **High** |
| **Description** | The stack defines known weak passwords for PostgreSQL and pgAdmin, publishes PostgreSQL (`5432`), pgAdmin (`5050`), Eureka (`8761`), contact-service (`8081`), and the gateway (`8080`) to the host. This pattern is common in tutorials but is unsafe on shared or internet-facing hosts. |
| **Potential attack scenario** | An attacker on the LAN runs a PostgreSQL client to `host:5432` with `contactuser`/`contactpass` from the compose file, dumps the database, or holds data for ransom. Alternatively they sign into pgAdmin at port `5050` using `admin@admin.com` / `admin` and manage the server with a full UI. |

---

### 5. Mass assignment via binding the JPA entity to the HTTP body

| Field | Detail |
|--------|--------|
| **File(s) / line(s)** | `backend/contact-service/src/main/java/com/contactmanagement/contactservice/controller/ContactController.java` (lines 27-28, 46-47); `backend/contact-service/src/main/java/com/contactmanagement/contactservice/model/Contact.java` (lines 18-21, 56-62, 73-80, 138-152) |
| **Name / type** | Mass assignment / unsafe deserialization into domain model |
| **Severity** | **High** |
| **Description** | Create and update endpoints accept `@RequestBody Contact`. The `Contact` entity exposes setters for `id`, `createdAt`, and `updatedAt` in addition to business fields. Clients can supply those fields in JSON. That can interfere with identifier integrity, audit timestamps, and (depending on persistence semantics) merge or overwrite behavior for existing rows. |
| **Potential attack scenario** | An attacker sends a `POST` or `PUT` body that includes a victim’s `id` and replaces phone/email fields with attacker-controlled values, or sets `createdAt` to falsify record history. Supplying an existing `id` on create may lead to updates or conflicts depending on JPA `save()` / `merge()` behavior, enabling unauthorized modification paths. |

---

### 6. Sensitive information disclosure through SQL and application logging

| Field | Detail |
|--------|--------|
| **File(s) / line(s)** | `backend/contact-service/src/main/resources/application.yml` (lines 14-18, 26-28) |
| **Name / type** | Information disclosure (verbose logging / SQL in logs) |
| **Severity** | **Medium** |
| **Description** | `show-sql: true`, `format_sql: true`, package `DEBUG`, and `org.springframework.web` `INFO` cause SQL and detailed application behavior to be logged. In production, logs often aggregate to third parties and are broadly readable, increasing exposure of PII appearing in queries and parameters. |
| **Potential attack scenario** | A support engineer exports logs for an unrelated issue; exported files contain live contact data from bound parameters or result patterns. An attacker with read access to log storage (SIEM misconfiguration, stolen credentials) harvests emails and phone numbers without hitting the API. |

---

### 7. Hibernate `ddl-auto: update` in default configuration

| Field | Detail |
|--------|--------|
| **File(s) / line(s)** | `backend/contact-service/src/main/resources/application.yml` (lines 13-14); `backend/contact-service/src/test/resources/application-test.yml` (lines 8-9) |
| **Name / type** | Unsafe deployment configuration / schema management |
| **Severity** | **Medium** |
| **Description** | Automatic DDL updates allow the application to alter schema at runtime based on entities. In production this can cause unintended migrations, downtime, or destructive changes; it also encourages running untested schema states. |
| **Potential attack scenario** | A compromised application process or a malicious insider deploys a modified entity mapping; Hibernate applies schema changes that drop columns or weaken constraints, leading to data loss or integrity bugs that enable further abuse. |

---

### 8. Dual exposure of contact-service and API gateway

| Field | Detail |
|--------|--------|
| **File(s) / line(s)** | `docker-compose.yml` (lines 49-50, 66-67); `backend/api-gateway/src/main/resources/application.yml` (lines 9-15) |
| **Name / type** | Security control bypass / inconsistent enforcement surface |
| **Severity** | **Medium** |
| **Description** | The contact microservice is published on `8081` while the gateway listens on `8080`. Any future security control implemented only on the gateway (rate limits, WAF rules, authentication) can be bypassed by calling the service directly on `8081`. |
| **Potential attack scenario** | After the team adds OAuth to the gateway only, an attacker discovers `8081` via port scan and continues to use unauthenticated CRUD against the backend, nullifying the control. |

---

### 9. No transport encryption or TLS configuration in service configs

| Field | Detail |
|--------|--------|
| **File(s) / line(s)** | `backend/contact-service/src/main/resources/application.yml` (lines 1-2); `backend/api-gateway/src/main/resources/application.yml` (lines 1-2); `backend/eureka-server/src/main/resources/application.yml` (lines 1-2) |
| **Name / type** | Cleartext transport / missing TLS |
| **Severity** | **Medium** |
| **Description** | Configurations only define plain HTTP ports. Service-to-service and client-to-service traffic would be unencrypted on the wire unless terminated entirely outside these files (not defined here). |
| **Potential attack scenario** | On a shared network, an attacker performs passive sniffing or active MITM between the gateway and contact-service or between Eureka and registrants, capturing PII in JSON payloads or stealing metadata useful for targeting admin interfaces. |

---

### 10. Unbounded search input size (denial-of-service and log amplification)

| Field | Detail |
|--------|--------|
| **File(s) / line(s)** | `backend/contact-service/src/main/java/com/contactmanagement/contactservice/dto/ContactSearchRequest.java` (lines 9-12) |
| **Name / type** | Input validation gap / resource exhaustion |
| **Severity** | **Low** |
| **Description** | Query parameters `name`, `email`, `phone`, and `zip` have no `@Size` or length limits. Extremely large strings can increase query cost, memory use, and log volume when combined with verbose SQL logging. |
| **Potential attack scenario** | A script sends megabyte-sized `name` values to `/contacts/search` repeatedly, stressing the database and application thread pool, degrading availability for legitimate users. |

---

### 11. Eureka server self-preservation disabled

| Field | Detail |
|--------|--------|
| **File(s) / line(s)** | `backend/eureka-server/src/main/resources/application.yml` (lines 12-14) |
| **Name / type** | Availability / resilience misconfiguration |
| **Severity** | **Low** |
| **Description** | `enable-self-preservation: false` allows Eureka to evict instances aggressively during partial network partitions or heartbeat blips. This is mainly an availability concern but can contribute to traffic being routed to bad instances or unexpected client retry storms. |
| **Potential attack scenario** | During a partial outage, clients receive stale or empty registry data and retry aggressively, amplifying load and indirectly aiding DoS conditions; security appliances may see abnormal traffic patterns that mask real attacks. |

---

### 12. Operational documentation in tests reinforces default database usernames

| Field | Detail |
|--------|--------|
| **File(s) / line(s)** | `backend/contact-service/src/test/java/com/contactmanagement/contactservice/controller/ContactControllerPersistentTest.java` (lines 30-32, 119-120, 137-138) |
| **Name / type** | Information disclosure (credential workflow hints) |
| **Severity** | **Low** |
| **Description** | Comments and `System.out` messages print exact `docker exec` and `psql` commands using the default `contactuser` database user, lowering the bar for opportunistic misuse when combined with weak passwords. |
| **Potential attack scenario** | A junior developer shares CI logs or terminal output in a public gist; attackers combine the documented username with the well-known password from `docker-compose.yml` to access a developer’s local database left exposed. |

---

### 13. Incomplete microservice modules (gateway and Eureka application code absent)

| Field | Detail |
|--------|--------|
| **File(s) / line(s)** | `backend/api-gateway/` (only `pom.xml`, `src/main/resources/application.yml`); `backend/eureka-server/` (same pattern) |
| **Name / type** | Supply chain / build and deployment integrity risk |
| **Severity** | **Low** |
| **Description** | There are no `@SpringBootApplication` Java entrypoints under `api-gateway` or `eureka-server` in this tree. `docker-compose.yml` expects images built from those directories. Incomplete sources increase the chance of ad-hoc or unaudited build steps elsewhere. |
| **Potential attack scenario** | A future maintainer adds opaque JARs or copy-pasted mains without review; security controls and dependency versions diverge from what is audited in Git, introducing unpatched libraries or backdoored artifacts. |

---

### 14. Frontend dependency surface (transitive risk; audit not executed here)

| Field | Detail |
|--------|--------|
| **File(s) / line(s)** | `frontend/package.json` (lines 6-11) |
| **Name / type** | Third-party dependency risk (`react-scripts`, `axios`, etc.) |
| **Severity** | **Low** |
| **Description** | The frontend relies on `react-scripts` 5.0.1 and other packages with deep transitive trees. No `npm audit` was run in this engagement (Node.js was not available in the audit environment). Historical advisories often target dev/build tooling. |
| **Potential attack scenario** | A developer runs `npm install` on a compromised network or installs a package with a known RCE in a postinstall script, compromising the workstation and stealing keys or inserting malicious code into builds. |

---

## Summary Table

| ID | File(s) (primary) | Line(s) | Vulnerability | Type | Severity |
|----|-------------------|---------|---------------|------|----------|
| 1 | `backend/contact-service/.../ContactController.java` | 16-71 | Missing authentication | Broken access control | Critical |
| 2 | `backend/contact-service/.../ContactController.java` | 39-63 | IDOR on CRUD | Broken access control | Critical |
| 3 | `backend/contact-service/src/main/resources/application.yml` | 8-10 | Hardcoded DB credentials | Secret management | High |
| 4 | `docker-compose.yml` | 7-26, 49-54, ports | Weak creds; exposed DB/admin/Eureka/services | Config / exposure | High |
| 5 | `ContactController.java` / `Contact.java` | 27-28, 46-47 / 18-21, 73-152 | Mass assignment on entity | Input handling | High |
| 6 | `application.yml` | 14-18, 26-28 | Verbose SQL and DEBUG logs | Information disclosure | Medium |
| 7 | `application.yml` / `application-test.yml` | 13-14 / 8-9 | `ddl-auto: update` | Unsafe deployment | Medium |
| 8 | `docker-compose.yml` / `api-gateway/.../application.yml` | 49-67 / 9-15 | Gateway bypass via direct service port | Architecture | Medium |
| 9 | `application.yml` (all services) | server.port blocks | No TLS in config | Transport security | Medium |
| 10 | `ContactSearchRequest.java` | 9-12 | Unbounded search fields | DoS / validation | Low |
| 11 | `eureka-server/.../application.yml` | 12-14 | Self-preservation disabled | Availability | Low |
| 12 | `ContactControllerPersistentTest.java` | 30-32, 119-120, 137-138 | DB user hints in output | Information disclosure | Low |
| 13 | `backend/api-gateway/`, `backend/eureka-server/` | N/A (missing mains) | Incomplete service sources | Process / integrity | Low |
| 14 | `frontend/package.json` | 6-11 | Unaudited npm tree | Supply chain | Low |

---

## Count by Severity

| Severity | Count |
|----------|-------|
| Critical | 2 |
| High | 3 |
| Medium | 4 |
| Low | 5 |
| **Total** | **14** |

---

## Overall Repository Risk Rating

**Critical**

The API that stores and serves personally identifiable information (names, emails, phones, addresses) has **no authentication or authorization**. Combined with **hardcoded database and admin credentials** and a **Docker Compose profile that publishes database and management UIs** with weak passwords, a realistic attacker with network reach can **read, modify, or destroy all contact data** with little effort. Medium-rated issues (logging, TLS, `ddl-auto`, gateway bypass) further widen blast radius or complicate safe operations. Address access control and secret management first; then reduce exposure, tighten logging, and enforce TLS and migration discipline before any production use.

---

*End of report.*
