/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.variable.impl;

import net.kwatee.agiledeployment.common.VariableResolver;
import net.kwatee.agiledeployment.conduit.ServerInstance;
import net.kwatee.agiledeployment.core.deploy.PlatformService;
import net.kwatee.agiledeployment.repository.entity.Release;
import net.kwatee.agiledeployment.repository.entity.ReleaseVariable;
import net.kwatee.agiledeployment.repository.entity.Server;
import net.kwatee.agiledeployment.repository.entity.Version;
import net.kwatee.agiledeployment.repository.entity.VersionVariable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.StringUtils;

/**
 * 
 * @author mac
 * 
 */
public class RepoVariableResolver implements VariableResolver {

	final private Release release;
	final private Server server;
	final private ServerInstance instance;
	final private Version version;

	public RepoVariableResolver(Version version) {
		this(null, null, null, version);
	}

	public RepoVariableResolver(Release release, Server server) {
		this(release, server, null, null);
	}

	public RepoVariableResolver(Release release, Server server, ServerInstance instance) {
		this(release, server, instance, null);
	}

	public RepoVariableResolver(Release release, Server server, Version version) {
		this(release, server, null, version);
	}

	public RepoVariableResolver(Release release, Server server, ServerInstance instance, Version version) {
		this.release = release;
		this.server = server;
		this.instance = instance;
		this.version = version;
	}

	@Override
	public String getVariableValue(String varName) {
		if (ENVIRONMENT_NAME.equals(varName))
			return this.release == null ? null : this.release.getEnvironment().getName();
		if (RELEASE_NAME.equals(varName))
			return this.release == null ? null : this.release.getName();
		if (ARTIFACT_NAME.equals(varName))
			return this.version == null ? null : this.version.getArtifact().getName();
		if (VERSION_NAME.equals(varName))
			return this.version == null ? null : this.version.getName();
		if (this.server != null) {
			if (SERVER_PLATFORM.equals(varName)) {
				Integer platform = this.server.getPlatform();
				if (platform == null)
					return StringUtils.EMPTY;
				return PlatformService.getInstance().getName(platform);
			}
			if (this.instance != null) {
				String val = this.instance.getProperty(varName);
				if (val != null)
					return val;
			}
			String val = this.server.getServerProperties().get(varName);
			if (val != null)
				return val;

		}

		String val = null;
		if (this.release != null) {
			val = getReleaseVariable(varName);
		}
		if (val == null && version != null) {
			val = getVersionVariable(varName);
		}
		return val;
	}

	/**
	 * 
	 * @param varName
	 * @param release
	 * @param server
	 * @param version
	 * @return
	 */
	private String getReleaseVariable(final String varName) {
		ReleaseVariable var = null;
		/* Check for perfect match */
		final Long serverId = this.server == null ? null : server.getId();
		if (serverId != null && this.version != null) {
			var = (ReleaseVariable) CollectionUtils.find(this.release.getVariables(),
					new Predicate() {

						public boolean evaluate(Object v) {
							ReleaseVariable var = (ReleaseVariable) v;
							if (var.getServerId() == null || var.getArtifactId() == null) {
								return false;
							}
							if (!serverId.equals(var.getServerId()) ||
									!RepoVariableResolver.this.version.getArtifact().getId().equals(var.getArtifactId())) {
								return false;
							}
							return varName.equals(var.getName());
						}
					});
		}
		/* Otherwise check for artifact wildcard and server match */
		if (var == null && serverId != null) {
			var = (ReleaseVariable) CollectionUtils.find(this.release.getVariables(),
					new Predicate() {

						public boolean evaluate(Object v) {
							ReleaseVariable var = (ReleaseVariable) v;
							if (var.getArtifactId() != null) {
								return false;
							}
							if (var.getServerId() == null || !serverId.equals(var.getServerId())) {
								return false;
							}
							return varName.equals(var.getName());
						}
					});
		}
		/* Otherwise check for server wildcard and artifact match */
		if (var == null && this.version != null) {
			var = (ReleaseVariable) CollectionUtils.find(this.release.getVariables(),
					new Predicate() {

						public boolean evaluate(Object v) {
							ReleaseVariable var = (ReleaseVariable) v;
							if (var.getServerId() != null) {
								return false;
							}
							if (var.getArtifactId() == null ||
									!RepoVariableResolver.this.version.getArtifact().getId().equals(var.getArtifactId())) {
								return false;
							}
							return varName.equals(var.getName());
						}
					});
		}
		/* Otherwise check for artifact wildcard and server wildcard */
		if (var == null) {
			var = (ReleaseVariable) CollectionUtils.find(this.release.getVariables(),
					new Predicate() {

						public boolean evaluate(Object v) {
							ReleaseVariable var = (ReleaseVariable) v;
							if (var.getServerId() != null || var.getArtifactId() != null) {
								return false;
							}
							return varName.equals(var.getName());
						}
					});
		}
		return var == null ? null : var.getValue();
	}

	/**
	 * 
	 * @param varName
	 * @param version
	 * @return
	 */
	private String getVersionVariable(final String varName) {
		if (this.version != null) {
			VersionVariable var = (VersionVariable) CollectionUtils.find(this.version.getVariablesDefaultValues(), new Predicate() {

				public boolean evaluate(Object v) {
					return ((VersionVariable) v).getName().equals(varName);
				}
			});
			if (var != null) {
				return var.getDefaultValue();
			}
		}
		return null;
	}

	@Override
	public Character getVariablePrefixChar() {
		return this.version == null ? null : version.getVarPrefixChar();
	}

	@Override
	public String getResolverName() {
		if (this.version == null)
			return this.server == null ? StringUtils.EMPTY : this.server.getName();
		String artifact = this.version.getArtifact().getName() + '[' + this.version.getName() + ']';
		return this.server == null ? artifact : (artifact + "@" + this.server.getName());
	}

	@Override
	public Integer getServerPlatform() {
		return this.server == null ? null : this.server.getPlatform();
	}
}
