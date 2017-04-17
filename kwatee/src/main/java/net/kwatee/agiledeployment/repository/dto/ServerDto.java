/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.dto;

import java.util.Collection;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

public class ServerDto {

	private Boolean disabled;
	@Size(max = 50)
	@Pattern(regexp = "(?:[a-zA-Z0-9]||[a-zA-Z0-9][a-zA-Z0-9_\\-\\.]*)\\z", message = "invalid character")
	private String name;
	@Size(max = 255)
	private String description;
	@Size(max = 20)
	private String poolType;
	private String conduitType;
	private Integer poolConcurrency;
	private Integer platform;
	@Size(max = 30)
	private String ipAddress;
	private Integer port;
	@Valid
	private CredentialsDto credentials;
	@Valid
	private Map<String, String> properties;
	@Valid
	private Map<String, String> poolProperties;
	@Valid
	private Collection<ServerPoolDescriptorDto> poolDescriptors;
	private Boolean useSudo;

	public ServerDto() {}

	public Boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(Boolean disabled) {
		if (BooleanUtils.isTrue(disabled))
			this.disabled = disabled;
		else
			this.disabled = null;
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
		if (StringUtils.isNotEmpty(description))
			this.description = description;
		else
			this.description = null;
	}

	public String getPoolType() {
		return poolType;
	}

	public void setPoolType(String poolType) {
		this.poolType = poolType;
	}

	public String getConduitType() {
		return conduitType;
	}

	public void setConduitType(String conduitType) {
		this.conduitType = conduitType;
	}

	public Integer getPoolConcurrency() {
		return poolConcurrency;
	}

	public void setPoolConcurrency(Integer poolConcurrency) {
		if (poolConcurrency != null && poolConcurrency >= 0)
			this.poolConcurrency = poolConcurrency;
		else
			this.poolConcurrency = null;
	}

	public Integer getPlatform() {
		return platform;
	}

	public void setPlatform(Integer platform) {
		if (platform != null)
			this.platform = platform;
		else
			this.platform = null;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		if (StringUtils.isNotEmpty(ipAddress))
			this.ipAddress = ipAddress;
		else
			this.ipAddress = null;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		if (port != null && port != 0)
			this.port = port;
		else
			this.port = null;
	}

	public CredentialsDto getCredentials() {
		return credentials;
	}

	public void setCredentials(CredentialsDto credentials) {
		this.credentials = credentials;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		if (!MapUtils.isEmpty(properties))
			this.properties = properties;
		else
			this.properties = null;
	}

	public Map<String, String> getPoolProperties() {
		return poolProperties;
	}

	public void setPoolProperties(Map<String, String> poolProperties) {
		if (!MapUtils.isEmpty(poolProperties))
			this.poolProperties = poolProperties;
		else
			this.poolProperties = null;
	}

	public Collection<ServerPoolDescriptorDto> getPoolDescriptors() {
		return poolDescriptors;
	}

	public void setPoolDescriptors(Collection<ServerPoolDescriptorDto> poolDescriptors) {
		if (CollectionUtils.isNotEmpty(poolDescriptors))
			this.poolDescriptors = poolDescriptors;
		else
			this.poolDescriptors = null;
	}

	public Boolean getUseSudo() {
		return useSudo;
	}

	public void setUseSudo(Boolean useSudo) {
		if (BooleanUtils.isTrue(useSudo))
			this.useSudo = useSudo;
		else
			this.useSudo = null;
	}
}
