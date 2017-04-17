package net.kwatee.agiledeployment.repository.dto;

import java.util.Collection;

public class OperationProgressDto {

	private String operation;
	private String status;
	private Collection<ServerOperationStatusDto> servers;

	public String getOperation() {
		return this.operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public String getStatus() {
		return this.status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Collection<ServerOperationStatusDto> getServers() {
		return this.servers;
	}

	public void setServers(Collection<ServerOperationStatusDto> servers) {
		this.servers = servers;
	}

}
