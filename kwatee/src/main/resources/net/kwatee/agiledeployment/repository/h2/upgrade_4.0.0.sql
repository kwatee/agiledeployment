ALTER TABLE KWRepositoryFile ADD COLUMN original_owner VARCHAR(33);
ALTER TABLE KWRepositoryFile ADD COLUMN original_group VARCHAR(33);
ALTER TABLE KWRepositoryFile ADD COLUMN original_mode INT;

UPDATE KWApplicationParameter set schema_version='4.0.1' WHERE id = 0;
