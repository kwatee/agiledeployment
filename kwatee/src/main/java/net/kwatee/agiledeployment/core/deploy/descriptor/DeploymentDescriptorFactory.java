/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.deploy.descriptor;

import java.util.ArrayList;
import java.util.Collection;

import net.kwatee.agiledeployment.common.VariableResolver;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.common.utils.CryptoUtils;
import net.kwatee.agiledeployment.common.utils.NameValue;
import net.kwatee.agiledeployment.common.utils.XmlBuffer;
import net.kwatee.agiledeployment.core.deploy.Deployment;
import net.kwatee.agiledeployment.core.variable.VariableService;
import net.kwatee.agiledeployment.core.variable.impl.DeploymentVariableResolver;
import net.kwatee.agiledeployment.repository.dto.ArtifactVersionDto;
import net.kwatee.agiledeployment.repository.dto.PermissionDto;
import net.kwatee.agiledeployment.repository.dto.ServerArtifactsDto;
import net.kwatee.agiledeployment.repository.dto.ServerDto;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeploymentDescriptorFactory {

	final public static String ACTION_PRE_SETUP = "pre_setup";
	final public static String ACTION_POST_SETUP = "post_setup";
	final public static String ACTION_PRE_CLEANUP = "pre_cleanup";
	final public static String ACTION_POST_CLEANUP = "post_cleanup";

	final private static String XML_KWATEE_NODE = "kwatee";
	final private static String XML_DEPLOYMENT_ATTR = "deployment";
	final private static String XML_DEPLOY_IN_ATTR = "deploy_in";
	final private static String XML_SIGNATURE_ATTR = "signature";
	final private static String XML_ACTIONS_NODE = "actions";
	final private static String XML_ACTION_NODE = "action";
	final private static String XML_TYPE_ATTR = "type";
	final private static String XML_ARTIFACTS_NODE = "artifacts";
	final private static String XML_ARTIFACT_NODE = "artifact";
	final private static String XML_NAME_ATTR = "name";
	final private static String XML_VERSION_ATTR = "version";
	final private static String XML_FILE_OWNER_ATTR = "file_owner";
	final private static String XML_FILE_GROUP_ATTR = "file_group";
	final private static String XML_FILE_MODE_ATTR = "file_mode";
	final private static String XML_DIR_MODE_ATTR = "dir_mode";

	static private DeploymentDescriptorFactory instance;

	@Autowired
	private VariableService variableService;

	public DeploymentDescriptorFactory() {
		instance = this;
	}

	static public DeploymentDescriptor loadFromServer(Deployment deployment, ServerDto server) throws MissingVariableException {
		return instance.load(deployment, server);
	}

	private DeploymentDescriptor load(Deployment deployment, ServerDto server) throws MissingVariableException {
		DeploymentDescriptor descriptor = new DeploymentDescriptor();
		descriptor.setDeploymentName(deployment.getEnvironmentName());
		VariableResolver resolver = new DeploymentVariableResolver(deployment, server.getName(), server.getPlatform());
		String dir = this.variableService.fetchVariableValue(VariableResolver.DEPLOYMENT_DIR, resolver);
		descriptor.setDeployInDir(dir);
		PermissionDto permissions = deployment.getRelease().getPermissions();
		if (permissions != null) {
			descriptor.setFileOwner(this.variableService.instantiateVariables(permissions.getFileOwner(), resolver));
			descriptor.setFileOwner(this.variableService.instantiateVariables(permissions.getFileGroup(), resolver));
			descriptor.setFileMode(permissions.getFileMode());
			descriptor.setDirMode(permissions.getDirMode());
		}

		Collection<NameValue> actions = new ArrayList<>();
		String action;
		action = this.variableService.instantiateVariables(deployment.getRelease().getPreSetupAction(), resolver);
		if (StringUtils.isNotEmpty(action))
			actions.add(new NameValue(ACTION_PRE_SETUP, action));
		action = this.variableService.instantiateVariables(deployment.getRelease().getPostSetupAction(), resolver);
		if (StringUtils.isNotEmpty(action))
			actions.add(new NameValue(ACTION_POST_SETUP, action));
		action = this.variableService.instantiateVariables(deployment.getRelease().getPreCleanupAction(), resolver);
		if (StringUtils.isNotEmpty(action))
			actions.add(new NameValue(ACTION_PRE_CLEANUP, action));
		action = this.variableService.instantiateVariables(deployment.getRelease().getPostCleanupAction(), resolver);
		if (StringUtils.isNotEmpty(action))
			actions.add(new NameValue(ACTION_POST_CLEANUP, action));
		descriptor.setActions(actions);

		ServerArtifactsDto serverArtifacts = ServerArtifactsDto.findByName(deployment.getRelease().getServers(), server.getName());
		descriptor.setArtifacts(serverArtifacts.getArtifacts());

		descriptor.setSignature(CryptoUtils.computeStringSignature(toXml(descriptor)));
		return descriptor;
	}

	static public String toXml(DeploymentDescriptor descriptor) {
		XmlBuffer buffer = new XmlBuffer("Deployed with Kwatee (http://www.kwatee.net)");
		buffer.openTag(XML_KWATEE_NODE);
		buffer.addAttribute(XML_DEPLOYMENT_ATTR, descriptor.getDeploymentName());
		buffer.addAttribute(XML_DEPLOY_IN_ATTR, descriptor.getDeployInDir());
		buffer.addAttribute(XML_FILE_OWNER_ATTR, descriptor.getFileOwner());
		buffer.addAttribute(XML_FILE_GROUP_ATTR, descriptor.getFileGroup());
		buffer.addAttribute(XML_FILE_MODE_ATTR, descriptor.getFileMode());
		buffer.addAttribute(XML_DIR_MODE_ATTR, descriptor.getDirMode());
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
		buffer.openTag(XML_ARTIFACTS_NODE);
		for (ArtifactVersionDto artifact : descriptor.getArtifacts()) {
			buffer.openTag(XML_ARTIFACT_NODE);
			buffer.addAttribute(XML_NAME_ATTR, artifact.getArtifact());
			buffer.addAttribute(XML_VERSION_ATTR, artifact.getVersion());
			buffer.closeTag();
		}
		buffer.closeTag();
		buffer.closeTag();
		return buffer.toString();
	}
}
