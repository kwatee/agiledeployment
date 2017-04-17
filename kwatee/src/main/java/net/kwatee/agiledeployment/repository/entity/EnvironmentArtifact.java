/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.entity;

import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@SuppressWarnings("serial")
@Embeddable
@Table(name = "kw_environment_package")
public class EnvironmentArtifact implements java.io.Serializable, Comparable<EnvironmentArtifact> {

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "package_id", nullable = true)
	private Artifact artifact;
	private Integer pos;

	public Artifact getArtifact() {
		return this.artifact;
	}

	public void setArtifact(Artifact artifact) {
		this.artifact = artifact;
	}

	public Integer getPos() {
		return this.pos;
	}

	public void setPos(Integer pos) {
		this.pos = pos;
	}

	public int hashCode() {
		return this.artifact.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		EnvironmentArtifact that = (EnvironmentArtifact) o;
		if (that == null || getClass() != o.getClass()) {
			return false;
		}
		if (!this.artifact.getId().equals(that.artifact.getId())) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(EnvironmentArtifact other) {
		if (other == null || this.pos == null) {
			return -1;
		}
		return this.pos.compareTo(other.pos);
	}
}
