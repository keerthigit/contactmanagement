import React, { useCallback, useEffect, useState } from 'react';
import ContactForm from './ContactForm';
import { getAllContacts, getContactById, getErrorMessage, updateContact } from '../services/contactApi';
import {
  contactToForm,
  formToPayload,
  getContactEmailOptions,
} from '../utils/contactHelpers';

function UpdateContactTab() {
  const [contacts, setContacts] = useState([]);
  const [selectedKey, setSelectedKey] = useState('');
  const [selectedContactId, setSelectedContactId] = useState(null);
  const [form, setForm] = useState(null);
  const [loadingContacts, setLoadingContacts] = useState(true);
  const [loadingContact, setLoadingContact] = useState(false);
  const [submitting, setSubmitting] = useState(false);
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
      setSelectedContactId(null);
      setForm(null);
      return;
    }

    const option = emailOptions.find((item) => item.key === key);
    if (!option) {
      return;
    }

    setLoadingContact(true);
    try {
      const contact = await getContactById(option.contactId);
      setSelectedContactId(contact.id);
      setForm(contactToForm(contact));
    } catch (error) {
      setMessage({ type: 'error', text: getErrorMessage(error) });
      setSelectedContactId(null);
      setForm(null);
    } finally {
      setLoadingContact(false);
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!selectedContactId || !form) {
      return;
    }

    setSubmitting(true);
    setMessage(null);

    try {
      const updated = await updateContact(selectedContactId, formToPayload(form));
      setMessage({ type: 'success', text: `Contact updated: ${updated.firstName} ${updated.lastName}` });
      await loadContacts();
    } catch (error) {
      setMessage({ type: 'error', text: getErrorMessage(error) });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="tab-panel">
      <h2>Update Contact</h2>
      <p className="tab-description">Select a contact by email to load and edit their details.</p>

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

      {form && !loadingContact && (
        <ContactForm
          form={form}
          onChange={setForm}
          onSubmit={handleSubmit}
          submitLabel={submitting ? 'Updating...' : 'Update Contact'}
          disabled={submitting}
        />
      )}
    </div>
  );
}

export default UpdateContactTab;
