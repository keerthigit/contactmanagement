# Post-Merge Review Report

## Scope Reviewed
- Branch: `main`
- Commit reviewed: `df64044c6ecfa35f8d43826731e1bf585259e083` (`first commit`)
- Note: this is the repository's first commit on `main`, so all current code is treated as introduced by this merge.

## Security Vulnerabilities Introduced

### 1) Missing authentication/authorization on contact APIs (Critical)
- **File:** `backend/contact-service/src/main/java/com/contactmanagement/contactservice/controller/ContactController.java` (lines 16-71)
- **Issue:** All CRUD/search endpoints are public; no authn/authz checks are present.
- **Risk:** Any caller with network access can read, create, update, and delete contacts (PII exposure + integrity loss).

### 2) Hardcoded credentials in source-controlled config (High)
- **Files:**
  - `backend/contact-service/src/main/resources/application.yml` (lines 9-10)
  - `docker-compose.yml` (lines 10, 22-23, 53-54)
- **Issue:** Plaintext DB and pgAdmin credentials (`contactpass`, `admin`) are committed.
- **Risk:** Credential leakage enables direct DB/admin access and bypass of app-layer controls.

### 3) Excessive network exposure in compose stack (High)
- **File:** `docker-compose.yml` (lines 11-12, 25-27, 38-40, 49-50, 66-67)
- **Issue:** PostgreSQL, pgAdmin, Eureka, contact service, and gateway are all published to host.
- **Risk:** Expands attack surface; weak/default credentials become remotely exploitable in shared networks.

### 4) Mass assignment via entity binding (High)
- **Files:**
  - `backend/contact-service/src/main/java/com/contactmanagement/contactservice/controller/ContactController.java` (lines 28, 47)
  - `backend/contact-service/src/main/java/com/contactmanagement/contactservice/model/Contact.java` (lines 78-80, 142-152)
- **Issue:** `@RequestBody Contact` binds directly to JPA entity that exposes setters for `id`, `createdAt`, `updatedAt`.
- **Risk:** Client-controlled protected fields can lead to record tampering/audit manipulation.

### 5) PII leakage via verbose SQL/debug logging (Medium)
- **File:** `backend/contact-service/src/main/resources/application.yml` (lines 14-15, 26-29)
- **Issue:** `ddl-auto: update`, `show-sql: true`, and debug-level app logging in default config.
- **Risk:** Query/data details may leak into logs and increase post-compromise blast radius.

## Breaking Changes

### 1) No backward-compatibility baseline exists (Informational)
- **Context:** This is the first commit on `main`; there is no prior API/schema version to diff for traditional breaking changes.

### 2) Compose deployment is non-runnable as committed (High)
- **Files:**
  - `docker-compose.yml` (lines 35-37, 46-47, 63-64)
  - Missing: `backend/eureka-server/Dockerfile`, `backend/contact-service/Dockerfile`, `backend/api-gateway/Dockerfile`
- **Issue:** Compose expects image builds from service directories, but no Dockerfiles are present.
- **Impact:** `docker compose up --build` cannot build expected services, breaking documented deployment path.

## Code Quality Issues

### 1) Generic exception flow in service/controller (Medium)
- **Files:**
  - `backend/contact-service/src/main/java/com/contactmanagement/contactservice/service/ContactService.java` (lines 46, 61)
  - `backend/contact-service/src/main/java/com/contactmanagement/contactservice/controller/ContactController.java` (lines 51-53, 61-63)
- **Issue:** Uses `RuntimeException` for business conditions and catches broadly in controller.
- **Impact:** Poor error semantics, harder observability, and fragile behavior as codebase grows.

### 2) Entity used directly as API contract (Medium)
- **Files:**
  - `backend/contact-service/src/main/java/com/contactmanagement/contactservice/controller/ContactController.java` (lines 28, 47)
  - `backend/contact-service/src/main/java/com/contactmanagement/contactservice/model/Contact.java`
- **Issue:** No request/response DTO boundary for create/update paths.
- **Impact:** Tight coupling between persistence and API, raises accidental exposure and change-risk.

### 3) Incomplete microservice implementation footprint (Medium)
- **Files:** `backend/api-gateway/`, `backend/eureka-server/`
- **Issue:** Only `pom.xml` + `application.yml` are present; no Java application classes in these modules.
- **Impact:** Reduces maintainability clarity and increases operational confusion (what actually runs where).

## Risks From This Merge

1. **Data confidentiality risk is high** due to unauthenticated PII endpoints plus exposed DB/admin surfaces.
2. **Data integrity risk is high** because arbitrary clients can modify/delete contact records.
3. **Operational risk is medium-high** since compose-based deployment path is incomplete (missing Dockerfiles).
4. **Compliance/privacy risk is high** from potential PII leakage in logs and unrestricted API access.
5. **Future change risk is medium** due to entity/API coupling and generic exception handling.

## Overall Assessment
- **Overall merge risk:** **Critical**
- **Reason:** This merge introduces production-blocking security gaps (missing access control, hardcoded secrets, broad exposure) and an incomplete deployment path.

## Validation Performed
- Reviewed latest commit metadata and changed file set via Git.
- Performed targeted static review across backend, frontend, and infrastructure config.
- Verified Maven packaging for `api-gateway` and `eureka-server` modules succeeds locally; runtime/deployment concerns remain due to missing Dockerfiles referenced by compose.
