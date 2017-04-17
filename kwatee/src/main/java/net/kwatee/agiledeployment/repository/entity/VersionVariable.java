/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.entity;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@SuppressWarnings("serial")
@Entity(name = "KWVersionVariable")
@Table(name = "KWVersionVariable")
public class VersionVariable implements java.io.Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String name;
	private String description;
	private String default_value;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "version_id", nullable = false)
	private Version version;

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDefaultValue() {
		return this.default_value;
	}

	public void setDefaultValue(String value) {
		if (value == null) {
			this.default_value = value;
		} else {
			this.default_value = value.length() <= 2000 ? value : value.substring(0, 1999);
		}
	}

	public Version getVersion() {
		return this.version;
	}

	public void setVersion(Version version) {
		this.version = version;
	}

	public VersionVariable duplicate() {
		VersionVariable duplicateVariable = new VersionVariable();
		duplicateVariable.setName(this.name);
		duplicateVariable.setDescription(this.description);
		duplicateVariable.setDefaultValue(this.default_value);
		duplicateVariable.setVersion(this.version);
		return duplicateVariable;
	}

	@Override
	public String toString() {
		return this.name + '=' + this.default_value;
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public boolean equals(Object that) {
		return that != null && this.name.equals(((VersionVariable) that).name);
	}
}
