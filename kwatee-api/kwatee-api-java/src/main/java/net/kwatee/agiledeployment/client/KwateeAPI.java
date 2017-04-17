/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.client;

import java.io.File;
import java.io.IOException;

public class KwateeAPI {

	private Session session;

	static public KwateeAPI createInstance(String serviceUrl, String login, String password) throws IOException {
		return new KwateeAPI(serviceUrl, login, password);
	}

	static public KwateeAPI createInstance(String serviceUrl, String authToken) throws IOException {
		return new KwateeAPI(serviceUrl, authToken);
	}

	/**
	 * Creates a new api connection instance
	 * 
	 * @param serviceUrl
	 *            url of the form <code>http://<b>user</b>:<b>password</b>@<b>host</b>:<b>port</b>/kwatee</code>
	 * @return an instance
	 * @throws IOException
	 */
	static public KwateeAPI createInstance(String serviceUrl) throws IOException {
		if (serviceUrl == null) {
			throw new IOException("Service url not defined");
		}
		int start = serviceUrl.indexOf("://");
		if (start < 0) {
			throw new IOException("Malformed service url");
		}
		int end = serviceUrl.indexOf('@', start);
		if (end < 0) {
			throw new IOException("Login not defined");
		}
		String userAndPassword = serviceUrl.substring(start + 3, end);
		int sep = userAndPassword.indexOf(':');
		String login = sep < 0 ? userAndPassword : userAndPassword.substring(0, sep);
		String password = sep < 0 ? null : userAndPassword.substring(sep + 1);
		if (password == null) {
			throw new IOException("Password not defined");
		}
		serviceUrl = serviceUrl.substring(0, start + 3) + serviceUrl.substring(end + 1);
		return new KwateeAPI(serviceUrl, login, password);
	}

	private KwateeAPI(String serviceUrl, String login, String password) throws IOException {
		this.session = new Session(serviceUrl);
		this.session.doLogin(login, password);
	}

	private KwateeAPI(String serviceUrl, String authToken) throws IOException {
		this.session = new Session(serviceUrl);
		this.session.setAuthToken(authToken);
	}

	/**
	 * Clean logout from the session
	 */
	public void close() {
		this.session.doLogout();
	}

	/**
	 * @return authentication token
	 */
	public String getAuthToken() {
		return this.session.getAuthToken();
	}

	/**
	 * Retrieves the list of artifacts in the repository
	 * 
	 * @return an array of artifacts in the repository (plain string JSON)
	 * @throws IOException
	 */
	public String getArtifacts() throws IOException {
		return Artifact.getArtifacts(this.session);
	}

	/**
	 * Retrieves an artifact's properties
	 * 
	 * @param artifactName
	 *            the name of an artifact
	 * @return the artifact's properties (plain string JSON)
	 * @throws IOException
	 */
	public String getArtifact(String artifactName) throws IOException {
		return Artifact.getArtifact(this.session, artifactName);
	}

	/**
	 * Updates an artifact's properties
	 * 
	 * @param artifactName
	 *            the name of an artifact
	 * @param artifactProperties
	 *            the artifact's properties to update (plain string JSON)
	 * @throws IOException
	 */
	public void updateArtifact(String artifactName, String artifactProperties) throws IOException {
		Artifact.updateArtifact(this.session, artifactName, artifactProperties);
	}

	/**
	 * Creates a new artifact and optionally set additional properties
	 * 
	 * @param artifactName
	 *            the name of the artifact to create
	 * @param artifactProperties
	 *            the artifact's properties to set (plain string JSON) or <code>null</code>
	 * @throws IOException
	 */
	public void createArtifact(String artifactName, String artifactProperties) throws IOException {
		Artifact.createArtifact(this.session, artifactName, artifactProperties);
	}

	/**
	 * Deletes an artifact
	 * 
	 * @param artifactName
	 *            the name of an artifact
	 * @throws IOException
	 */
	public void deleteArtifact(String artifactName) throws IOException {
		Artifact.deleteArtifact(this.session, artifactName);
	}

