/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.deploy.descriptor;

import java.util.Collection;

import net.kwatee.agiledeployment.common.utils.NameValue;
import net.kwatee.agiledeployment.repository.dto.FileDto;

import org.apache.commons.lang3.StringUtils;

public class PackageDescriptor {

	private String deploymentName;
	private String artifactName;
	private String versionName;
	private String deployInDir;
	private String fileOwner;
	private String fileGroup;
	private String fileMode;
	private String dirMode;
	private Collection<NameValue> actions;
	private Collection<FileDto> files;
	private String signature;

	PackageDescriptor() {
		this.signature = StringUtils.EMPTY;
	}

	public Collection<FileDto> getFiles() {
		return this.files;
	}

	void setFiles(Collection<FileDto> files) {
		this.files = files;
	}

	public String getSignature() {
		return this.signature;
	}

	void setSignature(String signature) {
		this.signature = signature;
	}

	public String getDeploymentName() {
		return this.deploymentName;
	}

	void setDeploymentName(String deploymentName) {
		this.deploymentName = deploymentName;
	}

	public String getArtifactName() {
		return this.artifactName;
	}

	void setArtifactName(String artifactName) {
		this.artifactName = artifactName;
	}

	public String getVersionName() {
		return this.versionName;
	}

	void setVersionName(String versionName) {
		this.versionName = versionName;
	}

	public String getDeployInDir() {
		return this.deployInDir;
	}

	void setDeployInDir(String deployInDir) {
		this.deployInDir = deployInDir;
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

	String getFileMode() {
		return this.fileMode;
	}

	void setFileMode(String fileMode) {
		this.fileMode = fileMode;
	}

	public String getDirMode() {
		return StringUtils.isEmpty(this.dirMode) ? "755" : this.dirMode;
	}

	void setDirMode(String dirMode) {
		this.dirMode = dirMode;
	}

	public Collection<NameValue> getActions() {
		return this.actions;
	}

	void setActions(Collection<NameValue> actions) {
		this.actions = actions;
	}

	public String toXml() {
		return PackageDescriptorFactory.toXml(this);
	}

	public String toString() {
		StringBuilder s = new StringBuilder();
		for (FileDto f : this.files) {
			if (s.length() > 0) {
				s.append('\n');
			}
			s.append(f.getPath());
		}
		return s.toString();
	}
}
