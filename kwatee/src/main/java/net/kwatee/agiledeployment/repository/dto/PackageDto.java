/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.dto;

import java.util.Collection;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

public class PackageDto {

	private String name;
	private String size;
	private Long uploadDate;
	private Collection<FileDto> files;
	private Boolean rescanNeeded;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		if (StringUtils.isNotEmpty(name))
			this.name = name;
		else
			this.name = null;
	}

	public String getSize() {
		return this.size;
	}

	public void setSize(String size) {
		if (StringUtils.isNotEmpty(size))
			this.size = size;
		else
			this.size = null;
	}

	public Long getUploadDate() {
		return this.uploadDate;
	}

	public void setUploadDate(Long date) {
		this.uploadDate = date;
	}

	public Collection<FileDto> getFiles() {
		return this.files;
	}

	public void setFiles(Collection<FileDto> files) {
		if (CollectionUtils.isNotEmpty(files))
			this.files = files;
		else
			this.files = null;
	}

	public boolean isRescanNeeded() {
		return BooleanUtils.isTrue(this.rescanNeeded);
	}

	public void setRescanNeeded(Boolean rescanNeeded) {
		if (rescanNeeded != null && rescanNeeded.booleanValue())
			this.rescanNeeded = rescanNeeded;
		else
			this.rescanNeeded = null;
	}

}
