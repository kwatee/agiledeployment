ALTER TABLE KWServer ADD COLUMN use_sudo BOOLEAN;

UPDATE KWApplicationParameter set schema_version='4.0.2' WHERE id = 0;
