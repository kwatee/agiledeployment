/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.deploy.packager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.common.utils.XmlBuffer;
import net.kwatee.agiledeployment.core.deploy.Deployment;
import net.kwatee.agiledeployment.core.repository.DeployStreamProvider;
import net.kwatee.agiledeployment.core.variable.VariableService;
import net.kwatee.agiledeployment.core.variable.impl.DeploymentVariableResolver;
import net.kwatee.agiledeployment.repository.dto.ArtifactVersionDto;
import net.kwatee.agiledeployment.repository.dto.FileDto;
import net.kwatee.agiledeployment.repository.dto.PermissionDto;
import net.kwatee.agiledeployment.repository.dto.ServerDto;
import net.kwatee.agiledeployment.repository.dto.VersionDto;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

public class TarBuilder {

	final private TarArchiveOutputStream stream;
	final private String rootDir;
	private VariableService variableService;
	private DeployStreamProvider deployStreamProvider;
	private File tempArchive;
	private String serverDir;

	TarBuilder(TarArchiveOutputStream stream, String rootDir, VariableService variableService, DeployStreamProvider deployStreamProvider) throws IOException {
		this.stream = stream;
		this.rootDir = rootDir;
		this.variableService = variableService;
		this.deployStreamProvider = deployStreamProvider;
	}

	TarBuilder(VariableService variableService, DeployStreamProvider deployStreamProvider) throws IOException {
		this.tempArchive = File.createTempFile("kwatee", ".bundle");
		this.tempArchive.deleteOnExit();
		try {
			this.stream = new TarArchiveOutputStream(new FileOutputStream(tempArchive));
			this.stream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
		} catch (IOException e) {
			this.tempArchive.delete();
			throw e;
		}
		this.rootDir = StringUtils.EMPTY;
		this.variableService = variableService;
		this.deployStreamProvider = deployStreamProvider;
	}

	File closeBundle() throws IOException {
		if (this.tempArchive != null) {
			IOUtils.closeQuietly(this.stream);
			File archiveFile = this.tempArchive;
			this.tempArchive = null;
			return archiveFile;
		}
		return null;
	}

	void addArtifact(
			String artifactVersionName,
			InputStream packageStream,
			Collection<FileDto> overlays,
			Collection<FileDto> filesToPackage
			) throws IOException {
		File archiveFile = File.createTempFile("kwatee", "artifact");
		TarArchiveOutputStream tos = null;
		TarArchiveInputStream tis = null;
		try {
			if (packageStream != null) {
				tos = new TarArchiveOutputStream(new GzipCompressorOutputStream(new FileOutputStream(archiveFile)));
				tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
				tis = new TarArchiveInputStream(packageStream);

				/*
				 * Copy all original package files that match list elements
				 * and that are not templates / overlays
				 */
				TarArchiveEntry tarEntry;
				while ((tarEntry = tis.getNextTarEntry()) != null) {
					boolean includeEntry = false;
					if (filesToPackage == null) {
						includeEntry = true;
					} else if (!tarEntry.isDirectory()) {
						for (FileDto f : filesToPackage) {
							if (f.getPath().equals(tarEntry.getName())) {
								if (!BooleanUtils.isTrue(f.isDir())) {
									includeEntry = true;
									break;
								}
							}
						}
					}
					if (includeEntry) {
						tarEntry.setName(this.rootDir + tarEntry.getName());
						tos.putArchiveEntry(tarEntry);
						if (!tarEntry.isDirectory()) {
							IOUtils.copy(tis, tos);
						}
						tos.closeArchiveEntry();
					}
				}
				/*
				 * Recreate directories just in case
				 */
				if (filesToPackage != null) {
					for (FileDto f : filesToPackage) {
						if (BooleanUtils.isTrue(f.isDir())) {
							tarEntry = new TarArchiveEntry(this.rootDir + f.getPath());
							tarEntry.setModTime(new Date());
							tarEntry.setSize(0);
							tos.putArchiveEntry(tarEntry);
							tos.closeArchiveEntry();
						}
					}
				}
				IOUtils.closeQuietly(tos);
				tos = null;
			}
			TarArchiveEntry tarEntry = new TarArchiveEntry(this.rootDir + artifactVersionName + ".artifact");
			tarEntry.setSize(archiveFile.length());
			this.stream.putArchiveEntry(tarEntry);
			InputStream in = new FileInputStream(archiveFile);
			try {
				IOUtils.copy(in, this.stream);
				this.stream.closeArchiveEntry();
			} finally {
				IOUtils.closeQuietly(in);
			}
		} finally {
			IOUtils.closeQuietly(tis);
			IOUtils.closeQuietly(tos);
			archiveFile.delete();
		}
	}

