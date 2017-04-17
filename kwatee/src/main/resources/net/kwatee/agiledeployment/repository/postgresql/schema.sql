SET client_min_messages='warning';

CREATE SEQUENCE KWPackage_id_seq START WITH 1 MINVALUE 0;
CREATE TABLE KWPackage (
	id BIGINT PRIMARY KEY DEFAULT NEXTVAL('KWPackage_id_seq'),
	name VARCHAR(50) NOT NULL,
	description VARCHAR(255),
	creation_ts BIGINT NOT NULL,
	disable_ts BIGINT
);
CREATE UNIQUE INDEX index_KWPackage ON KWPackage (name);

CREATE SEQUENCE KWVersion_id_seq START WITH 1 MINVALUE 0;
CREATE TABLE KWVersion (
	id BIGINT PRIMARY KEY DEFAULT NEXTVAL('KWVersion_id_seq'),
	package_id BIGINT NOT NULL,
	name VARCHAR(50) NOT NULL,
	description VARCHAR(255) NOT NULL,
	platform VARCHAR(20),
	pre_deploy_action VARCHAR(2000) NOT NULL,
	post_deploy_action VARCHAR(2000) NOT NULL,
	pre_undeploy_action VARCHAR(2000) NOT NULL,
	post_undeploy_action VARCHAR(2000) NOT NULL,
	archive_file_name VARCHAR(255),
	archive_size BIGINT,
	archive_upload_date BIGINT,
	creation_ts BIGINT NOT NULL,
	disable_ts BIGINT,
	has_properties BOOLEAN,
	var_prefix_char CHAR(1),
	fileOwner VARCHAR(200),
	fileGroup VARCHAR(200),
	file_mode INT,
	dir_mode INT,
	need_package_rescan BOOLEAN NOT NULL
);
ALTER TABLE KWVersion ADD CONSTRAINT kwuc_version_name UNIQUE (package_id, name);
ALTER TABLE KWVersion ADD CONSTRAINT kwfk_version_package FOREIGN KEY (package_id) REFERENCES KWPackage(id);
CREATE INDEX index_KWVersion ON KWVersion (package_id);

CREATE TABLE kw_version_platform (
	version_id BIGINT NOT NULL,
	platform_id SMALLINT NOT NULL
);
ALTER TABLE kw_version_platform ADD CONSTRAINT kwfk_version_platform_version FOREIGN KEY (version_id) REFERENCES KWVersion(id);

CREATE SEQUENCE KWExecutable_id_seq START WITH 1 MINVALUE 0;
CREATE TABLE KWExecutable (
	id BIGINT PRIMARY KEY DEFAULT NEXTVAL('KWExecutable_id_seq'),
	version_id BIGINT NOT NULL,
	name VARCHAR(50) NOT NULL,
	description VARCHAR(255) NOT NULL,
	start_action VARCHAR(2000) NOT NULL,
	stop_action VARCHAR(2000) NOT NULL,
	status_action VARCHAR(2000) NOT NULL
);
ALTER TABLE KWExecutable ADD CONSTRAINT kwuc_executable_name UNIQUE (version_id, name);
ALTER TABLE KWExecutable ADD CONSTRAINT kwfk_executable_version FOREIGN KEY (version_id) REFERENCES KWVersion(id);
CREATE INDEX index_KWExecutable ON KWExecutable (version_id);

CREATE SEQUENCE KWServer_id_seq START WITH 1 MINVALUE 0;
CREATE TABLE KWServer (
	id BIGINT PRIMARY KEY DEFAULT NEXTVAL('KWServer_id_seq'),
	name VARCHAR(50) NOT NULL,
	description VARCHAR(255) NOT NULL,
	conduit_type VARCHAR(20) NOT NULL,
	pool_type VARCHAR(20),
	pool_concurrency SMALLINT NOT NULL,
	platform_id SMALLINT NOT NULL,
	ip_address VARCHAR(30) NOT NULL,
	port INT NOT NULL,
	creation_ts BIGINT NOT NULL,
	disable_ts BIGINT,
	use_sudo BOOLEAN
);
CREATE UNIQUE INDEX index_KWServer ON KWServer (name);

