ALTER TABLE KWDeploymentVariable ALTER COLUMN name type VARCHAR(200);
ALTER TABLE KWVersionVariable ALTER COLUMN name type VARCHAR(200);
UPDATE KWSystemProperty set value='2.1.14' WHERE name='kwatee_schema_version';