/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.conduit;

import java.util.List;

public class PropertyDescriptor {

	public enum TYPE {
		STRING, INTEGER, BOOLEAN, SELECTION
	};

	private String name;
	private String label;
	private boolean mandatory;
	private TYPE type;
	private String defaultValue;
	private List<String> selection;

	/**
	 * 
	 * @param name
	 *            property name
	 * @param label
	 *            property display name
	 */
	PropertyDescriptor(String name, String label) {
		this(name, label, false);
	}

	/**
	 * 
	 * @param name
	 *            property name
	 * @param label
	 *            property display name
	 * @param mandatory
	 *            true if a value is required for the property
	 */
	PropertyDescriptor(String name, String label, boolean mandatory) {
		this(name, label, mandatory, TYPE.STRING);
	}

	/**
	 * 
	 * @param name
	 *            property name
	 * @param label
	 *            property display name
	 * @param mandatory
	 *            true if a value is required for the property
	 * @param type
	 *            the {@link TYPE} of the property
	 */
	PropertyDescriptor(String name, String label, boolean mandatory, TYPE type) {
		this.name = name;
		this.label = label;
		this.mandatory = mandatory;
		this.type = type;
	}

	/**
	 * 
	 * @return the name of the property
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 
	 * @return the label (display name) of the property
	 */
	public String getLabel() {
		return this.label;
	}

	/**
	 * 
	 * @return the {@link TYPE} of the property
	 */
	public TYPE getType() {
		return this.type;
	}

	/**
	 * 
	 * @return true if the property requires a value
	 */
	public boolean isMandatory() {
		return this.mandatory;
	}

	/**
	 * Sets the default value of the property
	 * 
	 * @param defaultValue
	 *            of the property
	 */
	void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * 
	 * @return the default value of the property
	 */
	public String getDefaultValue() {
		return this.defaultValue;
	}

	/**
	 * Sets the current value of a multivalued property
	 * 
	 * @param selection
	 */
	void setSelection(List<String> selection) {
		this.selection = selection;
	}

	/**
	 * 
	 * @return the current value of a multivalued property
	 */
	public List<String> getSelection() {
		return this.selection;
	}
}