	/**
	 * Retrieves the properties of an artifact's version
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @return the version's properties (plain string JSON)
	 * @throws IOException
	 */
	public String getVersion(String artifactName, String versionName) throws IOException {
		return Artifact.getVersion(this.session, artifactName, versionName);
	}

	/**
	 * Updates an artifact's version properties
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param versionProperties
	 *            the version properties to update (plain string JSON)
	 * @throws IOException
	 */
	public void updateVersion(String artifactName, String versionName, String versionProperties) throws IOException {
		Artifact.updateVersion(this.session, artifactName, versionName, versionProperties);
	}

	/**
	 * 
	 * Creates a version in an artifact and optionally set additional properties
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of the version to create
	 * @param versionProperties
	 *            the version properties to set (plain string JSON) or <code>null</code>
	 * @throws IOException
	 */
	public void createVersion(String artifactName, String versionName, String versionProperties) throws IOException {
		Artifact.createVersion(this.session, artifactName, versionName, versionProperties);
	}

	/**
	 * 
	 * Duplicates an existing version and optionally set additional properties
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of the version to create
	 * @param duplicateFrom
	 *            name of the version (within same artifact) to duplicate
	 * @param versionProperties
	 *            the version properties to set (plain string JSON) or <code>null</code>
	 * @throws IOException
	 */
	public void duplicateVersion(String artifactName, String versionName, String duplicateFrom, String versionProperties) throws IOException {
		Artifact.duplicateVersion(this.session, artifactName, versionName, duplicateFrom, versionProperties);
	}

	/**
	 * Deletes an artifact's version
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @throws IOException
	 */
	public void deleteVersion(String artifactName, String versionName) throws IOException {
		Artifact.deleteVersion(this.session, artifactName, versionName);
	}

	/**
	 * Uploads (Http POST) a package to an artifact's version. Replaces whatever existing package there is but retains
	 * previously uploaded overlays unless <code>deleteOverlays=true</code>
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param uploadFile
	 *            the package file to upload
	 * @param deleteOverlays
	 *            if <code>false</code>, preserves existing overlays
	 * @throws IOException
	 */
	public void uploadArtifactPackage(String artifactName, String versionName, File uploadFile, boolean deleteOverlays) throws IOException {
		Artifact.uploadPackage(this.session, artifactName, versionName, uploadFile, deleteOverlays);
	}

	/**
	 * Uploads (from URL) a package to an artifact's version. Replaces whatever existing package there is but retains
	 * previously uploaded overlays unless <code>deleteOverlays=true</code>
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param uploadUrl
	 *            the package url (can be file:///) to upload
	 * @param deleteOverlays
	 *            if <code>false</code>, preserves existing overlays
	 * @throws IOException
	 */
	public void uploadArtifactPackage(String artifactName, String versionName, String uploadUrl, boolean deleteOverlays) throws IOException {
		Artifact.uploadPackage(this.session, artifactName, versionName, uploadUrl, deleteOverlays);
	}

	/**
	 * Retrieves the files present in a version's package at a given relative path
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param path
	 *            the relative path of the directory to list within package (<code>null</code> for root of package
	 *            listing)
	 * @return the files present in a package at a given relative path as an array (plain string JSON)
	 * @throws IOException
	 */
	public String getArtifactPackageFiles(String artifactName, String versionName, String path) throws IOException {
		return Artifact.getPackageFiles(this.session, artifactName, versionName, path);
	}

	/**
	 * Retrieves all the <i>special files</i> (overlays, with variables, with custom flags) within the package
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @return all the <i>special files</i> (overlays, with variables, with custom flags) within the package as an array
	 *         (plain string JSON)
	 * @throws IOException
	 */
	public String getArtifactSpecialFiles(String artifactName, String versionName) throws IOException {
		return Artifact.getSpecialFiles(this.session, artifactName, versionName);
	}

