/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.entity;

import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import net.kwatee.agiledeployment.common.utils.CompareUtils;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("serial")
@Entity(name = "KWEnvironment")
@Table(name = "KWEnvironment")
public class Environment implements java.io.Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private long creation_ts = new java.util.Date().getTime();
	private Long disable_ts;
	private String name;
	private String description = StringUtils.EMPTY;
	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "kw_environment_server", joinColumns = @JoinColumn(name = "environment_id"))
	@OrderBy("pos")
	private Collection<EnvironmentServer> servers = new java.util.ArrayList<EnvironmentServer>(0);
	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "kw_environment_package", joinColumns = @JoinColumn(name = "environment_id"))
	@OrderBy("pos")
	private Collection<EnvironmentArtifact> artifacts = new java.util.ArrayList<EnvironmentArtifact>(0);
	@OneToMany(mappedBy = "environment", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	Collection<Release> releases = new java.util.ArrayList<Release>(0);
	private boolean sequential_deployment;

	public Environment() {}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public long getCreationTs() {
		return this.creation_ts;
	}

	public void setCreationTs(long creationTs) {
		this.creation_ts = creationTs;
	}

	public Long getDisableTs() {
		return this.disable_ts;
	}

	public void setDisableTs(Long disableTs) {
		this.disable_ts = disableTs;
	}

	public boolean isDisabled() {
		return this.disable_ts != null;
	}

	public void setDisabled(boolean disabled) {
		if (!disabled) {
			setDisableTs(null);
		} else if (getDisableTs() == null) {
			setDisableTs(new java.util.Date().getTime());
		}
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Collection<Release> getReleases() {
		return this.releases;
	}

	public Collection<EnvironmentServer> getServers() {
		return this.servers;
	}

	public void setServers(Collection<EnvironmentServer> servers) {
		this.servers = servers;
	}

	public Collection<EnvironmentArtifact> getArtifacts() {
		return this.artifacts;
	}

	public void setArtifacts(Collection<EnvironmentArtifact> artifacts) {
		this.artifacts = artifacts;
	}

	public boolean isDeploymentSequential() {
		return this.sequential_deployment;
	}

	public void setSequentialDeployment(boolean sequential) {
		this.sequential_deployment = sequential;
	}

	public Environment duplicate(String newName) {
		Environment duplicateEnvironment = new Environment();
		duplicateEnvironment.setName(newName);
		duplicateEnvironment.setDescription(this.description);
		duplicateEnvironment.setSequentialDeployment(this.sequential_deployment);
		for (Release release : this.releases) {
			// Leave tagged releases alone. Duplicate only snapshot
			if (release.isSnapshot()) {
				Release duplicatedRelease = release.duplicate(null, release.getName());
				duplicatedRelease.setEnvironment(duplicateEnvironment);
				duplicateEnvironment.getReleases().add(duplicatedRelease);
			}
		}
		for (EnvironmentArtifact artifact : this.artifacts) {
			EnvironmentArtifact duplicateArtifact = new EnvironmentArtifact();
			duplicateArtifact.setArtifact(artifact.getArtifact());
			duplicateArtifact.setPos(artifact.getPos());
			duplicateEnvironment.artifacts.add(duplicateArtifact);
		}
		for (EnvironmentServer server : this.servers) {
			EnvironmentServer duplicateServer = new EnvironmentServer();
			duplicateServer.setServer(server.getServer());
			duplicateServer.setPos(server.getPos());
			duplicateEnvironment.servers.add(duplicateServer);
		}
		return duplicateEnvironment;
	}

	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public int hashCode() {
		return this.id == null ? 0 : this.id.hashCode();
	}

	@Override
	public boolean equals(Object that) {
		return that != null && CompareUtils.compareTo(this.id, ((Environment) that).id) == 0;
	}
}
