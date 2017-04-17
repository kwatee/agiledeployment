package net.kwatee.agiledeployment.core.deploy.task;

import net.kwatee.agiledeployment.core.conduit.ConduitService;
import net.kwatee.agiledeployment.core.conduit.InstanceService;
import net.kwatee.agiledeployment.core.deploy.DeployService;
import net.kwatee.agiledeployment.core.deploy.packager.PackagerService;
import net.kwatee.agiledeployment.core.variable.VariableService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * @author kwatee
 * 
 */
@Service
public class DeployTaskServices {

	@Autowired
	private VariableService variableService;
	@Autowired
	private DeployService deployService;
	@Autowired
	private PackagerService packagerService;
	@Autowired
	private ConduitService conduitService;
	@Autowired
	private InstanceService instanceService;

	static private DeployTaskServices instance;

	public DeployTaskServices() {
		instance = this;
	}

	static public VariableService getVariableService() {
		return instance.variableService;
	}

	static public DeployService getDeployService() {
		return instance.deployService;
	}

	static public PackagerService getPackagerService() {
		return instance.packagerService;
	}

	static public ConduitService getConduitService() {
		return instance.conduitService;
	}

	static public InstanceService getInstanceService() {
		return instance.instanceService;
	}
}
