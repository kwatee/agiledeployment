/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.variable;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class VarInfo {

	private List<VarItem> vars = null;

	protected VarInfo() {}

	protected void add(String varName, String defaultValue) {
		if (defaultValue == null)
			add(varName);
		else {
			if (this.vars == null)
				this.vars = new ArrayList<>();
			this.vars.add(new VarItem(varName, defaultValue));
		}
	}

	protected void add(String varName) {
		if (this.vars == null) {
			this.vars = new ArrayList<>();
		}
		for (VarItem v : this.vars) {
			if (v.getName().equals(varName) && v.getDefaultValue() == null) {
				v.inc();
				return;
			}
		}
		this.vars.add(new VarItem(varName, 1));
	}

	public int size() {
		return this.vars == null ? 0 : this.vars.size();
	}

	public String getName(int i) {
		return this.vars.get(i).getName();
	}

	public String getDefaultValue(int i) {
		return this.vars.get(i).getDefaultValue();
	}

	public int getUsageCount(int i) {
		return vars.get(i).getCount();
	}

	public String toString() {
		if (this.vars == null)
			return null;
		final StringBuilder sb = new StringBuilder();
		for (final VarItem v : this.vars) {
			if (sb.length() > 0)
				sb.append(',');
			sb.append(v.getName());
			if (v.getDefaultValue() != null) {
				try {
					String defaultValue = URLEncoder.encode(v.getDefaultValue(), "UTF-8");
					sb.append(':');
					sb.append(defaultValue);
				} catch (UnsupportedEncodingException e) {}
			}
			sb.append('=');
			sb.append(v.getCount());
		}
		return sb.toString();
	}

	public static VarInfo valueOf(String vars) {
		VarInfo varInfo = new VarInfo();
		varInfo.vars = new ArrayList<>();
		for (String v : vars.split(",")) {
			String[] var = v.split("=");
			String count = var[1];
			var = var[0].split(":");
			String name = var[0];
			String defaultValue;
			try {
				if (var.length == 1)
					defaultValue = v.indexOf(':') > 0 ? StringUtils.EMPTY : null;
				else
					defaultValue = URLDecoder.decode(var[1], "UTF-8");
			} catch (UnsupportedEncodingException e) {
				defaultValue = null;
			}
			if (defaultValue == null)
				varInfo.vars.add(varInfo.new VarItem(name, Integer.valueOf(count)));
			else
				varInfo.vars.add(varInfo.new VarItem(name, defaultValue));
		}
		return varInfo;
	}

	private class VarItem {

		private String varName;
		private String defaultValue; // if defaultValue is defined then count is always 1
		private int count;

		private VarItem(String name, String defaultValue) {
			this.varName = name;
			this.defaultValue = defaultValue;
			this.count = 1;
		}

		private VarItem(String name, int count) {
			this.varName = name;
			this.defaultValue = null;
			this.count = count;
		}

		protected String getName() {
			return this.varName;
		}

		protected String getDefaultValue() {
			return this.defaultValue;
		}

		protected int getCount() {
			return this.count;
		}

		protected void inc() {
			this.count++;
		}
	}
}
