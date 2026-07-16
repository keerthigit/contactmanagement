# Implementation Plan: "View" Tab for Contact Management System

## Overview

Add a new **View** tab that lists all contacts stored in the system in a
paginated table. This is a read-only browsing view — no filters, no sort
controls, no row actions. It complements the existing **Search** tab (which
requires the user to enter filter criteria) by giving a zero-input way to
just browse everyone.

## Decisions (confirmed with user)

| Decision | Choice |
|---|---|
| Backend approach | Reuse the existing `GET /contacts/search` endpoint with no filters, rather than building a new endpoint |
| Sorting | Fixed default sort (no user-facing sort controls) — **last name, ascending** |
| Row actions | Read-only table, no click-through to Update/Delete |
| Tab position | First tab in the nav bar, before "Create": **View, Create, Update, Search, Delete, Upload** |
| Page size | Fixed at 20 (matches the Search tab's default) |

## Backend

The `GET /contacts/search` endpoint (`ContactController.java`,
`ContactService.searchContacts()`, `ContactSpecification.java`) already
supports calling with **zero filters** plus `page`, `size`, `sortBy`,
`sortDirection` — in that case `ContactSpecification.buildSpecification()`
returns `criteriaBuilder.conjunction()` (always-true predicate), i.e. all
contacts, paginated and sorted. This was confirmed working via a live
`curl` test in a prior session.

**No new backend code is required.** The View tab's task list is a
verification pass to confirm the existing endpoint behaves correctly for
this "browse everything" use case, since it was originally built and
tested for filtered search, not for the empty-filter case at realistic
data volumes.

### Backend Tasks

- [ ] **B1** — Verify `GET /contacts/search?page=0&size=20&sortBy=lastName&sortDirection=ASC` (no filter params) returns the correct page of results with accurate `totalElements`/`totalPages`/`hasNext`/`hasPrevious`, against a dataset of >20 contacts.
- [ ] **B2** — Confirm `sortBy=lastName` is accepted (it's in the existing allowlist: `firstName`, `lastName`, `createdAt`, `updatedAt` — already satisfied, this is a confirmation, not a change).
- [ ] **B3** — Spot-check performance/behavior with the current dataset size; no index or query changes anticipated given `idx_contacts_last_name` already exists in `schema.sql`.

If B1 uncovers any bug (e.g. incorrect total counts on the unfiltered path), that becomes a new task added here — not anticipated based on the existing implementation.

## Frontend

### API layer — `frontend/src/services/contactApi.js`

No changes needed. `searchContacts(params)` (added for the Search tab) is generic enough to call with just `{ page, size, sortBy: 'lastName', sortDirection: 'ASC' }` and no filter keys.

### New component — `frontend/src/components/ViewContactTab.js`

Follows the same conventions as sibling tabs (functional component, hooks only, `message`/`loading` state pattern), but differs from `SearchContactTab.js` in one key way: **it auto-loads on mount** (via `useEffect`) instead of waiting for an explicit action, since there's nothing to fill in — it's a pure browse view.

- [ ] **F1** — Create `ViewContactTab.js`:
  - State: `page`, `results`, `pageInfo` (`totalElements`/`totalPages`/`hasNext`/`hasPrevious`), `loading`, `message`.
  - `loadPage(targetPage)` — calls `searchContacts({ page: targetPage, size: 20, sortBy: 'lastName', sortDirection: 'ASC' })`, sets results/pageInfo/page; on error sets `message = { type: 'error', text: getErrorMessage(error) }`.
  - `useEffect(() => { loadPage(0); }, [])` — loads page 1 automatically when the tab mounts.
  - Renders `<div className="tab-panel"><h2>View Contacts</h2><p className="tab-description">...</p>` wrapper, consistent with other tabs.
  - Loading state: `<p className="status-text">Loading contacts...</p>`.
  - Empty state (zero contacts in the whole system): `<p className="status-text">No contacts found.</p>`.
  - Results table: reuse the exact same `search-results-table` CSS class and column set introduced for the Search tab (Name, Email, Phone, Status, Tags, Updated) — no new CSS needed.
  - Pagination: reuse the exact same `search-pagination` / `search-pagination-info` CSS classes and Previous/Next button pattern from `SearchContactTab.js`.
- [ ] **F2** — Register the tab in `frontend/src/App.js`:
  - Import `ViewContactTab`.
  - Insert `{ id: 'view', label: 'View' }` as the **first** entry in the `TABS` array.
  - Add `case 'view': return <ViewContactTab />;` to the `renderTab` switch.
  - Change the initial `useState('create')` to `useState('view')` so the app opens on the View tab by default (matches "first tab" placement — confirm this is desired; if not, leave default active tab as `'create'` and just reorder the nav).
- [ ] **F3** — No new CSS. Confirm the reused `search-*` classes render correctly for the View tab's table/pagination (visual check only).

### Explicitly out of scope

- No filters, no sort controls, no row click-through — matches the confirmed decisions above.
- No changes to `Search`, `Update`, `Delete`, or `Upload` tabs.

## Verification

1. Start backend (`contact-service`) and frontend dev server.
2. Seed enough contacts (>20) via the Create tab (or existing data) to exercise pagination.
3. Load the app — confirm it opens on the **View** tab (or navigate to it) and the tab appears first in the nav bar, before Create.
4. Confirm the table populates **automatically** with no user action required, sorted alphabetically by last name.
5. Confirm pagination: Previous disabled on page 1, page indicator shows correct "Page X of Y (Z total)", Next fetches the next page, Next disabled on the last page.
6. Confirm loading state briefly shows on initial mount and on page navigation.
7. Confirm error handling by temporarily stopping the backend and reloading the View tab — an `alert-error` should appear.
8. Regression check: click through Create/Update/Search/Delete/Upload to confirm nothing else broke.
