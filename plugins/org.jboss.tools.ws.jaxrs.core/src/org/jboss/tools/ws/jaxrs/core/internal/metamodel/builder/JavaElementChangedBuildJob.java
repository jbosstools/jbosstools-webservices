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

import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsMetamodelLocator;

/** @author xcoulon */
public class JavaElementChangedBuildJob extends Job {

	private final ElementChangedEvent event;
	
	public JavaElementChangedBuildJob(final ElementChangedEvent event) {
		super("Incremental JAX-RS Metamodel build..."); //$NON-NLS-1$
		this.event = event;
		Logger.debug("Initiating an incremental JAX-RS Metamodel build after " + event); //$NON-NLS-1$
	}
	
	public void execute() {
		run(new NullProgressMonitor());
	}
	
	@Override
	protected IStatus run(final IProgressMonitor progressMonitor) {
		final long startTime = new Date().getTime();
		IJavaElement element = null;
		try {
			progressMonitor.beginTask("Building JAX-RS Metamodel", 3 * SCALE);
			progressMonitor.worked(SCALE);
			Logger.debug("Building JAX-RS Metamodel after Java element changed...");
			if (progressMonitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			// scan and filter delta, retrieve a list of java changes
			final List<JavaElementDelta> affectedJavaElements = new JavaElementDeltaScanner().scanAndFilterEvent(event,
					new SubProgressMonitor(progressMonitor, SCALE));
			if(affectedJavaElements.isEmpty()) {
				Logger.debug("No relevant affected element to process");
				return Status.OK_STATUS;
			}
			if (progressMonitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			for(JavaElementDelta delta : affectedJavaElements) {
				element = delta.getElement();
				final IJavaProject javaProject = element.getJavaProject();
				final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
				// prevent NPE when opening a closed project (ie, there's no metamodel yet).
				if(metamodel != null) {
					try {
						metamodel.processJavaElementChange(delta, progressMonitor);
						if (progressMonitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
					} catch(Throwable e) {
						final IStatus status = Logger.error("Failed to build or refresh the JAX-RS metamodel", e);
						metamodel.setBuildStatus(status);
						return status;
					} finally {
						if(Logger.isDebugEnabled()) {
							Logger.debug(metamodel.getStatus());
						}
					}
				}
			}
		} catch (Throwable e) {
			if(element != null) {
				return Logger.error("Failed to build or refresh the JAX-RS metamodel while processing " + element.getElementName() + " in project " + element.getAncestor(IJavaElement.JAVA_PROJECT).getElementName(), e);
			} else {
				return Logger.error("Failed to build or refresh the JAX-RS metamodel while processing a change in the Java Model", e);
			}
		} finally {
			progressMonitor.done();
			long endTime = new Date().getTime();
			if (Logger.isDebugEnabled()) {
				Logger.debug("Java element changes processed in {} ms.", (endTime - startTime));
			}
			
		}
		return Status.OK_STATUS;
	}
}