	void openServerDeployment(String serverName, String descriptorXml) throws IOException {
		this.serverDir = serverName + ".server/";
		if (descriptorXml != null) {
			TarArchiveEntry tarEntry = new TarArchiveEntry(this.rootDir + this.serverDir + "DEPLOYMENT_DESCRIPTOR");
			byte[] xmlBytes = descriptorXml.getBytes();
			tarEntry.setSize(xmlBytes.length);
			this.stream.putArchiveEntry(tarEntry);
			this.stream.write(xmlBytes);
			this.stream.closeArchiveEntry();
		}
	}

	void closeServerDeployment() throws IOException {
		this.serverDir = null;
	}

	void addArtifactOverlays(Deployment deployment, String serverName, ArtifactVersionDto artifactVersion) throws IOException {
		String environmentName = deployment.getEnvironmentName();
		String releaseName = deployment.getName();
		String versionName = artifactVersion.getVersion();
		String artifactName = artifactVersion.getArtifact();
		String path = this.rootDir + this.serverDir + artifactName + "/";
		for (FileDto f : artifactVersion.getCustomFiles()) {
			TarArchiveEntry tarEntry = new TarArchiveEntry(path + f.getPath());
			tarEntry.setModTime(new Date());
			PermissionDto permissions = f.getProperties() == null ? null : f.getProperties().getPermissions();
			if (permissions != null && StringUtils.isNotEmpty(permissions.getFileOwner()))
				tarEntry.setUserName(permissions.getFileOwner());
			if (permissions != null && StringUtils.isNotEmpty(permissions.getFileGroup()))
				tarEntry.setGroupName(permissions.getFileGroup());
			if (BooleanUtils.isTrue(f.isDir())) {
				if (permissions != null && StringUtils.isNotEmpty(permissions.getDirMode()))
					tarEntry.setMode(Integer.parseInt(permissions.getDirMode(), 8));
				tarEntry.setSize(0);
				this.stream.putArchiveEntry(tarEntry);
			} else {
				if (permissions != null && StringUtils.isNotEmpty(permissions.getFileMode()))
					tarEntry.setMode(Integer.parseInt(permissions.getFileMode(), 8));
				InputStream in = this.deployStreamProvider.getOverlayFileInputStream(f.getPath(), environmentName, releaseName, serverName, artifactName, versionName);
				try {
					tarEntry.setSize(f.getSize());
					// Create a buffered input stream out of the file
					// we're trying to add into the Tar
					this.stream.putArchiveEntry(tarEntry);
					IOUtils.copy(in, this.stream);
				} finally {
					IOUtils.closeQuietly(in);
				}
			}
			this.stream.closeArchiveEntry();
		}

	}

