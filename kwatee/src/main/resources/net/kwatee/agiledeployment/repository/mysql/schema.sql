CREATE TABLE KWPackage (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	name VARCHAR(50) NOT NULL,
	description VARCHAR(255),
	creation_ts BIGINT NOT NULL,
	disable_ts BIGINT
) ENGINE=InnoDB;
ALTER TABLE KWPackage AUTO_INCREMENT = 1;
CREATE UNIQUE INDEX index_KWPackage ON KWPackage (name);

CREATE TABLE KWVersion (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
	file_owner VARCHAR(200),
	file_group VARCHAR(200),
	file_mode INT,
	dir_mode INT,
	need_package_rescan BOOLEAN NOT NULL
) ENGINE=InnoDB;
ALTER TABLE KWVersion ADD CONSTRAINT kwuc_version_name UNIQUE (package_id, name);
ALTER TABLE KWVersion ADD CONSTRAINT kwfk_version_package FOREIGN KEY (package_id) REFERENCES KWPackage(id);
ALTER TABLE KWVersion AUTO_INCREMENT = 1;
CREATE INDEX index_KWVersion ON KWVersion (package_id);

CREATE TABLE kw_version_platform (
	version_id BIGINT NOT NULL,
	platform_id SMALLINT NOT NULL
) ENGINE=InnoDB;
ALTER TABLE kw_version_platform ADD CONSTRAINT kwfk_version_platform_version FOREIGN KEY (version_id) REFERENCES KWVersion(id);

CREATE TABLE KWExecutable (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	version_id BIGINT NOT NULL,
	name VARCHAR(50) NOT NULL,
	description VARCHAR(255) NOT NULL,
	start_action VARCHAR(2000) NOT NULL,
	stop_action VARCHAR(2000) NOT NULL,
	status_action VARCHAR(2000) NOT NULL
) ENGINE=InnoDB;
ALTER TABLE KWExecutable ADD CONSTRAINT kwuc_executable_name UNIQUE (version_id, name);
ALTER TABLE KWExecutable ADD CONSTRAINT kwfk_executable_version FOREIGN KEY (version_id) REFERENCES KWVersion(id);
ALTER TABLE KWExecutable AUTO_INCREMENT = 1;
CREATE INDEX index_KWExecutable ON KWExecutable (version_id);

CREATE TABLE KWServer (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
) ENGINE=InnoDB;
ALTER TABLE KWServer AUTO_INCREMENT = 1;
CREATE UNIQUE INDEX index_KWServer ON KWServer (name);

CREATE TABLE KWServerProperty (
	server_id BIGINT NOT NULL,
	name varchar(255) NOT NULL,
	value varchar(4096) NOT NULL
) ENGINE=InnoDB;
ALTER TABLE KWServerProperty ADD CONSTRAINT kwfk_serverproperty_version FOREIGN KEY (server_id) REFERENCES KWServer(id);
ALTER TABLE KWServerProperty ADD CONSTRAINT kwuc_serverproperty_name UNIQUE (server_id, name);
CREATE INDEX index_KWServerProperty ON KWServerProperty (server_id);

CREATE TABLE KWEnvironment (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	name VARCHAR(50) NOT NULL,
	description VARCHAR(255) NOT NULL,
	sequential_deployment BOOLEAN NOT NULL,
	creation_ts BIGINT NOT NULL,
	disable_ts BIGINT
) ENGINE=InnoDB;
ALTER TABLE KWEnvironment AUTO_INCREMENT = 1;
CREATE UNIQUE INDEX index_KWEnvironment ON KWEnvironment (name);

CREATE TABLE kw_environment_server (
	environment_id BIGINT NOT NULL,
	server_id BIGINT NOT NULL,
	pos INT NOT NULL
) ENGINE=InnoDB;
ALTER TABLE kw_environment_server ADD CONSTRAINT kwfk_environment_server_environment FOREIGN KEY (environment_id) REFERENCES KWEnvironment(id);
ALTER TABLE kw_environment_server ADD CONSTRAINT kwfk_environment_server_server FOREIGN KEY (server_id) REFERENCES KWServer(id);
CREATE INDEX index_kw_environment_server ON kw_environment_server (environment_id);