CREATE TABLE KWServerProperty (
	server_id BIGINT NOT NULL,
	name varchar(255) NOT NULL,
	value varchar(255) NOT NULL
);
ALTER TABLE KWServerProperty ADD CONSTRAINT kwfk_serverproperty_version FOREIGN KEY (server_id) REFERENCES KWServer(id);
ALTER TABLE KWServerProperty ADD CONSTRAINT kwuc_serverproperty_name UNIQUE (server_id, name);
CREATE INDEX index_KWServerProperty ON KWServerProperty (server_id);

CREATE SEQUENCE KWEnvironment_id_seq START WITH 1 MINVALUE 0;
CREATE TABLE KWEnvironment (
	id BIGINT PRIMARY KEY DEFAULT NEXTVAL('KWEnvironment_id_seq'),
	name VARCHAR(50) NOT NULL,
	description VARCHAR(255) NOT NULL,
	sequential_deployment BOOLEAN NOT NULL,
	creation_ts BIGINT NOT NULL,
	disable_ts BIGINT
);
CREATE UNIQUE INDEX index_KWEnvironment ON KWEnvironment (name);

CREATE TABLE kw_environment_server (
	environment_id BIGINT NOT NULL,
	server_id BIGINT NOT NULL,
	pos INT NOT NULL
);
ALTER TABLE kw_environment_server ADD CONSTRAINT kwfk_environment_server_environment FOREIGN KEY (environment_id) REFERENCES KWEnvironment(id);
ALTER TABLE kw_environment_server ADD CONSTRAINT kwfk_environment_server_server FOREIGN KEY (server_id) REFERENCES KWServer(id);
CREATE INDEX index_kw_environment_server ON kw_environment_server (environment_id);

CREATE TABLE kw_environment_package (
	environment_id BIGINT NOT NULL,
	package_id BIGINT NOT NULL,
	pos INT NOT NULL
);
ALTER TABLE kw_environment_package ADD CONSTRAINT kwfk_environment_package_environment FOREIGN KEY (environment_id) REFERENCES KWEnvironment(id);
ALTER TABLE kw_environment_package ADD CONSTRAINT kwfk_environment_package_package FOREIGN KEY (package_id) REFERENCES KWPackage(id);
CREATE INDEX index_kw_environment_package ON kw_environment_package (environment_id);

CREATE SEQUENCE KWDeployment_id_seq START WITH 1 MINVALUE 0;
CREATE TABLE KWDeployment (
	id BIGINT PRIMARY KEY DEFAULT NEXTVAL('KWDeployment_id_seq'),
	environment_id BIGINT NOT NULL,
	name VARCHAR(50) NOT NULL,
	description VARCHAR(255) NOT NULL,
	pre_setup_action VARCHAR(2000) NOT NULL,
	post_setup_action VARCHAR(2000) NOT NULL,
	pre_cleanup_action VARCHAR(2000) NOT NULL,
	post_cleanup_action VARCHAR(2000) NOT NULL,
	stop_on_first_error BOOLEAN NOT NULL,
	has_errors BOOLEAN NOT NULL,
	creation_ts BIGINT NOT NULL,
	disable_ts BIGINT,
	file_owner VARCHAR(200),
	file_group VARCHAR(200),
	file_mode INT,
	dir_mode INT
);
ALTER TABLE KWDeployment ADD CONSTRAINT kwuc_deployment_name UNIQUE (environment_id, name);
CREATE INDEX index_KWDeployment ON KWDeployment (environment_id);

