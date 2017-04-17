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
@Table(name = "kw_environment_server")
public class EnvironmentServer implements java.io.Serializable, Comparable<EnvironmentServer> {

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "server_id", nullable = true)
	private Server server;
	private Integer pos;

	public Server getServer() {
		return this.server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public Integer getPos() {
		return this.pos;
	}

	public void setPos(Integer pos) {
		this.pos = pos;
	}

	@Override
	public int hashCode() {
		return this.server.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		EnvironmentServer that = (EnvironmentServer) o;
		if (that == null || getClass() != o.getClass()) {
			return false;
		}
		if (!this.server.getId().equals(that.server.getId())) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(EnvironmentServer other) {
		if (other == null || this.pos == null) {
			return -1;
		}
		return this.pos.compareTo(other.pos);
	}
}
