/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.kwatee.agiledeployment.repository.entity.Authority;
import net.kwatee.agiledeployment.repository.entity.User;

import org.springframework.web.util.WebUtils;

/**
 * Wrapper aroung JWTProcessor to manage kwatee-specific JWT claims.
 * 
 * @author mac
 * 
 */
class ApiTokenUtils {

	public final static String API_AUTH_HEADER = "X-API-AUTH";
	private final static long REFRESH_TIMEOUT = 3600L * 1000L; // 1 hour
	private final static long EXPIRATION_TIMEOUT = 72L * 3600L * 1000L; // 72 hours
	private final static String CLAIM_USER_ID = "userid";
	private final static String CLAIM_USER_NAME = "username";
	private final static String CLAIM_ROLES = "roles";
	private final static String CLAIM_REFRESH_TIME = "refreshAfter";

	final static private JWTProcessor jwt = new JWTProcessor(EXPIRATION_TIMEOUT);

	static public String getApiToken(HttpServletRequest request) {
		String token = request.getHeader(ApiTokenUtils.API_AUTH_HEADER);
		if (token == null) {
			Cookie authCookie = WebUtils.getCookie(request, "api-token");
			if (authCookie != null)
				token = authCookie.getValue();
		}
		return isValidToken(token) ? token : null;
	}

	static void setApiToken(HttpServletResponse response, String token) {
		response.setHeader(ApiTokenUtils.API_AUTH_HEADER, token);
	}

	static private boolean isValidToken(String token) {
		if (token != null) {
			Map<String, Object> claims = jwt.getClaims(token);
			if (claims != null)
				return true;
		}
		return false;
	}

	static User verifyApiToken(String token) {
		if (token != null) {
			Map<String, Object> claims = jwt.getClaims(token);
			try {
				User user = new User();
				user.setId((Long) claims.get(CLAIM_USER_ID));
				user.setLogin((String) claims.get(CLAIM_USER_NAME));
				String[] roles = ((String) claims.get(CLAIM_ROLES)).split(",");
				ArrayList<Authority> authorities = new ArrayList<>();
				for (String role : roles) {
					authorities.add(new Authority(role));
				}
				user.setAuthorities(authorities);
				return user;
			} catch (Exception e) {}
		}
		return null;
	}

	static String buildApiToken(User user) {
		Map<String, Object> claims = new HashMap<>();
		claims.put(CLAIM_USER_ID, user.getId());
		claims.put(CLAIM_USER_NAME, user.getUsername());
		claims.put(CLAIM_REFRESH_TIME, System.currentTimeMillis() + REFRESH_TIMEOUT);
		StringBuffer roles = new StringBuffer();
		for (Authority authority : user.getAuthorities()) {
			if (roles.length() > 0)
				roles.append(',');
			roles.append(authority.getAuthority());
		}
		claims.put(CLAIM_ROLES, roles.toString());
		return jwt.buildToken(claims);
	}
}
