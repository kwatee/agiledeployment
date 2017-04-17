/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.entity;

import javax.persistence.Embeddable;
import javax.persistence.Table;

@SuppressWarnings("serial")
@Table(name = "KWServerProperty")
@Embeddable
public class ServerProperty implements java.io.Serializable {

	private String name;
	private String value;

	public ServerProperty() {}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public boolean equals(Object that) {
		return that != null && this.getName().equals(((ServerProperty) that).getName());
	}
}
