/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.entity;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import net.kwatee.agiledeployment.common.Constants.LayerType;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("serial")
@Entity(name = "KWRepositoryFile")
@Table(name = "KWRepositoryFile")
public class RepositoryFile implements java.io.Serializable, java.lang.Comparable<RepositoryFile> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private LayerType layer_type = LayerType.ALL;
	private String path = StringUtils.EMPTY;
	private boolean is_symbolic_link = false;
	private boolean is_directory = false;
	@SuppressWarnings("unused")
	private boolean can_execute = false;
	private boolean ignore_variables = false;
	private boolean ignore_integrity = false;
	private boolean dont_delete = false;
	@Basic
	private String variables = null;
	private long size = 0;
	private String signature = StringUtils.EMPTY;
	private String file_owner;
	private String file_group;
	private Integer file_mode;
	private Integer dir_mode;
	private String original_owner;
	private String original_group;
	private Integer original_mode;

	public RepositoryFile() {}

	public RepositoryFile(String path) {
		this(path, false);
	}

	public RepositoryFile(String path, boolean isDir) {
		this.path = path;
		this.is_directory = isDir;
	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LayerType getLayerType() {
		return this.layer_type;
	}

	public void setLayerType(LayerType layerType) {
		this.layer_type = layerType;
	}

	public String getRelativePath() {
		return this.path;
	}

	public void setRelativePath(String path) {
		this.path = path;
	}

	public boolean isTemplatized() {
		return this.variables != null;
	}

	public String getVariables() {
		return this.variables;
	}

	public void setVariables(String varNames) {
		this.variables = varNames;
	}

	public boolean isSymbolicLink() {
		return this.is_symbolic_link;
	}

	public void setSymbolicLink(boolean isSymbolicLink) {
		this.is_symbolic_link = isSymbolicLink;
	}

	public boolean isDirectory() {
		return this.is_directory;
	}

	public void setDirectory(boolean isDirectory) {
		this.is_directory = isDirectory;
	}

	public boolean ignoreVariables() {
		return this.ignore_variables;
	}

	public void setIgnoreVariables(boolean ignoreVariables) {
		this.ignore_variables = ignoreVariables;
	}

	public boolean ignoreIntegrity() {
		return this.ignore_integrity;
	}

	public void setIgnoreIntegrity(boolean ignoreIntegrity) {
		this.ignore_integrity = ignoreIntegrity;
	}

	public boolean dontDelete() {
		return this.dont_delete;
	}

	public void setDontDelete(boolean dontDelete) {
		this.dont_delete = dontDelete;
	}

	public boolean hasExtraInfo() {
		return this.ignore_variables || this.ignore_integrity || this.dont_delete;
	}

	public long getSize() {
		return this.size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getSignature() {
		return this.signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public String getFileOwner() {
		return this.file_owner;
	}

	public void setFileOwner(String fileOwner) {
		this.file_owner = fileOwner;
	}

	public String getFileGroup() {
		return this.file_group;
	}

	public void setFileGroup(String fileGroup) {
		this.file_group = fileGroup;
	}

	public Integer getFileMode() {
		return this.file_mode;
	}

	public void setFileMode(Integer fileMode) {
		this.file_mode = fileMode;
	}

	public Integer getDirMode() {
		return this.dir_mode;
	}

	public void setDirMode(Integer dirMode) {
		this.dir_mode = dirMode;
	}

	public String getOriginalOwner() {
		return this.original_owner;
	}

	public void setOriginalOwner(String owner) {
		this.original_owner = owner;
	}

	public String getOriginalGroup() {
		return this.original_group;
	}

	public void setOriginalGroup(String group) {
		this.original_group = group;
	}

	public Integer getOriginalMode() {
		return this.original_mode;
	}

	public void setOriginalMode(Integer mode) {
		this.original_mode = mode;
	}

	public RepositoryFile duplicate() {
		RepositoryFile duplicateFile = new RepositoryFile();
		duplicateFile.setDirectory(this.is_directory);
		duplicateFile.setLayerType(this.layer_type);
		duplicateFile.setRelativePath(this.path);
		duplicateFile.setSignature(this.signature);
		duplicateFile.setSymbolicLink(this.is_symbolic_link);
		duplicateFile.setSize(this.size);
		duplicateFile.setIgnoreIntegrity(this.ignore_integrity);
		duplicateFile.setIgnoreVariables(this.ignore_variables);
		duplicateFile.setDontDelete(this.dont_delete);
		if (this.variables != null) {
			duplicateFile.variables = this.variables;
		}
		duplicateFile.setFileOwner(this.file_owner);
		duplicateFile.setFileGroup(this.file_group);
		duplicateFile.setFileMode(this.file_mode);
		duplicateFile.setDirMode(this.dir_mode);
		duplicateFile.setOriginalOwner(this.original_owner);
		duplicateFile.setOriginalGroup(this.original_group);
		duplicateFile.setOriginalMode(this.original_mode);
		return duplicateFile;
	}

	// @Override
	public int compareTo(RepositoryFile that) {
		return this.path.compareTo(that.path);
	}

	@Override
	public int hashCode() {
		return this.path == null ? 0 : this.path.hashCode();
	}

	@Override
	public boolean equals(Object that) {
		return that != null && compareTo((RepositoryFile) that) == 0;
	}

	@Override
	public String toString() {
		return this.path + '[' + this.layer_type.toString() + ']';
	}
}
