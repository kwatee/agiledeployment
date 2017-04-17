/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.importexport.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import net.kwatee.agiledeployment.common.exception.ArtifactNotInReleaseException;
import net.kwatee.agiledeployment.common.exception.CompatibilityException;
import net.kwatee.agiledeployment.common.exception.NoActiveVersionException;
import net.kwatee.agiledeployment.common.utils.XmlBuffer;
import net.kwatee.agiledeployment.conduit.AccessLevel;
import net.kwatee.agiledeployment.core.deploy.packager.XMLConstants;
import net.kwatee.agiledeployment.repository.entity.Artifact;
import net.kwatee.agiledeployment.repository.entity.EnvironmentArtifact;
import net.kwatee.agiledeployment.repository.entity.EnvironmentServer;
import net.kwatee.agiledeployment.repository.entity.Release;
import net.kwatee.agiledeployment.repository.entity.ReleaseArtifact;
import net.kwatee.agiledeployment.repository.entity.ReleaseVariable;
import net.kwatee.agiledeployment.repository.entity.RepositoryFile;
import net.kwatee.agiledeployment.repository.entity.Server;
import net.kwatee.agiledeployment.repository.entity.ServerProperty;
import net.kwatee.agiledeployment.repository.entity.SystemProperty;
import net.kwatee.agiledeployment.repository.entity.Version;
import net.kwatee.agiledeployment.repository.entity.VersionVariable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.StringUtils;

public class ConfigurationXmlBuilder {

	final private Release release;
	final private XmlBuffer xml;

	public ConfigurationXmlBuilder(Release release) {
		this.release = release;
		this.xml = new XmlBuffer();
	}

	/**
	 * @param globalVariables
	 * @throws ArtifactNotInReleaseException
	 */
	public String toXml(Collection<SystemProperty> globalVariables) throws ArtifactNotInReleaseException {
		this.xml.openTag(XMLConstants.ENVIRONMENT);
		this.xml.addAttribute(XMLConstants.NAME, this.release.getEnvironment().getName());

		this.xml.openTag(XMLConstants.SERVERS);
		this.xml.addAttribute(XMLConstants.SEQUENTIAL, Boolean.toString(this.release.getEnvironment().isDeploymentSequential()));
		/*
		 * Traverse servers list in environment order
		 */
		for (EnvironmentServer es : this.release.getEnvironment().getServers()) {
			Server server = es.getServer();
			if (!server.isDisabled()) {
				Collection<ReleaseArtifact> releaseArtifacts = this.release.getServerArtifacts(server.getName());
				/*
				 * Create list of non-disabled server versions ordered per environment artifact order
				 */
				Collection<ReleaseArtifact> artifacts = new ArrayList<>();
				for (final EnvironmentArtifact ea : this.release.getEnvironment().getArtifacts()) {
					if (!ea.getArtifact().isDisabled()) {
						ReleaseArtifact artifact = (ReleaseArtifact) CollectionUtils.find(releaseArtifacts, new Predicate() {

							@Override
							public boolean evaluate(Object artifact) {
								ReleaseArtifact ra = (ReleaseArtifact) artifact;
								try {
									return ra.getArtifact().getName().equals(ea.getArtifact().getName()) && !ra.getActiveVersion().isDisabled();
								} catch (NoActiveVersionException | CompatibilityException e) {
									return false;
								}
							}
						});
						if (artifact != null)
							artifacts.add(artifact);
					}
				}
				if (!artifacts.isEmpty()) {
					this.xml.openTag(XMLConstants.SERVER);
					server2Xml(server);
					serverArtifacts2xml(artifacts);
					this.xml.closeTag();
				}
			}
		}
		this.xml.closeTag();
		environmentVariables2xml(globalVariables);
		this.xml.closeTag();
		return this.xml.toString();
	}

