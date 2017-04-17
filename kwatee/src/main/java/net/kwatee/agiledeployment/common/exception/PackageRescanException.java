/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class PackageRescanException extends KwateeException {

	public PackageRescanException(String name) {
		super("Package rescan or new upload required for " + name);
	}
}
