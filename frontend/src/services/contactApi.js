import axios from 'axios';

const api = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL || '',
  auth: {
    username: 'contactapi',
    password: 'contactapi-secret',
  },
  headers: {
    'Content-Type': 'application/json',
  },
});

export const getAllContacts = () => api.get('/contacts').then((res) => res.data);

export const getContactById = (id) => api.get(`/contacts/${id}`).then((res) => res.data);

export const createContact = (contact) => api.post('/contacts', contact).then((res) => res.data);

export const updateContact = (id, contact) => api.put(`/contacts/${id}`, contact).then((res) => res.data);

export const deleteContact = (id) => api.delete(`/contacts/${id}`);

export const getErrorMessage = (error) => {
  if (error.response?.data?.message) {
    return error.response.data.message;
  }
  if (error.response?.data?.error) {
    return error.response.data.error;
  }
  return error.message || 'An unexpected error occurred';
};