	void addArtifactOverlays(Deployment deployment, ServerDto server, ArtifactVersionDto artifactVersion, String descriptorXml, Collection<FileDto> filesToPackage) throws IOException, MissingVariableException {
		String artifactName = artifactVersion.getArtifact();
		Set<FileDto> fileSet = new HashSet<FileDto>(filesToPackage);
		String descriptorPath = this.rootDir + this.serverDir + artifactName + "/" + artifactName + ".artifact";
		TarArchiveEntry tarEntry = new TarArchiveEntry(descriptorPath);
		byte[] xmlBytes = descriptorXml.getBytes();
		tarEntry.setSize(xmlBytes.length);
		this.stream.putArchiveEntry(tarEntry);
		this.stream.write(xmlBytes);
		this.stream.closeArchiveEntry();

		VersionDto version = deployment.findVersion(artifactVersion);
		for (FileDto f : artifactVersion.getCustomFiles()) {
			addPackageFile(f, deployment, server, version, fileSet);
		}

		if (version.getPackageInfo() != null) {
			for (FileDto f : version.getPackageInfo().getFiles()) {
				if (StringUtils.isNotEmpty(f.getVariables()))
					addPackageFile(f, deployment, server, version, fileSet);
			}
		}

	}

	void addArtifactTemplates(Deployment deployment, ServerDto server, ArtifactVersionDto artifactVersion, Collection<FileDto> filesToPackage) throws IOException {
		String environmentName = deployment.getEnvironmentName();
		String releaseName = deployment.getName();
		String artifactName = artifactVersion.getArtifact();
		String versionName = artifactVersion.getVersion();

		String prefixPath = this.rootDir + this.serverDir + artifactName + "/templates/";
		writeArtifactVariables(deployment, artifactVersion, prefixPath);
		for (FileDto f : filesToPackage) {
			if (StringUtils.isNotEmpty(f.getVariables())) {
				PermissionDto permissions = f.getProperties() == null ? null : f.getProperties().getPermissions();
				TarArchiveEntry tarEntry = new TarArchiveEntry(prefixPath + f.getPath());
				tarEntry.setModTime(new Date());
				tarEntry.setSize(f.getSize());
				if (permissions != null && StringUtils.isNotEmpty(permissions.getFileOwner()))
					tarEntry.setUserName(permissions.getFileOwner());
				if (permissions != null && StringUtils.isNotEmpty(permissions.getFileGroup()))
					tarEntry.setGroupName(permissions.getFileGroup());
				if (permissions != null && StringUtils.isNotEmpty(permissions.getFileMode()))
					tarEntry.setMode(Integer.parseInt(permissions.getFileMode(), 8));
				InputStream in = this.deployStreamProvider.getOverlayFileInputStream(f.getPath(), environmentName, releaseName, server.getName(), artifactName, versionName);
				try {
					// Create a buffered input stream out of the file
					// we're trying to add into the Tar
					this.stream.putArchiveEntry(tarEntry);
					IOUtils.copy(in, this.stream);
					this.stream.closeArchiveEntry();
				} finally {
					IOUtils.closeQuietly(in);
				}
			}
		}

	}

	private void writeArtifactVariables(Deployment deployment, ArtifactVersionDto artifactVersion, String path) throws IOException {
		VersionDto version = deployment.findVersion(artifactVersion);
		DeploymentVariableResolver resolver = new DeploymentVariableResolver(deployment, version, null);
		Map<String, String> variables = resolver.getDeploymentVariables();
		XmlBuffer xml = new XmlBuffer();
		xml.openTag(XMLConstants.ENVIRONMENT);
		xml.openTag(XMLConstants.VARIABLES);
		for (String varName : variables.keySet()) {
			xml.openTag(XMLConstants.VARIABLE);
			xml.addAttribute(XMLConstants.NAME, varName);
			xml.addCData(variables.get(varName));
			xml.closeTag();
		}
		xml.closeTag();
		variables = resolver.getArtifactVariables();
		xml.openTag(XMLConstants.ARTIFACT);
		xml.addAttribute(XMLConstants.NAME, version.getArtifactName());
		xml.openTag(XMLConstants.VARIABLES);
		xml.addAttribute(XMLConstants.VAR_PREFIX, Character.toString(version.getVarPrefixChar()));
		for (String varName : variables.keySet()) {
			xml.openTag(XMLConstants.VARIABLE);
			xml.addAttribute(XMLConstants.NAME, varName);
			xml.addCData(variables.get(varName));
			xml.closeTag();
		}
		xml.closeTag(); // variables
		xml.closeTag(); // artifact
		xml.closeTag(); // environment
		byte text[] = xml.toString().getBytes();
		TarArchiveEntry tarEntry = new TarArchiveEntry(path + "kwatee_variables.xml");
		tarEntry.setModTime(new Date());
		tarEntry.setSize(text.length);
		this.stream.putArchiveEntry(tarEntry);
		IOUtils.write(text, this.stream);
		this.stream.closeArchiveEntry();
	}

