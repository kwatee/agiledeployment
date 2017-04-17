/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.entity;

import java.io.Serializable;

import javax.persistence.Embeddable;

import net.kwatee.agiledeployment.common.utils.CompareUtils;

@SuppressWarnings("serial")
@Embeddable
public class EnvironmentServerPk implements Serializable {

	Long server_id;
	Long environment_id;

	@Override
	public int hashCode() {
		int result;
		result = (this.server_id != null ? this.server_id.hashCode() : 0);
		result = 31 * result + (this.environment_id != null ? this.environment_id.hashCode() : 0);
		return result;
	}

	@Override
	public boolean equals(Object o) {
		EnvironmentServerPk that = (EnvironmentServerPk) o;
		if (that == null)
			return false;
		return CompareUtils.equals(this.server_id, that.server_id) && CompareUtils.equals(this.environment_id, that.environment_id);
	}
}
