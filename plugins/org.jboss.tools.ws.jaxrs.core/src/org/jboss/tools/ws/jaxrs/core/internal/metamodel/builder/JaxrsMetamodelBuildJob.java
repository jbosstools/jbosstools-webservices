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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsMetamodelBuilder.SCALE;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;

/** @author xcoulon */
public class JaxrsMetamodelBuildJob extends Job {

	private final JavaElementChangedProcessor javaElementChangedProcessor = new JavaElementChangedProcessor();

	private final JaxrsElementChangedProcessor jaxrsElementChangedProcessor = new JaxrsElementChangedProcessor();

	private final ElementChangedEvent event;

	public JaxrsMetamodelBuildJob(final ElementChangedEvent event) {
		super("Incremental JAX-RS Metamodel build..."); //$NON-NLS-1$
		this.event = event;
		Logger.debug("Initiating an incremental JAX-RS Metamodel build after " + event); //$NON-NLS-1$
	}

	@Override
	protected IStatus run(final IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("Building JAX-RS Metamodel", 4 * SCALE);
			progressMonitor.worked(SCALE);
			Logger.debug("Building JAX-RS Metamodel...");
			if (progressMonitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			// scan and filter delta, retrieve a list of java changes
			final List<JavaElementChangedEvent> events = new JavaElementChangedEventScanner().scanAndFilterEvent(event,
					new SubProgressMonitor(progressMonitor, SCALE));
			if (progressMonitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			final List<JaxrsElementChangedEvent> jaxrsElementChanges = javaElementChangedProcessor.processEvents(
					events, new SubProgressMonitor(progressMonitor, SCALE));
			if (progressMonitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			final List<JaxrsEndpointChangedEvent> jaxrsEndpointChanges = jaxrsElementChangedProcessor.processEvents(
					jaxrsElementChanges, new SubProgressMonitor(progressMonitor, SCALE));
			if (progressMonitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			new JaxrsElementChangedPublisher().publish(jaxrsEndpointChanges, new SubProgressMonitor(progressMonitor,
					SCALE));
			
		} catch (Throwable e) {
			Logger.error("Failed to build or refresh the JAX-RS metamodel", e);
		} finally {
			progressMonitor.done();
		}
		return Status.OK_STATUS;
	}
}
