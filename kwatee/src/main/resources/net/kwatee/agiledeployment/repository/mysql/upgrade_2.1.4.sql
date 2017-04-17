ALTER TABLE KWDeploymentVariable CHANGE name name VARCHAR(200);
ALTER TABLE KWVersionVariable CHANGE name name VARCHAR(200);
UPDATE KWSystemProperty set value='2.1.14' WHERE name='kwatee_schema_version';