	/**
	 * Update custom flags (ignoreIdenty, dontDelete, ...) of a file within a package
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param path
	 *            the relative path of the file within the package
	 * @param fileProperties
	 *            the file's properties to update (plain string JSON)
	 * @throws IOException
	 */
	public void updateArtifactPackageFileProperties(String artifactName, String versionName, String path, String fileProperties) throws IOException {
		Artifact.updatePackageFileProperties(this.session, artifactName, versionName, path, fileProperties);
	}

	/**
	 * Downloads a file within the package in the specified location
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param path
	 *            the relative path of the file within the package
	 * @param downloadFile
	 *            the location to store the result into
	 * @throws IOException
	 */
	public void downloadArtifactPackageFile(String artifactName, String versionName, String path, File downloadFile) throws IOException {
		Artifact.downloadPackageFile(this.session, artifactName, versionName, path, downloadFile);
	}

	/**
	 * Uploads (Http POST) an overlay at a relative path within the package
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param path
	 *            the relative path of the overlay directory within the package
	 * @param uploadFile
	 *            a file to upload
	 * @throws IOException
	 */
	public void uploadArtifactPackageOverlay(String artifactName, String versionName, String path, File uploadFile) throws IOException {
		Artifact.uploadPackageOverlay(this.session, artifactName, versionName, path, uploadFile);
	}

	/**
	 * Uploads (from URL) an overlay at a relative path within the package
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param path
	 *            the relative path of the overlay directory within the package
	 * @param uploadUrl
	 *            the url to a file (can be file:///) to upload
	 * @throws IOException
	 */
	public void uploadArtifactPackageOverlay(String artifactName, String versionName, String path, String uploadUrl) throws IOException {
		Artifact.uploadPackageOverlay(this.session, artifactName, versionName, path, uploadUrl);
	}

	/**
	 * Deletes an existing version overlay
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param path
	 *            the relative path of the file within the package
	 * @throws IOException
	 */
	public void deleteArtifactPackageOverlay(String artifactName, String versionName, String path) throws IOException {
		Artifact.deletePackageOverlay(this.session, artifactName, versionName, path);
	}

	/**
	 * Retrieves the list of version variables
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @return an array of version variables (plain string JSON)
	 * @throws IOException
	 */
	public String getArtifactVariables(String artifactName, String versionName) throws IOException {
		return Artifact.getVariables(this.session, artifactName, versionName);
	}

	/**
	 * Updates version variables
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param variables
	 *            array of variables (plain string JSON)
	 * @throws IOException
	 */
	public void updateArtifactVariables(String artifactName, String versionName, String variables) throws IOException {
		Artifact.updateVariables(this.session, artifactName, versionName, variables);
	}

	/**
	 * Uploads (Http Post) an artifacts bundle into the repository
	 * 
	 * @param bundleFile
	 *            the artifacts bundle to import
	 * @throws IOException
	 */
	public void importActifactsBundle(File bundleFile) throws IOException {
		Artifact.importBundle(this.session, bundleFile);
	}

	/**
	 * Uploads (from URL) an artifacts bundle into the repository
	 * 
	 * @param bundleUrl
	 *            the url to an artifacts bundle (can be file:///) to import
	 * @throws IOException
	 */
	public void importActifactsBundle(String bundleUrl) throws IOException {
		Artifact.importBundle(this.session, bundleUrl);
	}

	/**
	 * Retrieves the list of servers in the repository
	 * 
	 * @return an array of servers (plain string JSON)
	 * @throws IOException
	 */
	public String getServers() throws IOException {
		return Server.getServers(this.session);
	}

	/**
	 * Retrieves the properties of a server
	 * 
	 * @param serverName
	 *            the name of the server
	 * @return the server's properties (plain string JSON)
	 * @throws IOException
	 */
	public String getServer(String serverName) throws IOException {
		return Server.getServer(this.session, serverName);
	}

