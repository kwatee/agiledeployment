package net.kwatee.agiledeployment.webapp.filter;

import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.stereotype.Component;

@Component
public class ResponseHeaderRegisteration extends FilterRegistrationBean {

	public ResponseHeaderRegisteration() {
		super(new ResponseHeaderFilter());
		this.addUrlPatterns("*.nocache.js", "*.json");
		this.addInitParameter("Expires", "Sun, 06 Nov 1994 08:49:37 GMT");
		this.addInitParameter("Cache-Control", "no-cache, must-revalidate");
		this.addInitParameter("Pragma", "no-cache");

	}
}
