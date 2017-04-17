/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.utils;

public class NameValue {

	private String name;
	private String value;

	public NameValue(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return this.name;
	}

	public String getValue() {
		return this.value;
	}
}
