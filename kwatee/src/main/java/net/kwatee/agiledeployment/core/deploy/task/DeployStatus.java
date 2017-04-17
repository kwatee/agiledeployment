/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.deploy.task;

import org.apache.commons.lang3.StringUtils;

public class DeployStatus implements java.lang.Cloneable {

	public static final int STATUS_OK = 0;
	public static final int STATUS_FAILED = 1;
	public static final int STATUS_DISABLED = 2;
	public static final int STATUS_UNDETERMINED = 3;
	public static final int STATUS_CANCELED = 4;
	public static final int STATUS_RUNNING = 5;
	public static final int STATUS_STOPPED = 6;
	public static final int STATUS_INPROGRESS = 7;
	public static final int STATUS_PENDING = 8;

	private String level1Ref = StringUtils.EMPTY;
	private String level2Ref;
	private String messages = StringUtils.EMPTY;
	private int status = STATUS_PENDING;

	public DeployStatus() {}

	public void setReference(String ref) {
		this.level1Ref = ref;
	}

	public void setReference(String ref1, String ref2) {
		this.level1Ref = ref1;
		this.level2Ref = ref2;
	}

	public String getReference1() {
		return this.level1Ref;
	}

	public String getReference2() {
		return this.level2Ref;
	}

	public boolean matchesReferences(String ref1, String ref2) {
		return ref1.equals(this.level1Ref) && ((ref2 == null && this.level2Ref == null) || (ref2 != null && ref2.equals(this.level2Ref)));
	}

	public String getDisplayName() {
		if (this.level2Ref == null) {
			return this.level1Ref;
		}
		return this.level2Ref + '@' + this.level1Ref;
	}

	public String getMessages() {
		return this.messages;
	}

	public void setMessage(String message) {
		this.messages = message;
	}

	public void appendMessage(String message) {
		if (this.messages.length() > 0) {
			this.messages += '\n';
		}
		this.messages += message;
	}

	public void overwriteLastMessage(String message) {
		int idx = this.messages.lastIndexOf("\n");
		if (idx < 0) {
			this.messages = message;
		} else {
			this.messages = this.messages.substring(0, idx + 1) + message;
		}
	}

	public int getStatus() {
		return this.status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public Object clone() {
		DeployStatus s = new DeployStatus();
		s.level1Ref = new String(this.level1Ref);
		if (this.level2Ref != null) {
			s.level2Ref = new String(this.level2Ref);
		}
		s.messages = this.messages == null ? null : (new String(this.messages));
		s.status = this.status;
		return s;
	}
}
