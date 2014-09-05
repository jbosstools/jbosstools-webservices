/******************************************************************************* 
 * Copyright (c) 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.core.internal.utils;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

/**
 * Job Monitor. Logs jobs change events.
 * @author xcoulon
 *
 */
public class JobMonitor extends JobChangeAdapter {
	
	@Override
	public void sleeping(IJobChangeEvent event) {
		Logger.traceJobs("*** Job #{} sleeping", getJobId(event.getJob()));
	}
	
	@Override
	public void scheduled(IJobChangeEvent event) {
		Logger.traceJobs("*** Job #{} scheduled with priority {}", getJobId(event.getJob()),
				ConstantUtils.getStaticFieldName(Job.class, event.getJob().getPriority()));
	}
	
	@Override
	public void running(IJobChangeEvent event) {
		Logger.traceJobs("*** Job #{} running in thread {} with priority {}", getJobId(event.getJob()), event.getJob()
				.getThread().getName(),
				ConstantUtils.getStaticFieldName(Job.class, event.getJob().getPriority()));
	}
	
	@Override
	public void done(IJobChangeEvent event) {
		Logger.traceJobs("*** Job #{} done", getJobId(event.getJob()));
	}
	
	@Override
	public void aboutToRun(IJobChangeEvent event) {
		Logger.traceJobs("*** Job #{} about to run", getJobId(event.getJob()));
	}
	
	public static String getJobId(final Job job) {
		final String label = job.toString();
		return label.substring(label.lastIndexOf('(') + 1, label.length() - 1);
	}
}