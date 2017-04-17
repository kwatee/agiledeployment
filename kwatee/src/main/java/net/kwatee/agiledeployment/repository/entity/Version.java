/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.entity;

import java.security.MessageDigest;
import java.util.Collection;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.common.utils.CompareUtils;
import net.kwatee.agiledeployment.common.utils.CryptoUtils;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("serial")
@Entity(name = "KWVersion")
@Table(name = "KWVersion")
public class Version implements java.io.Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id = null;
	private long creation_ts = new java.util.Date().getTime();
	private Long disable_ts;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "package_id", nullable = false)
	private Artifact artifact;
	private String name;
	private String description = StringUtils.EMPTY;
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "kw_version_platform", joinColumns = @JoinColumn(name = "version_id"))
	@Column(name = "platform_id")
	private Set<Integer> platforms = new java.util.HashSet<Integer>(0);
	private String archive_file_name = null;
	private Long archive_size = null;
	private Long archive_upload_date = null;
	private String pre_deploy_action = StringUtils.EMPTY;
	private String post_deploy_action = StringUtils.EMPTY;
	private String pre_undeploy_action = StringUtils.EMPTY;
	private String post_undeploy_action = StringUtils.EMPTY;
	private Boolean has_properties = null;
	@OneToMany(mappedBy = "version", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private Set<Executable> executables = new java.util.HashSet<Executable>(0);
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinTable(name = "kw_version_file", joinColumns = @JoinColumn(name = "version_id"), inverseJoinColumns = @JoinColumn(name = "file_id"))
	private Collection<RepositoryFile> files = new java.util.ArrayList<RepositoryFile>(0);
	@OneToMany(mappedBy = "version", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private Set<VersionVariable> variables_default_values = new java.util.HashSet<VersionVariable>(0);
	private Character var_prefix_char = null;
	private String file_owner;
	private String file_group;
	private Integer file_mode;
	private Integer dir_mode;
	private boolean need_package_rescan;

	transient private String error_message = null;

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Artifact getArtifact() {
		return this.artifact;
	}

	public void setArtifact(Artifact artifact) {
		this.artifact = artifact;
	}

	public long getCreationTs() {
		return this.creation_ts;
	}

	public void setCreationTs(long creationTs) {
		this.creation_ts = creationTs;
	}

	public Long getDisableTs() {
		return this.disable_ts;
	}

	public void setDisableTs(Long disableTs) {
		this.disable_ts = disableTs;
	}

	public boolean isDisabled() {
		return this.disable_ts != null;
	}

	public void setDisabled(boolean disabled) {
		if (!disabled) {
			setDisableTs(null);
		} else if (getDisableTs() == null) {
			setDisableTs(new java.util.Date().getTime());
		}
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Collection<Integer> getPlatforms() {
		return this.platforms;
	}

	public void setPlatforms(Set<Integer> platforms) {
		this.platforms = platforms;
	}

	public String getPackageFileName() {
		return this.archive_file_name;
	}

	public void setPackageFileName(String packageFileName) {
		this.archive_file_name = (packageFileName == null || packageFileName.length() <= 255) ? packageFileName : packageFileName.substring(0, 255);
	}

	public long getPackageSize() {
		return this.archive_size == null ? 0 : this.archive_size;
	}

	public void setPackageSize(long size) {
		this.archive_size = size;
	}

	public long getPackageUploadDate() {
		return this.archive_upload_date == null ? 0 : this.archive_upload_date;
	}

	public void setPackageUploadDate(long date) {
		this.archive_upload_date = date;
	}

	public String getPreDeployAction() {
		return this.pre_deploy_action;
	}

	public void setPreDeployAction(String action) {
		this.pre_deploy_action = action;
	}

	public String getPostDeployAction() {
		return this.post_deploy_action;
	}

	public void setPostDeployAction(String action) {
		this.post_deploy_action = action;
	}

	public String getPreUndeployAction() {
		return this.pre_undeploy_action;
	}

	public void setPreUndeployAction(String action) {
		this.pre_undeploy_action = action;
	}

	public String getPostUndeployAction() {
		return this.post_undeploy_action;
	}

	public void setPostUndeployAction(String action) {
		this.post_undeploy_action = action;
	}

	public boolean hasBuiltinProperties() {
		return has_properties != null && has_properties.booleanValue();
	}

	public void setBuiltinProperties(boolean hasProperties) {
		this.has_properties = hasProperties;
	}

	public Set<Executable> getExecutables() {
		return this.executables;
	}

	public void setExecutables(Set<Executable> executables) {
		this.executables = executables;
	}

	public Collection<RepositoryFile> getFiles() {
		return this.files;
	}

	public Set<VersionVariable> getVariablesDefaultValues() {
		return this.variables_default_values;
	}

	public char getVarPrefixChar() {
		if (this.var_prefix_char == null) {
			return '%';
		}
		return this.var_prefix_char;
	}

	public void setVarPrefixChar(Character varPrefixChar) {
		this.var_prefix_char = varPrefixChar;
	}

	public String getSignature() {
		MessageDigest md = CryptoUtils.getNewDigest(toString());
		md.update(this.pre_deploy_action.getBytes());
		md.update(this.post_deploy_action.getBytes());
		md.update(this.pre_undeploy_action.getBytes());
		md.update(this.post_undeploy_action.getBytes());
		for (Executable e : this.executables) {
			md.update(e.getName().getBytes());
			md.update(e.getStartAction().getBytes());
			md.update(e.getStopAction().getBytes());
			md.update(e.getStatusAction().getBytes());
		}
		for (RepositoryFile f : this.files) {
			md.update((byte) f.getLayerType().ordinal());
			if (f.dontDelete()) {
				md.update("dontDelete".getBytes());
			}
			if (f.ignoreIntegrity()) {
				md.update("ignoreIntegrity".getBytes());
			}
			if (f.ignoreVariables()) {
				md.update("ignoreVariables".getBytes());
			}
			md.update(f.getSignature().getBytes());
		}
		return CryptoUtils.getSignature(md);
	}

	/**
	 * Find topmost layer file
	 * 
	 * @param path
	 * @return repository file
	 * @throws ObjectNotExistException
	 */
	public RepositoryFile getFile(String path) throws ObjectNotExistException {
		RepositoryFile foundFile = null;
		for (RepositoryFile f : this.files) {
			if (f.getRelativePath().equals(path)) {
				if (foundFile == null) {
					foundFile = f;
				} else if (f.getLayerType().ordinal() > foundFile.getLayerType().ordinal()) {
					foundFile = f;
				}
			}
		}
		if (foundFile == null) {
			throw new ObjectNotExistException(ObjectNotExistException.FILE, path);
		}
		return foundFile;
	}

	public String getFileOwner() {
		return file_owner;
	}

	public void setFileOwner(String fileOwner) {
		this.file_owner = fileOwner;
	}

	public String getFileGroup() {
		return file_group;
	}

	public void setFileGroup(String fileGroup) {
		this.file_group = fileGroup;
	}

	public Integer getFileMode() {
		return file_mode;
	}

	public void setFileMode(Integer fileMode) {
		this.file_mode = fileMode;
	}

	public Integer getDirMode() {
		return dir_mode;
	}

	public void setDirMode(Integer dirMode) {
		this.dir_mode = dirMode;
	}

	public boolean isPackageRescanNeeded() {
		return need_package_rescan;
	}

	public void setNeedPackageRescan(boolean need_package_rescan) {
		this.need_package_rescan = need_package_rescan;
	}

	public Version duplicate(String newName) {
		Version duplicateVersion = new Version();
		duplicateVersion.setName(newName);
		duplicateVersion.setDescription(this.description);
		duplicateVersion.getPlatforms().addAll(this.platforms);
		duplicateVersion.setPreDeployAction(this.pre_deploy_action);
		duplicateVersion.setPostDeployAction(this.post_deploy_action);
		duplicateVersion.setPreUndeployAction(this.pre_undeploy_action);
		duplicateVersion.setPostUndeployAction(this.post_undeploy_action);
		duplicateVersion.setVarPrefixChar(this.var_prefix_char);
		for (Executable executable : this.executables) {
			Executable duplicateExecutable = executable.duplicate();
			duplicateExecutable.setVersion(duplicateVersion);
			duplicateVersion.getExecutables().add(duplicateExecutable);
		}
		if (this.archive_file_name != null) {
			duplicateVersion.setPackageFileName(this.archive_file_name);
			duplicateVersion.setPackageUploadDate(this.archive_upload_date);
			duplicateVersion.setPackageSize(this.archive_size);
		}
		for (RepositoryFile file : this.files) {
			RepositoryFile duplicateFile = file.duplicate();
			duplicateVersion.getFiles().add(duplicateFile);
		}
		for (VersionVariable defaultVarValue : this.variables_default_values) {
			VersionVariable duplicate = defaultVarValue.duplicate();
			duplicate.setVersion(duplicateVersion);
			duplicateVersion.getVariablesDefaultValues().add(duplicate);
		}
		duplicateVersion.setArtifact(this.artifact);
		duplicateVersion.setFileOwner(this.file_owner);
		duplicateVersion.setFileGroup(this.file_group);
		duplicateVersion.setFileMode(this.file_mode);
		duplicateVersion.setDirMode(this.dir_mode);
		duplicateVersion.setNeedPackageRescan(this.need_package_rescan);
		return duplicateVersion;
	}

	public void lazyLoadNow() {
		this.platforms.size();
		this.pre_deploy_action.length();
		this.post_deploy_action.length();
		this.pre_undeploy_action.length();
		this.post_undeploy_action.length();
		for (Executable exe : this.executables) {
			exe.lazyLoad();
		}
		this.files.size();
		this.variables_default_values.size();
	}

	public boolean hasError() {
		return this.error_message != null;
	}

	public String getErrorMessage() {
		return this.error_message;
	}

	public void setErrorMessage(String errorMessage) {
		this.error_message = errorMessage;
	}

	@Override
	public String toString() {
		if (this.name == null) {
			return this.artifact.getName();
		}
		return this.artifact.getName() + '[' + this.name + ']';
	}

	@Override
	public int hashCode() {
		return this.id == null ? 0 : this.id.hashCode();
	}

	@Override
	public boolean equals(Object that) {
		return that != null && CompareUtils.compareTo(this.id, ((Version) that).id) == 0;
	}
}
