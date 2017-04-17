/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.dto;

public class DBInfoDto {

	private String schemaVersion;
	private String requiredSchemaVersion;
	private String jdbcUserName;
	private String jdbcUrl;

	public String getSchemaVersion() {
		return this.schemaVersion;
	}

	public void setSchemaVersion(String schemaVersion) {
		this.schemaVersion = schemaVersion;
	}

	public String getRequiredSchemaVersion() {
		return this.requiredSchemaVersion;
	}

	public void setRequiredSchemaVersion(String requiredSchemaVersion) {
		this.requiredSchemaVersion = requiredSchemaVersion;
	}

	public String getJdbcUserName() {
		return this.jdbcUserName;
	}

	public void setJdbcUserName(String jdbcUserName) {
		this.jdbcUserName = jdbcUserName;
	}

	public String getJdbcUrl() {
		return this.jdbcUrl;
	}

	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}
}
