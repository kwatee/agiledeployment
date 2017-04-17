/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.utils;

import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class CryptoUtils {

	final private static String CRYPT_PREFIX = "<<";
	final private static String CRYPT_SUFFIX = ">>";
	final private static String M = "miliopglcfniqrmqdcmnposuhloneqmf";

	/**
	 * Computes a MD5 digest of a message
	 * 
	 * @param message
	 *            the source message
	 * @return the digest of message
	 */
	static public MessageDigest getNewDigest(String message) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(message.getBytes());
			return md;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("MD5 not available");
		}
	}

	/**
	 * @param digest
	 *            a MD5 digest
	 * @return a hexadecimal representation of a MD5 digest
	 */
	static public String getSignature(MessageDigest digest) {
		return toHexString(digest.digest());
	}

	/**
	 * Computes the signature of a string
	 * 
	 * @param text
	 *            the text for which the signature must be generates
	 * @return the hexadecimal string representation of the md5 digest of text
	 */
	static public String computeStringSignature(String text) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(text.getBytes());
			return getSignature(md);
		} catch (Exception e) {
			System.err.println("Failed to compute MD5\n" + e.fillInStackTrace());
			return StringUtils.EMPTY;
		}
	}

	/**
	 * Computes the signature of an {@link java.io.InputStream}
	 * 
	 * @param stream
	 *            the input stream
	 * @param text
	 *            an additional text to append to the stream which is included in the signature
	 * @return the hexadecimal string representation of the md5 digest of the stream content + text
	 */
	static public String computeStreamSignature(InputStream stream, String text) {
		try {
			MessageDigest md = getNewDigest(text);
			try {
				byte[] buffer = new byte[8192]; // arbitrary buffer size
				int count = stream.read(buffer);
				while (count != -1) {
					md.update(buffer, 0, count);
					count = stream.read(buffer);
				}
				return getSignature(md);
			} finally {
				IOUtils.closeQuietly(stream);
			}
		} catch (Exception e) {
			System.err.println("Failed to compute MD5 for file " + text + '\n' + e.fillInStackTrace());
			return StringUtils.EMPTY;
		}
	}

	/**
	 * Convert a byte array to a hexadecimal string representation
	 * 
	 * @param b
	 *            byte[] parameter
	 * @return the hexadecimal string representation of <code>b</code>
	 */
	static private String toString(byte[] s) {
		StringBuilder sb = new StringBuilder();
		int prev = 0;
		for (byte b : s) {
			sb.append((char) ('a' + ((b >> 4) & 0x0F) + (prev % 10)));
			sb.append((char) ('a' + (b & 0x0F) + (prev % 10)));
			prev = b & 0x00FF;
		}
		return sb.toString();
	}

	/**
	 * Convert a byte array to a hexadecimal string representation
	 * 
	 * @param b
	 *            byte[] parameter
	 * @return the hexadecimal string representation of <code>b</code>
	 */
	static private String toHexString(byte[] s) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < s.length; i++) {
			result.append(Integer.toString((s[i] & 0xff) + 0x100, 16).substring(1));
		}
		return result.toString();
	}

	/**
	 * @param nibble
	 * @param prev
	 * @return hex char
	 */
	private static int nibbleToInt(char nibble, int prev) {
		return (int) (nibble - 'a' - (prev % 10)) & 0x0F;
	}

	/**
	 * @param hex
	 * @return raw data
	 */
	private static byte[] hexStringToBytes(String hex) {
		byte buffer[] = new byte[hex.length() / 2];
		int prev = 0;
		for (int i = 0; i < buffer.length; i++) {
			int n1 = nibbleToInt(hex.charAt(2 * i), prev);
			int n2 = nibbleToInt(hex.charAt(2 * i + 1), prev);
			buffer[i] = (byte) ((n1 << 4) + n2);
			prev = buffer[i] & 0x00FF;
		}
		return buffer;
	}

	/**
	 * @param message
	 * @return cryptotext
	 */
	public static String encrypt(String message) {
		if (message != null) {
			try {
				SecretKeySpec key = new SecretKeySpec(hexStringToBytes(M), "AES");
				Cipher cipher = Cipher.getInstance("AES");
				cipher.init(Cipher.ENCRYPT_MODE, key);
				byte[] buf = cipher.doFinal(message.getBytes(Charsets.UTF_8));
				return CRYPT_PREFIX + toString(buf) + CRYPT_SUFFIX;
			} catch (GeneralSecurityException e) {}
		}
		return message;
	}

	/**
	 * @param ciphertext
	 * @return plaintext
	 */
	public static String decrypt(String ciphertext) {
		if (ciphertext != null && ciphertext.startsWith(CRYPT_PREFIX) && ciphertext.endsWith(CRYPT_SUFFIX)) {
			try {
				SecretKeySpec key = new SecretKeySpec(hexStringToBytes(M), "AES");
				Cipher cipher = Cipher.getInstance("AES");
				cipher.init(Cipher.DECRYPT_MODE, key);
				byte[] buf = cipher.doFinal(hexStringToBytes(ciphertext.substring(2, ciphertext.length() - 2)));
				return new String(buf);
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
			}
		}
		return ciphertext;
	}
}
