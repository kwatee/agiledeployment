
INSERT INTO KWUser (login, password, description, email, creation_ts) VALUES ('admin', '1b771698e9d4723bfd35818165db49b7', 'default administrator', '', 0);
INSERT INTO KWAuthority (user_id, authority) VALUES (1, 'ROLE_USER');
INSERT INTO KWAuthority (user_id, authority) VALUES (1, 'ROLE_DEPLOYER');
INSERT INTO KWAuthority (user_id, authority) VALUES (1, 'ROLE_SRM');
INSERT INTO KWAuthority (user_id, authority) VALUES (1, 'ROLE_ADMIN');
INSERT INTO KWAuthority (user_id, authority) VALUES (1, 'ROLE_SUPER');

INSERT INTO KWSystemProperty (name, value, description, pos) VALUES ('kwatee_root_dir', '/var/tmp/kwatee', 'Kwatee agent and metadata directory', 100);
INSERT INTO KWSystemProperty (name, value, description, pos) VALUES ('kwatee_deployment_dir', '', 'Default deployment directory', 201);
INSERT INTO KWSystemProperty (name, value, description, pos) VALUES ('kwatee_package_dir', '%{kwatee_deployment_dir}/%{kwatee_package_name}', 'Default deployment artifact directory', 202);

INSERT INTO KWApplicationParameter (id, schema_version, title, excluded_extensions) VALUES (0, '4.0.2', '<your organisation name here>', 'zip,bzip,gz,gzip,tar,tgz,bz2,bzip2,png,jpg,jpeg,gif,cab,img,iso,tif,tiff,bmp,exif,svg,pcf,pdf,mp3,mp4,mpeg,avi,aiff,wav,m4a,aac,ico,bin,so,a,o,exe,com,dll,class,jar,war,ear,pyc');