CREATE SEQUENCE KWRepositoryFile_id_seq START WITH 1 MINVALUE 0;
CREATE TABLE KWRepositoryFile (
	id BIGINT PRIMARY KEY DEFAULT NEXTVAL('KWRepositoryFile_id_seq'),
	layer_type INT NOT NULL,
	path VARCHAR(255) NOT NULL,
	is_symbolic_link BOOLEAN NOT NULL,
	is_directory BOOLEAN NOT NULL,
	can_execute BOOLEAN NOT NULL,
	ignore_variables BOOLEAN NOT NULL,
	ignore_integrity BOOLEAN NOT NULL,
	dont_delete BOOLEAN NOT NULL,
	variables VARCHAR(4000),
	size BIGINT NOT NULL,
	signature VARCHAR(33) NOT NULL,
	file_owner VARCHAR(200),
	file_group VARCHAR(200),
	file_mode INT,
	dir_mode INT,
	original_owner VARCHAR(200),
	original_group VARCHAR(200),
	original_mode INT
);
CREATE INDEX index_KWRepositoryFile ON KWRepositoryFile (path);

CREATE TABLE kw_version_file (
	version_id BIGINT NOT NULL,
	file_id BIGINT NOT NULL
);
ALTER TABLE kw_version_file ADD CONSTRAINT kwfk_version_file_version FOREIGN KEY (version_id) REFERENCES KWVersion(id);
ALTER TABLE kw_version_file ADD CONSTRAINT kwfk_version_file_file FOREIGN KEY (file_id) REFERENCES KWRepositoryFile(id);
CREATE INDEX index_kw_version_file ON kw_version_file (version_id);

CREATE SEQUENCE KWDeploymentPackage_id_seq START WITH 1 MINVALUE 0;
CREATE TABLE KWDeploymentPackage (
	id BIGINT PRIMARY KEY DEFAULT NEXTVAL('KWDeploymentPackage_id_seq'),
	deployment_id BIGINT NOT NULL,
	server_id BIGINT,
	package_id BIGINT NOT NULL,
	version_id BIGINT,
	has_overlays BOOLEAN
);
ALTER TABLE KWDeploymentPackage ADD CONSTRAINT kwfk_deploymentserverpackage_deployment FOREIGN KEY (deployment_id) REFERENCES KWDeployment(id);
ALTER TABLE KWDeploymentPackage ADD CONSTRAINT kwfk_deploymentserverpackage_server FOREIGN KEY (server_id) REFERENCES KWServer(id);
ALTER TABLE KWDeploymentPackage ADD CONSTRAINT kwfk_deploymentserverpackage_package FOREIGN KEY (package_id) REFERENCES KWPackage(id);
ALTER TABLE KWDeploymentPackage ADD CONSTRAINT kwfk_deploymentserverpackage_version FOREIGN KEY (version_id) REFERENCES KWVersion(id);
CREATE INDEX index_KWDeploymentPackage ON KWDeploymentPackage (deployment_id);

CREATE TABLE kw_dp_file (
	dp_id BIGINT NOT NULL,
	file_id BIGINT NOT NULL
);
ALTER TABLE kw_dp_file ADD CONSTRAINT kwfk_dp_file_dp FOREIGN KEY (dp_id) REFERENCES KWDeploymentPackage(id);
ALTER TABLE kw_dp_file ADD CONSTRAINT kwfk_dp_file_file FOREIGN KEY (file_id) REFERENCES KWRepositoryFile(id);
CREATE INDEX index_kw_dp_file ON kw_dp_file (dp_id);

CREATE SEQUENCE KWVersionVariable_id_seq START WITH 1 MINVALUE 0;
CREATE TABLE KWVersionVariable (
	id BIGINT PRIMARY KEY DEFAULT NEXTVAL('KWVersionVariable_id_seq'),
	version_id BIGINT NOT NULL,
	name VARCHAR(40) NOT NULL,
	default_value VARCHAR(2000) NOT NULL,
	description VARCHAR(200)
);
ALTER TABLE KWVersionVariable ADD CONSTRAINT kwfk_versionvariable_version FOREIGN KEY (version_id) REFERENCES KWVersion(id);
ALTER TABLE KWVersionVariable ADD CONSTRAINT kwuc_versionvariable_name UNIQUE (version_id, name);
CREATE INDEX index_KWVersionVariable ON KWVersionVariable(version_id);

