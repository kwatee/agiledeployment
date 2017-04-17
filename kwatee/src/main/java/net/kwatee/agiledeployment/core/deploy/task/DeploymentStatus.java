/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.deploy.task;

import java.util.ArrayList;
import java.util.List;

public class DeploymentStatus {

	final static public int OP_DEPLOY = 1;
	final static public int OP_UNDEPLOY = 2;
	final static public int OP_START = 3;
	final static public int OP_STOP = 4;
	final static public int OP_CHECK = 5;
	final static public int OP_STATUS = 6;
	final static public int COMPLETION_INPROGRESS = 0;
	final static public int COMPLETION_CANCELING = 1;
	final static public int COMPLETION_OK = 2;
	final static public int COMPLETION_INTERRUPTED = 3;

	private String batchRef;
	private int op;
	private int completionType;
	final private ArrayList<DeployStatus> statusList = new ArrayList<>();

	public String getBatchRef() {
		return this.batchRef;
	}

	public void setDeploymentOperationRef(String batchRef) {
		this.batchRef = batchRef;
	}

	public int getOperation() {
		return this.op;
	}

	public void setOperation(int op) {
		this.op = op;
	}

	public int getCompletionType() {
		return this.completionType;
	}

	public void setCompletionType(int completionType) {
		this.completionType = completionType;
	}

	public List<DeployStatus> getStatusList() {
		return this.statusList;
	}
}
