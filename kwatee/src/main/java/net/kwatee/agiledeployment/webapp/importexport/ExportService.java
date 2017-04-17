/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.importexport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;

import javax.sql.DataSource;

import net.kwatee.agiledeployment.common.exception.InternalErrorException;
import net.kwatee.agiledeployment.core.service.FileStoreService;
import net.kwatee.agiledeployment.repository.entity.Release;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.h2.engine.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ExportService {

	static public final String BUNDLE_EXTENSION = ".kwatee_bundle";

	final static String EXPORT_USER = "kwdeployer";
	final static String EXPORT_PASSWORD = "deployit!";
	final static String EXPORT_SCHEMA_CREATION1_SQL = "/net/kwatee/agiledeployment/repository/h2/schema.sql";
	final static String EXPORT_SCHEMA_CREATION2_SQL = "/net/kwatee/agiledeployment/repository/h2/export_schema.sql";

	@Autowired
	private DataSource kwateeDatasource;
	@Autowired
	private FileStoreService fileStoreService;

	public File exportBackup() throws IOException {
		File dbFile = File.createTempFile("kwatee_", "backup.h2.db");
		dbFile.deleteOnExit();
		DataSource exportDS = getExportDataSource(dbFile);
		initializeSchema(exportDS, false);
		try (Connection kwateeConn = this.kwateeDatasource.getConnection()) {
			try (Connection exportConn = exportDS.getConnection()) {
				Helper helper = new Helper(kwateeConn, exportConn);
				helper.copyAdmin();
				helper.copyUsers();
				helper.copyServers();
				helper.copyArtifacts();
				helper.copyEnvironments();
				exportConn.commit();
				return dbFile;
			}
		} catch (SQLException e) {
			FileUtils.deleteQuietly(dbFile);
			throw new InternalErrorException(e);
		}
	}

	public File exportDeployment(Release release) throws IOException {
		File dbFile = File.createTempFile("kwatee_", "export.h2.db");
		dbFile.deleteOnExit();
		DataSource exportDS = getExportDataSource(dbFile);
		initializeSchema(exportDS, true);
		try (Connection kwateeConn = this.kwateeDatasource.getConnection()) {
			try (Connection exportConn = exportDS.getConnection()) {
				long releaseId = release.getId();
				Helper helper = new Helper(kwateeConn, exportConn);
				exportReleaseAdmin(helper);
				exportReleaseServers(helper, releaseId);
				exportReleaseArtifacts(helper, releaseId);
				exportReleaseEnvironment(helper, releaseId);
				exportRelease(helper, releaseId);
				copyArtifactFiles(exportConn);
				copyEnvironmentFiles(exportConn);
				exportConn.commit();
				try {
					return gzipPackage(dbFile);
				} finally {
					FileUtils.deleteQuietly(dbFile);
				}
			}
		} catch (IOException | SQLException e) {
			FileUtils.deleteQuietly(dbFile);
			throw new InternalErrorException(e);
		}
	}

	private File gzipPackage(File file) throws IOException {
		File exportPackage = File.createTempFile(file.getName(), ".gz");
		exportPackage.deleteOnExit();
		try (OutputStream out = new GzipCompressorOutputStream(new FileOutputStream(exportPackage))) {
			try (InputStream in = new FileInputStream(file)) {
				IOUtils.copy(in, out);
				out.flush();
			}
		}
		return exportPackage;
	}

	private void exportReleaseAdmin(Helper helper) throws SQLException {
		try (Statement stmt = helper.getToConn().createStatement()) {
			stmt.execute("INSERT INTO KWUser (id, login, password, creation_ts) VALUES (1, 'admin', '1b771698e9d4723bfd35818165db49b7', 0)");
			stmt.execute("INSERT INTO KWAuthority (user_id, authority) VALUES (1, 'ROLE_DEPLOYER')");
			stmt.execute("INSERT INTO KWAuthority (user_id, authority) VALUES (1, 'ROLE_USER')");
		}
		helper.copyData("KWSystemProperty");
		helper.copyData("KWApplicationParameter");
	}

	private void exportReleaseServers(Helper helper, long releaseId) throws SQLException {
		helper.copyData("KWServer", "SELECT DISTINCT s.* FROM KWServer s join KWDeploymentPackage dp on dp.server_id = s.id where dp.deployment_id = " + releaseId);
		helper.copyData("KWServerProperty", "SELECT DISTINCT sp.* FROM KWServerProperty sp join KWDeploymentPackage dp on dp.server_id = sp.server_id where dp.deployment_id = " + releaseId);
		helper.copyData("KWServerCredentials", "SELECT DISTINCT sc.* FROM KWServerCredentials sc join KWDeploymentPackage dp on dp.server_id = sc.server_id where dp.deployment_id = " + releaseId);
	}

	private void exportReleaseArtifacts(Helper helper, long releaseId) throws SQLException {
		helper.copyData("KWPackage", "SELECT DISTINCT p.* FROM KWPackage p join KWDeploymentPackage dp on dp.package_id = p.id where dp.deployment_id = " + releaseId);
		helper.copyData("KWVersion", "SELECT DISTINCT v.* FROM KWVersion v join KWDeploymentPackage dp on dp.version_id = v.id where dp.deployment_id = " + releaseId);
		helper.copyData("kw_version_platform", "SELECT DISTINCT vp.* FROM kw_version_platform vp join KWDeploymentPackage dp on dp.version_id = vp.version_id where dp.deployment_id = " + releaseId);
		helper.copyData("KWExecutable", "SELECT DISTINCT e.* FROM KWExecutable e join KWDeploymentPackage dp on dp.version_id = e.version_id where dp.deployment_id = " + releaseId);
		helper.copyData("KWVersionVariable", "SELECT DISTINCT vv.* FROM KWVersionVariable vv join KWDeploymentPackage dp on dp.version_id = vv.version_id where dp.deployment_id = " + releaseId);
		helper.copyData("KWRepositoryFile", "SELECT DISTINCT r.* FROM KWRepositoryFile r join kw_version_file vf on vf.file_id = r.id join KWDeploymentPackage dp on dp.version_id = vf.version_id where dp.deployment_id = " + releaseId);
		helper.copyData("KWRepositoryFile", "SELECT DISTINCT r.* FROM KWRepositoryFile r join kw_dp_file df on df.file_id = r.id join KWDeploymentPackage dp on dp.id = df.dp_id where dp.deployment_id = " + releaseId);
		helper.copyData("kw_version_file", "SELECT DISTINCT vf.* FROM kw_version_file vf join KWDeploymentPackage dp on dp.version_id = vf.version_id where dp.deployment_id = " + releaseId);
	}

	private void exportReleaseEnvironment(Helper helper, long releaseId) throws SQLException {
		helper.copyData("KWEnvironment", "SELECT DISTINCT e.* FROM KWEnvironment e join KWDeployment d on d.environment_id = e.id where d.id = " + releaseId);
		helper.copyData("kw_environment_server", "SELECT DISTINCT es.* FROM kw_environment_server es join KWDeploymentPackage dp on dp.server_id = es.server_id where dp.deployment_id = " + releaseId);
		helper.copyData("kw_environment_package", "SELECT DISTINCT ep.* FROM kw_environment_package ep join KWDeploymentPackage dp on dp.package_id = ep.package_id where dp.deployment_id = " + releaseId);
	}

	private void exportRelease(Helper helper, long releaseId) throws SQLException {
		helper.copyData("KWDeployment", "SELECT DISTINCT d.* FROM KWDeployment d where d.id = " + releaseId);
		helper.copyData("KWDeploymentPackage", "SELECT DISTINCT dp.* FROM KWDeploymentPackage dp where dp.deployment_id = " + releaseId);
		helper.copyData("kw_dp_file", "SELECT DISTINCT dpf.* FROM kw_dp_file dpf join KWDeploymentPackage dp on dp.id = dpf.dp_id where dp.deployment_id = " + releaseId);
		helper.copyData("KWDeploymentVariable", "SELECT DISTINCT dv.* FROM KWDeploymentVariable dv where dv.deployment_id = " + releaseId);
	}

	/**
	 * @return
	 */
	private DataSource getExportDataSource(File dbFile) {
		String dbFilePath = dbFile.getAbsolutePath().substring(0, dbFile.getAbsolutePath().length() - Constants.SUFFIX_MV_FILE.length());
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setUsername(EXPORT_USER);
		dataSource.setPassword(EXPORT_PASSWORD);
		dataSource.setDriverClassName("org.h2.Driver");
		String url = "jdbc:h2:" + dbFilePath + ";FILE_LOCK=NO";
		dataSource.setUrl(url);
		return dataSource;
	}

	private void initializeSchema(DataSource exportDS, boolean includeRepository) {
		try (Connection exportConn = exportDS.getConnection()) {
			executeScript(exportConn, EXPORT_SCHEMA_CREATION1_SQL);
			if (includeRepository)
				executeScript(exportConn, EXPORT_SCHEMA_CREATION2_SQL);
			exportConn.commit();
		} catch (SQLException e) {
			throw new InternalErrorException(e);
		}

	}

	/**
	 * 
	 * @param script
	 * @param properties
	 */
	private void executeScript(Connection conn, String script) {
		InputStream in = this.getClass().getResourceAsStream(script);
		String sql;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
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
		} catch (IOException e) {
			throw new InternalErrorException(e);
		}

		if (StringUtils.hasLength(sql)) {
			try (Statement stmt = conn.createStatement()) {
				stmt.setEscapeProcessing(false);
				String[] statements = sql.split(";\n");
				for (String statement : statements) {
					// execute sql
					try {
						stmt.execute(statement);
					} catch (SQLSyntaxErrorException s) {
						s.printStackTrace();
					} catch (SQLException s) {
						s.printStackTrace();
					}
				}
			} catch (SQLException e) {
				throw new InternalErrorException(e);
			}
		}
	}

	private void copyArtifactFiles(Connection conn) throws SQLException, IOException {
		try (ResultSet rs = conn.createStatement().executeQuery("SELECT p.name, v.name from KWVersion v join KWPackage p where p.id = v.package_id")) {
			while (rs.next()) {
				String artifactName = rs.getString(1);
				String versionName = rs.getString(2);
				String basePath = this.fileStoreService.getVersionPath(artifactName, versionName);
				copyRepoFiles(basePath, conn);
			}
		}
	}

	private void copyEnvironmentFiles(Connection conn) throws SQLException, IOException {
		try (ResultSet rs = conn.createStatement().executeQuery("SELECT e.name, d.name from KWEnvironment e join KWDeployment d where e.id = d.environment_id")) {
			while (rs.next()) {
				String environmentName = rs.getString(1);
				String releaseName = rs.getString(2);
				String basePath = this.fileStoreService.getReleasePath(environmentName, releaseName);
				copyRepoFiles(basePath, conn);
			}
		}
	}

	private void copyRepoFiles(String basePath, Connection conn) throws IOException, SQLException {
		for (String path : this.fileStoreService.listFilesRecursively(basePath)) {
			long len = this.fileStoreService.getFileLength(basePath, path);
			try (InputStream in = this.fileStoreService.getFileInputStream(basePath, path)) {
				writeBlob(in, len, basePath + "/" + path, conn);
			}
		}
	}

	private void writeBlob(InputStream in, long len, String path, Connection conn) throws IOException, SQLException {
		String sql = "INSERT INTO repository (path, data) VALUES (?, ?)";
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, path);
			stmt.setBinaryStream(2, in, len);
			stmt.execute();
		}
	}

}