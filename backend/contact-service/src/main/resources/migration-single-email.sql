-- Migrate from multi-email (contact_emails table) to single email column on contacts

ALTER TABLE contacts ADD COLUMN IF NOT EXISTS email VARCHAR(255);

UPDATE contacts c
SET email = sub.email
FROM (
    SELECT contact_id, MIN(email) AS email
    FROM contact_emails
    GROUP BY contact_id
) sub
WHERE c.id = sub.contact_id
  AND (c.email IS NULL OR c.email = '');

UPDATE contacts
SET email = 'unknown@example.com'
WHERE email IS NULL OR email = '';

ALTER TABLE contacts ALTER COLUMN email SET NOT NULL;

DROP TABLE IF EXISTS contact_emails CASCADE;

CREATE INDEX IF NOT EXISTS idx_contacts_email ON contacts(LOWER(email));
