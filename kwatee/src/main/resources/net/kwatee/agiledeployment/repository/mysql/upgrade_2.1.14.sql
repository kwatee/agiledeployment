DELETE from KWSystemProperty where name='kwatee_schema_version';

ALTER TABLE KWVersion ADD COLUMN var_prefix_char CHAR(1);

ALTER TABLE KWServerCredentials CHANGE password password VARCHAR(80);
ALTER TABLE KWServerCredentials CHANGE pem pem VARCHAR(4000);
ALTER TABLE KWServerCredentials ADD COLUMN password_prompt SMALLINT DEFAULT 0;

CREATE TABLE KWApplicationParameter (
	id SMALLINT PRIMARY KEY,
	schema_version VARCHAR(20),
	title VARCHAR(80),
	excluded_extensions VARCHAR(2000)
) ENGINE=InnoDB;
INSERT INTO KWApplicationParameter (id, schema_version, title, excluded_extensions) VALUES (0, '2.2.0', '<your organisation name here>', 'zip,bzip,gz,gzip,tar,tgz,bz2,bzip2,png,jpg,jpeg,gif,cab,img,iso,tif,tiff,bmp,exif,svg,pcf,pdf,mp3,mp4,mpeg,avi,aiff,wav,m4a,aac,ico,bin,so,a,o,exe,dll,Class,jar,war,ear,pyc');

CREATE TABLE KWAgent (
	id SMALLINT PRIMARY KEY,
	pos SMALLINT NOT NULL,
	name VARCHAR(50) NOT NULL,
	agent_executable VARCHAR(50),
	path_type VARCHAR(20),
	get_version_command VARCHAR(500),
	check_integrity_command VARCHAR(500),
	get_descriptor_command VARCHAR(500),
	remove_command VARCHAR(500),
	expand_command VARCHAR(500),
	make_executable_command VARCHAR(500),
	exist_file_command VARCHAR(500),
	change_dir_command VARCHAR(500),
	make_dir_command VARCHAR(500),
	delete_file_command VARCHAR(500),
	delete_dir_command VARCHAR(500),
	force_delete_dir_command VARCHAR(500),
	diagnostics_command VARCHAR(500)
) ENGINE=InnoDB;
ALTER TABLE KWAgent ADD CONSTRAINT kwuc_agent_name UNIQUE (name);

INSERT INTO KWAgent (id, pos, name, agent_executable, path_type,
		get_version_command,
		check_integrity_command,
		get_descriptor_command,
		remove_command,
		expand_command,
		make_executable_command,
		exist_file_command,
		change_dir_command,
		make_dir_command,
		delete_file_command,
		delete_dir_command,
		force_delete_dir_command,
		diagnostics_command
	) VALUES (0, 0, 'default', 'kwagent', 'unix',
		'"%{dir:kwatee_root_dir}kwagent" --version',
		'"%{dir:kwatee_root_dir}kwagent" --check_integrity "%{dir:kwatee_package_dir}%{kwateep_package_file}" %{kwateep_signature}',
		'"%{dir:kwatee_root_dir}kwagent" --get_descriptor "%{dir:kwatee_package_dir}%{kwateep_package_file}"',
		'"%{dir:kwatee_root_dir}kwagent" --remove "%{dir:kwatee_package_dir}%{kwateep_package_file}"',
		'"%{dir:kwatee_root_dir}kwagent" --expand "%{dir:kwatee_tmp_dir}%{kwateep_archive_name}" "%{dir:kwatee_package_dir}"',
		'/bin/chmod u+x "%{dir:kwatee_root_dir}kwagent"; echo [kwatee_$?]',
		'/bin/ls -d "%{file:kwateep_file}"; echo [kwatee_$?]',
		'cd "%{file:kwateep_dir}"',
		'/bin/mkdir -p "%{file:kwateep_dir}"; echo [kwatee_$?]',
		'/bin/rm "%{file:kwateep_file}"; echo [kwatee_$?]',
		'cd "%{dir\:kwateep_dir}"..\n/bin/rmdir "%{file\:kwateep_dir}"; echo [kwatee_$?]',
		'cd "%{dir\:kwateep_dir}"..\n/bin/rm -rf "%{file\:kwateep_dir}"; echo [kwatee_$?]',
		'echo "Test platform command availability (support=0 is good)";/bin/ls > /dev/null;echo ls support=$?;cd;echo cd support=$?;/bin/ls /bin/chmod > /dev/null;echo chmod support=$?;/bin/ls /bin/mkdir > /dev/null;echo mkdir support=$?;/bin/ls /bin/rmdir > /dev/null;echo rmdir support=$?;/bin/ls /bin/rm > /dev/null;echo rm support=$?;/bin/ls /bin/cat > /dev/null;echo cat support=$? [kwatee_ok]'
	);