	private void addPackageFile(FileDto f, Deployment deployment, ServerDto server, VersionDto version, Set<FileDto> fileSet) throws IOException, MissingVariableException {
		if (fileSet.contains(f)) {
			fileSet.remove(f);
			String environmentName = deployment.getEnvironmentName();
			String releaseName = deployment.getName();
			String versionName = version.getName();
			String artifactName = version.getArtifactName();
			String prefixPath = this.rootDir + this.serverDir + artifactName + "/overlays/";
			PermissionDto permissions = f.getProperties() == null ? null : f.getProperties().getPermissions();
			TarArchiveEntry tarEntry = new TarArchiveEntry(prefixPath + f.getPath());
			tarEntry.setModTime(new Date());
			if (permissions != null && StringUtils.isNotEmpty(permissions.getFileOwner()))
				tarEntry.setUserName(permissions.getFileOwner());
			if (permissions != null && StringUtils.isNotEmpty(permissions.getFileGroup()))
				tarEntry.setGroupName(permissions.getFileGroup());
			if (BooleanUtils.isTrue(f.isDir())) {
				if (permissions != null && StringUtils.isNotEmpty(permissions.getDirMode()))
					tarEntry.setMode(Integer.parseInt(permissions.getDirMode(), 8));
				tarEntry.setSize(0);
				this.stream.putArchiveEntry(tarEntry);
			} else {
				if (permissions != null && StringUtils.isNotEmpty(permissions.getFileMode()))
					tarEntry.setMode(Integer.parseInt(permissions.getFileMode(), 8));
				InputStream in = this.deployStreamProvider.getOverlayFileInputStream(f.getPath(), environmentName, releaseName, server.getName(), artifactName, versionName);
				try {
					if (StringUtils.isNotEmpty(f.getVariables())) {
						// pre-calculate size of instantiated file
						DeploymentVariableResolver resolver = new DeploymentVariableResolver(deployment, version, server.getName(), server.getPlatform());
						tarEntry.setSize(this.variableService.getInstantiatedTemplateFileSize(f.getSize(), f.getVariables(), resolver));
						// instantiate directly into package
						this.stream.putArchiveEntry(tarEntry);
						this.variableService.instantiateTemplate(f.getPath(), in, this.stream, resolver);
					} else {
						tarEntry.setSize(f.getSize());
						// Create a buffered input stream out of the file
						// we're trying to add into the Tar
						this.stream.putArchiveEntry(tarEntry);
						IOUtils.copy(in, this.stream);
					}
				} finally {
					IOUtils.closeQuietly(in);
				}
			}
			// Close this entry in the Tar stream.
			this.stream.closeArchiveEntry();
		}
	}

	/**
	 * Adds an file to a tar archive
	 * 
	 * @param stream
	 * @param path
	 * @param size
	 * @param isExecutable
	 * @throws IOException
	 */
	void addFile(
			InputStream stream,
			String path,
			long size,
			boolean isExecutable
			) throws IOException {
		TarArchiveEntry tarEntry = new TarArchiveEntry(this.rootDir + path);
		if (isExecutable)
			tarEntry.setMode(tarEntry.getMode() | 0110); // th.mode |= 0110;
		tarEntry.setSize(size);
		this.stream.putArchiveEntry(tarEntry);
		if (size > 0)
			IOUtils.copy(stream, this.stream);
		IOUtils.closeQuietly(stream);
		this.stream.closeArchiveEntry();
	}

	void cancel() {
		if (this.tempArchive != null) {
			IOUtils.closeQuietly(this.stream);
			this.tempArchive.delete();
		}
	}
}