	/**
	 * Updates the properties of a server
	 * 
	 * @param serverName
	 *            the name of the server
	 * @param serverProperties
	 *            the server properties to update (plain string JSON)
	 * @throws IOException
	 */
	public void updateServer(String serverName, String serverProperties) throws IOException {
		Server.updateServer(this.session, serverName, serverProperties);
	}

	/**
	 * Creates a new server and optionally sets additional properties
	 * 
	 * @param serverName
	 *            the name of the server
	 * @param serverProperties
	 *            the server properties to set (plain string JSON) or <code>null</code>
	 * @throws IOException
	 */
	public void createServer(String serverName, String serverProperties) throws IOException {
		Server.createServer(this.session, serverName, serverProperties);
	}

	/**
	 * Duplicates an existing server and optionally set additional properties
	 * 
	 * @param serverName
	 *            the name of the server to create
	 * @param duplicateFrom
	 *            the name of the server to duplicate
	 * @param serverProperties
	 *            the properties to set (plain string JSON) or <code>null</code>
	 * @throws IOException
	 */
	public void duplicateServer(String serverName, String duplicateFrom, String serverProperties) throws IOException {
		Server.duplicateServer(this.session, serverName, duplicateFrom, serverProperties);
	}

	/**
	 * Deletes a server
	 * 
	 * @param serverName
	 *            the name of the server
	 * @throws IOException
	 */
	public void deleteServer(String serverName) throws IOException {
		Server.deleteServer(this.session, serverName);
	}

	/**
	 * Tests a connection to the server and returns server capabilities
	 * 
	 * @param serverName
	 *            the name of the server
	 * @param properties
	 *            JSON server properties (typically for credentials)
	 * @return JSON array of server capabilities (plain string JSON)
	 * @throws IOException
	 */
	public String serverDiagnostics(String serverName, String properties) throws IOException {
		return Server.testConnection(this.session, serverName, properties);
	}

	/**
	 * Retrieves the list of environments in the repository
	 * 
	 * @return an array of environments (plain string JSON)
	 * @throws IOException
	 */
	public String getEnvironments() throws IOException {
		return Environment.getEnvironments(this.session);
	}

	/**
	 * Retrieves an environment's properties
	 * 
	 * @param environmentName
	 *            the name of an environment
	 * @return the JSON environment properties (plain string JSON)
	 * @throws IOException
	 */
	public String getEnvironment(String environmentName) throws IOException {
		return Environment.getEnvironment(this.session, environmentName);
	}

	/**
	 * Updates the environment with new properties
	 * 
	 * @param environmentName
	 *            the name of an environment
	 * @param environmentProperties
	 *            the environment properties to update (plain string JSON)
	 * @throws IOException
	 */
	public void updateEnvironment(String environmentName, String environmentProperties) throws IOException {
		Environment.updateEnvironment(this.session, environmentName, environmentProperties);
	}

	/**
	 * Creates a new environment and optionally sets additional properties.
	 * 
	 * @param environmentName
	 *            the name of the environment to create.
	 * @param environmentProperties
	 *            the environment properties to set (plain string JSON) or <code>null</code>
	 * @throws IOException
	 */
	public void createEnvironment(String environmentName, String environmentProperties) throws IOException {
		Environment.createEnvironment(this.session, environmentName, environmentProperties);
	}

	/**
	 * Duplicates an existing environment and optionally set additional properties.
	 * Note that only the snapshot environment is included in a duplicate operation.
	 * 
	 * @param environmentName
	 *            the name of the environment to create
	 * @param duplicateFrom
	 *            name of the environment to duplicate
	 * @param environmentProperties
	 *            the environment properties to set (plain string JSON) or <code>null</code>
	 * @throws IOException
	 */
	public void duplicateEnvironment(String environmentName, String duplicateFrom, String environmentProperties) throws IOException {
		Environment.duplicateEnvironment(this.session, environmentName, duplicateFrom, environmentProperties);
	}

