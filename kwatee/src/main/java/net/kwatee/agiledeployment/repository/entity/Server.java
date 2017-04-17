/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import net.kwatee.agiledeployment.common.utils.CompareUtils;
import net.kwatee.agiledeployment.conduit.ConduitServer;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("serial")
@Entity(name = "KWServer")
@Table(name = "KWServer")
public class Server implements java.io.Serializable, Comparable<Server>, ConduitServer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;
	private long creation_ts = new java.util.Date().getTime();
	private Long disable_ts;
	private String name;
	private String description = StringUtils.EMPTY;
	private String conduit_type;
	private String pool_type;
	private int pool_concurrency = 0;
	private Integer platform_id;
	private String ip_address = StringUtils.EMPTY;
	private int port = 22;
	@OneToOne(mappedBy = "server", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private ServerCredentials credentials;
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "KWServerProperty", joinColumns = @JoinColumn(name = "server_id"))
	private Set<ServerProperty> server_properties = new java.util.HashSet<ServerProperty>(0);
	private Boolean use_sudo;

	public Server() {}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public long getCreationTs() {
		return creation_ts;
	}

	public void setCreationTs(long creationTs) {
		creation_ts = creationTs;
	}

	public Long getDisableTs() {
		return disable_ts;
	}

	public void setDisableTs(Long disableTs) {
		disable_ts = disableTs;
	}

	public boolean isDisabled() {
		return disable_ts != null;
	}

	public void setDisabled(boolean disabled) {
		if (!disabled) {
			setDisableTs(null);
		} else if (getDisableTs() == null) {
			setDisableTs(new java.util.Date().getTime());
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getConduitType() {
		return conduit_type;
	}

	public void setConduitType(String conduitType) {
		conduit_type = conduitType;
	}

	public String getPoolType() {
		return pool_type;
	}

	public void setPoolType(String poolType) {
		pool_type = poolType;
	}

	public int getPoolConcurrency() {
		return pool_concurrency;
	}

	public void setPoolConcurrency(int poolConcurrency) {
		pool_concurrency = poolConcurrency;
	}

	public Integer getPlatform() {
		return platform_id;
	}

	public void setPlatform(Integer platformId) {
		platform_id = platformId;
	}

	public String getIPAddress() {
		return ip_address;
	}

	public void setIPAddress(String ipAddress) {
		if (StringUtils.isEmpty(ipAddress))
			ip_address = StringUtils.EMPTY;
		else
			ip_address = ipAddress.length() <= 30 ? ipAddress : ipAddress.substring(0, 30);
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public ServerCredentials getCredentials() {
		return credentials;
	}

	public void setCredentials(ServerCredentials credentials) {
		this.credentials = credentials;
	}

	public Set<ServerProperty> getProperties() {
		return server_properties;
	}

	public Map<String, String> getServerProperties() {
		Map<String, String> serverProperties = new HashMap<>(server_properties.size());
		for (ServerProperty p : server_properties) {
			serverProperties.put(p.getName(), p.getValue());
		}
		return serverProperties;
	}

	public boolean getUseSudo() {
		return BooleanUtils.isTrue(use_sudo);
	}

	public void setUseSudo(boolean useSudo) {
		use_sudo = useSudo;
	}

	public void lazyLoadNow() {
		if (credentials != null)
			credentials.getId();
		server_properties.size();
	}

	public Server duplicate(String newName) {
		Server duplicateServer = new Server();
		duplicateServer.setName(newName);
		duplicateServer.setDescription(description);
		duplicateServer.setPlatform(platform_id);
		duplicateServer.setIPAddress(ip_address);
		duplicateServer.setPort(port);
		ServerCredentials duplicateCredentials = credentials.duplicate();
		duplicateCredentials.setServer(duplicateServer);
		duplicateServer.setCredentials(duplicateCredentials);
		duplicateServer.setConduitType(conduit_type);
		duplicateServer.setPoolType(pool_type);
		duplicateServer.setPoolConcurrency(pool_concurrency);
		duplicateServer.getProperties().addAll(server_properties);
		duplicateServer.setUseSudo(use_sudo);
		return duplicateServer;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		return id == null ? 0 : id.hashCode();
	}

	@Override
	public boolean equals(Object that) {
		return that != null && CompareUtils.equals(id, ((Server) that).id);
	}

	public int compareTo(Server s) {
		return name.compareToIgnoreCase(s.name);
	}
}
