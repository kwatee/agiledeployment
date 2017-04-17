package net.kwatee.agiledeployment.webapp.filter;

import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.stereotype.Component;

@Component
public class CORSHeaderRegisteration extends FilterRegistrationBean {

	public CORSHeaderRegisteration() {
		super(new CORSHeaderFilter());
		this.addUrlPatterns("/api/*");

	}
}
