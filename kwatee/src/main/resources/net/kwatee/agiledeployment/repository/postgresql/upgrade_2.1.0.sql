DROP TABLE KWAuditLogEntry;
DROP SEQUENCE KWAuditLogEntry_id_seq;
UPDATE KWSystemProperty set value='2.1.4' WHERE name='kwatee_schema_version';