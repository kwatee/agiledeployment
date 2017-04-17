package net.kwatee.agiledeployment.core.repository;

import java.util.Collection;

import net.kwatee.agiledeployment.repository.entity.ApplicationParameter;
import net.kwatee.agiledeployment.repository.entity.SystemProperty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AdminRepository {

	@Autowired
	private PersistenceService persistenceService;

	/**
	 * The list of system properties
	 * 
	 * @return The list of system properties
	 */
	public Collection<SystemProperty> getSystemProperties() {
		return this.persistenceService.getSystemProperties();
	}

	/**
	 * Updates systemproperties
	 * 
	 * @param properties
	 */
	public void updateSystemProperties(Collection<SystemProperty> properties) {
		this.persistenceService.updateSystemProperties(properties);
	}

	/**
	 * 
	 * @return
	 */
	public ApplicationParameter getApplicationParameters() {
		return this.persistenceService.getApplicationParameters();
	}

	/**
	 * 
	 * @param parameters
	 */
	public void updateApplicationParameters(ApplicationParameter parameters) {
		this.persistenceService.updateApplicationParameters(parameters);
	}

}
