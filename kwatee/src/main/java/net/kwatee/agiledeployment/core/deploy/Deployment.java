/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.deploy;

import java.util.Collection;

import net.kwatee.agiledeployment.repository.dto.ArtifactVersionDto;
import net.kwatee.agiledeployment.repository.dto.ReleaseDto;
import net.kwatee.agiledeployment.repository.dto.VariableDto;
import net.kwatee.agiledeployment.repository.dto.VersionDto;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class Deployment {

	final private ReleaseDto release;
	final private boolean sequential;
	final private boolean skipIntegrityCheck;
	private Collection<DeploymentServer> servers;
	private Collection<VersionDto> versions;
	private Collection<VariableDto> variables;
	private String actionParams;

	public Deployment(ReleaseDto release, boolean sequential, boolean skipIntegrityCheck) {
		this.release = release;
		this.sequential = sequential;
		this.skipIntegrityCheck = skipIntegrityCheck;
	}

	public String getName() {
		return this.release.getName();
	}

	public String getEnvironmentName() {
		return this.release.getEnvironmentName();
	}

	public ReleaseDto getRelease() {
		return this.release;
	}

	public boolean isSequential() {
		return this.sequential;
	}

	public boolean skipIntegrityCheck() {
		return this.skipIntegrityCheck;
	}

	public Collection<DeploymentServer> getServers() {
		return this.servers;
	}

	public void setServers(Collection<DeploymentServer> servers) {
		this.servers = servers;
	}

	public Collection<VersionDto> getVersions() {
		return this.versions;
	}

	public void setVersions(Collection<VersionDto> versions) {
		this.versions = versions;
	}

	public VersionDto findVersion(ArtifactVersionDto artifactVersion) {
		for (VersionDto version : this.versions) {
			if (version.getArtifactName().equals(artifactVersion.getArtifact()) && version.getName().equals(artifactVersion.getVersion()))
				return version;
		}
		return null;
	}

	public Collection<VariableDto> getVariables() {
		return this.variables;
	}

	public void setVariables(Collection<VariableDto> variables) {
		this.variables = variables;
	}

	public String toString() {
		return this.release.toString();
	}

	public DeploymentServer findDeploymentServer(String name) {
		if (CollectionUtils.isNotEmpty(this.servers)) {
			for (DeploymentServer server : this.servers) {
				if (name.equals(server.getName()))
					return server;
			}
		}
		return null;
	}

	public void setActionParams(String actionParams) {
		if (StringUtils.isNotEmpty(actionParams))
			this.actionParams = actionParams;
		else
			this.actionParams = null;
	}

	public String getActionParams() {
		return this.actionParams;
	}
}
