package net.kwatee.agiledeployment.webapp.security;

import net.kwatee.agiledeployment.repository.entity.User;

import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * 
 * Wrapper to an a JWT authentication (extract from the request headers by ApiTokenAuthenticationFilter)
 * or to an authenticated UserShortDto (UserDetails).
 * 
 * @author mac
 * 
 */
public class ApiTokenAuthentication extends AbstractAuthenticationToken {

	private static final long serialVersionUID = 1L;

	final private String token;
	final private User user;

	public ApiTokenAuthentication(String token) {
		super(null);
		this.user = null;
		this.token = token;
	}

	public ApiTokenAuthentication(User user) {
		super(user.getAuthorities());
		this.user = user;
		this.token = null;
	}

	@Override
	public Object getCredentials() {
		return this.token;
	}

	@Override
	public Object getPrincipal() {
		return this.user;
	}

}
