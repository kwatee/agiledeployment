/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.dao.ReflectionSaltSource;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;

/**
 * 
 * @author mac
 * 
 */

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Value("${kwatee.debug.token:}")
	private String debugToken;

	@Bean(name = "kwateeUserDetails")
	UserDetailsService kwateeUserDetails() {
		return new UserDetailsImpl();
	}

	@Bean(name = "authenticationManager")
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}

	@Bean
	ApiLoginFilter apiLoginFilter() throws Exception {
		return new ApiLoginFilter("/api/authenticate/**", authenticationManagerBean());
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authenticationProvider(new ApiTokenAuthenticationProvider(this.debugToken));
		ReflectionSaltSource rss = new ReflectionSaltSource();
		rss.setUserPropertyToUse("username");
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setSaltSource(rss);
		provider.setUserDetailsService(kwateeUserDetails());
		provider.setPasswordEncoder(new Md5PasswordEncoder());
		http.authenticationProvider(provider);
		http.exceptionHandling()
				.authenticationEntryPoint(new AuthenticationEntryPoint() {

					@Override
					public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
						response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
					}

				});
		http.csrf()
				.disable();
		http.authorizeRequests()
				.antMatchers("/api/xternal/**").permitAll()
				.antMatchers("/api/db/**").permitAll()
				.antMatchers("/api/info/context.json").permitAll()
				.antMatchers("/api/**").authenticated()
				.anyRequest().permitAll();
		http.sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		http.securityContext()
				.securityContextRepository(new NullSecurityContextRepository());
		http
				.addFilterAfter(new ApiTokenAuthFilter(authenticationManagerBean()), AbstractPreAuthenticatedProcessingFilter.class)
				.addFilterBefore(apiLoginFilter(), ApiTokenAuthFilter.class);
	}
}
