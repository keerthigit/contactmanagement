# Contact model: audit fields, mobile, home phone

**Date:** 2026-05-12  
**Status:** Approved for implementation planning  
**Service:** `backend/contact-service`

## Summary

Extend each contact with `created_by`, `updated_by`, `mobile` (required), and `home_phone` (optional). Remove the generic `phones` collection and the `contact_phones` table. Populate audit fields from the authenticated principal; clients must not control them via JSON.

## Decisions (locked)

| Topic | Decision |
|--------|----------|
| `created_by` / `updated_by` | Opaque string from authenticated principal (`Authentication#getName()`). Not client-writable. |
| Phone shape | Replace `phones` entirely with scalar `mobile` (required) and `home_phone` (optional). |
| Existing `contact_phones` data | Greenfield / dev acceptable: dropping phone rows and the collection is allowed. |
| Audit implementation | Spring Data JPA auditing (`@CreatedBy`, `@LastModifiedBy`) with `AuditorAware<String>`, unless implementation plan finds a blocker; alternative is explicit service-layer assignment from `SecurityContextHolder`. |
| JSON API | Continue exposing `Contact` as the REST body; add new fields; remove `phones`. Audit fields read-only on input (e.g. Jackson access). |
| Search query `phone` | Keep parameter name `phone`; match substring against `mobile` OR `home_phone` using two `LIKE` predicates (OR), no collection join. |
| Security scope | All `/contacts` endpoints require an authenticated user for v1 of this change (avoids null principal when writing audit fields). |

## Data model and schema

### Columns on `contacts`

- `mobile` — `VARCHAR(50)` NOT NULL (length aligned with prior phone column usage).
- `home_phone` — `VARCHAR(50)` NULL.
- `created_by` — `VARCHAR(255)` NOT NULL.
- `updated_by` — `VARCHAR(255)` NOT NULL.

### Removed

- JPA `@ElementCollection` field `phones` and table `contact_phones`, including related indexes from `schema.sql`.

### Schema maintenance

- Update `backend/contact-service/src/main/resources/schema.sql` to reflect the new shape for fresh databases.
- Existing deployments use `spring.jpa.hibernate.ddl-auto: update`; developers should recreate or migrate dev DBs when dropping `contact_phones` is destructive.

## API and validation

- **Create / update body:** Must include `mobile`; may include `home_phone`; must not include `phones`. If clients send audit field names, they are ignored (read-only deserialization).
- **Validation:** `mobile` required (`@NotBlank`); optional `@Pattern` for phone format is out of scope unless added in implementation.
- **Responses:** Include `createdBy`, `updatedBy`, `mobile`, `homePhone` (per Java bean naming / JSON naming strategy in use).

## Authentication and audit behavior

- Add Spring Security with a mechanism that yields a stable string principal name for development and tests (concrete choice: implementation plan), for example HTTP Basic with an in-memory user or project-standard auth.
- **Create:** Set `createdBy` and `updatedBy` to the current principal.
- **Update:** Preserve `createdBy`; set `updatedBy` to the current principal.
- **Unauthenticated access:** Any request without authentication receives **401** on all `/contacts` routes in this version.

## Search

- `ContactSpecification`: replace join on `phones` with predicates on `root.get("mobile")` and `root.get("homePhone")` combined with OR when `phone` search parameter is present.
- `ContactSearchRequest.sortBy`: initial implementation keeps the existing whitelist (`firstName`, `lastName`, `createdAt`, `updatedAt`). Sorting by `mobile` is a follow-up if needed.

## Testing

- Update unit, integration, and persistent tests that build contacts with `phones`; use `mobile` (and optional `home_phone`).
- Add tests with `@WithMockUser` (or equivalent) to assert:
  - Authenticated create persists `createdBy` / `updatedBy`.
  - Authenticated update changes `updatedBy` and leaves `createdBy` unchanged.
  - Unauthenticated requests to secured endpoints return **401**.
- Missing `mobile` on create/update returns **400** via bean validation.

## Error handling

- Rely on existing global exception handling where present; otherwise Spring Boot defaults for validation (**400**) and security (**401**).

## Out of scope

- Production-grade Flyway/Liquibase migrations (optional follow-up).
- Phone number normalization (E.164) and duplicate detection across contacts.
- OAuth2/JWT specifics beyond what the implementation plan selects for local dev parity.

## Approaches considered

1. **Service-layer audit** — explicit set on save; simple but repetitive.
2. **JPA auditing** — recommended; centralizes audit field population.
3. **DB triggers** — rejected; poor fit for app-level identity and connection pooling.
