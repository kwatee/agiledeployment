/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.conduit;

public interface StatusWriter {

	/**
	 * Writes a status message to the user interface
	 * 
	 * @param message
	 */
	void statusMessage(String message);
}
