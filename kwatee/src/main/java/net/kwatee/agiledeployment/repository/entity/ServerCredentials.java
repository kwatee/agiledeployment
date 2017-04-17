/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.entity;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import net.kwatee.agiledeployment.conduit.AccessLevel;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("serial")
@Entity(name = "KWServerCredentials")
@Table(name = "KWServerCredentials")
public class ServerCredentials implements java.io.Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "server_id", nullable = false)
	private Server server;
	private AccessLevel access_level;
	private int password_prompt;
	private String login = StringUtils.EMPTY;
	private String password = StringUtils.EMPTY;
	private String pem;

	public ServerCredentials() {}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Server getServer() {
		return this.server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public AccessLevel getAccessLevel() {
		return this.access_level;
	}

	public void setAccessLevel(AccessLevel accessLevel) {
		this.access_level = accessLevel;
	}

	public String getLogin() {
		return this.login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public boolean isPasswordPrompted() {
		return this.password_prompt != 0;
	}

	public void setPasswordPrompted(boolean password_prompt) {
		this.password_prompt = password_prompt ? 1 : 0;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPem() {
		return this.pem;
	}

	public void setPem(String pem) {
		this.pem = pem;
	}

	public ServerCredentials duplicate() {
		ServerCredentials duplicateCredentials = new ServerCredentials();
		duplicateCredentials.setAccessLevel(this.access_level);
		duplicateCredentials.setLogin(this.login);
		duplicateCredentials.setPasswordPrompted(this.password_prompt != 0);
		duplicateCredentials.setPassword(this.password);
		duplicateCredentials.setPem(this.pem);
		return duplicateCredentials;
	}
}
