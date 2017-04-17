/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.repository;

import java.util.Collection;

import net.kwatee.agiledeployment.common.exception.CannotDeleteObjectInUseException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.core.conduit.ConduitService;
import net.kwatee.agiledeployment.repository.entity.Environment;
import net.kwatee.agiledeployment.repository.entity.Server;
import net.kwatee.agiledeployment.repository.entity.Version;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * @author mac
 * 
 */
@Service
public class ServerRepository {

	@Autowired
	private EnvironmentRepository environmentRepository;
	@Autowired
	private PersistenceService persistenceService;
	@Autowired
	private ConduitService conduitService;

	/**
	 * List of kwatee servers
	 * 
	 * @return List of kwatee servers
	 */
	public Collection<Server> getServers() {
		Collection<Server> servers = this.persistenceService.getServers();
		return servers;
	}

	/**
	 * Retrieves a server by its id or null if not found
	 * 
	 * @param serverId
	 *            A kwatee server id
	 * @return A kwatee server or null if not found
	 */
	public Server getUncheckedServer(long serverId) {
		return this.persistenceService.getServerById(serverId);
	}

	/**
	 * Retrieves a server by its id or throws an exception if not found
	 * 
	 * @param serverId
	 *            A kwatee server id
	 * @return A kwatee server
	 * @throws ObjectNotExistException
	 *             If server not found
	 */
	public Server getCheckedServer(long serverId) throws ObjectNotExistException {
		Server server = this.persistenceService.getServerById(serverId);
		if (server == null) {
			throw new ObjectNotExistException(ObjectNotExistException.SERVER, serverId);
		}
		return server;
	}

	/**
	 * Retrieves a server by its name or null if not found
	 * 
	 * @param serverName
	 *            A kwatee server name
	 * @return A kwatee server or null if not found
	 */
	public Server getUncheckedServer(String serverName) {
		return this.persistenceService.getServerByName(serverName);
	}

	/**
	 * Retrieves a server by its name or throws an exception if not found
	 * 
	 * @param serverName
	 *            A kwatee server name
	 * @return A kwatee server
	 * @throws ObjectNotExistException
	 *             If server not found
	 */
	public Server getCheckedServer(String serverName) throws ObjectNotExistException {
		Server server = this.persistenceService.getServerByName(serverName);
		if (server == null) {
			throw new ObjectNotExistException(ObjectNotExistException.SERVER, serverName);
		}
		return server;
	}

	//
	//	/**
	//	 * 
	//	 * @param server
	//	 * @param properties
	//	 */
	//	public void updateProperties(Server server, final Collection<ServerProperty> properties) {
	//		/*
	//		 * Remove obsolete properties
	//		 */
	//		CollectionUtils.filter(server.getProperties(),
	//				new Predicate() {
	//
	//					public boolean evaluate(Object p) {
	//						return properties.contains(p);
	//					}
	//				});
	//		/*
	//		 * Add and update properties
	//		 */
	//		for (final ServerProperty prop : properties) {
	//			ServerProperty p = (ServerProperty) CollectionUtils.find(server.getProperties(),
	//					new Predicate() {
	//
	//						public boolean evaluate(Object p) {
	//							return prop.equals(p);
	//						}
	//					});
	//			if (p == null) {
	//				server.getProperties().add(prop);
	//			}
	//			else {
	//				p.setValue(prop.getValue());
	//			}
	//		}
	//	}

	/**
	 * Save kwatee server
	 * 
	 * @param server
	 *            A katee server
	 */
	public void saveServer(Server server) {
		this.persistenceService.saveEntity(server);
	}

	/**
	 * Delete kwatee server
	 * 
	 * @param server
	 *            Kwatee server
	 * @throws CannotDeleteObjectInUseException
	 */
	public void deleteServer(Server server) throws CannotDeleteObjectInUseException {
		Environment e = this.environmentRepository.getEnvironmentContainingServer(server.getId());
		if (e != null) {
			throw new CannotDeleteObjectInUseException("server " + server, "environment " + e);
		}
		this.persistenceService.deleteEntity(server);
	}

	public Collection<Server> getServersContainingVersion(Version version) {
		return this.persistenceService.getServersContainingVersion(version);
	}
}