	/**
	 * Deletes an environment
	 * 
	 * @param environmentName
	 *            the name of an environment
	 * @throws IOException
	 */
	public void deleteEnvironment(String environmentName) throws IOException {
		Environment.deleteEnvironment(this.session, environmentName);
	}

	/**
	 * Retrieve an environment's release properties
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @return the release properties (plain string JSON)
	 * @throws IOException
	 */
	public String getRelease(String environmentName, String releaseName) throws IOException {
		return Environment.getRelease(this.session, environmentName, releaseName);
	}

	/**
	 * Updates a release with new properties
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @param releaseProperties
	 *            the release properties to update (plain string JSON)
	 * @throws IOException
	 */
	public void updateRelease(String environmentName, String releaseName, String releaseProperties) throws IOException {
		Environment.updateRelease(this.session, environmentName, releaseName, releaseProperties);
	}

	/**
	 * Tags a release and optionally sets additional properties (e.g. description)
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of tagged release to create
	 * @param releaseProperties
	 *            the release properties to set (plain string JSON)
	 * @throws IOException
	 */
	public void tagRelease(String environmentName, String releaseName, String releaseProperties) throws IOException {
		Environment.tagRelease(this.session, environmentName, releaseName, releaseProperties);
	}

	/**
	 * Reedits a previously tagged release
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of existing tagged release
	 * @throws IOException
	 */
	public void reeditRelease(String environmentName, String releaseName) throws IOException {
		Environment.reeditRelease(this.session, environmentName, releaseName);
	}

	/**
	 * Deletes a release
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @throws IOException
	 */
	public void deleteRelease(String environmentName, String releaseName) throws IOException {
		Environment.deleteRelease(this.session, environmentName, releaseName);
	}

	/**
	 * Retrieves the effective release artifacts (resolves defaultVersions/serverVersions)
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @return an array of release artifacts (plain string JSON)
	 * @throws IOException
	 */
	public String getEffectiveReleaseArtifacts(String environmentName, String releaseName) throws IOException {
		return Environment.getEffectiveReleaseArtifacts(this.session, environmentName, releaseName);
	}

	/**
	 * Sets the active version (default of server-specific) of a release artifact
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @param artifactName
	 *            the name of an artifact
	 * @param versionName
	 *            the active version to be set
	 * @param serverName
	 *            <code>null</code> for default active version
	 * @throws IOException
	 */
	public void setReleaseArtifactActiveVersion(String environmentName, String releaseName, String artifactName, String versionName, String serverName) throws IOException {
		Environment.setArtifactActiveVersion(this.session, environmentName, releaseName, artifactName, versionName, serverName);
	}

	/**
	 * Retrieves the files present in an release artifact package at a given relative path with the package
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @param artifactName
	 *            the name of an artifact
	 * @param serverName
	 *            <code>null</code> for list files in default package
	 * @param path
	 *            the relative path of the directory to list within package (<code>null</code> for full recursive
	 *            listing)
	 * @return the files present in a package at a given relative path as a JSON array (plain string JSON)
	 * @throws IOException
	 */
	public String getReleasePackageFiles(String environmentName, String releaseName, String artifactName, String serverName, String path) throws IOException {
		return Environment.getPackageFiles(this.session, environmentName, releaseName, artifactName, serverName, path);
	}

	/**
	 * Retrieves all the <i>special files</i> (overlays, with variables, with custom flags) within the release artifact
	 * package
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @param artifactName
	 *            the name of an artifact
	 * @param serverName
	 *            <code>null</code> for list files in default package
	 * @return all the <i>special files</i> (overlays, with variables, with custom flags) within the package as an array
	 *         (plain string JSON)
	 * @throws IOException
	 */
	public String getReleaseSpecialFiles(String environmentName, String releaseName, String artifactName, String serverName) throws IOException {
		return Environment.getSpecialFiles(this.session, environmentName, releaseName, artifactName, serverName);
	}

