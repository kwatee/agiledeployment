/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.deploy.descriptor;

import java.util.Collection;

import net.kwatee.agiledeployment.common.utils.NameValue;
import net.kwatee.agiledeployment.repository.dto.ArtifactVersionDto;

import org.apache.commons.lang3.StringUtils;

public class DeploymentDescriptor {

	private String deploymentName;
	private String deployInDir;
	private Collection<NameValue> actions;
	private Collection<ArtifactVersionDto> artifacts;
	private String fileOwner;
	private String fileGroup;
	private String fileMode;
	private String dirMode;
	private String signature;

	DeploymentDescriptor() {
		this.signature = StringUtils.EMPTY;
	}

	public String getDeploymentName() {
		return this.deploymentName;
	}

	void setDeploymentName(String deploymentName) {
		this.deploymentName = deploymentName;
	}

	public String getDeployInDir() {
		return this.deployInDir;
	}

	void setDeployInDir(String deployInDir) {
		this.deployInDir = deployInDir;
	}

	public String getSignature() {
		return this.signature;
	}

	void setSignature(String signature) {
		this.signature = signature;
	}

	Collection<NameValue> getActions() {
		return this.actions;
	}

	void setActions(Collection<NameValue> actions) {
		this.actions = actions;
	}

	Collection<ArtifactVersionDto> getArtifacts() {
		return this.artifacts;
	}

	void setArtifacts(Collection<ArtifactVersionDto> artifacts) {
		this.artifacts = artifacts;
	}

	public String getFileOwner() {
		return this.fileOwner;
	}

	void setFileOwner(String fileOwner) {
		this.fileOwner = fileOwner;
	}

	public String getFileGroup() {
		return this.fileGroup;
	}

	void setFileGroup(String fileGroup) {
		this.fileGroup = fileGroup;
	}

	public String getFileMode() {
		return this.fileMode;
	}

	void setFileMode(String fileMode) {
		this.fileMode = fileMode;
	}

	public String getDirMode() {
		return this.dirMode;
	}

	void setDirMode(String dirMode) {
		this.dirMode = dirMode;
	}

	public String toXml() {
		return DeploymentDescriptorFactory.toXml(this);
	}

	public String toString() {
		return this.deploymentName;
	}
}
