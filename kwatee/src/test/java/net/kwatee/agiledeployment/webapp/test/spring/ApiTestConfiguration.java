/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.test.spring;

import java.util.List;

import javax.sql.DataSource;

import net.kwatee.agiledeployment.core.CoreServicesConfig;
import net.kwatee.agiledeployment.core.PersistenceConfig;
import net.kwatee.agiledeployment.webapp.ApiServicesConfig;
import net.kwatee.agiledeployment.webapp.security.SecurityConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@Import({ApiServicesConfig.class, CoreServicesConfig.class, PersistenceConfig.class, SecurityConfig.class})
public class ApiTestConfiguration extends WebMvcConfigurerAdapter {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/ui/**").addResourceLocations("classpath:/public/ui/").setCachePeriod(31556926);
	}

	@Override
	public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
		configurer.enable();
	}

	@Bean(name = "kwateeDatasource")
	public DataSource getKwateeDatasource() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
		EmbeddedDatabase db = builder
				.setType(EmbeddedDatabaseType.H2)
				.addScript("classpath:/net/kwatee/agiledeployment/repository/h2/schema.sql")
				.addScript("classpath:/net/kwatee/agiledeployment/repository/factory_settings.sql")
				.addScript("classpath:/net/kwatee/agiledeployment/repository/test_data.sql")
				.build();
		return db;
	}

	@Bean(name = "jdbcUrl")
	public String jdbcUrl() {
		return "jdbc:h2:mem:kwatee";
	}

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		converters.add(converter());
	}

	@Bean
	MappingJackson2HttpMessageConverter converter() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_EMPTY);
		mapper.setVisibilityChecker(mapper.getVisibilityChecker().withSetterVisibility(Visibility.NONE));
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper);
		return converter;

	}
}
