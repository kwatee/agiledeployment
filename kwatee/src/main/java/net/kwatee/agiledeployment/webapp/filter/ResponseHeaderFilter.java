/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class ResponseHeaderFilter implements Filter {

	private String headers[] = new String[0];
	private String values[] = new String[0];

	@Override
	public void destroy() {}

	private void addHeader(HttpServletResponse response) {
		for (int i = 0; i < this.headers.length; i++) {
			response.setHeader(this.headers[i], this.values[i]);
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (response instanceof HttpServletResponse)
			addHeader((HttpServletResponse) response);
		chain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
		Enumeration<String> names = config.getInitParameterNames();
		ArrayList<String> headers = new ArrayList<>();
		ArrayList<String> values = new ArrayList<>();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			headers.add(name);
			values.add(config.getInitParameter(name));
		}
		this.headers = headers.toArray(new String[0]);
		this.values = values.toArray(new String[0]);

	}

}