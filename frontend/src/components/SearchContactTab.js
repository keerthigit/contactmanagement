import React, { useState } from 'react';
import { getErrorMessage, searchContacts } from '../services/contactApi';
import { joinList } from '../utils/contactHelpers';

const EMPTY_FILTERS = { name: '', email: '', phone: '', zip: '' };
const SIZE = 20;

function SearchContactTab() {
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDirection, setSortDirection] = useState('DESC');
  const [page, setPage] = useState(0);
  const [results, setResults] = useState([]);
  const [pageInfo, setPageInfo] = useState({
    totalElements: 0,
    totalPages: 0,
    hasNext: false,
    hasPrevious: false,
  });
  const [loadingSearch, setLoadingSearch] = useState(false);
  const [message, setMessage] = useState(null);
  const [hasSearched, setHasSearched] = useState(false);

  const runSearch = async (targetPage) => {
    setLoadingSearch(true);
    setMessage(null);

    const params = {
      ...(filters.name.trim() && { name: filters.name.trim() }),
      ...(filters.email.trim() && { email: filters.email.trim() }),
      ...(filters.phone.trim() && { phone: filters.phone.trim() }),
      ...(filters.zip.trim() && { zip: filters.zip.trim() }),
      page: targetPage,
      size: SIZE,
      sortBy,
      sortDirection,
    };

    try {
      const data = await searchContacts(params);
      setResults(data.content);
      setPageInfo({
        totalElements: data.totalElements,
        totalPages: data.totalPages,
        hasNext: data.hasNext,
        hasPrevious: data.hasPrevious,
      });
      setPage(data.page);
    } catch (error) {
      setMessage({ type: 'error', text: getErrorMessage(error) });
      setResults([]);
    } finally {
      setHasSearched(true);
      setLoadingSearch(false);
    }
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    runSearch(0);
  };

  const handleReset = () => {
    setFilters(EMPTY_FILTERS);
    setSortBy('createdAt');
    setSortDirection('DESC');
    setPage(0);
    setResults([]);
    setPageInfo({ totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false });
    setMessage(null);
    setHasSearched(false);
  };

  const showNoResults = hasSearched && !loadingSearch && results.length === 0 && !message;

  return (
    <div className="tab-panel">
      <h2>Search Contacts</h2>
      <p className="tab-description">
        Filter contacts by name, email, phone, or zip code, then sort and page through the results.
      </p>

      {message && <div className={`alert alert-${message.type}`}>{message.text}</div>}

      <form className="contact-form" onSubmit={handleSubmit}>
        <div className="form-row">
          <label htmlFor="search-name">
            Name
            <input
              id="search-name"
              type="text"
              value={filters.name}
              onChange={(e) => setFilters((f) => ({ ...f, name: e.target.value }))}
              disabled={loadingSearch}
              placeholder="First or last name"
            />
          </label>
          <label htmlFor="search-email">
            Email
            <input
              id="search-email"
              type="text"
              value={filters.email}
              onChange={(e) => setFilters((f) => ({ ...f, email: e.target.value }))}
              disabled={loadingSearch}
              placeholder="Email address"
            />
          </label>
        </div>

        <div className="form-row">
          <label htmlFor="search-phone">
            Phone
            <input
              id="search-phone"
              type="text"
              value={filters.phone}
              onChange={(e) => setFilters((f) => ({ ...f, phone: e.target.value }))}
              disabled={loadingSearch}
              placeholder="Mobile or home phone"
            />
          </label>
          <label htmlFor="search-zip">
            Zip
            <input
              id="search-zip"
              type="text"
              value={filters.zip}
              onChange={(e) => setFilters((f) => ({ ...f, zip: e.target.value }))}
              disabled={loadingSearch}
              placeholder="Postal / zip code"
            />
          </label>
        </div>

        <div className="form-row">
          <label htmlFor="search-sort-by">
            Sort by
            <select
              id="search-sort-by"
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              disabled={loadingSearch}
            >
              <option value="createdAt">Created date</option>
              <option value="updatedAt">Updated date</option>
              <option value="firstName">First name</option>
              <option value="lastName">Last name</option>
            </select>
          </label>
          <label htmlFor="search-sort-dir">
            Direction
            <select
              id="search-sort-dir"
              value={sortDirection}
              onChange={(e) => setSortDirection(e.target.value)}
              disabled={loadingSearch}
            >
              <option value="DESC">Descending</option>
              <option value="ASC">Ascending</option>
            </select>
          </label>
        </div>

        <div className="search-actions">
          <button type="submit" className="btn btn-primary" disabled={loadingSearch}>
            {loadingSearch ? 'Searching...' : 'Search'}
          </button>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={handleReset}
            disabled={loadingSearch}
          >
            Reset
          </button>
        </div>
      </form>

      {showNoResults && <p className="status-text">No contacts matched your search.</p>}

      {results.length > 0 && (
        <div className="search-results-section">
          <table className="search-results-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Status</th>
                <th>Tags</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {results.map((contact) => (
                <tr key={contact.id}>
                  <td>
                    {contact.firstName} {contact.lastName}
                  </td>
                  <td>{contact.email}</td>
                  <td>{contact.mobile || contact.homePhone || '—'}</td>
                  <td>{contact.status}</td>
                  <td>{joinList(contact.tags) || '—'}</td>
                  <td>{contact.updatedAt ? new Date(contact.updatedAt).toLocaleString() : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {hasSearched && results.length > 0 && (
        <div className="search-pagination">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => runSearch(page - 1)}
            disabled={!pageInfo.hasPrevious || loadingSearch}
          >
            Previous
          </button>
          <span className="search-pagination-info">
            Page {page + 1} of {pageInfo.totalPages} ({pageInfo.totalElements} total)
          </span>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => runSearch(page + 1)}
            disabled={!pageInfo.hasNext || loadingSearch}
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}

export default SearchContactTab;
