/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.deploy.packager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import net.kwatee.agiledeployment.common.exception.InternalErrorException;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.core.deploy.Deployment;
import net.kwatee.agiledeployment.core.deploy.descriptor.DeploymentDescriptor;
import net.kwatee.agiledeployment.core.deploy.descriptor.DeploymentDescriptorFactory;
import net.kwatee.agiledeployment.core.deploy.descriptor.PackageDescriptor;
import net.kwatee.agiledeployment.core.deploy.descriptor.PackageDescriptorFactory;
import net.kwatee.agiledeployment.core.repository.DeployStreamProvider;
import net.kwatee.agiledeployment.core.variable.VariableService;
import net.kwatee.agiledeployment.repository.dto.ArtifactVersionDto;
import net.kwatee.agiledeployment.repository.dto.FileDto;
import net.kwatee.agiledeployment.repository.dto.PackageDto;
import net.kwatee.agiledeployment.repository.dto.ServerDto;
import net.kwatee.agiledeployment.repository.dto.VersionDto;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PackagerService {

	@Autowired
	private VariableService variableService;
	@Autowired
	DeployStreamProvider deployStreamProvider;

	final static private org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PackagerService.class);

	/**
	 * @param deployment
	 * @param serverName
	 * @param artifactVersion
	 * @param packageDescriptor
	 * @param filesToPackage
	 * @return package file
	 * @throws InterruptedException
	 * @throws MissingVariableException
	 */
	public File buildArtifactDeploymentBundle(
			Deployment deployment,
			ServerDto server,
			ArtifactVersionDto artifactVersion,
			PackageDescriptor packageDescriptor,
			Collection<FileDto> filesToPackage
			) throws InterruptedException, MissingVariableException {
		String serverName = server.getName();
		LOG.debug("buildArtifactBundle {}/{}", serverName, artifactVersion.toString());
		VersionDto version = deployment.findVersion(artifactVersion);
		TarBuilder builder = null;
		InputStream packageStream = null;
		try {
			builder = new TarBuilder(this.variableService, this.deployStreamProvider);
			PackageDto packageInfo = version.getPackageInfo();
			if (packageInfo != null || CollectionUtils.isNotEmpty(artifactVersion.getCustomFiles()))
				packageStream = this.deployStreamProvider.getPackageFileInputStream(artifactVersion.getArtifact(), artifactVersion.getVersion());
			builder.addArtifact(artifactVersion.toString(), packageStream, artifactVersion.getCustomFiles(), filesToPackage);
			DeploymentDescriptor deploymentDescriptor = DeploymentDescriptorFactory.loadFromServer(deployment, server);
			builder.openServerDeployment(serverName, deploymentDescriptor.toXml());
			if (packageDescriptor == null)
				packageDescriptor = PackageDescriptorFactory.loadFromArtifact(deployment, server, artifactVersion);
			builder.addArtifactOverlays(deployment, server, artifactVersion, packageDescriptor.toXml(), filesToPackage);
			builder.addArtifactTemplates(deployment, server, artifactVersion, filesToPackage);
			builder.closeServerDeployment();
			return builder.closeBundle();
		} catch (IOException e) {
			LOG.error("buildArtifactBundle", e);
			throw new InternalErrorException("Failed to build package");
		} finally {
			if (builder != null) {
				builder.cancel();
			}
			IOUtils.closeQuietly(packageStream);
		}
	}
}