/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import net.kwatee.agiledeployment.common.Constants;
import net.kwatee.agiledeployment.common.exception.ArtifactNotInReleaseException;
import net.kwatee.agiledeployment.common.utils.CompareUtils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("serial")
@Entity(name = "KWDeployment")
@Table(name = "KWDeployment")
public class Release implements java.io.Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private long creation_ts = 0;
	private Long disable_ts;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "environment_id", nullable = false)
	private Environment environment;
	private String name = Constants.SNAPSHOT_RELEASE_NAME;
	private String description = StringUtils.EMPTY;
	private String pre_setup_action = StringUtils.EMPTY;
	private String post_setup_action = StringUtils.EMPTY;
	private String pre_cleanup_action = StringUtils.EMPTY;
	private String post_cleanup_action = StringUtils.EMPTY;
	@OneToMany(mappedBy = "release", cascade = CascadeType.ALL, orphanRemoval = true)
	List<ReleaseVariable> variables = new java.util.ArrayList<ReleaseVariable>(0);
	@OneToMany(mappedBy = "release", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ReleaseArtifact> release_artifacts = new java.util.ArrayList<ReleaseArtifact>(0);
	private boolean stop_on_first_error = false;
	private boolean has_errors = false;
	private String file_owner;
	private String file_group;
	private Integer file_mode;
	private Integer dir_mode;

	transient boolean variables_sorted = false;
	transient boolean release_artifacts_sorted = false;

	public Release() {}

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
		}
		else if (getDisableTs() == null) {
			setDisableTs(new java.util.Date().getTime());
		}
	}

	public Environment getEnvironment() {
		return this.environment;
	}

	public void setEnvironment(Environment environment) {
		this.environment = environment;
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

	public String getPreSetupAction() {
		return this.pre_setup_action;
	}

	public void setPreSetupAction(String action) {
		this.pre_setup_action = action;
	}

	public String getPostSetupAction() {
		return this.post_setup_action;
	}

	public void setPostSetupAction(String action) {
		this.post_setup_action = action;
	}

	public String getPreCleanupAction() {
		return this.pre_cleanup_action;
	}

	public void setPreCleanupAction(String action) {
		this.pre_cleanup_action = action;
	}

	public String getPostCleanupAction() {
		return this.post_cleanup_action;
	}

	public void setPostCleanupAction(String action) {
		this.post_cleanup_action = action;
	}

	public void resetVariables() {
		this.variables_sorted = false;
	}

	public List<ReleaseVariable> getVariables() {
		if (!this.variables_sorted) {
			Collections.sort(this.variables);
			this.variables_sorted = true;
		}
		return this.variables;
	}

	public boolean stopOnFirstError() {
		return this.stop_on_first_error;
	}

	public void setStopOnFirstError(boolean stop) {
		this.stop_on_first_error = stop;
	}

	public boolean hasErrors() {
		return this.has_errors;
	}

	public void setHasErrors(boolean hasErrors) {
		this.has_errors = hasErrors;
	}

	/*
	 * Sorted release artifacts according to default artifact/server order, if necessary
	 */
	private void sortReleaseArtifacts() {
		if (!this.release_artifacts_sorted) {
			Collections.sort(this.release_artifacts, new Comparator<ReleaseArtifact>() {

				private int getServerPos(Server server) {
					if (server != null) {
						for (EnvironmentServer environmentServer : Release.this.environment.getServers()) {
							if (environmentServer.getServer().equals(server)) {
								return environmentServer.getPos() == null ? 0 : environmentServer.getPos();
							}
						}
					}
					return -1;
				}

				private int getArtifactPos(Artifact artifact) {
					if (artifact != null) {
						for (EnvironmentArtifact environmentArtifact : Release.this.environment.getArtifacts()) {
							if (environmentArtifact.getArtifact().equals(artifact)) {
								return environmentArtifact.getPos() == null ? 0 : environmentArtifact.getPos();
							}
						}
					}
					return -1;
				}

				public int compare(ReleaseArtifact dp1, ReleaseArtifact dp2) {
					int p1 = getServerPos(dp1.getServer());
					int p2 = getServerPos(dp2.getServer());
					if (p1 < p2)
						return -1;
					if (p1 > p2)
						return 1;
					// The servers are the same, compare the artifacts
					if (dp1.getArtifact().equals(dp2.getArtifact())) {
						return 0;
					}
					p1 = getArtifactPos(dp1.getArtifact());
					p2 = getArtifactPos(dp2.getArtifact());
					return p1 < p2 ? -1 : 1;
				}
			});
			this.release_artifacts_sorted = true;
		}
	}

	public Collection<ReleaseArtifact> getReleaseArtifacts() {
		sortReleaseArtifacts();
		return this.release_artifacts;
	}

	public Collection<ReleaseArtifact> getRawReleaseArtifacts() {
		return this.release_artifacts;
	}

	public void setReleaseArtifacts(List<ReleaseArtifact> releaseArtifacts) {
		this.release_artifacts = releaseArtifacts;
		this.release_artifacts_sorted = false;
	}

	public void resetReleaseArtifacts() {
		this.release_artifacts_sorted = false;
	}

	public Collection<ReleaseArtifact> getServerArtifacts(final String serverName) {
		@SuppressWarnings("unchecked")
		Collection<ReleaseArtifact> serverArtifacts = CollectionUtils.select(getReleaseArtifacts(), new Predicate() {

			public boolean evaluate(Object dp) {
				if (serverName == null) {
					if (((ReleaseArtifact) dp).getServer() == null) {
						return true;
					}
				} else if (((ReleaseArtifact) dp).getServer() != null && serverName.equals(((ReleaseArtifact) dp).getServer().getName())) {
					return true;
				}
				return false;
			}
		});
		return serverArtifacts != null ? serverArtifacts : new java.util.ArrayList<ReleaseArtifact>(0);
	}

	public ReleaseArtifact getDefaultReleaseArtifact(final String artifactName) {
		ReleaseArtifact dp = (ReleaseArtifact) CollectionUtils.find(release_artifacts, new Predicate() {

			public boolean evaluate(Object dp) {
				return ((ReleaseArtifact) dp).getServer() == null && ((ReleaseArtifact) dp).getArtifact().getName().equals(artifactName);
			}
		});
		return dp;
	}

	public void clearArtifactActiveVersions() {
		for (ReleaseArtifact dp : getReleaseArtifacts()) {
			dp.clearActiveVersion();
		}
	}

	public String getFileOwner() {
		return file_owner;
	}

	public void setFileOwner(String fileOwner) {
		this.file_owner = fileOwner;
	}

	public String getFileGroup() {
		return file_group;
	}

	public void setFileGroup(String fileGroup) {
		this.file_group = fileGroup;
	}

	public Integer getFileMode() {
		return file_mode;
	}

	public void setFileMode(Integer fileMode) {
		this.file_mode = fileMode;
	}

	public Integer getDirMode() {
		return dir_mode;
	}

	public void setDirMode(Integer dirMode) {
		this.dir_mode = dirMode;
	}

	public Release duplicate(Release duplicateRelease, String newName) {
		if (duplicateRelease == null) {
			duplicateRelease = new Release();
		} else {
			duplicateRelease.release_artifacts.clear();
			duplicateRelease.variables.clear();
			duplicateRelease.setDisabled(false);
		}
		if (newName != null) {
			duplicateRelease.setName(newName);
		}
		duplicateRelease.setDescription(this.description);
		duplicateRelease.setEnvironment(this.environment);
		duplicateRelease.setPreSetupAction(this.pre_setup_action);
		duplicateRelease.setPostSetupAction(this.post_setup_action);
		duplicateRelease.setPreCleanupAction(this.pre_cleanup_action);
		duplicateRelease.setPostCleanupAction(this.post_cleanup_action);
		for (ReleaseVariable variable : this.variables) {
			if (!variable.isFrozenSystemProperty()) {
				ReleaseVariable duplicateVariable = variable.duplicate();
				duplicateVariable.setRelease(duplicateRelease);
				duplicateRelease.variables.add(duplicateVariable);
			}
		}
		for (ReleaseArtifact dp : this.release_artifacts) {
			ReleaseArtifact dupdp = dp.duplicate();
			dupdp.setRelease(duplicateRelease);
			duplicateRelease.release_artifacts.add(dupdp);
		}
		duplicateRelease.setFileOwner(this.file_owner);
		duplicateRelease.setFileGroup(this.file_group);
		duplicateRelease.setFileMode(this.file_mode);
		duplicateRelease.setDirMode(this.dir_mode);

		return duplicateRelease;
	}

	public ReleaseArtifact getReleaseArtifact(Server server, Artifact artifact) throws ArtifactNotInReleaseException {
		for (ReleaseArtifact dp : getReleaseArtifacts()) {
			Server dpServer = dp.getServer();
			if ((server == null && dpServer == null) || (server != null && dpServer != null && server.getId().equals(dp.getServer().getId()))) {
				if (artifact.getId().equals(dp.getArtifact().getId())) {
					return dp;
				}
			}
		}
		throw new ArtifactNotInReleaseException(artifact.getName());
	}

	public boolean isSnapshot() {
		return this.creation_ts == 0;
	}

	@Override
	public String toString() {
		return this.environment.getName() + '-' + this.name;
	}

	@Override
	public int hashCode() {
		return this.id == null ? 0 : this.id.hashCode();
	}

	@Override
	public boolean equals(Object that) {
		return that != null && CompareUtils.equals(this.id, ((Release) that).id);
	}
}