	/**
	 * Update custom flags (ignoreIdenty, dontDelete, ...) of a file within a release artifact package
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @param artifactName
	 *            the name of an artifact
	 * @param serverName
	 *            <code>null</code> for list files in default package
	 * @param path
	 *            the relative path of the file within the package
	 * @param fileProperties
	 *            the file properties to update (plain string JSON)
	 * @throws IOException
	 */
	public void updateReleasePackageFileProperties(String environmentName, String releaseName, String artifactName, String serverName, String path, String fileProperties) throws IOException {
		Environment.updatePackageFileProperties(this.session, environmentName, releaseName, artifactName, serverName, path, fileProperties);
	}

	/**
	 * Downloads a file within the release artifact package in the specified location
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @param artifactName
	 *            the name of an artifact
	 * @param serverName
	 *            <code>null</code> for list files in default package
	 * @param path
	 *            the relative path of the file within the package
	 * @param downloadFile
	 *            the location to store the result into
	 * @throws IOException
	 */
	public void downloadReleasePackageFile(String environmentName, String releaseName, String artifactName, String serverName, String path, File downloadFile) throws IOException {
		Environment.downloadPackageFile(this.session, environmentName, releaseName, artifactName, serverName, path, downloadFile);
	}

	/**
	 * Uploads (Http Post) an overlay at a relative path within the release artifact package
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @param artifactName
	 *            the name of an artifact
	 * @param serverName
	 *            <code>null</code> for list files in default package
	 * @param path
	 *            the relative path of the overlay directory within the package
	 * @param uploadFile
	 *            the file to upload
	 * @throws IOException
	 */
	public void uploadReleasePackageOverlay(String environmentName, String releaseName, String artifactName, String serverName, String path, File uploadFile) throws IOException {
		Environment.uploadPackageOverlay(this.session, environmentName, releaseName, artifactName, serverName, path, uploadFile);
	}

	/**
	 * Uploads (from URL) an overlay at a relative path within the release artifact package
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @param artifactName
	 *            the name of an artifact
	 * @param serverName
	 *            <code>null</code> for list files in default package
	 * @param path
	 *            the relative path of the overlay directory within the package
	 * @param uploadUrl
	 *            the url to the file (can be file:///) to upload
	 * @throws IOException
	 */
	public void uploadReleasePackageOverlay(String environmentName, String releaseName, String artifactName, String serverName, String path, String uploadUrl) throws IOException {
		Environment.uploadPackageOverlay(this.session, environmentName, releaseName, artifactName, serverName, path, uploadUrl);
	}

	/**
	 * Deletes an existing release artifact overlay
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @param artifactName
	 *            the name of an artifact
	 * @param serverName
	 *            <code>null</code> for list files in default package
	 * @param path
	 *            the relative path of the file within the package
	 * @throws IOException
	 */
	public void deleteReleasePackageOverlay(String environmentName, String releaseName, String artifactName, String serverName, String path) throws IOException {
		Environment.deletePackageOverlay(this.session, environmentName, releaseName, artifactName, serverName, path);
	}

	/**
	 * Retrieves the list of release variables
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @return an array of variables (plain string JSON)
	 * @throws IOException
	 */
	public String getReleaseVariables(String environmentName, String releaseName) throws IOException {
		return Environment.getVariables(this.session, environmentName, releaseName);
	}

	/**
	 * Sets the release variables
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @param variables
	 *            an array of variables (plain string JSON)
	 * @throws IOException
	 */
	public void setReleaseVariables(String environmentName, String releaseName, String variables) throws IOException {
		Environment.setVariables(this.session, environmentName, releaseName, variables);
	}

	/**
	 * Retrieves the list of deployments
	 * 
	 * @return an array of deployments (plain string JSON)
	 * @throws IOException
	 */
	public String getDeployments() throws IOException {
		return Deployment.getDeployments(this.session);
	}

