/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

public class FileUtils {

	// /**
	// * @param from
	// * @param to
	// * @throws KwateeException
	// */
	// static public void copyFile(File from, File to) throws KwateeException {
	// try {
	// to.getParentFile().mkdirs();
	// FileChannel inChannel = null;
	// FileChannel outChannel = null;
	// try {
	// inChannel = new FileInputStream(from).getChannel();
	// outChannel = new FileOutputStream(to).getChannel();
	// inChannel.transferTo(0, inChannel.size(), outChannel);
	// } catch (IOException e) {
	// throw e;
	// } finally {
	// IOUtils.closeQuietly(inChannel);
	// IOUtils.closeQuietly(outChannel);
	// }
	// } catch (IOException e) {
	// throw new InternalErrorException(e);
	// }
	// }

	// /**
	// * @param from
	// * @param to
	// * @throws KwateeException
	// * @throws IOException
	// */
	// static public void copyFile(InputStream from, OutputStream to) throws IOException {
	// byte[] buffer = new byte[8192];
	// int len;
	// while ((len = from.read(buffer)) > 0) {
	// to.write(buffer, 0, len);
	// }
	// }

	// /**
	// * @param sourceLocation
	// * @param targetLocation
	// * @throws KwateeException
	// */
	// static public void copyFilesRecursively(File sourceLocation, File targetLocation) throws KwateeException {
	// if (sourceLocation.exists()) {
	// if (sourceLocation.isDirectory()) {
	// if (!targetLocation.exists()) {
	// targetLocation.mkdirs();
	// }
	// for (File child : sourceLocation.listFiles()) {
	// copyFilesRecursively(child, new File(targetLocation, child.getName()));
	// }
	// } else {
	// copyFile(sourceLocation, targetLocation);
	// }
	// }
	// }

	// /**
	// * @param dir
	// * @throws KwateeException
	// */
	// static public void deleteDirRecursively(File dir) throws KwateeException {
	// if (dir.exists()) {
	// File[] files = dir.listFiles();
	// for (File f : files) {
	// if (!f.getName().equals(".") && !f.getName().equals("..")) {
	// if (f.isDirectory() && f.getAbsolutePath().startsWith(dir.getAbsolutePath())) {
	// deleteDirRecursively(f);
	// }
	// else {
	// f.delete();
	// }
	// }
	// }
	// if (!dir.delete()) {
	// throw new InternalErrorException("Could not remove directory " + dir.getAbsolutePath());
	// }
	// }
	// }

	static public String getResourceAsString(Class<?> c, String path) {
		InputStream is = c.getResourceAsStream(path);
		if (is != null) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(is));
				StringBuilder fileData = new StringBuilder();
				char[] buf = new char[1024];
				int numRead = 0;
				while ((numRead = reader.read(buf)) != -1) {
					String readData = String.valueOf(buf, 0, numRead);
					fileData.append(readData);
				}
				return fileData.toString();
			} catch (IOException e) {
			} finally {
				IOUtils.closeQuietly(reader);
				IOUtils.closeQuietly(is);
			}
		}
		return null;
	}

	static public Properties getResourceAsProperties(Class<?> c, String path) {
		InputStream is = c.getResourceAsStream(path);
		if (is != null) {
			Properties p = new Properties();
			try {
				p.load(is);
				return p;
			} catch (IOException e) {
			} finally {
				IOUtils.closeQuietly(is);
			}
		}
		return null;
	}

	static public long getResourceSize(Class<?> c, String path) {
		InputStream is = c.getResourceAsStream(path);
		if (is != null) {
			try {
				long size = 0;
				int l;
				byte[] dummy = new byte[10000];
				while ((l = is.read(dummy)) > 0) {
					size += l;
				}
				return size;
			} catch (IOException e) {
			} finally {
				IOUtils.closeQuietly(is);
			}
		}
		return -1;
	}

	// static public void writeTextToFile(File file, String text) throws IOException {
	// OutputStream out = new FileOutputStream(file);
	// byte[] bytes = text.getBytes();
	// out.write(bytes);
	// IOUtils.closeQuietly(out);
	// }

	// static public String loadTextFile(File file) throws IOException {
	// InputStream is = new FileInputStream(file);
	// StringBuilder buffer = new StringBuilder();
	// try {
	// byte[] bytes = new byte[2048];
	// int len;
	// while ((len = is.read(bytes)) > 0)
	// buffer.append(new String(bytes, 0, len));
	// return buffer.toString();
	// } finally {
	// IOUtils.closeQuietly(is);
	// }
	// }

}
