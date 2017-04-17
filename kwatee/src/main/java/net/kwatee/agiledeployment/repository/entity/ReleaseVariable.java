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

import net.kwatee.agiledeployment.common.utils.CompareUtils;

@SuppressWarnings("serial")
@Entity(name = "KWDeploymentVariable")
@Table(name = "KWDeploymentVariable")
public class ReleaseVariable implements java.io.Serializable, Comparable<ReleaseVariable> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String name;
	private String description;
	private String value;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "deployment_id", nullable = false)
	private Release release;
	private Long package_id;
	private Long server_id;
	private boolean frozen_system_property;

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

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Release getRelease() {
		return this.release;
	}

	public void setRelease(Release release) {
		this.release = release;
	}

	public Long getArtifactId() {
		return this.package_id;
	}

	public void setArtifactId(Long artifactId) {
		this.package_id = artifactId;
	}

	public Long getServerId() {
		return this.server_id;
	}

	public void setServerId(Long serverId) {
		this.server_id = serverId;
	}

	public boolean isFrozenSystemProperty() {
		return this.frozen_system_property;
	}

	public void setFrozenSystemProperty(boolean frozenSystemProperty) {
		this.frozen_system_property = frozenSystemProperty;
	}

	public ReleaseVariable duplicate() {
		ReleaseVariable duplicateVariable = new ReleaseVariable();
		duplicateVariable.setName(this.name);
		duplicateVariable.setDescription(this.description);
		duplicateVariable.setValue(this.value);
		duplicateVariable.setRelease(this.release);
		duplicateVariable.setArtifactId(this.package_id);
		duplicateVariable.setServerId(this.server_id);
		return duplicateVariable;
	}

	@Override
	public String toString() {
		return this.name + '=' + this.value + " (server='" + server_id + "', artifact='" + package_id + "')";
	}

	@Override
	public boolean equals(Object t) {
		ReleaseVariable that = (ReleaseVariable) t;
		if (that == null || CompareUtils.notEquals(this.server_id, that.server_id) || CompareUtils.notEquals(this.package_id, that.package_id))
			return false;
		if (this.frozen_system_property != that.frozen_system_property)
			return false;
		return this.name.equals(that.name);
	}

	public int compareTo(ReleaseVariable that) {
		if (this == that)
			return 0;
		if (this.frozen_system_property != that.frozen_system_property)
			return this.frozen_system_property ? -1 : 1;
		int comp = CompareUtils.compareTo(this.server_id, that.server_id);
		if (comp != 0)
			return comp * -1;
		comp = CompareUtils.compareTo(this.package_id, that.package_id);
		if (comp != 0)
			return comp * -1;
		return this.name.compareTo(that.name);
	}
}
