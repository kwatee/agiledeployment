/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.deploy.task;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class ServerPoolMonitor {

	final private int concurrency;
	final private Lock lock;
	final private Condition canStart;
	private boolean firstUse;
	private Semaphore sem;

	ServerPoolMonitor(final int concurrency) {
		this.concurrency = concurrency;
		this.firstUse = true;
		this.lock = new ReentrantLock();
		this.canStart = this.lock.newCondition();
	}

	boolean isFirstUse() {
		return this.firstUse;
	}

	void canStart() {
		this.firstUse = false;
		this.lock.lock();

		try {
			this.sem = new Semaphore(this.concurrency);
			this.canStart.signalAll();
		} finally {
			this.lock.unlock();
		}
	}

	void acquire() throws InterruptedException {
		this.lock.lock();

		try {
			if (this.sem == null) {
				this.canStart.await();
			}
		} finally {
			this.lock.unlock();
		}
		sem.acquire();
	}

	void release() {
		this.sem.release();
	}

	boolean isEmpty() {
		return !this.sem.hasQueuedThreads();
	}

	int availablePermits() {
		return this.sem.availablePermits();
	}
}
