/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.dto;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;

public class FileDto implements java.lang.Comparable<FileDto> {

	private long id;
	private String name;
	private String path;
	private int layer;
	private Boolean symbolicLink;
	private Boolean dir;
	private Boolean hasVariables;
	private String variables;
	private Long size;
	@Valid
	private FilePropertiesDto properties;
	private String signature;

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getLayer() {
		return this.layer;
	}

	public void setLayer(int layer) {
		this.layer = layer;
	}

	public String getVariables() {
		return this.variables;
	}

	public void setVariables(String varNames) {
		if (StringUtils.isNotEmpty(varNames))
			this.variables = varNames;
		else
			this.variables = null;
	}

	public Boolean isSymbolicLink() {
		return this.symbolicLink;
	}

	public void setSymbolicLink(Boolean symbolicLink) {
		if (symbolicLink != null && symbolicLink.booleanValue())
			this.symbolicLink = symbolicLink;
		else
			this.symbolicLink = null;
	}

	public Boolean isDir() {
		return this.dir;
	}

	public void setDir(Boolean dir) {
		if (dir != null && dir.booleanValue())
			this.dir = dir;
		else
			this.dir = null;
	}

	public Boolean getHasVariables() {
		return this.hasVariables;
	}

	public void setHasVariables(Boolean hasVariables) {
		if (hasVariables != null && hasVariables.booleanValue())
			this.hasVariables = hasVariables;
		else
			this.hasVariables = null;
	}

	public Long getSize() {
		return this.size;
	}

	public void setSize(Long size) {
		if (size != null && size.longValue() > 0)
			this.size = size;
		else
			this.size = null;
	}

	public FilePropertiesDto getProperties() {
		return this.properties;
	}

	public void setProperties(FilePropertiesDto properties) {
		this.properties = properties;
	}

	public int compareTo(FileDto that) {
		if (this.path != null)
			return this.path.compareTo(that.path);
		return this.name.compareTo(that.name);
	}

	@Override
	public int hashCode() {
		if (this.path != null)
			return this.path.hashCode();
		return this.name.hashCode();
	}

	@Override
	public boolean equals(Object that) {
		if (that == null)
			return false;
		if (this.path != null)
			return this.path.equals(((FileDto) that).path);
		return this.name.equals(((FileDto) that).name);
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public String toString() {
		return this.path != null ? this.path : this.name;
	}
}
