/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.conduit;

import java.util.Properties;

import net.kwatee.agiledeployment.common.exception.MissingVariableException;

public interface ValueResolver {

	/**
	 * Returns the value of a server variable
	 * 
	 * @param variableName
	 * @param params
	 *            the server properties
	 * @param deployment
	 *            a deployment reference
	 * @param instance
	 *            the {@link ServerInstance}
	 * @param version
	 *            a version reference
	 * @return variable value
	 * @throws MissingVariableException
	 */
	String getValue(String variableName, Properties params, Object deployment, ServerInstance instance, Object version) throws MissingVariableException;
}
