/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common;

public interface VariableResolver {

	String ENVIRONMENT_NAME = "kwatee_environment_name";
	String ARTIFACT_NAME = "kwatee_artifact_name";
	String VERSION_NAME = "kwatee_version_name";
	String RELEASE_NAME = "kwatee_release_name";
	String SERVER_PLATFORM = "kwatee_server_platform";

	String DEPLOYMENT_DIR = "kwatee_deployment_dir";
	String ARTIFACT_DIR = "kwatee_package_dir";

	String getVariableValue(String varName);

	Character getVariablePrefixChar();

	String getResolverName();

	Integer getServerPlatform();
}
