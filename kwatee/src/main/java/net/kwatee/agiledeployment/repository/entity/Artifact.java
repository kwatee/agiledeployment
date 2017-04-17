/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.entity;

import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import net.kwatee.agiledeployment.common.utils.CompareUtils;

@SuppressWarnings("serial")
@Entity(name = "KWPackage")
@Table(name = "KWPackage")
public class Artifact implements java.io.Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private long creation_ts = new java.util.Date().getTime();
	private Long disable_ts;
	private String name;
	private String description;
	@OneToMany(mappedBy = "artifact", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	private Collection<Version> versions = new java.util.ArrayList<Version>(0);

	public Artifact() {}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
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
		}
		else if (getDisableTs() == null) {
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

	public Collection<Version> getVersions() {
		return this.versions;
	}

	public void lazyLoadNow() {
		this.versions.size();
	}

	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public int hashCode() {
		return this.id == null ? 0 : this.id.hashCode();
	}

	@Override
	public boolean equals(Object that) {
		return that != null && CompareUtils.compareTo(this.id, ((Artifact) that).id) == 0;
	}
}
