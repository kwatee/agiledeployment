/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("serial")
@Entity(name = "KWSystemProperty")
@Table(name = "KWSystemProperty")
public class SystemProperty implements java.io.Serializable {

	@Id
	private String name;
	private String value;
	private String description;
	private short pos;

	public SystemProperty() {}

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
		this.value = value.length() <= 512 ? value : value.substring(0, 512);
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public short getPos() {
		return this.pos;
	}

	public void setPos(short pos) {
		this.pos = pos;
	}

	public boolean isHidden() {
		return StringUtils.isEmpty(this.description);
	}

	@Override
	public String toString() {
		return this.name + '=' + this.value;
	}

	@Override
	public boolean equals(Object that) {
		return that != null && this.name.equals(((SystemProperty) that).name);
	}
}