	/**
	 * @param server
	 * @param release
	 */
	private void server2Xml(Server server) {
		this.xml.addAttribute(XMLConstants.NAME, server.getName());
		this.xml.addTagWithValue(XMLConstants.IPADDRESS, server.getIPAddress());
		this.xml.addTagWithValue(XMLConstants.PORT, String.valueOf(server.getPort()));
		this.xml.addTagWithValue(XMLConstants.PLATFORM, Integer.toString(server.getPlatform()));
		this.xml.addTagWithValue(XMLConstants.CONDUIT_TYPE, server.getConduitType());
		if (server.getPoolType() != null) {
			this.xml.addTagWithValue(XMLConstants.POOL_TYPE, server.getPoolType());
			this.xml.addTagWithValue(XMLConstants.CONCURRENCY, String.valueOf(server.getPoolConcurrency()));
		}
		this.xml.openTag(XMLConstants.PROPERTIES);
		for (ServerProperty p : server.getProperties()) {
			this.xml.openTag(XMLConstants.PROPERTY);
			this.xml.addAttribute(XMLConstants.NAME, p.getName());
			this.xml.addValue(p.getValue());
			this.xml.closeTag();
		}
		this.xml.closeTag();
		if (server.getCredentials() != null && server.getCredentials().getAccessLevel().ordinal() >= AccessLevel.VIEWER.ordinal()) {
			this.xml.openTag(XMLConstants.CREDENTIALS);
			if (!server.getCredentials().isPasswordPrompted()) {
				if (server.getCredentials().getPassword().isEmpty()) {
					this.xml.openTag(XMLConstants.PASSWORD);
					this.xml.closeTag();
				} else
					this.xml.addTagWithValue(XMLConstants.PASSWORD, server.getCredentials().getPassword());
			}
			this.xml.addTagWithValue(XMLConstants.LOGIN, server.getCredentials().getLogin());
			if (StringUtils.isNotEmpty(server.getCredentials().getPem())) {
				this.xml.addTagWithCData(XMLConstants.PEM, server.getCredentials().getPem());
			}
			this.xml.closeTag();
		}
	}

	private void serverArtifacts2xml(Collection<ReleaseArtifact> artifacts) throws ArtifactNotInReleaseException {
		this.xml.openTag(XMLConstants.ARTIFACTS);
		for (ReleaseArtifact artifact : artifacts) {
			this.xml.openTag(XMLConstants.ARTIFACT);
			this.xml.addAttribute(XMLConstants.NAME, artifact.getArtifact().getName());
			this.xml.openTag(XMLConstants.OVERLAYS);
			HashSet<RepositoryFile> files = new HashSet<RepositoryFile>();
			for (RepositoryFile f : artifact.getFiles()) {
				files.add(f);
			}
			ReleaseArtifact a = this.release.getReleaseArtifact(null, artifact.getArtifact());
			for (RepositoryFile f : a.getFiles()) {
				files.add(f);
			}
			for (RepositoryFile f : files) {
				BundleXmlBuilder.file2xml(this.xml, f);
			}
			this.xml.closeTag();
			this.xml.closeTag();
		}
		this.xml.closeTag();
	}

	private void environmentVariables2xml(Collection<SystemProperty> globalVariables) {
		Collection<ConfigurationVariable> variables = consolidateVariables(globalVariables);
		this.xml.openTag(XMLConstants.VARIABLES);
		for (ConfigurationVariable variable : variables) {
			if (variable.getArtifactId() != null && variable.getServerId() != null)
				variable2xml(variable);
		}
		for (ConfigurationVariable variable : variables) {
			if (variable.getArtifactId() == null && variable.getServerId() != null)
				variable2xml(variable);
		}
		for (ConfigurationVariable variable : variables) {
			if (variable.getArtifactId() != null && variable.getServerId() == null)
				variable2xml(variable);
		}
		for (ConfigurationVariable variable : variables) {
			if (variable.getArtifactId() == null && variable.getServerId() == null)
				variable2xml(variable);
		}
		this.xml.closeTag();
	}

	private void variable2xml(ConfigurationVariable variable) {
		Server server = getEnvironmentServer(variable.getServerId());
		Artifact artifact = getEnvironmentArtifact(variable.getArtifactId());
		if ((artifact == null || !artifact.isDisabled()) && (server == null || !server.isDisabled())) {
			this.xml.openTag(XMLConstants.VARIABLE);
			this.xml.addAttribute(XMLConstants.NAME, variable.getName());
			if (artifact != null) {
				this.xml.addAttribute(XMLConstants.ARTIFACT, artifact.getName());
			}
			if (server != null) {
				this.xml.addAttribute(XMLConstants.SERVER, server.getName());
			}
			this.xml.addValue(variable.getValue());
			this.xml.closeTag();
		}
	}

