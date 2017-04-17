ALTER TABLE KWAgent ADD COLUMN agent_command VARCHAR(500);
ALTER TABLE KWAgent DROP COLUMN get_version_command;
ALTER TABLE KWAgent DROP COLUMN check_integrity_command;
ALTER TABLE KWAgent DROP COLUMN get_descriptor_command;
ALTER TABLE KWAgent DROP COLUMN remove_command;
ALTER TABLE KWAgent DROP COLUMN expand_command;
ALTER TABLE KWAgent DROP COLUMN exist_file_command;
ALTER TABLE KWAgent DROP COLUMN force_delete_dir_command;
ALTER TABLE KWAgent DROP COLUMN delete_dir_command;
ALTER TABLE KWAgent DROP COLUMN delete_file_command;
	
UPDATE KWAgent SET
  agent_command = '"%{dir:kwatee_root_dir}%{kwateep_agent_name}" %{kwateep_agent_command}'
WHERE id = 0;

UPDATE KWAgent SET
  agent_command = '""%{dir:kwatee_root_dir}%{kwateep_agent_name}" %{kwateep_agent_command}"'
WHERE id = 5;

UPDATE KWAgent SET
  agent_command = '""%{dir:kwatee_root_dir}%{kwateep_agent_name}" %{kwateep_agent_command}"'
WHERE id = 9;

UPDATE KWApplicationParameter set schema_version='2.3.0' WHERE id = 0;
