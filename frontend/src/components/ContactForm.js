import React from 'react';
import { STATUS_OPTIONS } from '../utils/contactHelpers';

function ContactForm({ form, onChange, onSubmit, submitLabel, disabled }) {
  const handleChange = (field) => (event) => {
    onChange({ ...form, [field]: event.target.value });
  };

  return (
    <form className="contact-form" onSubmit={onSubmit}>
      <div className="form-row">
        <label>
          First Name *
          <input
            type="text"
            value={form.firstName}
            onChange={handleChange('firstName')}
            required
            disabled={disabled}
          />
        </label>
        <label>
          Last Name *
          <input
            type="text"
            value={form.lastName}
            onChange={handleChange('lastName')}
            required
            disabled={disabled}
          />
        </label>
      </div>

      <label>
        Email *
        <input
          type="email"
          value={form.email}
          onChange={handleChange('email')}
          placeholder="jane@example.com"
          required
          disabled={disabled}
        />
      </label>

      <div className="form-row">
        <label>
          Mobile *
          <input
            type="text"
            value={form.mobile}
            onChange={handleChange('mobile')}
            required
            disabled={disabled}
          />
        </label>
        <label>
          Home Phone
          <input
            type="text"
            value={form.homePhone}
            onChange={handleChange('homePhone')}
            disabled={disabled}
          />
        </label>
      </div>

      <label>
        Addresses (comma-separated)
        <input
          type="text"
          value={form.addresses}
          onChange={handleChange('addresses')}
          placeholder="123 Main St, City, State 12345"
          disabled={disabled}
        />
      </label>

      <label>
        Tags (comma-separated)
        <input
          type="text"
          value={form.tags}
          onChange={handleChange('tags')}
          placeholder="work, friend"
          disabled={disabled}
        />
      </label>

      <label>
        Status *
        <select value={form.status} onChange={handleChange('status')} required disabled={disabled}>
          {STATUS_OPTIONS.map((status) => (
            <option key={status} value={status}>
              {status}
            </option>
          ))}
        </select>
      </label>

      <button type="submit" className="btn btn-primary" disabled={disabled}>
        {submitLabel}
      </button>
    </form>
  );
}

export default ContactForm;
