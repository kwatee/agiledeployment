/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.entity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("serial")
@Entity(name = "KWApplicationParameter")
@Table(name = "KWApplicationParameter")
public class ApplicationParameter implements java.io.Serializable {

	@Id
	private short id;
	private String schema_version;
	private String title;
	private String excluded_extensions;

	public String getSchemaVersion() {
		return this.schema_version;
	}

	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getExcludedExtensions() {
		return this.excluded_extensions;
	}

	public void setExcludedExtensions(String excludedExtensions) {
		this.excluded_extensions = excludedExtensions;
	}

	public Set<String> getExcludedExtensionsAsSet() {
		if (StringUtils.isEmpty(this.excluded_extensions))
			return null;
		String[] extensions = this.excluded_extensions.split(",");
		return new HashSet<String>(Arrays.asList(extensions));
	}
}