	private Collection<ConfigurationVariable> consolidateVariables(Collection<SystemProperty> globalVariables) {
		Collection<ConfigurationVariable> variables = new HashSet<ConfigurationVariable>();
		Collection<ReleaseVariable> releaseVariables = this.release.getVariables();
		for (ReleaseVariable variable : releaseVariables) {
			if (variable.getArtifactId() != null && variable.getServerId() != null)
				variables.add(new ConfigurationVariable(variable.getName(), variable.getValue(), variable.getArtifactId(), variable.getServerId()));
		}
		for (ReleaseVariable variable : releaseVariables) {
			if (variable.getArtifactId() == null && variable.getServerId() != null)
				variables.add(new ConfigurationVariable(variable.getName(), variable.getValue(), null, variable.getServerId()));
		}
		for (ReleaseVariable variable : releaseVariables) {
			if (variable.getArtifactId() != null && variable.getServerId() == null)
				variables.add(new ConfigurationVariable(variable.getName(), variable.getValue(), variable.getArtifactId(), null));
		}
		for (ReleaseVariable variable : releaseVariables) {
			if (variable.getArtifactId() == null && variable.getServerId() == null)
				variables.add(new ConfigurationVariable(variable.getName(), variable.getValue(), null, null));
		}
		for (EnvironmentArtifact ea : this.release.getEnvironment().getArtifacts()) {
			Artifact artifact = ea.getArtifact();
			if (!artifact.isDisabled()) {
				long artifactId = artifact.getId();
				for (ReleaseArtifact ra : this.release.getReleaseArtifacts()) {
					try {
						Version version = ra.getActiveVersion();
						if (version != null && !version.isDisabled()) {
							for (VersionVariable variable : version.getVariablesDefaultValues()) {
								variables.add(new ConfigurationVariable(variable.getName(), variable.getDefaultValue(), artifactId, null));
							}
						}
					} catch (CompatibilityException | NoActiveVersionException e) {}
				}
			}
		}
		for (SystemProperty prop : globalVariables) {
			variables.add(new ConfigurationVariable(prop.getName(), prop.getValue(), null, null));
		}
		return variables;
	}

	private Server getEnvironmentServer(Long id) {
		if (id != null) {
			for (EnvironmentServer es : this.release.getEnvironment().getServers()) {
				if (id.equals(es.getServer().getId()))
					return es.getServer();
			}
		}
		return null;
	}

	private Artifact getEnvironmentArtifact(Long id) {
		if (id != null) {
			for (EnvironmentArtifact ea : this.release.getEnvironment().getArtifacts()) {
				if (id.equals(ea.getArtifact().getId()))
					return ea.getArtifact();
			}
		}
		return null;
	}

	static class ConfigurationVariable {

		final private String name;
		final private String value;
		final private Long artifactId;
		final private Long serverId;

		ConfigurationVariable(String name, String value, Long artifactId, Long serverId) {
			this.name = name;
			this.value = value;
			this.artifactId = artifactId;
			this.serverId = serverId;
		}

		String getName() {
			return this.name;
		}

		String getValue() {
			return this.value;
		}

		Long getArtifactId() {
			return this.artifactId;
		}

		Long getServerId() {
			return this.serverId;
		}

		@Override
		public int hashCode() {
			if (this.serverId != null)
				if (this.artifactId != null)
					return (this.name + this.artifactId + this.serverId).hashCode();
				else
					return (this.name + this.serverId).hashCode();
			else if (this.artifactId != null)
				return (this.name + this.artifactId).hashCode();
			else
				return this.name.hashCode();
		}

		@Override
		public boolean equals(Object that) {
			ConfigurationVariable v = (ConfigurationVariable) that;
			return this.name.equals(v.name) &&
					(this.artifactId == null && v.artifactId == null ||
					this.artifactId != null && this.artifactId.equals(v.artifactId) &&
							(this.serverId == null && v.serverId == null ||
							this.serverId != null && this.serverId.equals(v.serverId)));

		}

	}
}