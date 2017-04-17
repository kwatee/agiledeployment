ALTER TABLE KWVersion ADD COLUMN file_owner VARCHAR(33);
ALTER TABLE KWVersion ADD COLUMN file_group VARCHAR(33);
ALTER TABLE KWVersion ADD COLUMN file_mode INT;
ALTER TABLE KWVersion ADD COLUMN dir_mode INT;
ALTER TABLE KWVersion ADD COLUMN need_package_rescan BOOLEAN NOT NULL DEFAULT 1;

ALTER TABLE KWDeployment ADD COLUMN file_owner VARCHAR(33);
ALTER TABLE KWDeployment ADD COLUMN file_group VARCHAR(33);
ALTER TABLE KWDeployment ADD COLUMN file_mode INT;
ALTER TABLE KWDeployment ADD COLUMN dir_mode INT;

ALTER TABLE KWRepositoryFile ADD COLUMN file_owner VARCHAR(33);
ALTER TABLE KWRepositoryFile ADD COLUMN file_group VARCHAR(33);
ALTER TABLE KWRepositoryFile ADD COLUMN file_mode INT;
ALTER TABLE KWRepositoryFile ADD COLUMN dir_mode INT;

UPDATE KWApplicationParameter set schema_version='3.1.0' WHERE id = 0;
