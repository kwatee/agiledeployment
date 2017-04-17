/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class ArtifactNotInReleaseException extends KwateeException {

	public ArtifactNotInReleaseException(String name) {
		super("ArtifactShortDto " + name + " does not exist in release");
	}
}
