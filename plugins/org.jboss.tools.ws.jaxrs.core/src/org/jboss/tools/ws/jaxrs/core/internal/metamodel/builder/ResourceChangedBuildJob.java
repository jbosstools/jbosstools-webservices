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

import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsMetamodelBuilder.SCALE;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsMetamodelLocator;

/** @author xcoulon */
public class ResourceChangedBuildJob extends Job {

	private final IResourceChangeEvent event;

	private final IProject project;
	
	public ResourceChangedBuildJob(final IProject project, final IResourceChangeEvent event) {
		super("Incremental JAX-RS Metamodel build..."); //$NON-NLS-1$
		this.event = event;
		this.project = project;
		Logger.debug("Initiating an incremental JAX-RS Metamodel build after " + event); //$NON-NLS-1$
	}

	@Override
	protected IStatus run(final IProgressMonitor progressMonitor) {
		final long startTime = new Date().getTime();
		try {
			progressMonitor.beginTask("Building JAX-RS Metamodel", 3 * SCALE);
			Logger.debug("Building JAX-RS Metamodel after resource changed...");
			// extract the relevant delta bound to this built (some resources or entire project)
			final List<ResourceDelta> affectedResources = extractAffectedResources(event.getDelta(), progressMonitor);
			progressMonitor.worked(SCALE);
			if (progressMonitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			} 
			// compute changes on the JAX-RS Application(s), HttpMethods, Resources, etc.
			final IJavaProject javaProject = JavaCore.create(project);
			JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
			try {
				if (metamodel == null) {
					metamodel = JaxrsMetamodelLocator.get(javaProject, true);
					if(metamodel != null) {
						metamodel.processProject(progressMonitor);
					}
				} else if (event.getBuildKind() == IncrementalProjectBuilder.FULL_BUILD
						|| event.getBuildKind() == IncrementalProjectBuilder.CLEAN_BUILD) {
					metamodel.processProject(progressMonitor);
				} else {
					metamodel.processAffectedResources(affectedResources, progressMonitor);
				}
			} catch(CoreException e) {
				final IStatus status = Logger.error("Failed to (re)build the JAX-RS metamodel for projet " + project.getName(), e);
				if(metamodel != null) {
					metamodel.setBuildStatus(status);
				}
				return status;
			}
			return Status.OK_STATUS;
		} catch (Throwable e) {
			return Logger.error("Failed to (re)build the JAX-RS metamodel for projet " + project.getName(), e);
		} finally {
			long endTime = new Date().getTime();
			if (Logger.isDebugEnabled()) {
				Logger.debug("JAX-RS Metamodel for project '{}' built in {} ms.", project.getName(), (endTime - startTime));
				try {
					final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(project);
					if(metamodel != null) {
						Logger.debug(
								"JAX-RS Metamodel for project '{}' now has {} HttpMethods, {} Resources and {} Endpoints.",
								project.getName(), metamodel.findAllHttpMethods().size(),
								metamodel.getAllResources().size(), metamodel.getAllEndpoints().size());
					}
				} catch (Throwable e) {
					// debug level here since the purpose was to display a debug message
					Logger.debug("Error occurred: {}", e);
				}
			}
			progressMonitor.done();
		}
	}

	private List<ResourceDelta> extractAffectedResources(final IResourceDelta delta, final IProgressMonitor progressMonitor)
			throws CoreException {
		final List<ResourceDelta> events = new ArrayList<ResourceDelta>();
		if (delta != null) {
			events.addAll(new ResourceDeltaScanner().scanAndFilterEvent(delta, new SubProgressMonitor(progressMonitor,
					SCALE)));
		} else {
			events.add(new ResourceDelta(project, CHANGED, 0));
		}
		return events;
	}
}
