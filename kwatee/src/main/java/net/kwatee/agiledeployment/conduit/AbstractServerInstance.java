/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.conduit;

public abstract class AbstractServerInstance implements ServerInstance {

	private ConduitServer baseServer;

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.conduit.ServerInstance#getParent()
	 */
	/*
	 */
	@Override
	final public ConduitServer getParent() {
		return this.baseServer;
	}

	@Override
	final public void setParent(ConduitServer parent) {
		this.baseServer = parent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.conduit.ServerInstance#getName()
	 */
	@Override
	final public String getName() {
		return getParent().getName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.conduit.ServerInstance#getConduitType()
	 */
	@Override
	public String getConduitType() {
		return getParent().getConduitType();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.conduit.ServerInstance#getPlatform()
	 */
	@Override
	public Integer getPlatform() {
		return getParent().getPlatform();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.kwatee.agiledeployment.conduit.ServerInstance#getProperty(java.lang
	 * .String)
	 */
	@Override
	public String getProperty(String name) {
		if (SERVER_NAME.equals(name)) {
			return getName();
		}
		if (SERVER_IP.equals(name)) {
			return getIPAddress();
		}
		if (SERVER_INSTANCE.equals(name)) {
			return getInstanceName();
		}
		if (getParent().getServerProperties() != null
				&& !name.startsWith("kwatee_")) {
			return getParent().getServerProperties().get(name);
		}
		return null;
	}

	@Override
	public String getIPAddress() {
		return getParent().getIPAddress();
	}

	@Override
	public int getPort() {
		return getParent().getPort();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.kwatee.agiledeployment.conduit.ServerInstance#preSetup(net.kwatee
	 * .agiledeployment.conduit.StatusWriter)
	 */
	@Override
	public void preSetup(StatusWriter status) throws ServerInstanceException {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.kwatee.agiledeployment.conduit.ServerInstance#postSetup(net.kwatee
	 * .agiledeployment.conduit.StatusWriter)
	 */
	@Override
	public void postSetup(StatusWriter status) throws ServerInstanceException {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.kwatee.agiledeployment.conduit.ServerInstance#preCleanup(net.kwatee
	 * .agiledeployment.conduit.StatusWriter)
	 */
	@Override
	public void preCleanup(StatusWriter status) throws ServerInstanceException {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.kwatee.agiledeployment.conduit.ServerInstance#postCleanup(net.kwatee
	 * .agiledeployment.conduit.StatusWriter)
	 */
	@Override
	public void postCleanup(StatusWriter status) throws ServerInstanceException {
	}

	@Override
	public String toString() {
		return getParent().toString() + ':' + getInstanceName();
	}
}
