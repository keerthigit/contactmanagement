export const EMPTY_FORM = {
  firstName: '',
  lastName: '',
  email: '',
  mobile: '',
  homePhone: '',
  addresses: '',
  tags: '',
  status: 'ACTIVE',
};

export const STATUS_OPTIONS = ['ACTIVE', 'INACTIVE', 'ARCHIVED'];

export const parseList = (value) =>
  value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);

export const joinList = (items) => (items && items.length > 0 ? items.join(', ') : '');

export const contactToForm = (contact) => ({
  firstName: contact.firstName || '',
  lastName: contact.lastName || '',
  email: contact.email || '',
  mobile: contact.mobile || '',
  homePhone: contact.homePhone || '',
  addresses: joinList(contact.addresses),
  tags: joinList(contact.tags),
  status: contact.status || 'ACTIVE',
});

export const formToPayload = (form) => ({
  firstName: form.firstName.trim(),
  lastName: form.lastName.trim(),
  email: form.email.trim(),
  mobile: form.mobile.trim(),
  homePhone: form.homePhone.trim() || null,
  addresses: parseList(form.addresses),
  tags: parseList(form.tags),
  status: form.status,
});

export const getContactEmailLabel = (contact) => {
  const name = `${contact.firstName} ${contact.lastName}`.trim();
  if (contact.email) {
    return `${contact.email} (${name})`;
  }
  return `${name} (no email)`;
};

export const getContactEmailOptions = (contacts) =>
  contacts.map((contact) => ({
    key: contact.id,
    contactId: contact.id,
    label: getContactEmailLabel(contact),
    email: contact.email || '',
  }));
