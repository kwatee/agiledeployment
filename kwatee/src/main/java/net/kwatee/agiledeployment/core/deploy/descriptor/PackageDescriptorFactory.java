/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.deploy.descriptor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.kwatee.agiledeployment.common.VariableResolver;
import net.kwatee.agiledeployment.common.exception.InternalErrorException;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.common.utils.CryptoUtils;
import net.kwatee.agiledeployment.common.utils.NameValue;
import net.kwatee.agiledeployment.common.utils.XmlBuffer;
import net.kwatee.agiledeployment.conduit.ServerInstance;
import net.kwatee.agiledeployment.core.deploy.Deployment;
import net.kwatee.agiledeployment.core.repository.DeployStreamProvider;
import net.kwatee.agiledeployment.core.variable.VariableService;
import net.kwatee.agiledeployment.core.variable.impl.DeploymentVariableResolver;
import net.kwatee.agiledeployment.repository.dto.ArtifactDto;
import net.kwatee.agiledeployment.repository.dto.ArtifactVersionDto;
import net.kwatee.agiledeployment.repository.dto.ExecutableDto;
import net.kwatee.agiledeployment.repository.dto.FileDto;
import net.kwatee.agiledeployment.repository.dto.FilePropertiesDto;
import net.kwatee.agiledeployment.repository.dto.PermissionDto;
import net.kwatee.agiledeployment.repository.dto.ServerDto;
import net.kwatee.agiledeployment.repository.dto.VersionDto;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@Component
public class PackageDescriptorFactory {

	final public static String ACTION_PRE_DEPLOY = "pre_deploy";
	final public static String ACTION_POST_DEPLOY = "post_deploy";
	final public static String ACTION_PRE_UNDEPLOY = "pre_undeploy";
	final public static String ACTION_POST_UNDEPLOY = "post_uneploy";
	final public static String ACTION_START_PREFIX = "exe_start_";
	final public static String ACTION_STOP_PREFIX = "exe_stop_";
	final public static String ACTION_STATUS_PREFIX = "exe_status_";

	final private static String XML_KWATEE_NODE = "kwatee";
	final private static String XML_DEPLOYMENT_ATTR = "deployment_name";
	final private static String XML_ARTIFACT_ATTR = "artifact_name";
	final private static String XML_DEPLOY_IN_ATTR = "deploy_in";
	final private static String XML_SIGNATURE_ATTR = "signature";
	final private static String XML_ACTIONS_NODE = "actions";
	final private static String XML_ACTION_NODE = "action";
	final private static String XML_TYPE_ATTR = "type";
	final private static String XML_PACKAGE_NODE = "package";
	final private static String XML_DIR_NODE = "directory";
	final private static String XML_FILE_NODE = "file";
	final private static String XML_SYMLINK_ATTR = "symbolic_link";
	final private static String XML_NAME_ATTR = "name";
	final private static String XML_DONTDELETE_ATTR = "dont_delete";
	final private static String XML_FILE_OWNER_ATTR = "file_owner";
	final private static String XML_FILE_GROUP_ATTR = "file_group";
	final private static String XML_FILE_MODE_ATTR = "file_mode";
	final private static String XML_DIR_MODE_ATTR = "dir_mode";

