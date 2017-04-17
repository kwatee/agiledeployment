/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.importexport.xml;

import java.util.Collection;
import java.util.HashSet;

import net.kwatee.agiledeployment.common.exception.CompatibilityException;
import net.kwatee.agiledeployment.common.exception.NoActiveVersionException;
import net.kwatee.agiledeployment.common.exception.ObjectAlreadyExistsException;
import net.kwatee.agiledeployment.common.utils.XmlBuffer;
import net.kwatee.agiledeployment.core.deploy.packager.XMLConstants;
import net.kwatee.agiledeployment.repository.entity.Artifact;
import net.kwatee.agiledeployment.repository.entity.EnvironmentArtifact;
import net.kwatee.agiledeployment.repository.entity.Executable;
import net.kwatee.agiledeployment.repository.entity.Release;
import net.kwatee.agiledeployment.repository.entity.ReleaseArtifact;
import net.kwatee.agiledeployment.repository.entity.RepositoryFile;
import net.kwatee.agiledeployment.repository.entity.Version;
import net.kwatee.agiledeployment.repository.entity.VersionVariable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

public class BundleXmlBuilder {

	final private Release release;
	final private XmlBuffer xml;

	public BundleXmlBuilder(Release release) {
		this.release = release;
		this.xml = new XmlBuffer();
	}

	/**
	 * @return the environment/releases list in bundle format
	 */
	public String toXml() throws ObjectAlreadyExistsException {
		this.xml.openTag(XMLConstants.BUNDLE);
		this.xml.addAttribute(XMLConstants.NAME, this.release.getEnvironment().getName());
		this.xml.addAttribute(XMLConstants.VERSION, this.release.getName());
		this.xml.addAttribute(XMLConstants.FILE_OWNER, this.release.getFileOwner());
		this.xml.addAttribute(XMLConstants.FILE_GROUP, this.release.getFileGroup());
		this.xml.addAttribute(XMLConstants.FILE_MODE, this.release.getFileMode());
		this.xml.addAttribute(XMLConstants.DIR_MODE, this.release.getDirMode());
		HashSet<Version> versions = new HashSet<Version>();
		for (ReleaseArtifact ra : this.release.getReleaseArtifacts()) {
			try {
				Version version = ra.getActiveVersion();
				if (version != null && !version.isDisabled() && !version.getArtifact().isDisabled())
					versions.add(version);
			} catch (CompatibilityException | NoActiveVersionException e) {}
		}
		this.xml.addTagWithValue(XMLConstants.PRE_SETUP_ACTION, this.release.getPreSetupAction());
		this.xml.addTagWithValue(XMLConstants.POST_SETUP_ACTION, this.release.getPostSetupAction());
		this.xml.addTagWithValue(XMLConstants.PRE_CLEANUP_ACTION, this.release.getPreCleanupAction());
		this.xml.addTagWithValue(XMLConstants.POST_CLEANUP_ACTION, this.release.getPostCleanupAction());
		this.xml.addTagWithValue(XMLConstants.POST_CLEANUP_ACTION, this.release.getPostCleanupAction());
		this.xml.openTag(XMLConstants.ARTIFACTS);
		for (EnvironmentArtifact ea : this.release.getEnvironment().getArtifacts()) {
			final Artifact artifact = ea.getArtifact();
			@SuppressWarnings("unchecked")
			Collection<Version> artifactBundleVersions = CollectionUtils.select(versions, new Predicate() {

				@Override
				public boolean evaluate(Object version) {
					return artifact.getId().equals(((Version) version).getArtifact().getId());
				}

			});
			if (artifactBundleVersions.size() > 1)
				throw new ObjectAlreadyExistsException("Artifact");
			if (artifactBundleVersions.size() == 1)
				artifact2xml(artifactBundleVersions.iterator().next());
		}
		this.xml.closeTag();
		this.xml.closeTag();
		return this.xml.toString();
	}

