/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.dto;

import java.util.Collection;

import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class ApplicationParameterDto {

	private String schema_version;
	private String title;
	private Collection<String> excludedExtensions;

	public String getSchemaVersion() {
		return this.schema_version;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Collection<String> getExcludedExtensions() {
		return this.excludedExtensions;
	}

	public void setExcludedExtensions(Collection<String> excludedExtensions) {
		if (CollectionUtils.isNotEmpty(excludedExtensions))
			this.excludedExtensions = excludedExtensions;
		else
			this.excludedExtensions = null;
	}
}