	static private org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PackageDescriptorFactory.class);

	static private PackageDescriptorFactory instance;

	@Autowired
	private DeployStreamProvider deployStreamProvider;
	@Autowired
	private VariableService variableService;

	public PackageDescriptorFactory() {
		instance = this;
	}

	static public PackageDescriptor loadFromXml(String xml) {
		return instance.load(xml);
	}

	private PackageDescriptor load(String xml) {
		try {
			// Create a factory
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// Use document builder factory
			DocumentBuilder builder = factory.newDocumentBuilder();
			// Parse the document
			InputStream is = new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8));
			Document doc = builder.parse(is);
			Node node = doc.getDocumentElement();
			/*
			 * traverse the descriptor and check that signatures match for each file
			 */
			PackageDescriptor descriptor = new PackageDescriptor();
			descriptor.setFiles(new ArrayList<FileDto>());
			parseDir(descriptor, node.getFirstChild(), StringUtils.EMPTY);
			return descriptor;
		} catch (Exception e) {
			throw new InternalErrorException(e);
		}
	}

	/**
	 * Computes the PackageDescriptor for the deployed artifact.
	 * Template repository files will be replaced by transient instantiated files signed with their instantiated value
	 * 
	 * @param deployment
	 * @param server
	 *            instance
	 * @param artifactVersion
	 * @throws MissingVariableException
	 */
	static public PackageDescriptor loadFromArtifact(Deployment deployment, ServerInstance server, ArtifactVersionDto artifactVersion) throws MissingVariableException {
		VersionDto version = deployment.findVersion(artifactVersion);
		VariableResolver resolver = new DeploymentVariableResolver(deployment, version, server);
		return instance.build(deployment, server.getName(), artifactVersion, resolver);
	}

	/**
	 * Computes the PackageDescriptor for the deployed artifact.
	 * Template repository files will be replaced by transient instantiated files signed with their instantiated value
	 * 
	 * @param deployment
	 * @param server
	 *            dto
	 * @param artifactVersion
	 * @throws MissingVariableException
	 */
	static public PackageDescriptor loadFromArtifact(Deployment deployment, ServerDto server, ArtifactVersionDto artifactVersion) throws MissingVariableException {
		VersionDto version = deployment.findVersion(artifactVersion);
		VariableResolver resolver = new DeploymentVariableResolver(deployment, version, server.getName(), server.getPlatform());
		return instance.build(deployment, server.getName(), artifactVersion, resolver);
	}

	private PackageDescriptor build(Deployment deployment, String serverName, ArtifactVersionDto artifactVersion, VariableResolver resolver) throws MissingVariableException {
		LOG.debug("loadFromArtifact {}", artifactVersion.toString());
		PackageDescriptor descriptor = new PackageDescriptor();
		descriptor.setDeploymentName(deployment.getEnvironmentName());
		descriptor.setArtifactName(artifactVersion.getArtifact());
		descriptor.setVersionName(artifactVersion.getVersion());

		VersionDto version = deployment.findVersion(artifactVersion);
		String packageDir = this.variableService.fetchVariableValue(VariableResolver.ARTIFACT_DIR, resolver);
		descriptor.setDeployInDir(packageDir);
		PermissionDto permissions = deployment.getRelease().getPermissions();
		if (permissions != null) {
			descriptor.setFileOwner(this.variableService.instantiateVariables(permissions.getFileOwner(), resolver));
			descriptor.setFileGroup(this.variableService.instantiateVariables(permissions.getFileGroup(), resolver));
			descriptor.setFileMode(permissions.getFileMode());
			descriptor.setDirMode(permissions.getDirMode());
		}

		List<NameValue> actions = computeActions(version, resolver);
		descriptor.setActions(actions);
		Collection<FileDto> files = computeFiles(deployment, serverName, version, artifactVersion, resolver);
		descriptor.setFiles(files);

		descriptor.setSignature(CryptoUtils.computeStringSignature(toXml(descriptor)));
		return descriptor;
	}

	private List<NameValue> computeActions(VersionDto version, VariableResolver resolver) throws MissingVariableException {
		List<NameValue> actions = new ArrayList<>();
		String action = this.variableService.instantiateVariables(version.getPreDeployAction(), resolver);
		if (StringUtils.isNotEmpty(action))
			actions.add(new NameValue(ACTION_PRE_DEPLOY, action));
		action = this.variableService.instantiateVariables(version.getPostDeployAction(), resolver);
		if (StringUtils.isNotEmpty(action))
			actions.add(new NameValue(ACTION_POST_DEPLOY, action));
		action = this.variableService.instantiateVariables(version.getPreUndeployAction(), resolver);
		if (StringUtils.isNotEmpty(action))
			actions.add(new NameValue(ACTION_PRE_UNDEPLOY, action));
		action = this.variableService.instantiateVariables(version.getPostUndeployAction(), resolver);
		if (StringUtils.isNotEmpty(action))
			actions.add(new NameValue(ACTION_POST_UNDEPLOY, action));
		if (CollectionUtils.isNotEmpty(version.getExecutables())) {
			for (ExecutableDto exe : version.getExecutables()) {
				action = this.variableService.instantiateVariables(exe.getStartAction(), resolver);
				if (StringUtils.isNotEmpty(action))
					actions.add(new NameValue(ACTION_START_PREFIX + exe.getName(), action));
				action = this.variableService.instantiateVariables(exe.getStopAction(), resolver);
				if (StringUtils.isNotEmpty(action))
					actions.add(new NameValue(ACTION_STOP_PREFIX + exe.getName(), action));
				action = this.variableService.instantiateVariables(exe.getStatusAction(), resolver);
				if (StringUtils.isNotEmpty(action))
					actions.add(new NameValue(ACTION_STATUS_PREFIX + exe.getName(), action));
			}
		}
		return actions;
	}

	private Collection<FileDto> computeFiles(Deployment deployment, String serverName, VersionDto version, ArtifactVersionDto artifactVersion, VariableResolver resolver) throws MissingVariableException {
		Collection<FileDto> fileSet = new HashSet<FileDto>();
		if (artifactVersion.getCustomFiles() != null)
			fileSet.addAll(artifactVersion.getCustomFiles());
		if (version.getPackageInfo() != null)
			fileSet.addAll(version.getPackageInfo().getFiles());
		LOG.debug("loadFromArtifact {} parse files", version.toString());
		List<FileDto> files = new ArrayList<>();
		for (FileDto f : fileSet) {
			FileDto file = cloneFileIfNecessary(f, resolver);
			if (StringUtils.isNotEmpty(file.getVariables())) {
				LOG.debug("loadFromArtifact processing file {}", file.getPath());
				// calculate signature for descriptor even if file is not instantiated
				InputStream in = this.deployStreamProvider.getOverlayFileInputStream(
						file.getPath(),
						deployment.getEnvironmentName(),
						deployment.getName(),
						serverName,
						artifactVersion.getArtifact(),
						artifactVersion.getVersion());
				try {
					String signature = this.variableService.computeFileSignature(file.getPath(), in, resolver);
					file.setSignature(signature);
				} finally {
					IOUtils.closeQuietly(in);
				}
			}
			files.add(file);
		}
		Collections.sort(files);
		if (LOG.isDebugEnabled()) {
			LOG.debug("loadFromArtifact: {} common files", version.toString());
			if (version.getPackageInfo() == null)
				LOG.debug("  none");
			else {
				for (FileDto f : version.getPackageInfo().getFiles())
					LOG.debug("  {} - {}", f.getPath(), f.getSignature());
			}
			LOG.debug("loadFromArtifact: {} custom files", version.toString());
			if (artifactVersion.getCustomFiles() == null)
				LOG.debug("  none");
			else {
				for (FileDto f : artifactVersion.getCustomFiles())
					LOG.debug("  {} - {}", f.getPath(), f.getSignature());
			}
			LOG.debug("loadFromArtifact: {} descriptor files", version.toString());
			for (FileDto f : files) {
				LOG.debug("  {} - {}", f.getPath(), f.getSignature());
			}
		}
		return files;
	}

	public FileDto cloneFileIfNecessary(FileDto file, VariableResolver resolver) throws MissingVariableException {
		if (StringUtils.isEmpty(file.getVariables()) && (file.getProperties() == null || file.getProperties().getPermissions() == null))
			return file;
		FileDto clone = new FileDto();
		clone.setId(file.getId());
		clone.setName(file.getName());
		clone.setPath(file.getPath());
		clone.setLayer(file.getLayer());
		clone.setSymbolicLink(file.isSymbolicLink());
		clone.setDir(file.isDir());
		clone.setHasVariables(file.getHasVariables());
		clone.setVariables(file.getVariables());
		clone.setSize(file.getSize());
		clone.setSignature(file.getSignature());

		if (file.getProperties() != null) {
			FilePropertiesDto properties = new FilePropertiesDto();
			properties.setIgnoreVariables(file.getProperties().getIgnoreVariables());
			properties.setIgnoreIntegrity(file.getProperties().getIgnoreIntegrity());
			properties.setDontDelete(file.getProperties().getDontDelete());
			if (file.getProperties().getPermissions() != null) {
				PermissionDto permissions = new PermissionDto();
				permissions.setFileOwner(this.variableService.instantiateVariables(file.getProperties().getPermissions().getFileOwner(), resolver));
				permissions.setFileGroup(this.variableService.instantiateVariables(file.getProperties().getPermissions().getFileGroup(), resolver));
				permissions.setFileMode(file.getProperties().getPermissions().getFileMode());
				permissions.setDirMode(file.getProperties().getPermissions().getDirMode());
				properties.setPermissions(permissions);
			}
			clone.setProperties(properties);
		}
		return clone;
	}

	/**
	 * Recursively traverses the DOM
	 * 
	 * @param descriptor
	 * @param node
	 * @param basePath
	 * @return
	 */
	private boolean parseDir(PackageDescriptor descriptor, Node node, String basePath) {
		boolean empty = true;
		while (node != null) {
			if (XML_DIR_NODE.equals(node.getNodeName()) || XML_FILE_NODE.equals(node.getNodeName())) {
				Element element = (Element) node;
				String fileName = element.getAttribute(XML_NAME_ATTR);
				FileDto file = new FileDto();
				file.setPath(basePath + fileName);
				file.setSignature(element.getAttribute(XML_SIGNATURE_ATTR));
				if (node.getNodeName().equals(XML_DIR_NODE)) {
					empty = false;
					if (parseDir(descriptor, node.getFirstChild(), basePath + fileName + '/')) {// empty dir
						file.setDir(true);
						descriptor.getFiles().add(file);
					}
				} else {
					descriptor.getFiles().add(file);
					empty = false;
					if (StringUtils.isNotEmpty(element.getAttribute(XML_SYMLINK_ATTR)))
						file.setSymbolicLink(true);
				}
			}
			node = node.getNextSibling();
		}
		return empty;
	}

	static String toXml(PackageDescriptor descriptor) {
		XmlBuffer buffer = new XmlBuffer("Deployed with Kwatee (http://www.kwatee.net)");
		buffer.openTag(XML_KWATEE_NODE);
		buffer.addAttribute(XML_DEPLOYMENT_ATTR, descriptor.getDeploymentName());
		buffer.addAttribute(XML_ARTIFACT_ATTR, ArtifactDto.getRef(descriptor.getArtifactName(), descriptor.getVersionName()));
		buffer.addAttribute(XML_DEPLOY_IN_ATTR, descriptor.getDeployInDir());
		buffer.addAttribute(XML_SIGNATURE_ATTR, descriptor.getSignature());
		if (CollectionUtils.isNotEmpty(descriptor.getActions())) {
			buffer.openTag(XML_ACTIONS_NODE);
			for (NameValue action : descriptor.getActions()) {
				buffer.openTag(XML_ACTION_NODE);
				buffer.addAttribute(XML_TYPE_ATTR, action.getName());
				buffer.addCData(action.getValue());
				buffer.closeTag();
			}
			buffer.closeTag();
		}
		buffer.openTag(XML_PACKAGE_NODE);
		buffer.addAttribute(XML_FILE_OWNER_ATTR, descriptor.getFileOwner());
		buffer.addAttribute(XML_FILE_GROUP_ATTR, descriptor.getFileGroup());
		buffer.addAttribute(XML_FILE_MODE_ATTR, descriptor.getFileMode());
		buffer.addAttribute(XML_DIR_MODE_ATTR, descriptor.getDirMode());
		String basePath = null;
		int level = 1;
		for (FileDto layerFile : descriptor.getFiles()) {
			File f = new File(layerFile.getPath());
			String itemDir = f.getParent() == null ? null : f.getParent().replace('\\', '/');
			if (itemDir != null && !itemDir.equals(basePath) || itemDir == null && basePath != null) {
				// Directory of current item is different from previous
				level = openCloseDirectoryNesting(buffer, level, basePath, itemDir);
				basePath = itemDir;
			}
			FilePropertiesDto properties = layerFile.getProperties();
			if (layerFile.isDir() != null && layerFile.isDir()) {
				// it's an empty directory
				buffer.openTag(XML_DIR_NODE);
				buffer.addAttribute(XML_NAME_ATTR, f.getName());
			} else {
				buffer.openTag(XML_FILE_NODE);
				buffer.addAttribute(XML_NAME_ATTR, f.getName());
				if (BooleanUtils.isTrue(layerFile.isSymbolicLink()))
					buffer.addAttribute(XML_SYMLINK_ATTR, "true");
				if (properties != null && BooleanUtils.isTrue(properties.getDontDelete()))
					buffer.addAttribute(XML_DONTDELETE_ATTR, "true");
			}
			if (properties != null && properties.getPermissions() != null) {
				PermissionDto permissions = properties.getPermissions();
				buffer.addAttribute(XML_FILE_OWNER_ATTR, permissions.getFileOwner());
				buffer.addAttribute(XML_FILE_GROUP_ATTR, permissions.getFileGroup());
				buffer.addAttribute(XML_FILE_MODE_ATTR, permissions.getFileMode());
				buffer.addAttribute(XML_DIR_MODE_ATTR, permissions.getDirMode());
			}
			buffer.addAttribute(XML_SIGNATURE_ATTR, (properties != null && BooleanUtils.isTrue(properties.getIgnoreIntegrity())) ? StringUtils.EMPTY : layerFile.getSignature());
			buffer.closeTag();
		}
		if (level != 1)
			openCloseDirectoryNesting(buffer, level, basePath, null);
		buffer.closeTag();
		buffer.closeTag();
		return buffer.toString();
	}

	/**
	 * Manages indenting of a directory
	 * 
	 * @param buffer
	 * @param level
	 * @param oldPath
	 * @param newPath
	 * @return
	 */
	static private int openCloseDirectoryNesting(XmlBuffer buffer, int level, String oldPath, String newPath) {
		String[] oldDirs = oldPath != null ? oldPath.split("/") : new String[0];
		String[] newDirs = newPath != null ? newPath.split("/") : new String[0];
		// Determine the first subdir that is not common
		int start = 0;
		for (; start < oldDirs.length && start < newDirs.length; start++) {
			if (!oldDirs[start].equals(newDirs[start]))
				break;
		}
		// Close all the directory tags from oldPath that are no longer in newPath
		for (int i = start; i < oldDirs.length; i++) {
			buffer.closeTag();
		}

		// Open all the directory tags that are in newPath but not in oldPath
		for (int i = start; i < newDirs.length; i++) {
			buffer.openTag(XML_DIR_NODE);
			buffer.addAttribute(XML_NAME_ATTR, newDirs[i]);
		}
		return level;
	}

	/**
	 * @param descriptorRemote
	 * @return a descriptor with the differences
	 */
	static public DescriptorDifferences diff(PackageDescriptor descriptor, PackageDescriptor descriptorRemote) {
		DescriptorDifferences differences = new DescriptorDifferences();
		if (descriptorRemote == null) {
			for (FileDto refFile : descriptor.getFiles()) {
				differences.getAdditions().add(refFile);
			}
		} else {
			ArrayList<FileDto> sortedServerFiles = new ArrayList<>(descriptorRemote.getFiles());
			ArrayList<FileDto> sortedRepositoryFiles = new ArrayList<>(descriptor.getFiles());
			for (FileDto refFile : sortedRepositoryFiles) {
				boolean found = false;
				for (FileDto remoteFile : sortedServerFiles) {
					if (refFile.getPath().equals(remoteFile.getPath())) {
						if (!refFile.getSignature().equals(remoteFile.getSignature()))
							differences.getChanges().add(refFile);
						found = true;
						break;
					}
				}
				if (!found)
					differences.getAdditions().add(refFile);
			}

			for (FileDto remoteFile : sortedServerFiles) {
				if (!sortedRepositoryFiles.contains(remoteFile))
					differences.getRemovals().add(remoteFile);
			}
		}
		return differences;
	}
}
