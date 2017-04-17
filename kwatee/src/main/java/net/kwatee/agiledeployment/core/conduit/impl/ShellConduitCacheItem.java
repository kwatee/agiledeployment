/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.conduit.impl;

class ShellConduitCacheItem {

	final private AbstractShellConduit conduit;
	final private String ref;
	private long timeStamp;
	private int refCount;
	private boolean isNew;

	ShellConduitCacheItem(AbstractShellConduit conduit, String ref) {
		this.conduit = conduit;
		this.ref = ref;
		this.refCount = 0;
		this.isNew = true;
	}

	String getRef() {
		return this.ref;
	}

	long getTimeStamp() {
		return this.timeStamp;
	}

	void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	int getRefCount() {
		return this.refCount;
	}

	void incRefCount() {
		this.refCount++;
	}

	void decRefCount() {
		this.refCount--;
	}

	boolean isNew() {
		return this.isNew;
	}

	void setNew(boolean isNew) {
		this.isNew = isNew;
	}

	AbstractShellConduit getConduit() {
		return this.conduit;
	}
};
