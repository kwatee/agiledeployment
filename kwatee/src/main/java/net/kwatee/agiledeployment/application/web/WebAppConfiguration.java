/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.application.web;

/**
 * 
 * @author mac
 * 
 */

import net.kwatee.agiledeployment.core.CoreServicesConfig;
import net.kwatee.agiledeployment.core.DataSourceConfig;
import net.kwatee.agiledeployment.core.PersistenceConfig;
import net.kwatee.agiledeployment.webapp.ApiServicesConfig;
import net.kwatee.agiledeployment.webapp.security.SecurityConfig;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@Import({ApiServicesConfig.class, PersistenceConfig.class, DataSourceConfig.class, SecurityConfig.class, CoreServicesConfig.class})
public class WebAppConfiguration extends WebMvcConfigurerAdapter {

	@Value("${kwatee.addons.name:}")
	private String addOns;
	@Value("${kwatee.addons.path:}")
	private String addOnsPath;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/ui/**").addResourceLocations("classpath:/public/ui/").setCachePeriod(31556926);
		if (StringUtils.isNotEmpty(this.addOns) && StringUtils.isNotEmpty(this.addOnsPath))
			registry.addResourceHandler("/" + this.addOns + "/**").addResourceLocations("file:" + this.addOnsPath).setCachePeriod(31556926);
	}

	@Override
	public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
		configurer.enable();
	}

	@Bean
	@Primary
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_EMPTY);
		mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return mapper;

	}

	/*
	@Bean(name = "multipartResolver")
	public CommonsMultipartResolver multipartResolver() {
		return new CommonsMultipartResolver();
	}
	*/
}
