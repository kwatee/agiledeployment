/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.cli.spring;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class CLIUserDetailsImpl implements UserDetailsService {

	/**
	 * Implementation of <code>UserDetailsService</code>
	 */
	@Override
	public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
		GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_SRM");
		Collection<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(authority);
		return new User(login, "dummy", authorities);
	}

}
