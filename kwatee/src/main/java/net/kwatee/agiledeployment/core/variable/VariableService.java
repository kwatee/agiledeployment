/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.variable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.kwatee.agiledeployment.common.VariableResolver;
import net.kwatee.agiledeployment.common.exception.InternalErrorException;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.common.utils.CryptoUtils;
import net.kwatee.agiledeployment.common.utils.PathUtils;
import net.kwatee.agiledeployment.core.deploy.PlatformService;
import net.kwatee.agiledeployment.repository.dto.VariableReference;

import org.apache.commons.compress.utils.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VariableService {

	final static private String OLD_RELEASE_NAME = "kwatee_deployment_name";
	final static private String OLD_ARTIFACT_NAME = "kwatee_package_name";

	static private enum Qualifier {
		none, dir, file, uxdir, uxfile, windir, winfile, winescdir, winescfile
	}

	final static private int MAX_TEMPLATIZED_FILE_LENGTH = 1024 * 1024; /* 1M */
	final static public char VAR_PREFIX_CHAR = '%';
	final static private String VARIABLE_REGEXP = "(?:%\\{)(\\w+\\:)?([a-zA-Z0-9_\\.\\-]+)(=[^\\}]*)?(?:\\})";

	final static private org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(VariableService.class);

	private Map<String, String> globalProperties;

	@Autowired
	private PlatformService platformService;

	static public Pattern getVariablePattern(char varPrefixChar) {
		String modifiedVariableRegExp;
		if (varPrefixChar == VAR_PREFIX_CHAR) {
			modifiedVariableRegExp = VARIABLE_REGEXP;
		} else {
			if ("*.():{}+-?[]!|~^'`\\".indexOf(varPrefixChar) >= 0) {
				modifiedVariableRegExp = VARIABLE_REGEXP.replaceFirst(Character.toString(VAR_PREFIX_CHAR), "\\\\" + varPrefixChar);
			} else {
				modifiedVariableRegExp = VARIABLE_REGEXP.replace(VAR_PREFIX_CHAR, varPrefixChar);
			}
		}
		return Pattern.compile(modifiedVariableRegExp);
	}

	/**
	 * 
	 * @param text
	 * @param resolver
	 * @return text with variables instantiated
	 * @throws MissingVariableException
	 */
	public String instantiateVariables(String text, VariableResolver resolver) throws MissingVariableException {
		if (text == null) {
			return text;
		}
		return instantiateVariables(text, resolver, null);
	}

	/**
	 * 
	 * @param text
	 * @param parameters
	 * @param release
	 * @param server
	 * @param version
	 * @param filePath
	 * @param varPrefixChar
	 * @return
	 * @throws MissingVariableException
	 * @throws MissingVariableException
	 */
	private String instantiateVariables(String text, VariableResolver resolver, String filePath) throws MissingVariableException {
		Character c = (filePath == null || resolver == null) ? null : resolver.getVariablePrefixChar();
		char varPrefixChar = c == null ? VAR_PREFIX_CHAR : c;
		if (text.indexOf(varPrefixChar) < 0)
			return text;
		Pattern varPattern = getVariablePattern(varPrefixChar);
		Matcher matcher = varPattern.matcher(text);
		StringBuffer sb = new StringBuffer();
		MissingVariableException exception = null;
		while (matcher.find()) {
			String variableName = matcher.group(2);
			try {
				String variableValue = fetchVariableValue(variableName, resolver, filePath);
				matcher.appendReplacement(sb, StringUtils.EMPTY);
				if (matcher.group(1) == null) {
					sb.append(variableValue);
				}
				else {
					Integer platform = resolver.getServerPlatform();
					Integer pathType = platform == null ? null : this.platformService.getPathType(platform);
					sb.append(applyQualifier(variableValue, matcher.group(1), pathType));
				}
			} catch (MissingVariableException e) {
				matcher.appendReplacement(sb, StringUtils.EMPTY);
				sb.append(StringUtils.EMPTY);
				if (exception == null) {
					exception = e;
				}
				else {
					exception.addMissingVariable(e, null);
				}
			} catch (IllegalArgumentException iae) {
				LOG.debug(text, iae);
				throw new MissingVariableException(variableName, resolver == null ? null : resolver.getResolverName(), filePath);
			}
		}
		if (exception != null) {
			throw exception;
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	/**
	 * 
	 * @param value
	 * @param qualifier
	 * @param pathType
	 * @return
	 * @throws MissingVariableException
	 */
	private String applyQualifier(String value, String qualifier, int pathType) throws IllegalArgumentException {
		switch (Qualifier.valueOf(qualifier.substring(0, qualifier.length() - 1))) {
			case dir:
				return PathUtils.platformPath(value + '/', pathType);
			case file:
				return PathUtils.platformPath(value, pathType);
			case uxdir:
				return PathUtils.uxPath(value + '/');
			case uxfile:
				return PathUtils.uxPath(value);
			case windir:
				return PathUtils.winEscapedPath(value + '\\');
			case winfile:
				return PathUtils.winEscapedPath(value);
			case winescdir:
				return PathUtils.winEscapedPath(value + '\\');
			case winescfile:
				return PathUtils.winEscapedPath(value);
			default:
				return value;
		}
	}

	/**
	 * Computes the digest of the stream and returns the collection of variables found
	 * 
	 * @param in
	 * @param md
	 * @param varPrefixChar
	 * @return collection of variables found in the stream, null if not a template
	 */
	public String extractStreamVariables(InputStream in, MessageDigest md, char varPrefixChar) {
		byte[] buffer = new byte[MAX_TEMPLATIZED_FILE_LENGTH];
		int len = 0;
		try {
			len = in.read(buffer, 0, MAX_TEMPLATIZED_FILE_LENGTH);
			if (len <= 0)
				return null;
			if (len == MAX_TEMPLATIZED_FILE_LENGTH) {
				/*
				 * File is too large to be scanned for variables
				 */
				if (md != null) {
					do {
						md.update(buffer, 0, len);
						len = in.read(buffer, 0, MAX_TEMPLATIZED_FILE_LENGTH);
					} while (len > 0);
				}
				return null;
			}
		} catch (IOException e) {
			throw new InternalErrorException(e);
		}

		if (md != null) {
			md.update(buffer, 0, len);
		}

		Pattern varPattern = getVariablePattern(varPrefixChar);
		VarInfo variables = new VarInfo();
		String data = new String(buffer, 0, len, Charsets.UTF_8);
		Matcher matcher = varPattern.matcher(data);
		while (matcher.find()) {
			String varName = matcher.group(2);
			String defaultValue = matcher.group(3) == null ? null : matcher.group(3).substring(1);
			variables.add(varName, defaultValue);
		}
		return variables.toString();
	}

	/**
	 * 
	 * @param prop
	 * @return
	 */
	private String getGlobalProperty(final String prop) {
		if (this.globalProperties == null) {
			return null;
		}
		return this.globalProperties.get(prop);
	}

	/**
	 * 
	 */
	public void setGlobalProperties(Map<String, String> globalProperties) {
		this.globalProperties = globalProperties;
	}

	/**
	 * 
	 * @param variableName
	 * @param resolver
	 * @return value of variable
	 * @throws MissingVariableException
	 */
	public String fetchVariableValue(String variableName, VariableResolver resolver) throws MissingVariableException {
		return fetchVariableValue(variableName, resolver, null);
	}

	/**
	 * 
	 * @param variableName
	 * @param resolver
	 * @param filePath
	 * @return value of variable
	 * @throws MissingVariableException
	 */
	private String fetchVariableValue(String variableName, VariableResolver resolver, String filePath) throws MissingVariableException {
		if (OLD_ARTIFACT_NAME.equals(variableName))
			variableName = VariableResolver.ARTIFACT_NAME;
		if (OLD_RELEASE_NAME.equals(variableName))
			variableName = VariableResolver.RELEASE_NAME;
		String val = resolver == null ? null : resolver.getVariableValue(variableName);
		if (val == null) {
			val = getGlobalProperty(variableName);
		}
		if (val != null)
			return instantiateVariables(val, resolver, filePath);
		LOG.debug("fetchVariableValue unresolved variable {}", variableName);
		throw new MissingVariableException(variableName, resolver == null ? null : resolver.getResolverName(), filePath);
	}

	/**
	 * @param exception
	 * @param missingVariables
	 */
	public void addMissingVariables(MissingVariableException exception, Map<String, Collection<VariableReference>> missingVariables) {
		for (MissingVariableException.MissingVariableInfo i : exception.getMissing()) {
			Collection<VariableReference> whereMissing = missingVariables.get(i.variableName);
			if (whereMissing == null) {
				whereMissing = new ArrayList<>();
				missingVariables.put(i.variableName, whereMissing);
			}
			whereMissing.add(new VariableReference(i.getServerArtifact(), i.getFileName()));
		}
	}

	/**
	 * @param templateFileSize
	 * @param variables
	 * @param resolver
	 * @return size of the template if variables were instantiated
	 * @throws MissingVariableException
	 */
	public long getInstantiatedTemplateFileSize(long templateFileSize, String variables, VariableResolver resolver) throws MissingVariableException {
		long instantiatedSize = templateFileSize;
		VarInfo varInfo = VarInfo.valueOf(variables);
		for (int i = 0; i < varInfo.size(); i++) {
			String vName = varInfo.getName(i);
			String defaultValue = varInfo.getDefaultValue(i);
			String value = fetchVariableValue(vName, resolver);
			int vNameLen = vName.getBytes().length;
			int valueLen = value.getBytes().length;
			if (defaultValue == null)
				instantiatedSize += (valueLen - (vNameLen + 3 /* %{} */)) * varInfo.getUsageCount(i);
			else
				instantiatedSize += valueLen - (vNameLen + defaultValue.getBytes().length + 4 /* %{=} */);
		}
		return instantiatedSize;
	}

	/**
	 * @param path
	 * @param templateStream
	 * @param instanceStream
	 *            which is created with substituted variables only if file is templatized
	 * @param resolver
	 * @return signature of instantiated template
	 * @throws MissingVariableException
	 */
	public String instantiateTemplate(String path, InputStream templateStream, OutputStream instanceStream, VariableResolver resolver) throws MissingVariableException {
		try {
			LOG.debug("instantiateTemplate {}", path);
			String signature = processTemplate(templateStream, path, instanceStream, resolver);
			if (signature == null) {
				throw new InternalErrorException("Not a template");
			}
			return signature;
		} finally {
			IOUtils.closeQuietly(instanceStream);
		}
	}

	/**
	 * 
	 * @param path
	 * @param in
	 * @param resolver
	 * @return signature of file
	 * @throws MissingVariableException
	 */
	public String computeFileSignature(String path, InputStream in, VariableResolver resolver) throws MissingVariableException {
		LOG.debug("computeDeploymentFileSignature template {}", path);
		String signature = processTemplate(in, path, null, resolver);
		if (signature == null) {
			throw new InternalErrorException("Not a template");
		}
		return signature;
	}

	/**
	 * 
	 * @param in
	 * @param relPath
	 * @param out
	 * @param release
	 * @param server
	 * @param version
	 * @param md
	 * @param stopAtFirstError
	 * @return signature
	 * @throws MissingVariableException
	 */
	private String processTemplate(InputStream in, String relPath, OutputStream out, VariableResolver resolver) throws MissingVariableException {
		String data;
		try {
			data = IOUtils.toString(in, Charsets.UTF_8);
		} catch (IOException e) {
			throw new InternalErrorException(e);
		}

		MessageDigest md = CryptoUtils.getNewDigest(relPath);
		Character c = (resolver == null || relPath == null) ? null : resolver.getVariablePrefixChar();
		char varPrefixChar = c == null ? VAR_PREFIX_CHAR : c;
		Pattern varPattern = getVariablePattern(varPrefixChar);
		try {
			Matcher matcher = varPattern.matcher(data);
			while (matcher.find()) {
				int regionStart = matcher.regionStart();
				int startIdx = matcher.start();
				if (startIdx > regionStart) {
					String left = data.substring(regionStart, startIdx);
					byte[] bytes = left.getBytes(Charsets.UTF_8);
					md.update(bytes);
					if (out != null) {
						out.write(bytes);
					}
				}
				String varName = matcher.group(2);
				String value = fetchVariableValue(varName, resolver, relPath);
				if (matcher.group(1) != null) {
					Integer platform = resolver.getServerPlatform();
					Integer pathType = platform == null ? null : this.platformService.getPathType(platform);
					value = applyQualifier(value, matcher.group(1), pathType);
				}

				byte[] bytes = value.getBytes(Charsets.UTF_8);
				md.update(bytes);
				if (out != null) {
					out.write(bytes);
				}
				matcher.region(matcher.end(), data.length());
			}
			int regionStart = matcher.regionStart();
			if (regionStart < data.length()) {
				String right = data.substring(regionStart, data.length());
				byte[] bytes = right.getBytes(Charsets.UTF_8);
				md.update(bytes);
				if (out != null) {
					out.write(bytes);
				}
			}
			return CryptoUtils.getSignature(md);
		} catch (IOException e) {
			throw new InternalErrorException(e);
		} finally {
			IOUtils.closeQuietly(out);
		}
	}

	public boolean isScannable(long size) {
		return size > 0 && size < MAX_TEMPLATIZED_FILE_LENGTH;
	}
}
