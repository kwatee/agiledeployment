/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.conduit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeRegionsResult;
import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;

import net.kwatee.agiledeployment.conduit.ELBInstanceServerInstance;
import net.kwatee.agiledeployment.conduit.DefaultServerInstance;
import net.kwatee.agiledeployment.conduit.ServerInstance;
import net.kwatee.agiledeployment.conduit.ServerInstanceException;
import net.kwatee.agiledeployment.conduit.ServerInstanceFactory;

public class EC2ServerFactory implements ServerInstanceFactory {

	final protected static String PROPERTY_PREFIX = "ec2.";
	final protected static String PROPERTY_REGION = "ec2.region";
	final protected static String PROPERTY_ACCESS_KEY = "ec2.access-key";
	final protected static String PROPERTY_SECRET_KEY = "ec2.secret-key";
	final protected static String PROPERTY_LOAD_BALANCER_NAME = "ec2.elb-name";
	final protected static String PROPERTY_AMI_ID = "ec2.ami-id";
	final protected static String PROPERTY_SECURITY_GROUP = "ec2.security-group";
	final protected static String PROPERTY_USE_INTERNAL_IP = "ec2.internal";

	final private static String REGION_ELEMENT_LIST = "regions";

	final private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EC2ServerFactory.class);

	static public String getId() {
		return "ec2";
	}

	static public String getDisplayName() {
		return "Amazon EC2 pool";
	}

	/**
	 * 
	 * @return a list of PropertyDescriptor objects
	 */
	static public List<PropertyDescriptor> getPropertyDescriptors() {
		log.debug("getPropertyDescriptors in");
		ArrayList<PropertyDescriptor> descriptors = new ArrayList<>();
		descriptors.add(new PropertyDescriptor(PROPERTY_REGION, "Region", true));
		descriptors.add(new PropertyDescriptor(PROPERTY_ACCESS_KEY, "Access Key", true));
		descriptors.add(new PropertyDescriptor(PROPERTY_SECRET_KEY, "Secret Key", true));
		descriptors.add(new PropertyDescriptor(PROPERTY_LOAD_BALANCER_NAME, "Load balancer"));
		descriptors.add(new PropertyDescriptor(PROPERTY_AMI_ID, "AMI"));
		descriptors.add(new PropertyDescriptor(PROPERTY_SECURITY_GROUP, "Security Group"));
		descriptors.add(new PropertyDescriptor(PROPERTY_USE_INTERNAL_IP, "Use Internal IP", false, PropertyDescriptor.TYPE.BOOLEAN));
		log.debug("getPropertyDescriptors out");
		return descriptors;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.conduit.ServerInstanceFactory#getServerPool(Object)
	 */
	public Collection<ServerInstance> getServerPool(Map<String, String> ec2Properties) throws ServerInstanceException {
		log.debug("getServerPool in");
		Map<String, String> nameAndAddresses = getUninstantiatedServers(ec2Properties);
		Collection<ServerInstance> servers = new ArrayList<>(nameAndAddresses.size());
		boolean isELBInstance = ec2Properties.get(PROPERTY_LOAD_BALANCER_NAME) != null;
		for (Map.Entry<String, String> entry : nameAndAddresses.entrySet()) {
			ServerInstance s;
			if (isELBInstance)
				s = new ELBInstanceServerInstance(entry.getKey(), entry.getValue(), 0);
			else
				s = new DefaultServerInstance(entry.getKey(), entry.getValue(), 0);
			servers.add(s);
		}
		log.debug("getServerPool out");
		return servers;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.conduit.ServerInstanceFactory#getUninstantiatedPoolServers(java.util.Map)
	 */
	public Map<String, String> getUninstantiatedServers(Map<String, String> properties) throws ServerInstanceException {
		log.debug("getUninstantiatedServers in");
		Map<String, String> nameAndAddresses = new HashMap<>();
		String region = properties.get(PROPERTY_REGION);
		String accessKey = properties.get(PROPERTY_ACCESS_KEY);
		String secretKey = properties.get(PROPERTY_SECRET_KEY);
		String elbName = properties.get(PROPERTY_LOAD_BALANCER_NAME);
		String amiId = properties.get(PROPERTY_AMI_ID);
		String securityGroup = properties.get(PROPERTY_SECURITY_GROUP);
		boolean usePrivateAddr = Boolean.parseBoolean(properties.get(PROPERTY_USE_INTERNAL_IP));
		try {
			AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
			log.debug("getUninstantiatedServers credentials");
			AmazonEC2 ec2 = new AmazonEC2Client(credentials);
			log.debug("getUninstantiatedServers client");
			if (region != null)
				ec2.setEndpoint(region);
			DescribeInstancesResult describeInstances = ec2.describeInstances();
			List<Reservation> reservations = describeInstances.getReservations();
			Set<Instance> allInstances = new HashSet<Instance>();
			for (Reservation reservation : reservations) {
				allInstances.addAll(reservation.getInstances());
			}
			if (elbName != null) {
				AmazonElasticLoadBalancing elb = new AmazonElasticLoadBalancingClient(credentials);
				DescribeLoadBalancersResult describeLB = elb.describeLoadBalancers();
				for (LoadBalancerDescription lbDescription : describeLB.getLoadBalancerDescriptions()) {
					log.debug("getUninstantiatedServers elb " + lbDescription.getLoadBalancerName());
					if (lbDescription.getLoadBalancerName().equals(elbName)) {
						for (com.amazonaws.services.elasticloadbalancing.model.Instance instance : lbDescription.getInstances()) {
							Instance i = getEC2Instance(allInstances, instance.getInstanceId());
							if (i != null)
								nameAndAddresses.put(i.getInstanceId(), usePrivateAddr ? i.getPrivateDnsName() : i.getPublicDnsName());
						}
						break;
					}
				}
			} else if (securityGroup != null) {
				for (Instance instance : allInstances) {
					if (amiId != null) {
						if (instance.getImageId().equals(amiId))
							nameAndAddresses.put(instance.getInstanceId(), usePrivateAddr ? instance.getPrivateDnsName() : instance.getPublicDnsName());
					} else {
						for (GroupIdentifier group : instance.getSecurityGroups()) {
							log.debug("getUninstantiatedServers group " + group.getGroupName() + ":" + group.getGroupId());
							if (securityGroup.equals(group.getGroupName()))
								nameAndAddresses.put(instance.getInstanceId(), usePrivateAddr ? instance.getPrivateDnsName() : instance.getPublicDnsName());
						}
					}
				}
			}
		} catch (AmazonClientException e) {
			throw new ServerInstanceException(e);
		}
		log.debug("getUninstantiatedServers out");
		return nameAndAddresses;
	}

	/**
	 * @param instances
	 * @param instanceId
	 * @return
	 */
	private Instance getEC2Instance(Collection<Instance> instances, String instanceId) {
		for (Instance instance : instances) {
			if (instance.getInstanceId().equals(instanceId))
				return instance;
		}
		return null;
	}

	public Map<String, String> getElementList(String listType, Map<String, String> properties) throws ServerInstanceException {
		log.debug("getElementList in");
		if (REGION_ELEMENT_LIST.equals(listType)) {
			Map<String, String> regions = new HashMap<>();
			String accessKey = properties.get(PROPERTY_ACCESS_KEY);
			String secretKey = properties.get(PROPERTY_SECRET_KEY);
			try {
				AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
				log.debug("getElementList credentials");
				AmazonEC2 ec2 = new AmazonEC2Client(credentials);
				log.debug("getElementList client");
				DescribeRegionsResult awsRegions = ec2.describeRegions();
				for (Region r : awsRegions.getRegions()) {
					regions.put(r.getRegionName(), r.getEndpoint());
				}
				log.debug("getElementList out");
				return regions;
			} catch (Throwable e) {
				throw new ServerInstanceException(e);
			}
		}
		log.debug("getElementList out");
		return null;
	}
}
