/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

import java.util.List;

@SuppressWarnings("serial")
public class MissingVariableException extends KwateeException {

	public class MissingVariableInfo {

		public String variableName;
		public String serverArtifact;
		public String fileName;

		MissingVariableInfo(String variableName, String serverArtifact, String fileName) {
			this.variableName = variableName;
			this.serverArtifact = serverArtifact;
			this.fileName = fileName;
		}

		public String getServerArtifact() {
			return this.serverArtifact;
		}

		public String getFileName() {
			return this.fileName;
		}

		public String toString() {
			String s = this.serverArtifact;
			if (this.fileName != null) {
				s += " - " + this.fileName;
			}
			return s;
		}
	}

	private List<MissingVariableInfo> missing_list = new java.util.ArrayList<MissingVariableInfo>(0);

	public MissingVariableException() {
		super("Undefined variable(s)");
	}

	public MissingVariableException(String variableName, String serverArtifact, String fileName) {
		this();
		addMissingVariable(variableName, serverArtifact, fileName);
	}

	public void addMissingVariable(String variableName, String serverArtifact, String fileName) {
		this.missing_list.add(new MissingVariableInfo(variableName, serverArtifact, fileName));
	}

	public void addMissingVariable(MissingVariableException e, String comment) {
		for (MissingVariableInfo v : e.getMissing()) {
			if (comment != null) {
				v.fileName = comment;
			}
			this.missing_list.add(v);
		}
	}

	public List<MissingVariableInfo> getMissing() {
		return this.missing_list;
	}
}
