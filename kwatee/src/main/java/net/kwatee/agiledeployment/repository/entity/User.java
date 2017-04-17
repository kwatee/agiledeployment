/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.entity;

import java.security.MessageDigest;
import java.util.Collection;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@SuppressWarnings("serial")
@Entity(name = "KWUser")
@Table(name = "KWUser")
public class User implements UserDetails, java.io.Serializable {

	public static final int MAX_LOGIN_LEN = 20;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private long creation_ts = new java.util.Date().getTime();
	private Long disable_ts;
	private String login;
	private String password;
	private String description = StringUtils.EMPTY;
	private String email = StringUtils.EMPTY;
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "KWAuthority", joinColumns = @JoinColumn(name = "user_id"))
	private Collection<Authority> authorities = new java.util.HashSet<Authority>();

	public User() {}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public long getCreationTs() {
		return this.creation_ts;
	}

	public void setCreationTs(long creationTs) {
		this.creation_ts = creationTs;
	}

	public Long getDisableTs() {
		return this.disable_ts;
	}

	public void setDisableTs(Long disableTs) {
		this.disable_ts = disableTs;
	}

	public boolean isDisabled() {
		return this.disable_ts != null;
	}

	public void setDisabled(boolean disabled) {
		if (!disabled) {
			setDisableTs(null);
		} else if (getDisableTs() == null) {
			setDisableTs(new java.util.Date().getTime());
		}
	}

	public String getLogin() {
		return this.login;
	}

	public void setLogin(String name) {
		this.login = name;
	}

	@Override
	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setAndHashPassword(String password) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] b = md.digest((password + '{' + getUsername() + '}').getBytes());
			this.password = StringUtils.EMPTY;
			for (int i = 0; i < b.length; i++)
				this.password += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
		} catch (Exception e) {}
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		if (description == null) {
			this.description = null;
		} else {
			this.description = description.length() <= 255 ? description : description.substring(0, 255);
		}
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public Collection<Authority> getAuthorities() {
		return this.authorities;
	}

	public void setAuthorities(Collection<Authority> authorities) {
		this.authorities = authorities;
	}

	public boolean hasAuthority(final String authority) {
		return CollectionUtils.find(this.authorities, new Predicate() {

			public boolean evaluate(Object a) {
				return ((GrantedAuthority) a).getAuthority().equals(authority);
			}
		}) != null;
	}

	@Override
	public String getUsername() {
		return this.login;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return this.disable_ts == null;
	}

	@Override
	public boolean equals(Object that) {
		return that != null && this.login.equals(((User) that).login);
	}

	@Override
	public String toString() {
		return this.login;
	}
}
