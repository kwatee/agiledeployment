package net.kwatee.agiledeployment.webapp;

import net.kwatee.agiledeployment.webapp.controller.AdminController;
import net.kwatee.agiledeployment.webapp.controller.ArtifactController;
import net.kwatee.agiledeployment.webapp.controller.DeploymentController;
import net.kwatee.agiledeployment.webapp.controller.EnvironmentController;
import net.kwatee.agiledeployment.webapp.controller.InfoController;
import net.kwatee.agiledeployment.webapp.controller.JsonApi;
import net.kwatee.agiledeployment.webapp.controller.ServerController;
import net.kwatee.agiledeployment.webapp.filter.CORSHeaderRegisteration;
import net.kwatee.agiledeployment.webapp.filter.ResponseHeaderRegisteration;
import net.kwatee.agiledeployment.webapp.importexport.ExportService;
import net.kwatee.agiledeployment.webapp.mapper.AdminMapper;
import net.kwatee.agiledeployment.webapp.mapper.ArtifactMapper;
import net.kwatee.agiledeployment.webapp.mapper.EnvironmentMapper;
import net.kwatee.agiledeployment.webapp.mapper.ServerMapper;
import net.kwatee.agiledeployment.webapp.service.AdminService;
import net.kwatee.agiledeployment.webapp.service.ArtifactService;
import net.kwatee.agiledeployment.webapp.service.DeploymentService;
import net.kwatee.agiledeployment.webapp.service.EnvironmentService;
import net.kwatee.agiledeployment.webapp.service.InfoService;
import net.kwatee.agiledeployment.webapp.service.ServerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans are explicitly created instead of being component-scanned because of Proguard
 */
@Configuration
public class ApiServicesConfig {

	final private static Logger LOG = LoggerFactory.getLogger(ApiServicesConfig.class);

	@Bean
	public JsonApi jsonApi() {
		return new JsonApi();
	}

	@Bean
	public AdminController adminController() {
		AdminController controller = new AdminController();
		LOG.debug("Created AdminController bean");
		return controller;
	}

	@Bean
	public ArtifactController artifactController() {
		ArtifactController controller = new ArtifactController();
		LOG.debug("Created ArtifactController bean");
		return controller;
	}

	@Bean
	public DeploymentController deploymentController() {
		DeploymentController controller = new DeploymentController();
		LOG.debug("Created DeploymentController bean");
		return controller;
	}

	@Bean
	public EnvironmentController environmentController() {
		EnvironmentController controller = new EnvironmentController();
		LOG.debug("Created EnvironmentController bean");
		return controller;
	}

	@Bean
	public InfoController infoController() {
		InfoController controller = new InfoController();
		LOG.debug("Created InfoController bean");
		return controller;
	}

	@Bean
	public ServerController serverController() {
		ServerController controller = new ServerController();
		LOG.debug("Created ServerController bean");
		return controller;
	}

	@Bean
	public AdminService adminService() {
		AdminService service = new AdminService();
		LOG.debug("Created AdminService bean");
		return service;
	}

	@Bean
	public ArtifactService artifactService() {
		ArtifactService service = new ArtifactService();
		LOG.debug("Created ArtifactService bean");
		return service;
	}

	@Bean
	public DeploymentService deploymentService() {
		DeploymentService service = new DeploymentService();
		LOG.debug("Created DeploymentService bean");
		return service;
	}

	@Bean
	public EnvironmentService environmentService() {
		EnvironmentService service = new EnvironmentService();
		LOG.debug("Created EnvironmentService bean");
		return service;
	}

	@Bean
	public InfoService infoService() {
		InfoService service = new InfoService();
		LOG.debug("Created InfoService bean");
		return service;
	}

	@Bean
	public ServerService serverService() {
		ServerService service = new ServerService();
		LOG.debug("Created ServerService bean");
		return service;
	}

	@Bean
	public ExportService exportService() {
		ExportService service = new ExportService();
		LOG.debug("Created ExportService bean");
		return service;
	}

	@Bean
	public AdminMapper adminMapper() {
		AdminMapper mapper = new AdminMapper();
		LOG.debug("Created AdminMapper bean");
		return mapper;
	}

	@Bean
	public ArtifactMapper artifactMapper() {
		ArtifactMapper mapper = new ArtifactMapper();
		LOG.debug("Created ArtifactMapper bean");
		return mapper;
	}

	@Bean
	public EnvironmentMapper environmentMapper() {
		EnvironmentMapper mapper = new EnvironmentMapper();
		LOG.debug("Created EnvironmentMapper bean");
		return mapper;
	}

	@Bean
	public ServerMapper serverMapper() {
		ServerMapper mapper = new ServerMapper();
		LOG.debug("Created ServerMapper bean");
		return mapper;
	}

	@Bean
	public CORSHeaderRegisteration corsHeaderRegisteration() {
		CORSHeaderRegisteration bean = new CORSHeaderRegisteration();
		LOG.debug("Created CORSHeaderRegisteration bean");
		return bean;
	}

	@Bean
	ResponseHeaderRegisteration responseHeaderRegisteration() {
		ResponseHeaderRegisteration bean = new ResponseHeaderRegisteration();
		LOG.debug("Created ResponseHeaderRegisteration bean");
		return bean;
	}

}
