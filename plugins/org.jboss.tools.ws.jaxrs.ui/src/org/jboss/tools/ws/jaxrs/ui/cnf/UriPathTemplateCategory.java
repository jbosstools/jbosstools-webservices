/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/

package org.jboss.tools.ws.jaxrs.ui.cnf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsMetamodelLocator;
import org.jboss.tools.ws.jaxrs.ui.cnf.UriMappingsContentProvider.LoadingStub;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

public class UriPathTemplateCategory implements ITreeContentProvider {

	private final IJavaProject javaProject;

	private final UriMappingsContentProvider parent;

	private final Map<IJaxrsEndpoint, UriPathTemplateElement> wrapperCache = new HashMap<IJaxrsEndpoint, UriPathTemplateElement>();

	/**
	 * Constructor
	 * @param parent
	 * @param project
	 */
	public UriPathTemplateCategory(final UriMappingsContentProvider parent, final IProject project) {
		this(parent, JavaCore.create(project));
	}

	/**
	 * Constructor
	 * @param parent
	 * @param project
	 */
	public UriPathTemplateCategory(final UriMappingsContentProvider parent, final IJavaProject javaProject) {
		super();
		this.parent = parent;
		this.javaProject = javaProject;
	}

	/** {@inheritDoc} */
	@Override
	public Object[] getChildren(final Object parentElement) {
		try {
			final IJaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
			if (metamodel != null && !metamodel.isInitializing()) {
				// register for endpoint changes against the metamodel (in case it's not already done)
				metamodel.addJaxrsEndpointChangedListener(parent);
				
				final Collection<IJaxrsEndpoint> endpoints = metamodel.getAllEndpoints();
				Logger.debug("UriPathTemplateCategory contains {} endpoints", endpoints.size());
				List<UriPathTemplateElement> uriPathTemplateElements = new ArrayList<UriPathTemplateElement>();
				// Collections.sort(uriMappings);
				for (IJaxrsEndpoint endpoint : endpoints) {
					UriPathTemplateElement element = wrapperCache.get(endpoint);
					// LinkedList<IJaxrsResourceMethod> resourceMethods =
					// endpoint.getResourceMethods();
					if (element == null) {
						Logger.trace("Creating element for endpoint '{}' (was not found in wrapperCache)", endpoint);
						element = new UriPathTemplateElement(endpoint, this);
						wrapperCache.put(endpoint, element);
					}
					// after a clean build, the 'endpoint' reference should be
					// updated
					else if (element.getEndpoint().equals(endpoint)) {
						element.setEndpoint(endpoint);
					}
					uriPathTemplateElements.add(element);
				}
				return uriPathTemplateElements.toArray();
			} else if (metamodel == null || !metamodel.isInitializing()) {
				// immediately create a new metamodel to avoid entering this block twice (once for hasChildren() and once for getChildren())
				JaxrsMetamodelLocator.get(javaProject, true);
				launchLoadingMetamodelJob(this);
				// return a stub object that says loading...
				Logger.debug("Displaying the 'Loading...' stub for project '{}' and launching a build", javaProject.getElementName());
				return new Object[] { new LoadingStub(javaProject) };
			} else {
				// return a stub object that says loading while the metamodel is already initializing...
				Logger.debug("Just displaying the 'Loading...' stub for project '{}'", javaProject.getElementName());
				return new Object[] { new LoadingStub(javaProject) };
			}
		} catch (CoreException e) {
			Logger.error("Failed to retrieve JAX-RS Metamodel in project '" + javaProject.getElementName() + "'", e);
		}
		Logger.debug("*** There's no JAX-RS Metamodel for project '{}' -> no element to display ***", javaProject.getElementName());
		return new Object[0];
	}

	private void launchLoadingMetamodelJob(final UriPathTemplateCategory uriPathTemplateCategory) {
		final IProject project = uriPathTemplateCategory.getProject();
		Job job = new Job("Loading JAX-RS metamodel for project '" + javaProject.getElementName() + "'...") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Loading JAX-RS metamodel for project '" + javaProject.getElementName() + "'...",
						3);
				try {
					project.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));
					project.build(IncrementalProjectBuilder.FULL_BUILD, new SubProgressMonitor(monitor, 1));
					Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
				} catch (Exception e) {
					Logger.error("Failed to build project '" + javaProject.getElementName() + "'", e);
				}
				monitor.worked(1);
				refreshContent();
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.LONG);
		job.schedule();
	}
	
	@Override
	public Object getParent(Object element) {
		return javaProject.getProject();
	}

	/**
	 * @return the project
	 */
	public final IProject getProject() {
		return javaProject.getProject();
	}

	/**
	 * @return the Java project
	 */
	public final IJavaProject getJavaProject() {
		return javaProject;
	}
	
	@Override
	public boolean hasChildren(Object element) {
		try {
			final IJaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
			if (metamodel != null && !metamodel.isInitializing()) {
				final Collection<IJaxrsEndpoint> endpoints = metamodel.getAllEndpoints();
				Logger.debug("UriPathTemplateCategory contains {} endpoints", endpoints.size());
				return endpoints.size() > 0;
			}
		} catch (CoreException e) {
			Logger.error("Failed to retrieve JAX-RS Metamodel in project '" + javaProject.getElementName() + "'", e);
		}
		return true;
	}

	/**
	 * Returns the children elements for the given element
	 */
	@Override
	public Object[] getElements(final Object inputElement) {
		return getChildren(inputElement);
	}

	
	/**
	 * Returns the {@link UriPathTemplateElement} noe associated with the given {@link IJaxrsElement}
	 * @param endpoint the endpoint in the {@link IJaxrsMetamodel}
	 * @return its associated {@link UriPathTemplateElement} in the viewer
	 */
	public UriPathTemplateElement getUriPathTemplateElement(final IJaxrsEndpoint endpoint) {
		return wrapperCache.get(endpoint);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		Logger.debug("Input changed in UriPathTemplateCategory");
	}

	public int getProblemLevel() {
		int level = 0;
		try {
			final IJaxrsMetamodel metamodel= JaxrsMetamodelLocator.get(javaProject);
			if (metamodel != null) {
				level = metamodel.getMarkerSeverity();
				for (IJaxrsEndpoint endpoint : metamodel.getAllEndpoints()) {
					level = Math.max(level, endpoint.getProblemLevel());
				}
			}
		} catch (CoreException e) {
			Logger.error("Failed to determine the problem severity for the JAX-RS Web Services", e);
		}
		return level;
	}

	@Override
	public void dispose() {
	}

	/**
	 * Triggers a Refresh for all elements in this tree and ensures that the parent {@link UriMappingsContentProvider}
	 * is registered against the {@link IJaxrsMetamodel} to be notified of further changes
	 */
	public void refreshContent() {
		try {
			final IJaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
			// let's make sure this listener is registered
			if (metamodel != null) {
				// metamodel.addListener() avoids duplicate entries
				metamodel.addJaxrsEndpointChangedListener(this.parent);
			}
		} catch (CoreException e) {
			Logger.error("Failed to retrieve the JAX-RS Metamodel for project '" + javaProject.getElementName() + "'", e);
		}
		parent.refreshContent(javaProject.getProject());
	}

	public Object[] getChildren() {
		return getChildren(this);
	}
	
	@Override
	public String toString() {
		return "UriPathTemplateCategory on project '" + javaProject.getElementName() + "'";
	}

}
