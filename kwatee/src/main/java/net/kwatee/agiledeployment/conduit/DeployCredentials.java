/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.conduit;

import net.kwatee.agiledeployment.conduit.AccessLevel;

public class DeployCredentials {

	private AccessLevel accessLevel;
	private String login;
	private String password;
	private String pem;
	private boolean interactive;

	/**
	 * 
	 * @param accessLevel
	 * @param login
	 * @param password
	 * @param pem
	 * @param interactive
	 */
	public DeployCredentials(AccessLevel accessLevel, String login, String password, String pem, boolean interactive) {
		this.accessLevel = accessLevel;
		this.login = login;
		this.password = password;
		this.pem = pem;
		this.interactive = interactive;
	}

	/**
	 * 
	 * @return the credential's access level
	 */
	public AccessLevel getAccessLevel() {
		return this.accessLevel;
	}

	/**
	 * 
	 * @return the login
	 */
	public String getLogin() {
		return this.login;
	}

	/**
	 * If a pem (private key) is defined in the credentials, the password is used to decrypt an encrypted pem
	 * 
	 * @return the password
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * 
	 * @return the pem (private key)
	 */
	public String getPem() {
		return this.pem;
	}

	/**
	 * 
	 * @return true if the credentials has a pem (private key)
	 */
	public boolean hasPem() {
		return this.pem != null;
	}

	/**
	 * 
	 * @return true if the login/password where obtained by user prompt
	 */
	public boolean interactivelyObtained() {
		return this.interactive;
	}
	
	static public boolean isPrivateKey(String pem) {
		return pem != null && pem.startsWith("-----BEGIN");
	}
	
	static public boolean isPrivateKeyPath(String pem) {
		return pem != null && !pem.startsWith("-----BEGIN");
	}
}