INSERT INTO KWAgent (id, pos, name) VALUES (1, 1, 'linux_x86');
INSERT INTO KWAgent (id, pos, name) VALUES (2, 5, 'macosx_x86');
INSERT INTO KWAgent (id, pos, name) VALUES (3, 3, 'solaris_x86');
INSERT INTO KWAgent (id, pos, name) VALUES (4, 4, 'solaris_sparc');
INSERT INTO KWAgent (id, pos, name, agent_executable, path_type,
		get_version_command,
		check_integrity_command,
		get_descriptor_command,
		remove_command,
		expand_command,
		make_executable_command,
		exist_file_command,
		make_dir_command,
		delete_file_command,
		delete_dir_command,
		force_delete_dir_command,
		diagnostics_command
	) VALUES (5, 7, 'win32', 'kwagent.exe', 'windows',
		'""%{dir:kwatee_root_dir}kwagent.exe" --version"',
		'""%{dir:kwatee_root_dir}kwagent.exe" --check_integrity "%{dir:kwatee_package_dir}%{kwateep_package_file}" %{kwateep_signature}"',
		'""%{dir:kwatee_root_dir}kwagent.exe" --get_descriptor "%{dir:kwatee_package_dir}%{kwateep_package_file}""',
		'""%{dir:kwatee_root_dir}kwagent.exe" --remove "%{dir:kwatee_package_dir}%{kwateep_package_file}""',
		'""%{dir:kwatee_root_dir}kwagent.exe" --expand "%{dir:kwatee_tmp_dir}%{kwateep_archive_name}" "%{dir:kwatee_package_dir}""',
		'',
		'cmd.exe /C "if exist "%{file:kwateep_file}" (@echo [kwatee_ok]) else (@echo [kwatee_error])"',
		'cmd.exe /C "mkdir "%{file:kwateep_dir}" & if exist "%{file:kwateep_dir}" (@echo [kwatee_ok]) else (@echo [kwatee_error])"',
		'cmd.exe /C "del "%{file:kwateep_file}" & if not exist "%{file:kwateep_file}" (@echo [kwatee_ok]) else (@echo [kwatee_error])"',
		'cmd.exe /C "cd /D "%{dir:kwateep_dir}".. & rmdir "%{file:kwateep_dir}" & if not exist "%{file:kwateep_dir}" (@echo [kwatee_ok]) else (@echo [kwatee_error])"',
		'dir-force=cmd.exe /C "cd /D "%{dir:kwateep_dir}".. & rmdir /S /Q "%{file:kwateep_dir}" & if not exist "%{file:kwateep_dir}" (@echo [kwatee_ok]) else (@echo [kwatee_error])"',
		'cmd.exe /C "@echo connection ok [kwatee_ok]"'
	);
