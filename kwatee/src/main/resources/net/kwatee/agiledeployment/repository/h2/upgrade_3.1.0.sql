
ALTER TABLE KWDeploymentPackage ADD COLUMN has_overlays BOOLEAN;

UPDATE KWApplicationParameter set schema_version='4.0.0' WHERE id = 0;
