/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.test.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(ApiTestConfiguration.class)
public class TestApplication extends SpringBootServletInitializer {

	public static void main(String[] args) throws Exception {
		SpringApplication app = new SpringApplication(TestApplication.class);
		app.setShowBanner(false);
		app.run(args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(TestApplication.class);
	}
}