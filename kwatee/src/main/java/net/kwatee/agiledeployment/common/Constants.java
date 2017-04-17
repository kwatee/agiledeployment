/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common;

public class Constants {

	public static enum LayerType {
		ALL, ARTIFACT, ARTIFACT_TEMPLATE, ARTIFACT_OVERLAY, COMMON_OVERLAY, SERVER_OVERLAY
	}

	final public static String SNAPSHOT_RELEASE_NAME = "snapshot";
	final public static String REMOTE_ROOT_DIR = "kwatee_root_dir";

	final public static String ARTIFACTS_DIR = "artifacts/";
	final public static String ENVIRONMENTS_DIR = "environments/";
	final public static String OVERLAYS_DIR = "kwatee_overlays/";
	final public static String TEMPLATES_DIR = "kwatee_templates/";

	final public static String ADMIN_USER = "admin";

	final static public String CURRENT_SCHEMA_VERSION = "4.0.2";

	final static public String H2_JDBC_DRIVER = "org.h2.Driver";

}
