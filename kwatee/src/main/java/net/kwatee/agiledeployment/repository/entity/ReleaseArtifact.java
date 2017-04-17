/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.entity;

import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import net.kwatee.agiledeployment.common.exception.CompatibilityException;
import net.kwatee.agiledeployment.common.exception.NoActiveVersionException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("serial")
@Entity(name = "KWDeploymentPackage")
@Table(name = "KWDeploymentPackage")
public class ReleaseArtifact implements java.io.Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "deployment_id", nullable = false)
	private Release release;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "server_id", nullable = true)
	private Server server;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "package_id", nullable = false)
	private Artifact artifact;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "version_id", nullable = true)
	private Version version;
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinTable(name = "kw_dp_file", joinColumns = @JoinColumn(name = "dp_id"), inverseJoinColumns = @JoinColumn(name = "file_id"))
	private Collection<RepositoryFile> files = new java.util.ArrayList<RepositoryFile>(0);
	private Boolean has_overlays;

	transient private Version active_version = null;

	public ReleaseArtifact() {}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Release getRelease() {
		return this.release;
	}

	public void setRelease(Release release) {
		this.release = release;
	}

	public Server getServer() {
		return this.server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public Artifact getArtifact() {
		return this.artifact;
	}

	public void setArtifact(Artifact artifact) {
		this.artifact = artifact;
	}

	public Version getVersion() {
		return this.version;
	}

	public void setVersion(Version version) {
		this.active_version = null;
		this.version = version;
	}

	public boolean overlaysInitialized() {
		return this.has_overlays != null;
	}

	public boolean hasOverlays() {
		return BooleanUtils.isTrue(this.has_overlays);
	}

	public void setHasOverlays(boolean hasOverlays) {
		this.has_overlays = hasOverlays;
	}

	public Collection<RepositoryFile> getFiles() {
		return this.files;
	}

	/**
	 * Find topmost layer file
	 * 
	 * @param path
	 * @return repository file
	 * @throws ObjectNotExistException
	 */
	public RepositoryFile getFile(String path) throws ObjectNotExistException {
		for (RepositoryFile f : files) {
			if (f.getRelativePath().equals(path)) {
				return f;
			}
		}
		throw new ObjectNotExistException(ObjectNotExistException.FILE, path);
	}

	public Version getActiveVersion() throws CompatibilityException, NoActiveVersionException {
		if (this.active_version != null) {
			return this.active_version;
		}
		this.active_version = this.version;
		if (this.active_version == null) {
			ReleaseArtifact dp = this.release.getDefaultReleaseArtifact(this.artifact.getName());
			this.active_version = dp == null ? null : dp.getVersion();
		}
		if (this.active_version == null) {
			if (this.server == null) {
				return null;
			}
			else {
				throw new NoActiveVersionException(this.artifact.getName(), this.server.getName());
			}
		}
		if (this.server != null && !this.active_version.getPlatforms().isEmpty()) {
			int platformId = this.server.getPlatform();
			boolean found = false;
			for (Integer platform : this.active_version.getPlatforms()) {
				if (platformId == platform) {
					found = true;
					break;
				}
			}
			if (!found) {
				throw new CompatibilityException(this.artifact.getName(), this.server.getName());
			}
		}
		return this.active_version;
	}

	public void clearActiveVersion() {
		this.active_version = null;
	}

	public ReleaseArtifact duplicate() {
		ReleaseArtifact duplicateReleaseArtifact = new ReleaseArtifact();
		duplicateReleaseArtifact.setRelease(this.release);
		duplicateReleaseArtifact.setArtifact(this.artifact);
		duplicateReleaseArtifact.setServer(this.server);
		duplicateReleaseArtifact.setVersion(this.version);
		for (RepositoryFile file : this.files) {
			RepositoryFile duplicateFile = file.duplicate();
			duplicateReleaseArtifact.getFiles().add(duplicateFile);
		}
		return duplicateReleaseArtifact;
	}

	@Override
	public String toString() {
		return "artifact " + (this.active_version == null ? (this.version == null ? this.artifact.toString() : this.version.toString()) : this.active_version.toString()) + " in " +
				(this.release == null ? StringUtils.EMPTY : (this.release.toString() + '@')) +
				(this.server == null ? "<any>" : this.server.toString());

	}

	@Override
	public int hashCode() {
		return this.release == null ? this.version.hashCode() : (this.release.hashCode() + this.server.hashCode() + this.artifact.hashCode());
	}

	@Override
	public boolean equals(Object o) {
		ReleaseArtifact that = (ReleaseArtifact) o;
		if (that == null || (this.server == null ^ that.server == null) || (this.artifact == null ^ that.artifact == null))
			return false;
		if (this.server != null && !this.server.equals(that.server))
			return false;
		if (this.artifact != null && !this.artifact.equals(that.artifact))
			return false;
		return true;
	}
}
