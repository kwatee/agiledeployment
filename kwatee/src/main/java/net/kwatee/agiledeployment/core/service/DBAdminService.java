/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;
import javax.sql.DataSource;

import net.kwatee.agiledeployment.common.Constants;
import net.kwatee.agiledeployment.common.exception.DBAdminErrorException;
import net.kwatee.agiledeployment.common.exception.InternalErrorException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class DBAdminService {

	final static private org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DBAdminService.class);

	@Autowired
	private ResourceLoader resourceLoader;
	@Resource(name = "kwateeDatasource")
	private DataSource kwateeDatasource;
	@Value("${kwatee.jdbc.driver}")
	private String jdbcDriver;
	@Value("${kwatee.jdbc.url}")
	private String jdbcUrl;
	@Value("${kwatee.jdbc.schema}")
	private String jdbcSchema;
	@Value("${kwatee.jdbc.user}")
	private String jdbcUserName;
	@Value("${kwatee.jdbc.password}")
	private String jdbcPassword;

	private String getJdbcDatabasePlatform() {
		return this.jdbcUrl.split("\\:")[1];
	}

	/**
	 * @return
	 */
	private DataSource getAdminDataSource(String dbadminPassword) {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		// dataSource.setDataSourceName("kwadmin");
		dataSource.setUsername(this.jdbcUserName);
		dataSource.setPassword(dbadminPassword);
		dataSource.setDriverClassName(this.jdbcDriver);
		String adminUrl = this.jdbcUrl;
		if (!adminUrl.startsWith("jdbc:h2")) {
			int idx1 = adminUrl.lastIndexOf('/');
			if (idx1 >= 0) {
				int idx2 = adminUrl.indexOf(';');
				if (idx2 > idx1)
					adminUrl = adminUrl.substring(0, idx1) + adminUrl.substring(idx2 - 1);
				else
					adminUrl = adminUrl.substring(0, idx1);
			}
		}
		dataSource.setUrl(adminUrl);
		return dataSource;
	}

	public String getJdbcUserName() {
		return this.jdbcUserName;
	}

	public String getJdbcUrl() {
		return this.jdbcUrl;
	}

	public String checkSchema() {
		try (Connection connection = this.kwateeDatasource.getConnection()) {
			return checkSchema(connection);
		} catch (DBAdminErrorException | SQLException e) {
			return null;
		}
	}

	/**
	 * Checks if the current schema corresponds to the version expected byKwatee
	 * 
	 * @return null if there is no kwate database, an empty string if the schema match. Otherwise the version of the
	 *         installed schema
	 * @throws DBAdminErrorException
	 */
	private String checkSchema(Connection conn) throws DBAdminErrorException {
		String schemaVersion = null;
		try (Statement stmt = conn.createStatement()) {
			try (ResultSet rs = stmt.executeQuery("select value from KWSystemProperty where name='kwatee_schema_version'")) {
				if (rs.next()) {
					schemaVersion = rs.getString(1);
				}
			}
			if (schemaVersion == null) {
				try (ResultSet rs2 = stmt.executeQuery("select schema_version from KWApplicationParameter where id=0")) {
					if (rs2.next())
						schemaVersion = rs2.getString(1);
				}
			}
		} catch (SQLException e) {
			throw new DBAdminErrorException(e);
		}
		if (schemaVersion == null)
			throw new DBAdminErrorException("Corrupt schema");
		int c = compareVersions(schemaVersion, Constants.CURRENT_SCHEMA_VERSION);
		if (c > 0)
			throw new DBAdminErrorException("Database schema is more recent than web application. Impossible to revert, please upgrade web application");
		if (c == 0)
			return StringUtils.EMPTY;
		return schemaVersion;
	}

	/**
	 * Retrieves the kwateeDatasource. If this fails we check the exception to see if the cause was a missing kwatee
	 * database and return false
	 * 
	 * @return true if a kwatee database exists
	 */
	public boolean checkIfDBExists() {
		try (Connection connection = this.kwateeDatasource.getConnection()) {
			/* empty */
		} catch (SQLException e) {
			return false;
		}
		return true;
	}

	/**
	 * Create a new kwatee database and its schema
	 * 
	 * @param kwateePassword
	 * @return false if password did not match
	 * @throws DBAdminErrorException
	 */
	public boolean createSchema(String kwateePassword) throws DBAdminErrorException {
		if (!this.jdbcPassword.equals(kwateePassword)) {
			return false;
		}
		DataSource dataSource = getAdminDataSource(kwateePassword);
		try (Connection connection = dataSource.getConnection()) {
			executeScript(connection, "dropdb", jdbcProperties());
			executeScript(connection, "createdb", jdbcProperties());
		} catch (SQLException e) {
			LOG.error("Drop/create failed", e);
			throw new DBAdminErrorException("Drop/create failed");
		}
		try (Connection connection = this.kwateeDatasource.getConnection()) {
			executeScript(connection, "schema", null);
			executeScript(connection, "factory_settings", null);
		} catch (SQLException e) {
			LOG.error("Create schema failed", e);
			throw new DBAdminErrorException("Create schema failed");
		}
		return true;
	}

	private Properties jdbcProperties() {
		Properties props = new Properties();
		props.put("KWATEE_DB_USER", this.jdbcUserName);
		props.put("KWATEE_DB_PASSWORD", this.jdbcPassword);
		props.put("KWATEE_DB_SCHEMA", this.jdbcSchema);
		return props;
	}

	/**
	 * @param kwateePassword
	 * @return false if password did not match
	 * @throws DBAdminErrorException
	 */
	public boolean upgradeSchema(String kwateePassword) throws DBAdminErrorException {
		if (!this.jdbcPassword.equals(kwateePassword))
			return false;
		//		Connection connection = getConnection(this.kwateeDatasource);
		try (Connection connection = this.kwateeDatasource.getConnection()) {
			String fromVersion = checkSchema(connection);
			connection.setAutoCommit(false);
			try {
				while (StringUtils.isNotEmpty(fromVersion)) {
					executeScript(connection, "upgrade_" + fromVersion, null);
					fromVersion = checkSchema(connection);
				}
			} catch (Exception e) {
				connection.rollback();
				throw e;
			}
			connection.commit();
			return true;
		} catch (SQLException se) {
			throw new DBAdminErrorException(se);
		}
	}

	/**
	 * 
	 * @param connection
	 * @param script
	 * @param properties
	 * @throws DBAdminErrorException
	 */
	private void executeScript(Connection connection, String script, Properties properties) throws DBAdminErrorException {
		LOG.info("Executing script " + script);

		String prefix = "classpath:/net/kwatee/agiledeployment/repository/";
		org.springframework.core.io.Resource resource = this.resourceLoader.getResource(prefix + getJdbcDatabasePlatform() + "/" + script + ".sql");
		if (!resource.exists()) {
			resource = this.resourceLoader.getResource(prefix + script + ".sql");
			if (!resource.exists()) {
				LOG.error("Script " + prefix + script + ".sql not found");
				throw new InternalErrorException("No upgrade script found");
			}
		}

		String sql = null;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
			StringBuilder sb = new StringBuilder();
			String line = null;

			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (!line.isEmpty() && !line.startsWith("--")) {
					sb.append(line);
					sb.append('\n');
				}
			}

			sql = sb.toString();
			if (properties != null) {
				for (Map.Entry<Object, Object> var : properties.entrySet()) {
					sql = sql.replaceAll("@" + var.getKey(), (String) var.getValue());
				}
			}
		} catch (IOException e) {
			throw new InternalErrorException(e);
		}

		if (StringUtils.isNotEmpty(sql)) {
			try (Statement stmt = connection.createStatement()) {
				stmt.setEscapeProcessing(false);
				String[] statements = sql.split(";\n");
				for (String statement : statements) {
					// execute sql
					try {
						stmt.execute(statement);
					} catch (SQLSyntaxErrorException s) {
						LOG.error(statement + "\n==> " + s.getErrorCode() + " - " + s.getMessage());
					} catch (SQLException s) {
						LOG.error(statement + "\n==> " + s.getErrorCode() + " - " + s.getMessage());
					}
				}
			} catch (SQLException e) {
				LOG.error("Script error", e);
				throw new DBAdminErrorException("Creation error");
			}
		}
	}

	/**
	 * 
	 * @param version1
	 * @param version2
	 * @return
	 */
	private int compareVersions(String version1, String version2) {
		String[] version1Split = version1.split("\\.");
		String[] version2Split = version2.split("\\.");
		for (int i = 0; i < version1Split.length; i++) {
			if (i >= version2Split.length)
				return 1;
			int v1 = Integer.parseInt(version1Split[i]);
			int v2 = Integer.parseInt(version2Split[i]);
			if (v1 > v2)
				return 1;
			if (v1 < v2)
				return -1;
		}
		if (version1Split.length < version2Split.length)
			return -1;
		return 0;
	}
}
