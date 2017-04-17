ALTER TABLE KWDeployment ADD COLUMN has_errors BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE KWPackage DROP INDEX kwuc_package_name;
CREATE UNIQUE INDEX index_KWPackage ON KWPackage (name);

CREATE INDEX index_KWVersion ON KWVersion (package_id, name);

CREATE INDEX index_KWExecutable ON KWExecutable (version_id);

ALTER TABLE KWServer DROP INDEX kwuc_server_name;
CREATE UNIQUE INDEX index_KWServer ON KWServer (name);

CREATE INDEX index_KWServerProperty ON KWServerProperty (server_id);

ALTER TABLE KWEnvironment DROP INDEX kwuc_environment_name;
CREATE UNIQUE INDEX index_KWEnvironment ON KWEnvironment (name);

CREATE INDEX index_kw_environment_server ON kw_environment_server (environment_id);

CREATE INDEX index_kw_environment_package ON kw_environment_package (environment_id);

CREATE INDEX index_KWDeployment ON KWDeployment (environment_id);

CREATE INDEX index_KWRepositoryFile ON KWRepositoryFile (path);

CREATE INDEX index_kw_version_file ON kw_version_file (version_id);

CREATE INDEX index_KWDeploymentPackage ON KWDeploymentPackage (deployment_id);

CREATE INDEX index_kw_dp_file ON kw_dp_file (dp_id);

CREATE INDEX index_KWVersionVariable ON KWVersionVariable (version_id);

ALTER TABLE KWUser DROP INDEX kwuc_user_login;
CREATE UNIQUE INDEX index_KWUser ON KWUser (login);

CREATE INDEX index_KWAuthority ON KWAuthority (user_id);

CREATE INDEX index_KWServerCredentials ON KWServerCredentials (server_id);

CREATE INDEX index_KWDeploymentVariable ON KWDeploymentVariable (deployment_id);

ALTER TABLE KWSystemProperty DROP INDEX kwuc_systemproperty_name;
CREATE UNIQUE INDEX index_KWSystemProperty ON KWSystemProperty (name);

UPDATE KWApplicationParameter set schema_version='3.0.0' WHERE id = 0;
