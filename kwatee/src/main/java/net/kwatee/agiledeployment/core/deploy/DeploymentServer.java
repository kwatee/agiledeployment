package net.kwatee.agiledeployment.core.deploy;

import java.util.Map;

import net.kwatee.agiledeployment.conduit.ConduitServer;
import net.kwatee.agiledeployment.repository.dto.ServerDto;

public class DeploymentServer implements ConduitServer {

	final private ServerDto server;

	public DeploymentServer(ServerDto server, Deployment deployment) {
		this.server = server;
	}

	public ServerDto getServer() {
		return this.server;
	}

	@Override
	public Long getId() {
		return 0L;
	}

	@Override
	public String getName() {
		return this.server.getName();
	}

	@Override
	public String getConduitType() {
		return this.server.getConduitType();
	}

	@Override
	public String getPoolType() {
		return this.server.getPoolType();
	}

	@Override
	public int getPoolConcurrency() {
		return this.server.getPoolConcurrency() == null ? 0 : this.server.getPoolConcurrency();
	}

	@Override
	public Integer getPlatform() {
		return this.server.getPlatform();
	}

	@Override
	public String getIPAddress() {
		return this.server.getIpAddress();
	}

	@Override
	public int getPort() {
		return this.server.getPort() == null ? 0 : this.server.getPort();
	}

	@Override
	public Map<String, String> getServerProperties() {
		return this.server.getPoolProperties();
	}
}
