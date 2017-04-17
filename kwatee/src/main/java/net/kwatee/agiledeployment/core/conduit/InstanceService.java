/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.conduit;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import net.kwatee.agiledeployment.conduit.PropertyDescriptor;
import net.kwatee.agiledeployment.conduit.ServerInstanceFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class InstanceService {

	static final private org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(InstanceService.class);
	private Map<String, Class<? extends ServerInstanceFactory>> factories;

	@Value("${kwatee.conduit.serverFactories:}")
	private String customFactories;

	@PostConstruct
	void initialize() {
		List<String> factoryClassNames = new ArrayList<>();
		factoryClassNames.add(ManualServerInstanceFactory.class.getCanonicalName());
		if (StringUtils.isNotEmpty(customFactories))
			factoryClassNames.addAll(Arrays.asList(customFactories.split(",")));
		this.factories = new HashMap<>();
		for (String factoryClassName : factoryClassNames) {
			LOG.debug("Registering server instance factory: " + factoryClassName);
			try {
				@SuppressWarnings("unchecked")
				Class<? extends ServerInstanceFactory> factory = (Class<? extends ServerInstanceFactory>) Class.forName(factoryClassName);
				String id = (String) factory.getMethod("getId").invoke(null);
				factory.getMethod("getDisplayName").invoke(null); // to make sure it exists
				this.factories.put(id.toLowerCase(), factory);
			} catch (Throwable e) {
				LOG.error("Failed to register server instance factory: " + factoryClassName, e);
			}
		}
	}

	/**
	 * The server instance factory types available
	 * 
	 * @return The server pool factory types available
	 */
	public Map<String, String> getFactories() {
		Map<String, String> serverTypes = new HashMap<>(factories.size());
		for (String serverType : factories.keySet()) {
			Class<? extends ServerInstanceFactory> factory = factories.get(serverType);
			try {
				String label = (String) factory.getMethod("getDisplayName").invoke(null);
				serverTypes.put(serverType, label);
			} catch (Exception e) {}
		}
		return serverTypes;
	}

	/**
	 * The ServerInstanceFactory object for serverPoolTypeName
	 * 
	 * @param serverTypeName
	 *            The server pool factory type name
	 * @return The ServerPoolFactory object for serverPoolTypeName
	 * @throws ConduitException
	 */
	private Class<? extends ServerInstanceFactory> getFactoryClass(String serverTypeName) throws ConduitException {
		Class<? extends ServerInstanceFactory> factory = factories.get(serverTypeName.toLowerCase());
		if (factory != null) {
			try {
				return factory;
			} catch (Exception e) {}
		}
		throw new ConduitException("ServerShortDto type " + serverTypeName + " not found");
	}

	/**
	 * The ServerInstanceFactory object for serverPoolTypeName
	 * 
	 * @param serverTypeName
	 *            The server pool factory type name
	 * @return The ServerPoolFactory object for serverPoolTypeName
	 * @throws ConduitException
	 */
	public ServerInstanceFactory getFactory(String serverTypeName) throws ConduitException {
		Class<? extends ServerInstanceFactory> factory = getFactoryClass(serverTypeName);
		try {
			return factory.getConstructor().newInstance();
		} catch (Throwable e) {
			LOG.error("getServerInstanceFactory", e);
			throw new ConduitException("Failed to instantiate " + serverTypeName);
		}
	}

	/**
	 * The ServerInstanceFactory object for serverPoolTypeName
	 * 
	 * @param serverTypeName
	 *            The server pool factory type name
	 * @return List of PropertyDescriptor objects for the server pool type or null
	 * @throws ConduitException
	 */
	@SuppressWarnings("unchecked")
	public List<PropertyDescriptor> getDescriptors(String serverTypeName) throws ConduitException {
		Class<? extends ServerInstanceFactory> factory = getFactoryClass(serverTypeName);
		try {
			Method method = factory.getMethod("getPropertyDescriptors");
			if (method != null) {
				return (List<PropertyDescriptor>) method.invoke(null);
			}
		} catch (Throwable e) {}
		return null;
	}
}
