package net.kwatee.agiledeployment.webapp.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.kwatee.agiledeployment.common.exception.NoKwateeDBException;
import net.kwatee.agiledeployment.common.exception.NoKwateeSchemaException;
import net.kwatee.agiledeployment.common.exception.SchemaOutOfDateException;
import net.kwatee.agiledeployment.core.Audit;
import net.kwatee.agiledeployment.repository.entity.User;
import net.kwatee.agiledeployment.webapp.service.AdminService;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Processes the /authenticate url to authenticate the request. If the authentication was successful,
 * the api token is set on the response header for the client to use in the next request.
 * 
 * @author mac
 * 
 */
public class ApiLoginFilter extends AbstractAuthenticationProcessingFilter {

	private static final String API_VERSION = "4.0";

	private final static Logger LOG = LoggerFactory.getLogger(ApiLoginFilter.class);

	@Autowired
	private AdminService adminService;
	private boolean schemaUpToDate;

	public ApiLoginFilter(String urlMapping, AuthenticationManager authenticationManager) {
		super(new AntPathRequestMatcher(urlMapping));
		setAuthenticationManager(authenticationManager);
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		String version = request.getParameter("version");
		if (version != null && !API_VERSION.equals(version)) {
			try {
				LOG.warn("Client version does not match {}", version);
				response.sendError(HttpServletResponse.SC_EXPECTATION_FAILED);
			} catch (IOException e1) {}
			return null;
		}
		if (!this.schemaUpToDate) {
			try {
				this.adminService.checkSchema();
			} catch (Exception e) {
				try {
					if (e instanceof NoKwateeDBException) {
						LOG.warn("Database is unreachable");
						response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Database unreachable");
					} else if (e instanceof NoKwateeSchemaException) {
						LOG.warn("No kwatee schema");
						response.sendError(HttpServletResponse.SC_GONE, "No kwatee schema");
					} else if (e instanceof SchemaOutOfDateException) {
						LOG.warn("Kwatee schema out of date");
						response.sendError(426, "Upgrade required");
					}
				} catch (IOException e1) {}
				return null;
			}
			this.schemaUpToDate = true;
		}
		String userName = request.getParameter("username");
		LOG.debug("Login attempt from {}", userName);
		try {
			String password = IOUtils.toString(request.getInputStream(), Charsets.UTF_8);
			Authentication authentication = new UsernamePasswordAuthenticationToken(userName, password);
			try {
				authentication = getAuthenticationManager().authenticate(authentication);
				Audit.log("User {} logged in", userName);
				return authentication;
			} catch (AuthenticationException e) {
				try {
					Audit.log("Failed login attempt from {}", userName);
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				} catch (IOException e1) {}
			}
		} catch (IOException e) {}
		return null;
	}

	@Override
	protected void successfulAuthentication(HttpServletRequest req, HttpServletResponse res, FilterChain chain, Authentication authentication) throws IOException, ServletException {
		// Add the custom token as HTTP header to the response
		String token = ApiTokenUtils.buildApiToken((User) authentication.getPrincipal());
		ApiTokenUtils.setApiToken(res, token);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}
}