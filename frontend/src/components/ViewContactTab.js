import React, { useEffect, useState } from 'react';
import { getErrorMessage, searchContacts } from '../services/contactApi';
import { joinList } from '../utils/contactHelpers';

const SIZE = 20;
const SORT_BY = 'lastName';
const SORT_DIRECTION = 'ASC';

function ViewContactTab() {
  const [page, setPage] = useState(0);
  const [results, setResults] = useState([]);
  const [pageInfo, setPageInfo] = useState({
    totalElements: 0,
    totalPages: 0,
    hasNext: false,
    hasPrevious: false,
  });
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState(null);

  const loadPage = async (targetPage) => {
    setLoading(true);
    setMessage(null);

    try {
      const data = await searchContacts({
        page: targetPage,
        size: SIZE,
        sortBy: SORT_BY,
        sortDirection: SORT_DIRECTION,
      });
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
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPage(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const showEmpty = !loading && results.length === 0 && !message;

  return (
    <div className="tab-panel">
      <h2>View Contacts</h2>
      <p className="tab-description">Browse all contacts, sorted by last name.</p>

      {message && <div className={`alert alert-${message.type}`}>{message.text}</div>}

      {loading && <p className="status-text">Loading contacts...</p>}

      {showEmpty && <p className="status-text">No contacts found.</p>}

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

      {results.length > 0 && (
        <div className="search-pagination">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => loadPage(page - 1)}
            disabled={!pageInfo.hasPrevious || loading}
          >
            Previous
          </button>
          <span className="search-pagination-info">
            Page {page + 1} of {pageInfo.totalPages} ({pageInfo.totalElements} total)
          </span>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => loadPage(page + 1)}
            disabled={!pageInfo.hasNext || loading}
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}

export default ViewContactTab;
