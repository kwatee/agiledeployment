/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class DBAdminErrorException extends KwateeException {

	public DBAdminErrorException(Throwable t) {
		super(t);
	}

	public DBAdminErrorException(String msg) {
		super(msg);
	}
}
