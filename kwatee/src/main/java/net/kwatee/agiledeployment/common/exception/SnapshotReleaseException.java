/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class SnapshotReleaseException extends KwateeException {

	public SnapshotReleaseException() {
		super("Operation allowed only on snapshot release");
	}

	public SnapshotReleaseException(String environmentName) {
		super("Illegal operation on snapshot release of environment '" + environmentName + "'");
	}
}
