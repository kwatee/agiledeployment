/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.utils;

public class PathUtils {

	/**
	 * Converts windows path to unix-style path
	 * 
	 * @param path
	 *            a windows-style path
	 * @return unix-style path
	 */
	static public String uxPath(final String path) {
		return path.replace('\\', '/').replaceAll("//", "/");
	}

	/**
	 * Converts unix-style path to windows path
	 * 
	 * @param path
	 *            a unix-style path
	 * @return windows path
	 */
	static public String winPath(final String path) {
		return path.replace('/', '\\').replaceAll("\\\\\\\\", "\\\\");
	}

	/**
	 * 
	 * @param path
	 *            a windows path
	 * @return a windows escaped path
	 */
	static public String winEscapedPath(final String path) {
		return winPath(path).replaceAll("\\\\", "\\\\\\\\");
	}

	/**
	 * Converts window path to cygwin path
	 * 
	 * @param path
	 *            a windows-style path
	 * @return a cygwin-style path
	 */
	static public String cygwinPath(final String path) {
		return uxPath(path).replaceAll("^(\\p{Alpha}+):/", "/cygdrive/$1/");
	}

	/**
	 * Converts path to target path depending on pathType
	 * 
	 * @param path
	 *            a linux-style path
	 * @param pathType
	 *            1=windows, 2=cygwin, 3=uxPath
	 * @return the target-type path
	 */
	static public String platformPath(final String path, final int pathType) {
		switch (pathType) {
		case 1:
			return winPath(path);
		case 2:
			return cygwinPath(uxPath(path));
		default:
			return uxPath(path);
		}
	}

	static public String quotePathIfNeeded(String path) {
		if (path.contains(" ") && !path.startsWith("\"")) {
			return "\"" + path + "\"";
		}
		return path;
	}
}
