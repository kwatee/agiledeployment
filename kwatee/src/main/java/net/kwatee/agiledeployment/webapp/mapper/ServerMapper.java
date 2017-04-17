/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.kwatee.agiledeployment.conduit.AccessLevel;
import net.kwatee.agiledeployment.conduit.DeployCredentials;
import net.kwatee.agiledeployment.conduit.PropertyDescriptor;
import net.kwatee.agiledeployment.core.conduit.ConduitException;
import net.kwatee.agiledeployment.core.conduit.InstanceService;
import net.kwatee.agiledeployment.repository.dto.CredentialsDto;
import net.kwatee.agiledeployment.repository.dto.ServerDto;
import net.kwatee.agiledeployment.repository.dto.ServerPoolDescriptorDto;
import net.kwatee.agiledeployment.repository.entity.Server;
import net.kwatee.agiledeployment.repository.entity.ServerCredentials;
import net.kwatee.agiledeployment.repository.entity.ServerProperty;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServerMapper {

	@Autowired
	private InstanceService instanceService;

	/**
	 * 
	 * @param servers
	 * @return server dtos
	 */
	public Collection<ServerDto> toShortServerDtos(Collection<Server> servers) {
		List<Server> sortedServers = new ArrayList<>(servers);
		Collections.sort(sortedServers, new Comparator<Server>() {

			@Override
			public int compare(Server server1, Server server2) {
				return server1.getName().compareToIgnoreCase(server2.getName());
			}
		});
		Collection<ServerDto> dtos = new ArrayList<>();
		for (Server server : sortedServers) {
			dtos.add(toShortServerDto(server));
		}
		return dtos;
	}

	/**
	 * @param server
	 * @return server dto
	 * @throws ConduitException
	 */
	public ServerDto toServerDto(Server server, boolean withSecret) throws ConduitException {
		ServerDto dto = toShortServerDto(server);
		dto.setPlatform(server.getPlatform());
		dto.setIpAddress(server.getIPAddress());
		dto.setPort(server.getPort());
		dto.setConduitType(server.getConduitType());
		dto.setCredentials(toCredentialsDto(server.getCredentials(), withSecret));

		Collection<ServerProperty> properties = null;
		if (CollectionUtils.isNotEmpty(server.getProperties()))
			properties = new ArrayList<>(server.getProperties());

		if (server.getPoolType() != null) {
			dto.setPoolDescriptors(toPoolDescriptorDtos(server.getPoolType()));
			dto.setPoolProperties(toPoolPropertieDtos(server.getPoolType(), properties));
		}
		dto.setPoolConcurrency(server.getPoolConcurrency());

		Map<String, String> serverProperties = new HashMap<>();
		if (CollectionUtils.isNotEmpty(properties)) {
			for (ServerProperty p : server.getProperties()) {
				serverProperties.put(p.getName(), p.getValue());
			}
		}
		dto.setProperties(serverProperties);
		dto.setUseSudo(server.getUseSudo());

		return dto;
	}

	private CredentialsDto toCredentialsDto(ServerCredentials credentials, boolean withSecret) {
		CredentialsDto dto = new CredentialsDto();
		if (credentials != null && credentials.getAccessLevel().ordinal() > AccessLevel.OFFLINE.ordinal()) {
			dto.setPromptPassword(credentials.isPasswordPrompted());
			dto.setLogin(credentials.getLogin());
			if (withSecret)
				dto.setPassword(credentials.getPassword());
			String pem = credentials.getPem();
			if (pem != null) {
				if (DeployCredentials.isPrivateKeyPath(pem))
					dto.setPem(pem);
				else if (withSecret)
					dto.setPem(pem);
				else
					dto.setPem("*");
			}
		}
		return dto;
	}

	private Collection<ServerPoolDescriptorDto> toPoolDescriptorDtos(String poolType) throws ConduitException {
		List<PropertyDescriptor> descriptors = this.instanceService.getDescriptors(poolType);
		if (descriptors == null || descriptors.size() == 0) {
			return null;
		}
		Collection<ServerPoolDescriptorDto> dtos = new ArrayList<>();
		for (PropertyDescriptor descriptor : descriptors) {
			ServerPoolDescriptorDto dto = new ServerPoolDescriptorDto();
			dto.setName(descriptor.getName());
			dto.setType(descriptor.getType().ordinal());
			dto.setMandatory(descriptor.isMandatory());
			dto.setLabel(descriptor.getLabel());
			dto.setDefaultValue(descriptor.getDefaultValue());
			dto.setOptions(descriptor.getSelection());
			dtos.add(dto);
		}
		return dtos;
	}

	private Map<String, String> toPoolPropertieDtos(String poolType, Collection<ServerProperty> properties) {
		Map<String, String> poolProperties = new HashMap<>();
		if (CollectionUtils.isNotEmpty(properties)) {
			String prefix;
			prefix = poolType + ".";
			Iterator<ServerProperty> propertyIt = properties.iterator();
			while (propertyIt.hasNext()) {
				ServerProperty property = propertyIt.next();
				if (property.getName().startsWith(prefix)) {
					propertyIt.remove();
					poolProperties.put(property.getName().substring(prefix.length()), property.getValue());
				}
			}
		}
		return poolProperties;
	}

	/**
	 * Return JSON server
	 * 
	 * @param server
	 * @return JSON server
	 */
	private ServerDto toShortServerDto(Server server) {
		ServerDto dto = new ServerDto();
		dto.setName(server.getName());
		dto.setDescription(server.getDescription());
		dto.setDisabled(server.isDisabled());
		dto.setPoolType(server.getPoolType());
		return dto;
	}
}
