/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.entity;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("serial")
@Entity(name = "KWExecutable")
@Table(name = "KWExecutable")
public class Executable implements java.io.Serializable, Comparable<Executable> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id = null;
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "version_id", nullable = false)
	private Version version;
	private String name;
	private String description = StringUtils.EMPTY;
	private String start_action = StringUtils.EMPTY;
	private String stop_action = StringUtils.EMPTY;
	private String status_action = StringUtils.EMPTY;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Version getVersion() {
		return this.version;
	}

	public void setVersion(Version version) {
		this.version = version;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getStartAction() {
		return this.start_action;
	}

	public void setStartAction(String action) {
		this.start_action = action;
	}

	public String getStopAction() {
		return this.stop_action;
	}

	public void setStopAction(String action) {
		this.stop_action = action;
	}

	public String getStatusAction() {
		return this.status_action;
	}

	public void setStatusAction(String action) {
		this.status_action = action;
	}

	public Executable duplicate() {
		Executable duplicateExecutable = new Executable();
		duplicateExecutable.setName(this.name);
		duplicateExecutable.setDescription(this.description);
		duplicateExecutable.setStartAction(this.start_action);
		duplicateExecutable.setStopAction(this.stop_action);
		duplicateExecutable.setStatusAction(this.status_action);
		return duplicateExecutable;
	}

	public void lazyLoad() {
		this.start_action.length();
		this.stop_action.length();
		this.status_action.length();
	}

	@Override
	public String toString() {
		return this.version.toString() + ':' + this.name;
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public boolean equals(Object that) {
		return that != null && this.name.equals(((Executable) that).name);
	}

	public int compareTo(Executable that) {
		return this.name.compareToIgnoreCase(that.name);
	}
}
