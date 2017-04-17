/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Table;

import org.springframework.security.core.GrantedAuthority;

@SuppressWarnings("serial")
@Embeddable
@Table(name = "KWAuthority")
public class Authority implements GrantedAuthority, java.io.Serializable {

	final static public String ROLE_USER = "ROLE_USER";
	final static public String ROLE_OPERATOR = "ROLE_DEPLOYER";
	final static public String ROLE_SRM = "ROLE_SRM";
	final static public String ROLE_ADMIN = "ROLE_ADMIN";
	final static public String ROLE_EXTERNAL = "ROLE_EXTERNAL";

	@Column(name = "authority", nullable = false, updatable = false, insertable = false)
	String authority;

	public Authority() {}

	public Authority(String authority) {
		setAuthority(authority);
	}

	@Override
	public String getAuthority() {
		return this.authority;
	}

	public void setAuthority(String authority) {
		this.authority = authority;
	}

	@Override
	public boolean equals(Object that) {
		return that != null && getAuthority().equals(((Authority) that).getAuthority());
	}
}
