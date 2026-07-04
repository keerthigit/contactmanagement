import React, { useState } from 'react';
import ContactForm from './ContactForm';
import { createContact, getErrorMessage } from '../services/contactApi';
import { EMPTY_FORM, formToPayload } from '../utils/contactHelpers';

function CreateContactTab() {
  const [form, setForm] = useState(EMPTY_FORM);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState(null);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setMessage(null);

    try {
      const created = await createContact(formToPayload(form));
      setMessage({ type: 'success', text: `Contact created: ${created.firstName} ${created.lastName}` });
      setForm(EMPTY_FORM);
    } catch (error) {
      setMessage({ type: 'error', text: getErrorMessage(error) });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="tab-panel">
      <h2>Create Contact</h2>
      <p className="tab-description">Fill in the form below to add a new contact to the database.</p>

      {message && <div className={`alert alert-${message.type}`}>{message.text}</div>}

      <ContactForm
        form={form}
        onChange={setForm}
        onSubmit={handleSubmit}
        submitLabel={loading ? 'Saving...' : 'Create Contact'}
        disabled={loading}
      />
    </div>
  );
}

export default CreateContactTab;