CREATE SEQUENCE KWUser_id_seq START WITH 1 MINVALUE 0;
CREATE TABLE KWUser (
	id BIGINT PRIMARY KEY DEFAULT NEXTVAL('KWUser_id_seq'),
	login VARCHAR(20) NOT NULL,
	password VARCHAR(40) NOT NULL,
	description VARCHAR(255),
	email VARCHAR(40),
	creation_ts BIGINT NOT NULL,
	disable_ts BIGINT
);
CREATE UNIQUE INDEX index_KWUser ON KWUser(login);

CREATE TABLE KWAuthority (
	user_id BIGINT NOT NULL,
	authority VARCHAR(20) NOT NULL
);
ALTER TABLE KWAuthority ADD CONSTRAINT kwuc_authority_authority UNIQUE (user_id,authority);
ALTER TABLE KWAuthority ADD CONSTRAINT kwfk_authority_user FOREIGN KEY (user_id) REFERENCES KWUser(id);
CREATE INDEX index_KWAuthority ON KWAuthority(user_id);

CREATE SEQUENCE KWServerCredentials_id_seq START WITH 1 MINVALUE 0;
CREATE TABLE KWServerCredentials (
	id BIGINT PRIMARY KEY DEFAULT NEXTVAL('KWServerCredentials_id_seq'),
	server_id BIGINT NOT NULL,
	access_level INT NOT NULL,
	login VARCHAR(20) NOT NULL,
	password_prompt SMALLINT DEFAULT 0,
	password VARCHAR(80),
	pem VARCHAR(4000)
); 
ALTER TABLE KWServerCredentials ADD CONSTRAINT kwfk_servercredentials_server FOREIGN KEY (server_id) REFERENCES KWServer(id);
CREATE INDEX index_KWServerCredentials ON KWServerCredentials(server_id);

CREATE SEQUENCE KWDeploymentVariable_id_seq START WITH 1 MINVALUE 0;
CREATE TABLE KWDeploymentVariable (
	id BIGINT PRIMARY KEY DEFAULT NEXTVAL('KWDeploymentVariable_id_seq'),
	deployment_id BIGINT NOT NULL,
	name VARCHAR(40) NOT NULL,
	value VARCHAR(2000) NOT NULL,
	package_id BIGINT,
	server_id BIGINT,
	frozen_system_property BOOLEAN NOT NULL,
	description VARCHAR(200)
);
ALTER TABLE KWDeploymentVariable ADD CONSTRAINT kwfk_deploymentvariable_deployment FOREIGN KEY (deployment_id) REFERENCES KWDeployment(id);
ALTER TABLE KWDeploymentVariable ADD CONSTRAINT kwfk_deploymentvariable_package FOREIGN KEY (package_id) REFERENCES KWPackage(id);
ALTER TABLE KWDeploymentVariable ADD CONSTRAINT kwfk_deploymentvariable_server FOREIGN KEY (server_id) REFERENCES KWServer(id);
CREATE INDEX index_KWDeploymentVariable ON KWDeploymentVariable(deployment_id);

CREATE TABLE KWSystemProperty (
	name VARCHAR(40) NOT NULL,
	value VARCHAR(512) NOT NULL,
	description VARCHAR(255),
	pos SMALLINT NOT NULL
);
CREATE UNIQUE INDEX index_KWSystemProperty ON KWSystemProperty(name);

CREATE TABLE KWApplicationParameter (
	id SMALLINT PRIMARY KEY,
	schema_version VARCHAR(20),
	title VARCHAR(80),
	excluded_extensions VARCHAR(2000)
);
