/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.variable.impl;

import java.util.Properties;

import net.kwatee.agiledeployment.common.VariableResolver;

import org.apache.commons.lang3.StringUtils;

public class ParamVariableResolver implements VariableResolver {

	final private VariableResolver resolver;
	final private Properties params;

	public ParamVariableResolver(VariableResolver resolver, Properties params) {
		this.resolver = resolver;
		this.params = params;
	}

	@Override
	public String getVariableValue(String varName) {
		String value = this.resolver == null ? null : this.resolver.getVariableValue(varName);
		if (value == null && this.params != null)
			value = this.params.getProperty(varName);
		return value;
	}

	@Override
	public Character getVariablePrefixChar() {
		return this.resolver == null ? null : this.resolver.getVariablePrefixChar();
	}

	@Override
	public String getResolverName() {
		return this.resolver == null ? StringUtils.EMPTY : this.resolver.getResolverName();
	}

	@Override
	public Integer getServerPlatform() {
		return this.resolver == null ? null : this.resolver.getServerPlatform();
	}
}