CREATE TABLE kw_environment_package (
	environment_id BIGINT NOT NULL,
	package_id BIGINT NOT NULL,
	pos INT NOT NULL
) ENGINE=InnoDB;
ALTER TABLE kw_environment_package ADD CONSTRAINT kwfk_environment_package_environment FOREIGN KEY (environment_id) REFERENCES KWEnvironment(id);
ALTER TABLE kw_environment_package ADD CONSTRAINT kwfk_environment_package_package FOREIGN KEY (package_id) REFERENCES KWPackage(id);
CREATE INDEX index_kw_environment_package ON kw_environment_package (environment_id);

CREATE TABLE KWDeployment (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
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

) ENGINE=InnoDB;
ALTER TABLE KWDeployment ADD CONSTRAINT kwuc_deployment_name UNIQUE (environment_id,name);
ALTER TABLE KWDeployment AUTO_INCREMENT = 1;
CREATE INDEX index_KWDeployment ON KWDeployment (environment_id);

CREATE TABLE KWRepositoryFile (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
) ENGINE=InnoDB;
ALTER TABLE KWRepositoryFile AUTO_INCREMENT = 1;
CREATE INDEX index_KWRepositoryFile ON KWRepositoryFile (path);

CREATE TABLE kw_version_file (
	version_id BIGINT NOT NULL,
	file_id BIGINT NOT NULL
) ENGINE=InnoDB;
ALTER TABLE kw_version_file ADD CONSTRAINT kwfk_version_file_version FOREIGN KEY (version_id) REFERENCES KWVersion(id);
ALTER TABLE kw_version_file ADD CONSTRAINT kwfk_version_file_file FOREIGN KEY (file_id) REFERENCES KWRepositoryFile(id);
CREATE INDEX index_kw_version_file ON kw_version_file (version_id);

CREATE TABLE KWDeploymentPackage (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	deployment_id BIGINT NOT NULL,
	package_id BIGINT NOT NULL,
	server_id BIGINT,
	version_id BIGINT,
	has_overlays BOOLEAN
) ENGINE=InnoDB;
ALTER TABLE KWDeploymentPackage ADD CONSTRAINT kwfk_deploymentserverpackage_deployment FOREIGN KEY (deployment_id) REFERENCES KWDeployment(id);
ALTER TABLE KWDeploymentPackage ADD CONSTRAINT kwfk_deploymentserverpackage_server FOREIGN KEY (server_id) REFERENCES KWServer(id);
ALTER TABLE KWDeploymentPackage ADD CONSTRAINT kwfk_deploymentserverpackage_package FOREIGN KEY (package_id) REFERENCES KWPackage(id);
ALTER TABLE KWDeploymentPackage ADD CONSTRAINT kwfk_deploymentserverpackage_version FOREIGN KEY (version_id) REFERENCES KWVersion(id);
ALTER TABLE KWDeploymentPackage AUTO_INCREMENT = 1;
CREATE INDEX index_KWDeploymentPackage ON KWDeploymentPackage (deployment_id);

CREATE TABLE kw_dp_file (
	dp_id BIGINT NOT NULL,
	file_id BIGINT NOT NULL
) ENGINE=InnoDB;
ALTER TABLE kw_dp_file ADD CONSTRAINT kwfk_dp_file_dp FOREIGN KEY (dp_id) REFERENCES KWDeploymentPackage(id);
ALTER TABLE kw_dp_file ADD CONSTRAINT kwfk_dp_file_file FOREIGN KEY (file_id) REFERENCES KWRepositoryFile(id);
CREATE INDEX index_kw_dp_file ON kw_dp_file (dp_id);

