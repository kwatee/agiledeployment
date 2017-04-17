/*
 * ${kwatee_copyright}
 */
package net.kwatee.agiledeployment.core.service;

import java.io.InputStream;

import net.kwatee.agiledeployment.common.exception.InternalErrorException;
import net.kwatee.agiledeployment.core.repository.DeployStreamProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 
 * @author mac
 * 
 */
@Component
public class DeployStreamProviderImpl implements DeployStreamProvider {

	@Autowired
	private PackageService packageService;
	@Autowired
	private FileStoreService fileStoreService;

	@Override
	public InputStream getPackageFileInputStream(String artifactName, String versionName) {
		return this.packageService.getPackageInputStream(artifactName, versionName);
	}

	@Override
	public InputStream getOverlayFileInputStream(String path, String environmentName, String releaseName, String serverName, String artifactName, String versionName) {
		try {
			String layerPath = this.fileStoreService.getReleaseOverlayPath(environmentName, releaseName, serverName, artifactName);
			return this.fileStoreService.getFileInputStream(layerPath, path);
		} catch (InternalErrorException e) {}
		try {
			String layerPath = this.fileStoreService.getReleaseOverlayPath(environmentName, releaseName, null, artifactName);
			return this.fileStoreService.getFileInputStream(layerPath, path);
		} catch (InternalErrorException e) {}
		try {
			String layerPath = this.fileStoreService.getVersionOverlayPath(artifactName, versionName);
			return this.fileStoreService.getFileInputStream(layerPath, path);
		} catch (InternalErrorException e) {}
		String layerPath = this.fileStoreService.getVersionTemplatePath(artifactName, versionName);
		return this.fileStoreService.getFileInputStream(layerPath, path);
	}

}
