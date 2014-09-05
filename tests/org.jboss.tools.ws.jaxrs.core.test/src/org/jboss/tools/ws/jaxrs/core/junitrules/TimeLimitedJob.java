/******************************************************************************* 
 * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.core.junitrules;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jboss.tools.ws.jaxrs.core.internal.utils.TestLogger;

/**
 * @author xcoulon
 *
 */
public class TimeLimitedJob extends Job {

	
	/**
	 * @param name
	 */
	public TimeLimitedJob() {
		super("Waiting for other jobs...");
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		// do nothing
		return Status.OK_STATUS;
	}
	
	/**
	 * Schedules the job with a timeout
	 * @param timeout the timeout (in seconds)
	 */
	public void scheduleWithTimeout(final long timeout) {
		final Job killJob = new Job("Killing the wait job...") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if(TimeLimitedJob.this.getState() != Job.NONE) {
					TestLogger.error("Cancelling wait job after a timeout of {}s", null, timeout);
					TimeLimitedJob.this.cancel();
				}
				return Status.OK_STATUS;
			}
		};
		killJob.schedule(timeout * 1000);
		super.schedule();
	}
	
	

}