	/**
	 * Retrieves the deployment properties
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @return the deployment's properties (plain string JSON)
	 * @throws IOException
	 */
	public String getDeployment(String environmentName, String releaseName) throws IOException {
		return Deployment.getDeployment(this.session, environmentName, releaseName);
	}

	/**
	 * Downloads a self-contained command-line installer (to install one server at a time) in the specified location
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @param downloadFile
	 *            the location to store the result into
	 * @throws IOException
	 */
	public void downloadLightweightInstaller(String environmentName, String releaseName, File downloadFile) throws IOException {
		Deployment.downloadLightweightInstaller(this.session, environmentName, releaseName, downloadFile);
	}

	/**
	 * Downloads an installer in the specified location
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @param downloadFile
	 *            the location to store the result into
	 * @throws IOException
	 */
	public void downloadInstaller(String environmentName, String releaseName, File downloadFile) throws IOException {
		Deployment.downloadInstaller(this.session, environmentName, releaseName, downloadFile);
	}

	/**
	 * Initiates a deploy operation
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @param serverName
	 *            the name of a server or <code>null</code> for all servers in environment
	 * @param artifactName
	 *            the name of an artifact or <code>null</code> for all artifacts within server/environment
	 * @return a deployment operation reference
	 * @throws IOException
	 */
	public String manageDeploy(String environmentName, String releaseName, String serverName, String artifactName) throws IOException {
		return Deployment.manage(this.session, environmentName, releaseName, serverName, artifactName, "deploy", false);
	}

	/**
	 * Initiates a undeploy operation
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @param serverName
	 *            the name of a server or <code>null</code> for all servers in environment
	 * @param artifactName
	 *            the name of an artifact or <code>null</code> for all artifacts within server/environment
	 * @param forceUndeploy
	 *            if <code>true</code> ignores errors and removes files
	 * @return a deployment operation reference
	 * @throws IOException
	 */
	public String manageUndeploy(String environmentName, String releaseName, String serverName, String artifactName, boolean forceUndeploy) throws IOException {
		return Deployment.manage(this.session, environmentName, releaseName, serverName, artifactName, "undeploy", forceUndeploy);
	}

	/**
	 * Initiates a check integrity operation
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @param serverName
	 *            the name of a server or <code>null</code> for all servers in environment
	 * @param artifactName
	 *            the name of an artifact or <code>null</code> for all artifacts within server/environment
	 * @return a deployment operation reference
	 * @throws IOException
	 */
	public String manageCheckIntegrity(String environmentName, String releaseName, String serverName, String artifactName) throws IOException {
		return Deployment.manage(this.session, environmentName, releaseName, serverName, artifactName, "check", false);
	}

	/**
	 * Initiates a start executables operation
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @param serverName
	 *            the name of a server or <code>null</code> for all servers in environment
	 * @param artifactName
	 *            the name of an artifact or <code>null</code> for all artifacts within server/environment
	 * @return a deployment operation reference
	 * @throws IOException
	 */
	public String manageStart(String environmentName, String releaseName, String serverName, String artifactName) throws IOException {
		return Deployment.manage(this.session, environmentName, releaseName, serverName, artifactName, "start", false);
	}

	/**
	 * Initiates a stop executables operation
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @param serverName
	 *            the name of a server or <code>null</code> for all servers in environment
	 * @param artifactName
	 *            the name of an artifact or <code>null</code> for all artifacts within server/environment
	 * @return a deployment operation reference
	 * @throws IOException
	 */
	public String manageStop(String environmentName, String releaseName, String serverName, String artifactName) throws IOException {
		return Deployment.manage(this.session, environmentName, releaseName, serverName, artifactName, "stop", false);
	}

	/**
	 * Initiates an executables status operation
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release or <code>null</code> for snapshot release
	 * @param serverName
	 *            the name of a server or <code>null</code> for all servers in environment
	 * @param artifactName
	 *            the name of an artifact or <code>null</code> for all artifacts within server/environment
	 * @return a deployment operation reference
	 * @throws IOException
	 */
	public String manageStatus(String environmentName, String releaseName, String serverName, String artifactName) throws IOException {
		return Deployment.manage(this.session, environmentName, releaseName, serverName, artifactName, "status", false);
	}

