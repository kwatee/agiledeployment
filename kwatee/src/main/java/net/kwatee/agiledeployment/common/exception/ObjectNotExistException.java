/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class ObjectNotExistException extends KwateeException {

	public final static String ARTIFACT = "artifact";
	public final static String VERSION = "version";
	public final static String ENVIRONMENT = "environment";
	public final static String RELEASE = "release";
	public final static String SERVER = "server";
	public final static String PLATFORM = "platform";
	public final static String FILE = "file";
	public final static String USER = "user";
	public final static String OPERATION = "remote operation";

	private final String type;

	public ObjectNotExistException(String type, String object) {
		super(object);
		this.type = type;
	}

	public ObjectNotExistException(String type, long object) {
		super(Long.toString(object));
		this.type = type;
	}

	public String getType() {
		return this.type;
	}
}
