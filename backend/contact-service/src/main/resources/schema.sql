-- Contact Management System - Database Schema
-- Phase 1: Contacts table and related tables

-- Main contacts table
CREATE TABLE IF NOT EXISTS contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    mobile VARCHAR(50) NOT NULL,
    home_phone VARCHAR(50),
    status VARCHAR(50) NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Contact emails table (one-to-many relationship)
CREATE TABLE IF NOT EXISTS contact_emails (
    contact_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    PRIMARY KEY (contact_id, email),
    FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE CASCADE
);

-- Contact addresses table (one-to-many relationship)
CREATE TABLE IF NOT EXISTS contact_addresses (
    contact_id UUID NOT NULL,
    address TEXT NOT NULL,
    PRIMARY KEY (contact_id, address),
    FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE CASCADE
);

-- Contact tags table (one-to-many relationship)
CREATE TABLE IF NOT EXISTS contact_tags (
    contact_id UUID NOT NULL,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (contact_id, tag),
    FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE CASCADE
);

-- Indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_contacts_status ON contacts(status);
CREATE INDEX IF NOT EXISTS idx_contacts_created_at ON contacts(created_at);
CREATE INDEX IF NOT EXISTS idx_contact_emails_contact_id ON contact_emails(contact_id);
CREATE INDEX IF NOT EXISTS idx_contact_addresses_contact_id ON contact_addresses(contact_id);
CREATE INDEX IF NOT EXISTS idx_contact_tags_contact_id ON contact_tags(contact_id);

-- Phase 2: Indexes for search functionality
CREATE INDEX IF NOT EXISTS idx_contacts_first_name ON contacts(LOWER(first_name));
CREATE INDEX IF NOT EXISTS idx_contacts_last_name ON contacts(LOWER(last_name));
CREATE INDEX IF NOT EXISTS idx_contact_emails_email ON contact_emails(LOWER(email));
CREATE INDEX IF NOT EXISTS idx_contacts_mobile ON contacts(mobile);
CREATE INDEX IF NOT EXISTS idx_contact_addresses_address ON contact_addresses(LOWER(address));
