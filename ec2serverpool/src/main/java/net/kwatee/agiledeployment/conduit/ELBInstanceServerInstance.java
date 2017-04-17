/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.conduit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;

import net.kwatee.agiledeployment.conduit.AbstractServerInstance;
import net.kwatee.agiledeployment.conduit.ServerInstanceException;
import net.kwatee.agiledeployment.conduit.ServerUtils;
import net.kwatee.agiledeployment.conduit.StatusWriter;

public class ELBInstanceServerInstance extends AbstractServerInstance {
	final private String instance_name;
	final private String ip_address;
	final private int port;

	final private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(this.getClass());

	ELBInstanceServerInstance(String instanceName, String ipAddress, int port) {
		this.instance_name = instanceName;
		this.ip_address = ipAddress;
		this.port = port;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.server.poolserver.PoolServerInstance#getInstanceName()
	 */
	public String getInstanceName() {
		return this.instance_name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.server.poolserver.PoolServerInstance#getIPAddress()
	 */
	@Override
	public String getIPAddress() {
		return this.ip_address == null ? super.getIPAddress() : this.ip_address;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.server.poolserver.PoolServerInstance#getPort()
	 */
	@Override
	public int getPort() {
		return this.port == 0 ? super.getPort() : this.port;
	}

	@Override
	public void preSetup(StatusWriter status) throws ServerInstanceException {
		log.debug("preSetup in");
		try {
			deregisterInstance(status);
		} catch (AmazonClientException e) {
			throw new ServerInstanceException(e.getMessage());
		}
		log.debug("preSetup out");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.server.poolserver.AbstractPoolServerInstance#postSetup()
	 */
	@Override
	public void postSetup(StatusWriter status) throws ServerInstanceException {
		log.debug("postSetup in");
		try {
			registerInstance(status);
		} catch (AmazonClientException e) {
			throw new ServerInstanceException(e.getMessage());
		}
		log.debug("postSetup out");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.server.poolserver.AbstractPoolServerInstance#preCleanup()
	 */
	@Override
	public void preCleanup(StatusWriter status) throws ServerInstanceException {
		log.debug("preCleanup in");
		try {
			deregisterInstance(status);
		} catch (AmazonClientException e) {
			throw new ServerInstanceException(e.getMessage());
		}
		log.debug("preCleanup out");
	}

	private void registerInstance(StatusWriter status) {
		Map<String, String> properties = ServerUtils.getServerProperties(getParent(), EC2ServerFactory.PROPERTY_PREFIX);
		String lbName = properties.get(EC2ServerFactory.PROPERTY_LOAD_BALANCER_NAME);
		AmazonElasticLoadBalancing elb = getELB(properties);
		List<Instance> list = new ArrayList<>(1);
		list.add(new Instance(instance_name));
		RegisterInstancesWithLoadBalancerRequest registerReq = new RegisterInstancesWithLoadBalancerRequest(lbName, list);
		elb.registerInstancesWithLoadBalancer(registerReq);
		status.statusMessage("Registered instance with load balancer");
	}

	private void deregisterInstance(StatusWriter status) {
		Map<String, String> properties = ServerUtils.getServerProperties(getParent(), EC2ServerFactory.PROPERTY_PREFIX);
		AmazonElasticLoadBalancing elb = getELB(properties);
		String lbName = properties.get(EC2ServerFactory.PROPERTY_LOAD_BALANCER_NAME);
		List<Instance> list = new ArrayList<>(1);
		list.add(new Instance(instance_name));
		DeregisterInstancesFromLoadBalancerRequest deregisterReq = new DeregisterInstancesFromLoadBalancerRequest(lbName, list);
		elb.deregisterInstancesFromLoadBalancer(deregisterReq);
		status.statusMessage("Deregistered instance from load balancer");
	}

	private AmazonElasticLoadBalancing getELB(Map<String, String> properties) {
		String accessKey = properties.get(EC2ServerFactory.PROPERTY_ACCESS_KEY);
		String secretKey = properties.get(EC2ServerFactory.PROPERTY_SECRET_KEY);
		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
		log.debug("getELB client");
		AmazonElasticLoadBalancing elb = new AmazonElasticLoadBalancingClient(credentials);
		log.debug("getELB client");
		return elb;
	}
}
