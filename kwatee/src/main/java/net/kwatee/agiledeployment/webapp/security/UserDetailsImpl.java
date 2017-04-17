/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.security;

import net.kwatee.agiledeployment.core.repository.UserRepository;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

/**
 * 
 * @author mac
 * 
 */
public class UserDetailsImpl implements UserDetailsService {

	/**
	 * Implementation of <code>UserDetailsService</code>
	 */
	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException, DataAccessException {
		UserDetails user = UserRepository.getInstance().getUncheckedUser(login);
		if (user != null)
			user.getAuthorities().size(); // lazy load
		if (user == null)
			throw new UsernameNotFoundException(login);
		return user;
	}

	static public UserDetails getAuthenticatedUser() {
		if (SecurityContextHolder.getContext() != null) {
			if (SecurityContextHolder.getContext().getAuthentication() != null) {
				if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof UserDetails) {
					UserDetails user = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
					return user;
				}
			}
		}
		return null;
	}

}
