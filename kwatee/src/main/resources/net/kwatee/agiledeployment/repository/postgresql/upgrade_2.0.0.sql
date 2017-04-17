DROP TABLE KWSubscriberVariable;
DROP SEQUENCE KWSubscriberVariable_id_seq;
DROP TABLE kw_subscriberversion_file;
DROP TABLE KWSubscriberVersion;
DROP SEQUENCE KWSubscriberVersion_id_seq;
DROP TABLE KWSubscriberPackage;
DROP SEQUENCE KWSubscriberPackage_id_seq;
DROP TABLE KWSubscriber;
DROP SEQUENCE KWSubscriber_id_seq;

CREATE SEQUENCE KWChannel_id_seq START WITH 1 MINVALUE 0;
CREATE TABLE KWChannel (
	id BIGINT PRIMARY KEY DEFAULT NEXTVAL('KWChannel_id_seq'),
	name VARCHAR(50) NOT NULL,
	description VARCHAR(255) NOT NULL,
	creation_ts BIGINT NOT NULL,
	disable_ts BIGINT
);
ALTER TABLE KWChannel ADD CONSTRAINT kwuc_channel_name UNIQUE (name);

CREATE SEQUENCE KWChannelVersion_id_seq START WITH 1 MINVALUE 0;
CREATE TABLE KWChannelVersion (
	id BIGINT PRIMARY KEY DEFAULT NEXTVAL('KWChannelVersion_id_seq'),
	channel_id BIGINT NOT NULL,
	version_id BIGINT NOT NULL,
	publish_ts BIGINT
);
ALTER TABLE KWChannelVersion ADD CONSTRAINT kwfk_channelversion_channel FOREIGN KEY (channel_id) REFERENCES KWChannel(id);
ALTER TABLE KWChannelVersion ADD CONSTRAINT kwfk_channelversion_version FOREIGN KEY (version_id) REFERENCES KWVersion(id);

ALTER TABLE KWVersion DROP COLUMN signature;

CREATE SEQUENCE KWVersionVariable_id_seq START WITH 1 MINVALUE 0;
CREATE TABLE KWVersionVariable (
	id BIGINT PRIMARY KEY DEFAULT NEXTVAL('KWVersionVariable_id_seq'),
	version_id BIGINT NOT NULL,
	name VARCHAR(40) NOT NULL,
	default_value VARCHAR(2000) NOT NULL
);
ALTER TABLE KWVersionVariable ADD CONSTRAINT kwfk_versionvariable_version FOREIGN KEY (version_id) REFERENCES KWVersion(id);
ALTER TABLE KWVersionVariable ADD CONSTRAINT kwuc_versionvariable_name UNIQUE (version_id,name);

ALTER TABLE KWDeploymentVariable ALTER COLUMN value type VARCHAR(2000);

UPDATE KWSystemProperty set value='2.1.0' WHERE name='kwatee_schema_version';