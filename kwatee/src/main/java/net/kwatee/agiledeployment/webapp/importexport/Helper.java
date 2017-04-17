/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.importexport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

class Helper {

	static final String TYPE_H2 = "h2";
	static final String TYPE_MYSQL = "mysql";
	static final String TYPE_POSTGRESQL = "postgresql";

	private final Connection fromConn;
	private final Connection toConn;
	private final String type;

	Helper(Connection fromConn, Connection toConn) {
		this(fromConn, toConn, TYPE_H2);
	}

	Helper(Connection fromConn, Connection toConn, String type) {
		this.fromConn = fromConn;
		this.toConn = toConn;
		this.type = type;
	}

	Connection getToConn() {
		return this.toConn;
	}

	void copyAdmin() throws SQLException {
		copyData("KWSystemProperty");
		copyData("KWApplicationParameter");
	}

	void copyUsers() throws SQLException {
		copyData("KWUser");
		copyData("KWAuthority");
	}

	void copyServers() throws SQLException {
		copyData("KWServer");
		copyData("KWServerProperty");
		copyData("KWServerCredentials");
	}

	void copyArtifacts() throws SQLException {
		copyData("KWPackage");
		copyData("KWVersion");
		copyData("kw_version_platform");
		copyData("KWExecutable");
		copyData("KWVersionVariable");
		copyData("KWRepositoryFile");
		copyData("kw_version_file");
	}

	void copyEnvironments() throws SQLException {
		copyData("KWEnvironment");
		copyData("kw_environment_server");
		copyData("kw_environment_package");
		copyData("KWDeployment");
		copyData("KWDeploymentPackage");
		copyData("kw_dp_file");
		copyData("KWDeploymentVariable");
	}

	void copyData(String tableName) throws SQLException {
		copyData(tableName, "SELECT * FROM " + tableName);
	}

	void copyData(String tableName, String query) throws SQLException {
		try (Statement queryStmt = this.fromConn.createStatement()) {
			try (ResultSet rs = queryStmt.executeQuery(query)) {
				boolean hasAutoIncrement = false;
				ResultSetMetaData md = rs.getMetaData();
				if (md.getColumnCount() == 0) {
					return;
				}
				StringBuilder insertStmt = new StringBuilder("INSERT INTO " + tableName + " (");
				StringBuilder values = new StringBuilder(") values (");
				int colCount = 0;
				for (int i = 1; i <= md.getColumnCount(); i++) {
					if (i != 1) {
						insertStmt.append(',');
						values.append(',');
					}
					String colName = md.getColumnName(i);
					if ("id".equalsIgnoreCase(colName))
						hasAutoIncrement = true;
					insertStmt.append(colName);
					values.append('?');
					colCount++;
				}
				insertStmt.append(values);
				insertStmt.append(')');
				try (PreparedStatement stmt = this.toConn.prepareStatement(insertStmt.toString())) {
					while (rs.next()) {
						for (int i = 1; i <= colCount; i++) {
							//				String s = rs.getString(i);
							//				stmt.setString(i, s);
							Object o = rs.getObject(i);
							stmt.setObject(i, o);
						}
						stmt.execute();
					}
				}
				if (hasAutoIncrement)
					resetAutoIncrement(tableName);
			}
		}
	}

	void resetAutoIncrement(String tableName) throws SQLException {
		if ("KWApplicationParameter".equalsIgnoreCase(tableName))
			return;
		try (Statement stmt = this.toConn.createStatement()) {
			try (ResultSet rs = stmt.executeQuery("SELECT MAX(id) FROM " + tableName)) {
				if (rs.next()) {
					long maxIndex = rs.getLong(1) + 1;
					if (TYPE_MYSQL.equals(this.type)) {
						stmt.execute("ALTER TABLE " + tableName + " AUTO_INCREMENT = " + maxIndex);
					} else if (TYPE_POSTGRESQL.equals(this.type)) {
						stmt.execute("SELECT pg_catalog.setval(pg_get_serial_sequence('" + tableName + "', 'id'), MAX(id)) FROM " + tableName);
					} else { // h2
						stmt.execute("ALTER TABLE " + tableName + " ALTER COLUMN id RESTART WITH " + maxIndex);
					}
				}
			}
		}
	}
	//1. list find all autoincrements/sequences in fromConn
	//2. create them in toConn if they don't exist
	//3. set the proper initial value
	//
	//if (h2) {
	//try (PreparedStatement stmt = toConn.prepareStatement("ALTER TABLE ALTER COLUMN RESTART WITH 1000")) {
	//	stmt.execute();
	//}
	//} else if (mysql) {
	//	
	//} else {
	//	try (PreparedStatement stmt = toConn.prepareStatement("ALTER SEQUENCE SEQ_ID RESTART WITH 1000")) {
	//	
	//}
	//
	//	}
}
