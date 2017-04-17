/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.entity;

import java.io.Serializable;

import javax.persistence.Embeddable;

import net.kwatee.agiledeployment.common.utils.CompareUtils;

@SuppressWarnings("serial")
@Embeddable
public class EnvironmentArtifactPk implements Serializable {

	Long package_id;
	Long environment_id;

	@Override
	public int hashCode() {
		int result;
		result = (this.package_id != null ? this.package_id.hashCode() : 0);
		result = 31 * result + (this.environment_id != null ? this.environment_id.hashCode() : 0);
		return result;
	}

	@Override
	public boolean equals(Object o) {
		EnvironmentArtifactPk that = (EnvironmentArtifactPk) o;
		if (that == null)
			return false;
		return CompareUtils.equals(this.package_id, that.package_id) && CompareUtils.equals(this.environment_id, that.environment_id);
	}
}