	private void artifact2xml(Version version) {
		this.xml.openTag(XMLConstants.ARTIFACT);
		this.xml.addAttribute(XMLConstants.NAME, version.getArtifact().getName());
		this.xml.addAttribute(XMLConstants.VERSION, version.getName());
		this.xml.addAttribute(XMLConstants.VAR_PREFIX, Character.toString(version.getVarPrefixChar()));
		this.xml.addTagWithValue(XMLConstants.PRE_DEPLOY_ACTION, version.getPreDeployAction());
		this.xml.addTagWithValue(XMLConstants.POST_DEPLOY_ACTION, version.getPostDeployAction());
		this.xml.addTagWithValue(XMLConstants.PRE_UNDEPLOY_ACTION, version.getPreUndeployAction());
		this.xml.addTagWithValue(XMLConstants.POST_UNDEPLOY_ACTION, version.getPostUndeployAction());
		variables2xml(version.getVariablesDefaultValues());
		executables2xml(version.getExecutables());
		package2xml(version);
		this.xml.closeTag();
	}

	private void variables2xml(Collection<VersionVariable> variables) {
		if (!variables.isEmpty()) {
			this.xml.openTag(XMLConstants.VARIABLES);
			for (VersionVariable var : variables) {
				this.xml.openTag(XMLConstants.VARIABLE);
				this.xml.addAttribute(XMLConstants.NAME, var.getName());
				this.xml.addValue(var.getDefaultValue());
				this.xml.closeTag();
			}
			this.xml.closeTag();
		}

	}

	private void executables2xml(Collection<Executable> executables) {
		if (!executables.isEmpty()) {
			this.xml.openTag(XMLConstants.EXECUTABLES);
			for (Executable exe : executables) {
				this.xml.openTag(XMLConstants.EXECUTABLE);
				this.xml.addAttribute(XMLConstants.NAME, exe.getName());
				this.xml.addTagWithValue(XMLConstants.START_ACTION, exe.getStartAction());
				this.xml.addTagWithValue(XMLConstants.STOP_ACTION, exe.getStopAction());
				this.xml.addTagWithValue(XMLConstants.STATUS_ACTION, exe.getStatusAction());
				this.xml.closeTag();
			}
			this.xml.closeTag();
		}

	}

	private void package2xml(Version version) {
		this.xml.openTag(XMLConstants.PACKAGE);
		this.xml.addAttribute(XMLConstants.FILE_OWNER, version.getFileOwner());
		this.xml.addAttribute(XMLConstants.FILE_GROUP, version.getFileGroup());
		this.xml.addAttribute(XMLConstants.FILE_MODE, version.getFileMode());
		this.xml.addAttribute(XMLConstants.DIR_MODE, version.getDirMode());
		this.xml.addAttribute(XMLConstants.SIZE, Long.toString(version.getPackageSize()));
		for (RepositoryFile file : version.getFiles()) {
			file2xml(this.xml, file);
		}
		this.xml.closeTag();
	}

	static void file2xml(XmlBuffer xml, RepositoryFile file) {
		xml.openTag(XMLConstants.FILE);
		xml.addAttribute(XMLConstants.SIZE, Long.toString(file.getSize()));
		if (file.isDirectory()) {
			xml.addAttribute(XMLConstants.DIRECTORY, "true");
			xml.addAttribute(XMLConstants.SIGNATURE, file.getSignature());
		} else {
			if (file.ignoreIntegrity())
				xml.addAttribute(XMLConstants.IGNORE_INTEGRITY, "true");
			if (file.dontDelete())
				xml.addAttribute(XMLConstants.DONT_DELETE, "true");
			if (file.isSymbolicLink())
				xml.addAttribute(XMLConstants.SYMLINK, "true");
			xml.addAttribute(XMLConstants.FILE_OWNER, file.getFileOwner());
			xml.addAttribute(XMLConstants.FILE_GROUP, file.getFileGroup());
			xml.addAttribute(XMLConstants.FILE_MODE, file.getFileMode());
			xml.addAttribute(XMLConstants.DIR_MODE, file.getDirMode());
			if (file.isTemplatized() && !file.ignoreVariables())
				xml.addAttribute(XMLConstants.VARIABLES, file.getVariables());
			else
				xml.addAttribute(XMLConstants.SIGNATURE, file.getSignature());
		}
		xml.addValue(file.getRelativePath());
		xml.closeTag();
	}
}