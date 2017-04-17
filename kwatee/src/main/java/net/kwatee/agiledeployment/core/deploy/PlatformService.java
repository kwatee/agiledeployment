/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.deploy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;

import net.kwatee.agiledeployment.common.VariableResolver;
import net.kwatee.agiledeployment.common.exception.InternalErrorException;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.common.utils.FileUtils;
import net.kwatee.agiledeployment.conduit.ServerInstance;
import net.kwatee.agiledeployment.core.variable.VariableService;
import net.kwatee.agiledeployment.core.variable.impl.ParamVariableResolver;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/*
 * TODO: add filtering and sorting
 */
@Service
public class PlatformService {

	final static private org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PlatformService.class);

	final static private String PLATFORM_ID = "id";
	final static private String AGENT_SIZE = "size";
	final static private String AGENT_EXECUTABLE = "executable_name";
	final static private String PATH_TYPE = "path_type";
	final static private String COMMAND_AGENT = "command.agent";
	final static private String COMMAND_MAKE_EXECUTABLE = "command.make_executable";
	final static private String COMMAND_MAKE_DIR = "command.make_dir";
	final static private String COMMAND_DIAGNOSTICS = "command.diagnostics";

	private static String agentVersion;
	private static PlatformService instance;

	private HashMap<String, Properties> platformProperties;
	private List<String> platformNames;

	@Autowired
	private VariableService variableService;

	public PlatformService() {
		instance = this;
	}

	@PostConstruct
	void initialize() {
		LOG.debug("Initializing platforms");

		Properties properties = FileUtils.getResourceAsProperties(this.getClass(), "/platforms/platforms.properties");
		if (properties == null)
			throw new RuntimeException("Missing platform resources");
		String[] platforms = properties.getProperty("platforms").split(",");
		agentVersion = properties.getProperty("version");
		if (StringUtils.isEmpty(agentVersion))
			throw new RuntimeException("Could not find master agent version");
		LOG.info("Agent version {}", agentVersion);
		this.platformProperties = new HashMap<>();
		this.platformNames = new ArrayList<>();
		try {
			for (int pos = 1; pos <= platforms.length; pos++) {
				String platformName = platforms[pos - 1];
				Properties p = FileUtils.getResourceAsProperties(this.getClass(), "/platforms/" + platformName + "/" + platformName + ".properties");
				if (p != null) {
					String executableName = p.getProperty(AGENT_EXECUTABLE, "kwagent");
					long fileSize = FileUtils.getResourceSize(this.getClass(), "/platforms/" + platformName + "/" + executableName);
					if (fileSize != -1) {
						p.put(AGENT_SIZE, Long.toString(fileSize));
						this.platformProperties.put(platformName, p);
						this.platformNames.add(platformName);
						LOG.debug("Added platform {}", platformName);
					} else
						LOG.warn("Failed to add platform {} (agent {} not found)", platformName, executableName);
				}
			}
		} catch (Exception e) {
			this.platformProperties = null;
			this.platformNames = null;
			LOG.error("Failed to initialize platforms", e);
			throw new RuntimeException("Could not initialize agents");
		}
	}

	/**
	 * @param platformId
	 * @return agent url
	 * @throws MissingVariableException
	 */
	public String getPlatformAgentUrl(int platformId, boolean stripped) throws MissingVariableException {
		String agentName = get(platformId, AGENT_EXECUTABLE, null, null, true);
		String resourcePath = "/platforms/" + getName(platformId) + "/" + agentName;
		String agentUrl = this.getClass().getResource(resourcePath).toExternalForm();
		if (stripped && agentUrl.startsWith("file:/")) {
			// Hack for debug mode
			agentUrl = agentUrl.substring(5); // strip file:
		}
		LOG.debug("agentUrl = " + agentUrl);
		return agentUrl;
	}

	/**
	 * @return platforms
	 */
	public Collection<String> listPlatforms() {
		return this.platformNames;
	}

	/**
	 * @param platformName
	 * @return platform id
	 */
	public int getPlatformId(String platformName) {
		Properties p = this.platformProperties.get(platformName);
		if (p == null)
			throw new InternalErrorException("Unknown platform " + platformName);
		return Integer.parseInt(p.getProperty(PLATFORM_ID));
	}

	/**
	 * @param platformId
	 * @return platform name
	 */
	public String getName(int platformId) {
		for (Map.Entry<String, Properties> entry : this.platformProperties.entrySet()) {
			if (platformId == Long.parseLong(entry.getValue().getProperty(PLATFORM_ID)))
				return entry.getKey();
		}
		return null;
	}

	/**
	 * @param platformId
	 * @return agent name
	 * @throws MissingVariableException
	 */
	public String getAgentExecutableName(int platformId) throws MissingVariableException {
		return get(platformId, AGENT_EXECUTABLE, null, null, true);
	}

	/**
	 * @param platformId
	 * @return pre-computed file size
	 * @throws MissingVariableException
	 */
	public long getAgentFileSize(int platformId) throws MissingVariableException {
		String size = get(platformId, AGENT_SIZE, null, null, true);
		return Long.parseLong(size);
	}

	/**
	 * @param platformId
	 * @return path type
	 */
	public int getPathType(int platformId) {
		try {
			String pathType = get(platformId, PATH_TYPE, null, null, false);
			if (pathType.equals("windows"))
				return 1;
			if (pathType.equals("cygwin"))
				return 2;
		} catch (MissingVariableException e) {}
		return 0;
	}

	/**
	 * @param platformId
	 * @param resolver
	 * @return agent make executable command
	 */
	public String getMakeAgentExecutableCommand(int platformId, VariableResolver resolver) {
		try {
			return get(platformId, COMMAND_MAKE_EXECUTABLE, resolver, null, false);
		} catch (MissingVariableException e) {}
		return null;
	}

	/**
	 * @param platformId
	 * @param resolver
	 * @return agent version command
	 * @throws MissingVariableException
	 */
	public String getAgentVersionCommand(int platformId, VariableResolver resolver) throws MissingVariableException {
		return getAgentCommand(platformId, "--version", resolver);
	}

	/**
	 * @param platformId
	 * @param deploymentName
	 * @param artifactName
	 * @param signature
	 * @param resolver
	 * @return agent integrity check command
	 * @throws MissingVariableException
	 */
	public String getAgentIntegrityCheckCommand(int platformId, final String deploymentName, final String artifactName, final String signature, VariableResolver resolver) throws MissingVariableException {
		String cmd = getAgentCommand(
				platformId,
				"check-integrity -d " + deploymentName + " -a " + artifactName,
				resolver);
		if (signature != null)
			cmd += " --signature " + signature;
		return cmd;
	}

	/**
	 * @param platformId
	 * @param deploymentName
	 * @param artifactName
	 * @param resolver
	 * @return agent descriptor command
	 * @throws MissingVariableException
	 */
	public String getAgentDescriptorCommand(int platformId, final String deploymentName, final String artifactName, VariableResolver resolver) throws MissingVariableException {
		String cmd = getAgentCommand(
				platformId,
				"get-descriptor -d " + deploymentName + " -a " + artifactName,
				resolver);
		return cmd;
	}

	/**
	 * @param platformId
	 * @param deploymentName
	 * @param artifactName
	 * @param skipIntegrityCheck
	 * @param force
	 * @param resolver
	 * @return agent remove command
	 * @throws MissingVariableException
	 */
	public String getAgentUndeployCommand(int platformId, final String deploymentName, final String artifactName, final boolean skipIntegrityCheck, final boolean force, VariableResolver resolver) throws MissingVariableException {
		String cmd = getAgentCommand(
				platformId,
				"undeploy -d " + deploymentName + (artifactName == null ? StringUtils.EMPTY : (" -a " + artifactName)),
				resolver);
		if (force)
			cmd += " --force true";
		if (skipIntegrityCheck)
			cmd += " --noIntegrityCheck true";
		return cmd;
	}

	/**
	 * @param platformId
	 * @param deploymentName
	 * @param artifactName
	 * @param resolver
	 * @return agent expand command
	 * @throws MissingVariableException
	 */
	public String getAgentDeployCommand(int platformId, final String deploymentName, final String serverName, final String artifactName, VariableResolver resolver) throws MissingVariableException {
		String cmd = getAgentCommand(
				platformId,
				"deploy -s " + serverName + " -f \"%{kwatee_root_dir}/" + deploymentName + ".kwatee\"" + (artifactName == null ? StringUtils.EMPTY : (" -a " + artifactName)),
				resolver);
		return cmd;
	}

	/**
	 * @param platformId
	 * @param deploymentName
	 * @param artifactName
	 * @param descriptorFileName
	 * @param resolver
	 * @return agent update-descriptor command
	 * @throws MissingVariableException
	 */
	public String getAgentUpdateDescriptorCommand(int platformId, String deploymentName, String artifactName, String descriptorFileName, VariableResolver resolver) throws MissingVariableException {
		String cmd = getAgentCommand(
				platformId,
				"update-descriptor -f " + descriptorFileName + " -d " + deploymentName + (artifactName == null ? StringUtils.EMPTY : (" -a " + artifactName)),
				resolver);
		return cmd;
	}

	/**
	 * @param platformId
	 * @param action
	 * @param actionParams
	 * @param deploymentName
	 * @param artifactName
	 * @param skipIntegrityCheck
	 * @param resolver
	 * @return remote action command
	 * @throws MissingVariableException
	 */
	public String getRemoteActionCommand(int platformId, final String action, String actionParams, final String deploymentName, final String artifactName, final boolean skipIntegrityCheck, VariableResolver resolver) throws MissingVariableException {
		String cmd = getAgentCommand(
				platformId,
				"action --id " + action + " -d " + deploymentName + (artifactName == null ? StringUtils.EMPTY : (" -a " + artifactName)),
				resolver);
		if (skipIntegrityCheck)
			cmd += " --noIntegrityCheck true";
		if (actionParams != null)
			cmd += " " + actionParams;
		return cmd;
	}

	/**
	 * @param platformId
	 * @param dir
	 *            “param resolver
	 * @return agent make dir command
	 * @throws MissingVariableException
	 */
	@SuppressWarnings("serial")
	public String getMakeSystemDirCommand(int platformId, final String dir, VariableResolver resolver) throws MissingVariableException {
		return get(platformId, COMMAND_MAKE_DIR,
				resolver,
				new Properties() {

					{
						put("kwateep_dir", dir);
					}
				},
				true);
	}

	/**
	 * @param platformId
	 * @param resolver
	 * @return agent verify platform command
	 */
	public String getVerifyPlatformCommands(int platformId, VariableResolver resolver) {
		try {
			return get(platformId, COMMAND_DIAGNOSTICS, resolver, null, false);
		} catch (MissingVariableException e) {
			return null;
		}
	}

	private String get(int platformId, String key, VariableResolver resolver, Properties params, boolean mustExist) throws MissingVariableException {
		if (params != null)
			resolver = new ParamVariableResolver(resolver, params);
		ServerInstance server = null;
		for (Properties p : this.platformProperties.values()) {
			if (platformId == Long.parseLong(p.getProperty(PLATFORM_ID))) {
				String variant = server == null ? null : server.getProperty("conduit_variant");
				String value = null;
				if (variant != null)
					value = p.getProperty(variant + "." + key);
				if (value == null)
					value = p.getProperty(key);
				if (value != null)
					return this.variableService.instantiateVariables(value, resolver);
				break;
			}
		}
		if (mustExist)
			throw new InternalErrorException("Missing property " + key + " in platform " + platformId);
		return null;
	}

	private String getAgentCommand(int platformId, String command, VariableResolver resolver) throws MissingVariableException {
		Properties params = new Properties();
		params.put("kwateep_agent_name", getAgentExecutableName(platformId));
		params.put("kwateep_agent_command", command);
		String cmd = get(platformId, COMMAND_AGENT, resolver, params, true);
		return cmd;
	}

	public static String requiredAgentVersion() {
		return agentVersion;
	}

	/**
	 * @param platformId
	 * @return platform name
	 */
	static public PlatformService getInstance() {
		return instance;
	}
}
