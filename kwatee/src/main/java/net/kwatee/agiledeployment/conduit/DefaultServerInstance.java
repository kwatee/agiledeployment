/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.conduit;

public class DefaultServerInstance extends AbstractServerInstance {

	final private String instanceName;
	final private String ipAddress;
	final private int port;

	public DefaultServerInstance(String instanceName, String ipAddress, int port) {
		this.instanceName = instanceName;
		this.ipAddress = ipAddress;
		this.port = port;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.conduit.ServerInstance#getInstanceName()
	 */
	@Override
	public String getInstanceName() {
		return this.instanceName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.conduit.ServerInstance#getIPAddress()
	 */
	@Override
	public String getIPAddress() {
		return this.ipAddress == null ? super.getIPAddress() : this.ipAddress;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.conduit.ServerInstance#getPort()
	 */
	@Override
	public int getPort() {
		return this.port == 0 ? super.getPort() : this.port;
	}
}
