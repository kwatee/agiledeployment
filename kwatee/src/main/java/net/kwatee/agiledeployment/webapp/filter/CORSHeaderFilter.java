/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CORSHeaderFilter implements Filter {

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (response instanceof HttpServletResponse) {
			String host = ((HttpServletRequest) request).getHeader("Origin");
			((HttpServletResponse) response).setHeader("Access-Control-Allow-Origin", host);
			((HttpServletResponse) response).setHeader("Access-Control-Allow-Credentials", "true");
			((HttpServletResponse) response).setHeader("Access-Control-Allow-Headers", "accept, accept-encoding, content-type, cookie, origin, www-authenticate, x-api-auth");
			((HttpServletResponse) response).setHeader("Access-Control-Allow-Methods", "GET, PUT, DELETE, POST, OPTIONS");
		}
		chain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
	}

}