/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.cli.spring;

import java.io.File;

import net.kwatee.agiledeployment.cli.CLIDeployStreamProviderImpl;
import net.kwatee.agiledeployment.cli.Deployer;
import net.kwatee.agiledeployment.common.exception.KwateeException;
import net.kwatee.agiledeployment.core.conduit.impl.FileSystemConduitFactory;
import net.kwatee.agiledeployment.core.repository.DeployStreamProvider;
import net.kwatee.agiledeployment.core.spring.CoreServicesConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.Environment;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;

@Configuration
@Import({ CoreServicesConfig.class })
@ImportResource({ "classpath:/net/kwatee/agiledeployment/cli/spring/security.xml" })
public class ApplicationConfig {

	ApplicationConfig() {
		System.setProperty("kwatee.conduit.conduitFactories", FileSystemConduitFactory.class.getCanonicalName());
		System.setProperty("kwatee.conduit.serverFactories", "");
	}

	@Autowired
	protected Environment environment;

	@Bean
	Deployer getDeployer() throws AuthenticationException, KwateeException {
		return new Deployer();
	}

	@Bean(name = "tempDirPath")
	public String getTempDirPath() {
		String dir = this.environment.getProperty("kwatee.tmp.path");
		if (dir == null) {
			dir = new File(System.getProperty("java.io.tmpdir"), "kwatee").getAbsolutePath() + "/";
		}
		return dir;
	}

	@Bean(name = "kwateeUserDetails")
	public UserDetailsService getKwateeUserDetails() {
		return new CLIUserDetailsImpl();
	}

	@Bean
	public DeployStreamProvider getCLIDeployStreamProvider() {
		String bundleDir = System.getProperty("kwatee.installer.bundle.dir");
		String configurationDir = System.getProperty("kwatee.installer.configuration.dir");
		return new CLIDeployStreamProviderImpl(bundleDir, configurationDir);
	}

}
