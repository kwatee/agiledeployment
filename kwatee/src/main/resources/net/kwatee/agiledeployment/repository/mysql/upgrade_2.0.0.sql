DROP TABLE KWSubscriberVariable;
DROP TABLE kw_subscriberversion_file;
DROP TABLE KWSubscriberVersion;
DROP TABLE KWSubscriberPackage;
DROP TABLE KWSubscriber;

CREATE TABLE KWChannel (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	name VARCHAR(50) NOT NULL,
	description VARCHAR(255) NOT NULL,
	creation_ts BIGINT NOT NULL,
	disable_ts BIGINT
) ENGINE=InnoDB;
ALTER TABLE KWChannel ADD CONSTRAINT kwuc_channel_name UNIQUE (name);
ALTER TABLE KWChannel AUTO_INCREMENT = 1;

CREATE TABLE KWChannelVersion (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	channel_id BIGINT NOT NULL,
	version_id BIGINT NOT NULL,
	publish_ts BIGINT
) ENGINE=InnoDB;
ALTER TABLE KWChannelVersion ADD CONSTRAINT kwfk_channelversion_channel FOREIGN KEY (channel_id) REFERENCES KWChannel(id);
ALTER TABLE KWChannelVersion ADD CONSTRAINT kwfk_channelversion_version FOREIGN KEY (version_id) REFERENCES KWVersion(id);
ALTER TABLE KWChannelVersion AUTO_INCREMENT = 1;

ALTER TABLE KWVersion DROP COLUMN signature;

CREATE TABLE KWVersionVariable (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	version_id BIGINT NOT NULL,
	name VARCHAR(40) NOT NULL,
	default_value VARCHAR(2000) NOT NULL
) ENGINE=InnoDB;
ALTER TABLE KWVersionVariable ADD CONSTRAINT kwfk_versionvariable_version FOREIGN KEY (version_id) REFERENCES KWVersion(id);
ALTER TABLE KWVersionVariable ADD CONSTRAINT kwuc_versionvariable_name UNIQUE (version_id,name);
ALTER TABLE KWVersionVariable AUTO_INCREMENT = 1;

ALTER TABLE KWDeploymentVariable CHANGE value value VARCHAR(2000);

UPDATE KWSystemProperty set value='2.1.0' WHERE name='kwatee_schema_version';