package net.kwatee.agiledeployment.webapp.security;

import java.util.ArrayList;
import java.util.Collection;

import net.kwatee.agiledeployment.repository.entity.Authority;
import net.kwatee.agiledeployment.repository.entity.User;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Checks the validity of the ApiTokenAuthentication (JWT). If valid, constructs and authentication
 * with a UserDetails (in-memory UserShortDto).
 * 
 * @author mac
 * 
 */
public class ApiTokenAuthenticationProvider implements AuthenticationProvider {

	final private String debugToken;

	public ApiTokenAuthenticationProvider(String debugToken) {
		this.debugToken = debugToken;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String token = (String) authentication.getCredentials();
		User user;
		if (StringUtils.isNotEmpty(this.debugToken) && this.debugToken.equals(token)) {
			user = new User();
			user.setLogin("admin");
			Collection<Authority> authorities = new ArrayList<>();
			authorities.add(new Authority(Authority.ROLE_USER));
			authorities.add(new Authority(Authority.ROLE_ADMIN));
			user.setAuthorities(authorities);
		} else {
			user = ApiTokenUtils.verifyApiToken(token);
		}
		if (user == null) {
			throw new BadCredentialsException("Invalid token");
		}
		Authentication auth = new ApiTokenAuthentication(user);
		auth.setAuthenticated(true);
		return auth;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return ApiTokenAuthentication.class.isAssignableFrom(authentication);
	}

}