CREATE TABLE KWVersionVariable (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	version_id BIGINT NOT NULL,
	name VARCHAR(40) NOT NULL,
	default_value VARCHAR(2000) NOT NULL,
	description VARCHAR(200)
) ENGINE=InnoDB;
ALTER TABLE KWVersionVariable ADD CONSTRAINT kwfk_versionvariable_version FOREIGN KEY (version_id) REFERENCES KWVersion(id);
ALTER TABLE KWVersionVariable ADD CONSTRAINT kwuc_versionvariable_name UNIQUE (version_id,name);
ALTER TABLE KWVersionVariable AUTO_INCREMENT = 1;
CREATE INDEX index_KWVersionVariable ON KWVersionVariable(version_id);

CREATE TABLE KWUser (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	login VARCHAR(20) NOT NULL,
	password VARCHAR(40) NOT NULL,
	description VARCHAR(255),
	email VARCHAR(40),
	creation_ts BIGINT NOT NULL,
	disable_ts BIGINT
) ENGINE=InnoDB;
ALTER TABLE KWUser AUTO_INCREMENT = 1;
CREATE UNIQUE INDEX index_KWUser ON KWUser(login);

CREATE TABLE KWAuthority (
	user_id BIGINT NOT NULL,
	authority VARCHAR(20) NOT NULL
) ENGINE=InnoDB;
ALTER TABLE KWAuthority ADD CONSTRAINT kwuc_authority_authority UNIQUE (user_id, authority);
ALTER TABLE KWAuthority ADD CONSTRAINT kwfk_authority_user FOREIGN KEY (user_id) REFERENCES KWUser(id);
CREATE INDEX index_KWAuthority ON KWAuthority(user_id);

CREATE TABLE KWServerCredentials (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	server_id BIGINT NOT NULL,
	access_level INT NOT NULL,
	login VARCHAR(20) NOT NULL,
	password_prompt SMALLINT DEFAULT 0,
	password VARCHAR(80),
	pem VARCHAR(4000)
) ENGINE=InnoDB; 
ALTER TABLE KWServerCredentials ADD CONSTRAINT kwfk_servercredentials_server FOREIGN KEY (server_id) REFERENCES KWServer(id);
ALTER TABLE KWServerCredentials AUTO_INCREMENT = 1;
CREATE INDEX index_KWServerCredentials ON KWServerCredentials(server_id);

CREATE TABLE KWDeploymentVariable (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	deployment_id BIGINT NOT NULL,
	name VARCHAR(40) NOT NULL,
	value VARCHAR(2000) NOT NULL,
	package_id BIGINT,
	server_id BIGINT,
	frozen_system_property BOOLEAN NOT NULL,
	description VARCHAR(200)
) ENGINE=InnoDB;
ALTER TABLE KWDeploymentVariable ADD CONSTRAINT kwfk_deploymentvariable_deployment FOREIGN KEY (deployment_id) REFERENCES KWDeployment(id);
ALTER TABLE KWDeploymentVariable ADD CONSTRAINT kwfk_deploymentvariable_package FOREIGN KEY (package_id) REFERENCES KWPackage(id);
ALTER TABLE KWDeploymentVariable ADD CONSTRAINT kwfk_deploymentvariable_server FOREIGN KEY (server_id) REFERENCES KWServer(id);
ALTER TABLE KWDeploymentVariable AUTO_INCREMENT = 1;
CREATE INDEX index_KWDeploymentVariable ON KWDeploymentVariable(deployment_id);

CREATE TABLE KWSystemProperty (
	name VARCHAR(40) NOT NULL,
	value VARCHAR(512) NOT NULL,
	description VARCHAR(255),
	pos SMALLINT NOT NULL
) ENGINE=InnoDB;
CREATE UNIQUE INDEX index_KWSystemProperty ON KWSystemProperty(name);

CREATE TABLE KWApplicationParameter (
	id SMALLINT PRIMARY KEY,
	schema_version VARCHAR(20),
	title VARCHAR(80),
	excluded_extensions VARCHAR(2000)
) ENGINE=InnoDB;
