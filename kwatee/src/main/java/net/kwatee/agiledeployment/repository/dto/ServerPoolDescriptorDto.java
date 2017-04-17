package net.kwatee.agiledeployment.repository.dto;

import java.util.Collection;

public class ServerPoolDescriptorDto {

	private String name;
	private Integer type;
	private Boolean mandatory;
	private String label;
	private String defaultValue;
	private Collection<String> options;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getType() {
		return this.type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public Boolean getMandatory() {
		return this.mandatory;
	}

	public void setMandatory(Boolean mandatory) {
		if (mandatory != null && mandatory.booleanValue())
			this.mandatory = mandatory;
	}

	public String getLabel() {
		return this.label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getDefaultValue() {
		return this.defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Collection<String> getOptions() {
		return this.options;
	}

	public void setOptions(Collection<String> options) {
		this.options = options;
	}

}
