# Post Implementation Plan

## Objective
Implement fixes for all findings in `POST_MERGE_REPORT.md` with priority on security-critical risks, while minimizing disruption and preserving API functionality.

## Guiding Principles
- Fix **Critical** and **High** issues first.
- Deliver in small, testable phases.
- Use secure-by-default configuration for all environments.
- Keep backward compatibility where feasible; document any intentional breaking changes.

## Workstreams and Phases

## Phase 0 - Preparation (0.5 to 1 day)
### Tasks
- Create a dedicated hardening branch from `main`.
- Define environment matrix: `dev`, `test`, `staging`, `prod`.
- Add issue tracker tickets mapped to each finding.
- Freeze new feature work until Phase 2 is complete.

### Deliverables
- Hardening branch and task board.
- Risk acceptance log template for deferred items.

---

## Phase 1 - Access Control Foundation (2 to 4 days) [Critical]
### Goals
- Block anonymous access to contact APIs.
- Introduce role-based authorization.

### Tasks
1. Add Spring Security dependencies to backend services handling API traffic.
2. Create `SecurityConfig` with:
   - Auth required for `/contacts/**`
   - Public-only endpoints for health/readiness if needed.
3. Choose auth model:
   - Preferred: JWT bearer auth at gateway + downstream token propagation.
   - Alternative (interim): Basic auth for non-production only.
4. Define minimal roles:
   - `CONTACT_READ`
   - `CONTACT_WRITE`
   - `CONTACT_ADMIN`
5. Enforce method/route authorization in controller/service layers.
6. Add standardized `401/403` response handling.

### Testing
- Unit tests for security config and denied access.
- Integration tests for allowed/denied role paths.
- Verify anonymous requests fail for all contact endpoints.

### Exit Criteria
- No unauthenticated CRUD access possible.
- Role checks validated in CI tests.

---

## Phase 2 - Secret Management and Exposure Reduction (1 to 2 days) [High]
### Goals
- Remove hardcoded credentials.
- Reduce externally exposed services.

### Tasks
1. Replace plaintext credentials in configs with environment variable references.
2. Add `.env.example` (no real secrets) and update docs.
3. Rotate all known credentials currently in repo history/use.
4. Update `docker-compose.yml`:
   - Remove host port mapping for internal-only services (`postgres`, `contact-service`, `eureka`) unless explicitly required.
   - Keep only necessary public ports (likely gateway).
   - Restrict pgAdmin usage to dev profile only.
5. Introduce compose profiles:
   - `dev` profile with optional admin tooling.
   - `prod-like` profile with minimal surface.

### Testing
- Smoke test startup with env-based secrets.
- Verify direct access to internal ports is blocked by default.

### Exit Criteria
- No committed plaintext secrets in runtime configs.
- Default stack exposes only intended public interfaces.

---

## Phase 3 - Input Binding and Data Integrity Hardening (2 to 3 days) [High]
### Goals
- Prevent mass assignment and domain over-posting.
- Strengthen request validation.

### Tasks
1. Introduce request DTOs:
   - `CreateContactRequest`
   - `UpdateContactRequest`
2. Remove direct entity binding from controller request bodies.
3. Map DTO -> entity explicitly in service layer.
4. Make server-controlled fields immutable from external input:
   - `id`, `createdAt`, `updatedAt`
5. Add validation constraints:
   - Size limits for search fields (`name`, `email`, `phone`, `zip`)
   - Format checks where appropriate.
6. Add centralized validation error response format.

### Testing
- Controller tests for rejection of protected fields.
- Validation tests for oversized/invalid input.

### Exit Criteria
- API no longer accepts sensitive system fields from clients.
- Search endpoints resilient to oversized inputs.

---

## Phase 4 - Logging, Transport Security, and Operational Safety (2 to 4 days) [Medium]
### Goals
- Prevent sensitive data leakage via logs.
- Ensure secure transport and safer schema handling.

### Tasks
1. Logging policy:
   - Disable `show-sql` outside local dev.
   - Reduce `DEBUG` defaults to `INFO` in non-dev.
   - Add sensitive-data masking where logs include payload fragments.
2. Profile-specific schema strategy:
   - `dev`: controlled local behavior only.
   - `staging/prod`: replace `ddl-auto: update` with migration tool (Flyway/Liquibase), ideally `validate`.
3. TLS plan:
   - TLS termination at gateway or ingress.
   - mTLS or trusted network policy for service-to-service where required.
4. Ensure gateway is single external entry point; avoid direct service exposure.

### Testing
- Verify logs do not emit sensitive fields under normal operations.
- Run migration scripts in clean database and upgrade path.
- Confirm HTTPS path in staging.

### Exit Criteria
- No sensitive SQL/debug leakage in non-dev logs.
- No runtime schema mutation in production.
- Secure transport in target deployment topology.

---

## Phase 5 - Code Quality and Reliability Improvements (1 to 2 days)
### Goals
- Improve maintainability and predictable error behavior.

### Tasks
1. Replace generic `RuntimeException` usage with typed exceptions:
   - `ContactNotFoundException`
   - Validation/business exceptions as needed.
2. Add global exception handler with consistent error schema.
3. Improve API contract documentation (OpenAPI/Swagger).
4. Ensure missing Dockerfiles are either:
   - Added and validated, or
   - Compose build strategy is corrected/documented.

### Testing
- Controller/service tests for exception mapping.
- `docker compose up --build` validation in dev profile.

### Exit Criteria
- Deterministic error contracts.
- Deployment path works as documented.

---

## Suggested Timeline
- Week 1: Phases 0-2
- Week 2: Phases 3-4
- Week 3 (partial): Phase 5 + final validation + signoff

## Ownership Matrix (Suggested)
- Security controls & policy: Security engineer
- Auth and API enforcement: Backend lead
- Secrets and deployment profiles: DevOps engineer
- Validation and DTO refactor: Backend developers
- Verification and regression testing: QA engineer

## Verification and Acceptance Checklist
- [ ] All Critical and High findings remediated.
- [ ] Security regression tests added to CI.
- [ ] Secret scanning enabled in CI/pre-commit.
- [ ] No plaintext credentials in committed runtime configs.
- [ ] Unauthorized API access returns `401/403`.
- [ ] Direct internal service access blocked by default deployment profile.
- [ ] Structured error responses documented and tested.
- [ ] Final re-audit report generated and approved.

## Risk Management for Deferred Items
If any Medium/Low items are deferred:
- Document explicit risk acceptance with expiration date.
- Define compensating controls.
- Create follow-up ticket with owner and deadline.

## Final Deliverables
- Security-hardened code changes (to be implemented in subsequent PRs).
- Updated environment/configuration documentation.
- CI checks for security and validation regressions.
- Post-remediation audit report with evidence.
