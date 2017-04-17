/*
 * ${kwatee_copyright}
 */
package net.kwatee.agiledeployment.core;

import net.kwatee.agiledeployment.core.conduit.ConduitService;
import net.kwatee.agiledeployment.core.conduit.InstanceService;
import net.kwatee.agiledeployment.core.deploy.DeployService;
import net.kwatee.agiledeployment.core.deploy.PlatformService;
import net.kwatee.agiledeployment.core.deploy.descriptor.DeploymentDescriptorFactory;
import net.kwatee.agiledeployment.core.deploy.descriptor.PackageDescriptorFactory;
import net.kwatee.agiledeployment.core.deploy.packager.PackagerService;
import net.kwatee.agiledeployment.core.deploy.task.DeployTaskServices;
import net.kwatee.agiledeployment.core.repository.AdminRepository;
import net.kwatee.agiledeployment.core.repository.ArtifactRepository;
import net.kwatee.agiledeployment.core.repository.DeployStreamProvider;
import net.kwatee.agiledeployment.core.repository.EnvironmentRepository;
import net.kwatee.agiledeployment.core.repository.ServerRepository;
import net.kwatee.agiledeployment.core.repository.UserRepository;
import net.kwatee.agiledeployment.core.service.DeployStreamProviderImpl;
import net.kwatee.agiledeployment.core.service.FileStoreService;
import net.kwatee.agiledeployment.core.service.FileSystemFileStoreService;
import net.kwatee.agiledeployment.core.service.LayerService;
import net.kwatee.agiledeployment.core.service.PackageService;
import net.kwatee.agiledeployment.core.variable.VariableService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreServicesConfig {

	final private static Logger LOG = LoggerFactory.getLogger(CoreServicesConfig.class);

	@Bean
	public ConduitService conduitService() {
		ConduitService service = new ConduitService();
		LOG.debug("Created ConduitService bean");
		return service;
	}

	@Bean
	public InstanceService instanceService() {
		InstanceService service = new InstanceService();
		LOG.debug("Created InstanceService bean");
		return service;
	}

	@Bean
	public DeployService deployService() {
		DeployService service = new DeployService();
		LOG.debug("Created DeployService bean");
		return service;
	}

	@Bean
	public PlatformService platformService() {
		PlatformService service = new PlatformService();
		LOG.debug("Created PlatformService bean");
		return service;
	}

	@Bean
	public DeploymentDescriptorFactory deploymentDescriptorFactory() {
		DeploymentDescriptorFactory factory = new DeploymentDescriptorFactory();
		LOG.debug("Created DeploymentDescriptorFactory bean");
		return factory;
	}

	@Bean
	public PackageDescriptorFactory packageDescriptorFactory() {
		PackageDescriptorFactory factory = new PackageDescriptorFactory();
		LOG.debug("Created PackageDescriptorFactory bean");
		return factory;
	}

	@Bean
	public PackagerService packagerService() {
		PackagerService service = new PackagerService();
		LOG.debug("Created PackagerService bean");
		return service;
	}

	@Bean
	public DeployTaskServices deployTaskServices() {
		DeployTaskServices service = new DeployTaskServices();
		LOG.debug("Created DeployTaskServices bean");
		return service;
	}

	@Bean
	public AdminRepository adminRepository() {
		AdminRepository repository = new AdminRepository();
		LOG.debug("Created AdminRepository bean");
		return repository;
	}

	@Bean
	public ArtifactRepository artifactRepository() {
		ArtifactRepository repository = new ArtifactRepository();
		LOG.debug("Created ArtifactRepository bean");
		return repository;
	}

	@Bean
	public EnvironmentRepository environmentRepository() {
		EnvironmentRepository repository = new EnvironmentRepository();
		LOG.debug("Created EnvironmentRepository bean");
		return repository;
	}

	@Bean
	public ServerRepository serverRepository() {
		ServerRepository repository = new ServerRepository();
		LOG.debug("Created ServerRepository bean");
		return repository;
	}

	@Bean
	public UserRepository userRepository() {
		UserRepository repository = new UserRepository();
		LOG.debug("Created UserRepository bean");
		return repository;
	}

	@Bean
	public DeployStreamProvider deployStreamProvider() {
		DeployStreamProvider provider = new DeployStreamProviderImpl();
		LOG.debug("Created DeployStreamProvider bean");
		return provider;
	}

	@Bean
	public FileStoreService fileStoreService() {
		FileStoreService service = new FileSystemFileStoreService();
		LOG.debug("Created FileStoreService bean");
		return service;
	}

	@Bean
	public LayerService layerService() {
		LayerService service = new LayerService();
		LOG.debug("Created LayerService bean");
		return service;
	}

	@Bean
	public PackageService packageService() {
		PackageService service = new PackageService();
		LOG.debug("Created PackageService bean");
		return service;
	}

	@Bean
	public VariableService variableService() {
		VariableService service = new VariableService();
		LOG.debug("Created VariableService bean");
		return service;
	}
}
