import React, { useCallback, useEffect, useState } from 'react';
import { deleteContact, getAllContacts, getContactById, getErrorMessage } from '../services/contactApi';
import { getContactEmailOptions, joinList } from '../utils/contactHelpers';

function DeleteContactTab() {
  const [contacts, setContacts] = useState([]);
  const [selectedKey, setSelectedKey] = useState('');
  const [selectedContact, setSelectedContact] = useState(null);
  const [loadingContacts, setLoadingContacts] = useState(true);
  const [loadingContact, setLoadingContact] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [message, setMessage] = useState(null);

  const loadContacts = useCallback(async () => {
    setLoadingContacts(true);
    setMessage(null);
    try {
      const data = await getAllContacts();
      setContacts(data);
    } catch (error) {
      setMessage({ type: 'error', text: getErrorMessage(error) });
    } finally {
      setLoadingContacts(false);
    }
  }, []);

  useEffect(() => {
    loadContacts();
  }, [loadContacts]);

  const emailOptions = getContactEmailOptions(contacts);

  const handleSelect = async (event) => {
    const key = event.target.value;
    setSelectedKey(key);
    setMessage(null);

    if (!key) {
      setSelectedContact(null);
      return;
    }

    const option = emailOptions.find((item) => item.key === key);
    if (!option) {
      return;
    }

    setLoadingContact(true);
    try {
      const contact = await getContactById(option.contactId);
      setSelectedContact(contact);
    } catch (error) {
      setMessage({ type: 'error', text: getErrorMessage(error) });
      setSelectedContact(null);
    } finally {
      setLoadingContact(false);
    }
  };

  const handleDelete = async () => {
    if (!selectedContact) {
      return;
    }

    const confirmed = window.confirm(
      `Delete contact ${selectedContact.firstName} ${selectedContact.lastName}?`
    );
    if (!confirmed) {
      return;
    }

    setDeleting(true);
    setMessage(null);

    try {
      await deleteContact(selectedContact.id);
      setMessage({
        type: 'success',
        text: `Contact deleted: ${selectedContact.firstName} ${selectedContact.lastName}`,
      });
      setSelectedKey('');
      setSelectedContact(null);
      await loadContacts();
    } catch (error) {
      setMessage({ type: 'error', text: getErrorMessage(error) });
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="tab-panel">
      <h2>Delete Contact</h2>
      <p className="tab-description">Select a contact by email to review and delete them.</p>

      {message && <div className={`alert alert-${message.type}`}>{message.text}</div>}

      <label className="selector-label">
        Select contact by email
        <select value={selectedKey} onChange={handleSelect} disabled={loadingContacts}>
          <option value="">
            {loadingContacts ? 'Loading contacts...' : 'Choose a contact email'}
          </option>
          {emailOptions.map((option) => (
            <option key={option.key} value={option.key}>
              {option.label}
            </option>
          ))}
        </select>
      </label>

      {loadingContact && <p className="status-text">Loading contact details...</p>}

      {selectedContact && !loadingContact && (
        <div className="contact-preview">
          <h3>
            {selectedContact.firstName} {selectedContact.lastName}
          </h3>
          <dl>
            <dt>Email</dt>
            <dd>{selectedContact.email || '—'}</dd>
            <dt>Mobile</dt>
            <dd>{selectedContact.mobile}</dd>
            <dt>Home Phone</dt>
            <dd>{selectedContact.homePhone || '—'}</dd>
            <dt>Addresses</dt>
            <dd>{joinList(selectedContact.addresses) || '—'}</dd>
            <dt>Tags</dt>
            <dd>{joinList(selectedContact.tags) || '—'}</dd>
            <dt>Status</dt>
            <dd>{selectedContact.status}</dd>
          </dl>
          <button
            type="button"
            className="btn btn-danger"
            onClick={handleDelete}
            disabled={deleting}
          >
            {deleting ? 'Deleting...' : 'Delete Contact'}
          </button>
        </div>
      )}
    </div>
  );
}

export default DeleteContactTab;
