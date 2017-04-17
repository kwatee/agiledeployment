/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.security;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Checks for the presence of an api token to be used for request authentication.
 * If present, sets a (refreshed) token in the response.
 * 
 * 
 * @author mac
 * 
 */
public class ApiTokenAuthFilter implements Filter {

	final private AuthenticationManager authenticationManager;

	public ApiTokenAuthFilter(AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		String token = ApiTokenUtils.getApiToken((HttpServletRequest) request);
		if (token != null) {
			Authentication auth = new ApiTokenAuthentication(token);
			try {
				auth = this.authenticationManager.authenticate(auth);
				SecurityContextHolder.getContext().setAuthentication(auth);
				ApiTokenUtils.setApiToken((HttpServletResponse) response, token);
			} catch (AuthenticationException e) {
				((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
		}
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {}

	@Override
	public void init(FilterConfig arg0) throws ServletException {}

};