	/**
	 * Retrieves an ongoing deployment operation
	 * 
	 * @return a deployment operation reference or <code>null</code> if no operation is in progress
	 * @throws IOException
	 */
	public String getOngoingOperation() throws IOException {
		return Deployment.getOngoingOperation(this.session);
	}

	/**
	 * @param ref
	 *            the reference of an ongoing deployment operation
	 * @return <code>200</code> if successfully completed, <code>204</code> if the operation is in progress,
	 *         <code>410</code> if gone and <code>400</code> in case of error
	 * @throws IOException
	 */
	public int getOperationStatus(String ref) throws IOException {
		return Deployment.getOperationStatus(this.session, ref);
	}

	/**
	 * Retrieves the progress of a deployment operation
	 * 
	 * @param ref
	 *            the reference of an ongoing deployment operation
	 * @return the properties of the deployment operation (plain string JSON)
	 * @throws IOException
	 */
	public String getOperationProgress(String ref) throws IOException {
		return Deployment.getOperationProgress(this.session, ref);
	}

	/**
	 * Retrieves the details of a deployment operation for a given server and or artifact
	 * 
	 * @param ref
	 *            the reference of an ongoing deployment operation
	 * @param serverName
	 *            the name of the server for which info is requested
	 * @param artifactName
	 *            the name of an artifact for which info is requested. If <code>null</code>, server-wide information is
	 *            returned
	 * @return the operation details (plain string JSON)
	 * @throws IOException
	 */
	public String getProgressMessages(String ref, String serverName, String artifactName) throws IOException {
		return Deployment.getProgressMessages(this.session, ref, serverName, artifactName);
	}

	/**
	 * Cancels an ongoing operation
	 * 
	 * @param ref
	 *            the reference of an ongoing deployment operation
	 * @param dontClear
	 *            if <code>true</code> the status is kept active
	 * @throws IOException
	 */
	public void manageCancel(String ref, boolean dontClear) throws IOException {
		Deployment.manageCancel(this.session, ref, dontClear);
	}

	/**
	 * Supply server credentials without storing them in kwatee
	 * 
	 * @param environmentName
	 *            the name of an environment
	 * @param serverName
	 *            the name of the server
	 * @param sameForAllServers
	 *            if <code>true</code>, these same credentials will be applied to all servers that need it.
	 * @param credentials
	 *            the optional credentials (plain string JSON or <code>null</code>)
	 * @throws IOException
	 */
	public void sendCredentials(String environmentName, String serverName, boolean sameForAllServers, String credentials) throws IOException {
		Deployment.sendCredentials(this.session, environmentName, serverName, sameForAllServers, credentials);
	}

	/**
	 * Retrieves kwatee information (version, ...)
	 * 
	 * @return kwatee properties (plain string JSON)
	 * @throws IOException
	 */
	public String getInfoContext() throws IOException {
		return Misc.getInfoContext(this.session);
	}

	/**
	 * Retrieves the available platforms (operating systems)
	 * 
	 * @return an array of platforms (plain string JSON)
	 * @throws IOException
	 */
	public String getInfoPlatforms() throws IOException {
		return Misc.getInfoPlatforms(this.session);
	}

	/**
	 * Retrieves the available conduit types (ssh, ftp, ...)
	 * 
	 * @return an array of conduit types (plain string JSON)
	 * @throws IOException
	 */
	public String getInfoConduitTypes() throws IOException {
		return Misc.getInfoConduitTypes(this.session);
	}

	/**
	 * Retrieves the available server pool types (ec2, ...)
	 * 
	 * @return an array of server pool types (plain string JSON)
	 * @throws IOException
	 */
	public String getInfoServerPoolTypes() throws IOException {
		return Misc.getInfoServerPoolTypes(this.session);
	}

}
