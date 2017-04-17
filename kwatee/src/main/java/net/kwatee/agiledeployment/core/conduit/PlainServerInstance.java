/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.conduit;

import net.kwatee.agiledeployment.conduit.AbstractServerInstance;

public class PlainServerInstance extends AbstractServerInstance {

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.ServerInstance#getInstanceName()
	 */
	public String getInstanceName() {
		return getParent().getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.ServerInstance#getIPAddress()
	 */
	public String getIPAddress() {
		return getParent().getIPAddress();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.ServerInstance#getPort()
	 */
	public int getPort() {
		return getParent().getPort();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.AbstractServerInstance#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}
}