INSERT INTO KWAgent (id, pos, name, agent_executable, path_type,
		get_version_command,
		check_integrity_command,
		get_descriptor_command,
		remove_command,
		expand_command,
		make_executable_command,
		exist_file_command,
		make_dir_command,
		delete_file_command,
		delete_dir_command,
		force_delete_dir_command,
		diagnostics_command
	) VALUES (9, 8, 'win64', 'kwagent64.exe', 'windows',
		'""%{dir:kwatee_root_dir}kwagent64.exe" --version"',
		'""%{dir:kwatee_root_dir}kwagent64.exe" --check_integrity "%{dir:kwatee_package_dir}%{kwateep_package_file}" %{kwateep_signature}"',
		'""%{dir:kwatee_root_dir}kwagent64.exe" --get_descriptor "%{dir:kwatee_package_dir}%{kwateep_package_file}""',
		'""%{dir:kwatee_root_dir}kwagent64.exe" --remove "%{dir:kwatee_package_dir}%{kwateep_package_file}""',
		'""%{dir:kwatee_root_dir}kwagent64.exe" --expand "%{dir:kwatee_tmp_dir}%{kwateep_archive_name}" "%{dir:kwatee_package_dir}""',
		'',
		'cmd.exe /C "if exist "%{file:kwateep_file}" (@echo [kwatee_ok]) else (@echo [kwatee_error])"',
		'cmd.exe /C "mkdir "%{file:kwateep_dir}" & if exist "%{file:kwateep_dir}" (@echo [kwatee_ok]) else (@echo [kwatee_error])"',
		'cmd.exe /C "del "%{file:kwateep_file}" & if not exist "%{file:kwateep_file}" (@echo [kwatee_ok]) else (@echo [kwatee_error])"',
		'cmd.exe /C "cd /D "%{dir:kwateep_dir}".. & rmdir "%{file:kwateep_dir}" & if not exist "%{file:kwateep_dir}" (@echo [kwatee_ok]) else (@echo [kwatee_error])"',
		'dir-force=cmd.exe /C "cd /D "%{dir:kwateep_dir}".. & rmdir /S /Q "%{file:kwateep_dir}" & if not exist "%{file:kwateep_dir}" (@echo [kwatee_ok]) else (@echo [kwatee_error])"',
		'cmd.exe /C "@echo connection ok [kwatee_ok]"'
	);
INSERT INTO KWAgent (id, pos, name, agent_executable, path_type,
		check_integrity_command,
		get_descriptor_command,
		remove_command,
		expand_command,
		make_dir_command,
		diagnostics_command
	) VALUES (6, 0, 'cygwin', 'kwagent.exe', 'cygwin',
		'"%{dir:kwatee_root_dir}kwagent.exe" --check_integrity "%{winescdir:kwatee_package_dir}%{kwateep_package_file}" %{kwateep_signature}',
		'"%{dir:kwatee_root_dir}kwagent.exe" --get_descriptor "%{winescdir:kwatee_package_dir}%{kwateep_package_file}"',
		'"%{dir:kwatee_root_dir}kwagent.exe" --remove "%{winescdir:kwatee_package_dir}%{kwateep_package_file}"',
		'"%{dir:kwatee_root_dir}kwagent.exe" --expand "%{winescdir:kwatee_tmp_dir}%{kwateep_archive_name}" "%{winescdir:kwatee_package_dir}"',
		'/bin/mkdir -p "%{winescfile:kwateep_dir}"; echo [kwatee_$?]',
		'echo "Test platform command availability (support=0 is good)";/bin/ls > /dev/null;echo ls support=$?;cd;echo cd support=$?;/bin/ls /bin/mkdir > /dev/null;echo mkdir support=$?;/bin/ls /bin/rmdir > /dev/null;echo rmdir support=$?;/bin/ls /bin/rm > /dev/null;echo rm support=$?;/bin/ls /bin/cat > /dev/null;echo cat support=$? [kwatee_ok]'
	);
INSERT INTO KWAgent (id, pos, name) VALUES (7, 2, 'linux_64');
INSERT INTO KWAgent (id, pos, name) VALUES (8, 6, 'aix');

ALTER TABLE KWVersionVariable ADD COLUMN description VARCHAR(200);
ALTER TABLE KWDeploymentVariable ADD COLUMN description VARCHAR(200);

UPDATE KWDeployment SET name = 'snapshot' where name = 'SKETCH';
