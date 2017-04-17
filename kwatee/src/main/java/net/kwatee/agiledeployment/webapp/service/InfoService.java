/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.service;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import net.kwatee.agiledeployment.core.conduit.ConduitService;
import net.kwatee.agiledeployment.core.conduit.InstanceService;
import net.kwatee.agiledeployment.core.deploy.DeployService;
import net.kwatee.agiledeployment.core.deploy.PlatformService;
import net.kwatee.agiledeployment.core.repository.AdminRepository;
import net.kwatee.agiledeployment.core.service.DBAdminService;
import net.kwatee.agiledeployment.repository.dto.IdNameDto;
import net.kwatee.agiledeployment.repository.entity.ApplicationParameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InfoService {

	final static private Logger LOG = LoggerFactory.getLogger(InfoService.class);

	@Autowired
	private PlatformService platformService;
	@Autowired
	private DBAdminService dbAdminService;
	@Autowired
	private AdminRepository adminRepository;
	@Autowired(required = false)
	private SessionRegistry sessionRegistry;
	@Autowired
	private DeployService deployService;
	@Autowired
	private InstanceService instanceService;
	@Autowired
	private ConduitService conduitService;

	@Secured({"ROLE_USER", "ROLE_EXTERNAL"})
	@Transactional(readOnly = true)
	public Map<String, String> getContextInfo(Principal principal) {
		Map<String, String> info = new HashMap<>();
		info.put("user", principal.getName());
		info.put("copyright", "Copyright © 2010-2015");
		ApplicationParameter parameters = this.adminRepository.getApplicationParameters();
		info.put("organization", parameters.getTitle());
		String version = "debug";
		Manifest manifest = getManifest();
		if (manifest != null) {
			if (manifest.getMainAttributes().containsKey(java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION)) {
				version = "VersionShortDto " + manifest.getMainAttributes().getValue(java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION);
				String build = manifest.getMainAttributes().getValue("buildNumber");
				if (build != null) {
					version += " [" + build + "]";
				}
			}
		}
		info.put("version", version);
		String dbType = this.dbAdminService.getJdbcUrl().split(":")[1];
		info.put("dbType", dbType);
		return info;
	}

	private Manifest getManifest() {
		try (InputStream is = this.getClass().getResourceAsStream("/META-INF/MANIFEST.MF")) {
			return new Manifest(is);
		} catch (IOException e) {
			LOG.error("Failed to load manifest", e);
		}
		return null;
	}

	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public List<IdNameDto> getPlatforms() {
		List<IdNameDto> list = new ArrayList<>(this.platformService.listPlatforms().size());
		for (String platform : this.platformService.listPlatforms()) {
			int id = this.platformService.getPlatformId(platform);
			list.add(new IdNameDto((long) id, platform));
		}
		return list;
	}

	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public List<IdNameDto> getConduitTypes() {
		Map<String, String> conduitTypes = this.conduitService.getFactories();
		List<IdNameDto> list = new ArrayList<>(conduitTypes.size());
		for (Map.Entry<String, String> entry : conduitTypes.entrySet()) {
			list.add(new IdNameDto(entry.getKey(), entry.getValue()));
		}
		return list;
	}

	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public List<IdNameDto> getServerPoolTypes() {
		Map<String, String> serverPoolTypes = this.instanceService.getFactories();
		List<IdNameDto> list = new ArrayList<>(serverPoolTypes.size());
		for (Map.Entry<String, String> entry : serverPoolTypes.entrySet()) {
			list.add(new IdNameDto(entry.getKey(), entry.getValue()));
		}
		return list;
	}
}