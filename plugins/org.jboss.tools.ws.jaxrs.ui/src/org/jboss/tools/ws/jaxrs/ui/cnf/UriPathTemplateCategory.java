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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JavaElementChangedBuildJob;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JavaElementDelta;
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
				final Collection<IJaxrsEndpoint> endpoints = metamodel.getAllEndpoints();
				Logger.debug("UriPathTemplateCategory contains {} endpoints", endpoints.size());
				final List<UriPathTemplateElement> uriPathTemplateElements = new ArrayList<UriPathTemplateElement>();
				// Collections.sort(uriMappings);
				for (IJaxrsEndpoint endpoint : endpoints) {
					final UriPathTemplateElement element = getUriPathTemplateElement(endpoint);
					uriPathTemplateElements.add(element);
				}
				return uriPathTemplateElements.toArray();
			} else if(metamodel != null && metamodel.isInitializing()){
				// return a stub object that says loading...
				Logger.debug("Displaying the 'Loading...' stub for project '{}' and launching a build", javaProject.getElementName());
				return new Object[] { new LoadingStub(javaProject) };
			} else {
				Logger.debug("*** There's no JAX-RS Metamodel for project '{}' -> no element to display ***", javaProject.getElementName());
			}
		} catch (CoreException e) {
			Logger.error("Failed to retrieve JAX-RS Metamodel in project '" + javaProject.getElementName() + "'", e);
		}
		return new Object[0];
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
	
	/**
	 * Returns {@code true} if the {@link IJaxrsMetamodel} associated with the {@link IJavaProject} given in the constructor has {@link IJaxrsElement}. Returns {@code false} otherwise (or if there is no {@link IJaxrsMetamodel} for the current {@link IJavaProject}).
	 * @param element not used (mandatory to implement the {@link ITreeContentProvider#hasChildren(Object)} method.
	 */
	@Override
	public boolean hasChildren(Object element) {
		try {
			final IJaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(javaProject);
			if (metamodel != null && !metamodel.isInitializing()) {
				final Collection<IJaxrsEndpoint> endpoints = metamodel.getAllEndpoints();
				Logger.debug("UriPathTemplateCategory has endpoints: {}", (!endpoints.isEmpty()));
				return (!endpoints.isEmpty());
			}
		} catch (CoreException e) {
			Logger.error("Failed to retrieve JAX-RS Metamodel in project '" + javaProject.getElementName() + "'", e);
		}
		return false;
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
		if(wrapperCache.containsKey(endpoint)) {
			return wrapperCache.get(endpoint);
		}
		Logger.trace("Creating element for endpoint '{}' (was not found in wrapperCache)", endpoint);
		final UriPathTemplateElement element = new UriPathTemplateElement(endpoint, this);
		wrapperCache.put(endpoint, element);
		return element;
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
				level = metamodel.getProblemSeverity();
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
			final IJaxrsMetamodel metamodel= JaxrsMetamodelLocator.get(javaProject, true);
			if(metamodel != null) {
				final JavaElementDelta delta = new JavaElementDelta(javaProject, null,
						IJavaElementDelta.ADDED, 0);
				final ElementChangedEvent event = new ElementChangedEvent(delta, ElementChangedEvent.POST_RECONCILE);
				final JavaElementChangedBuildJob job = new JavaElementChangedBuildJob(event);
				job.setRule(javaProject.getProject().getWorkspace().getRuleFactory().buildRule());
				job.schedule();
				job.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(final IJobChangeEvent event) {
						parent.refreshContent(metamodel);
					}
				});
				
			}
		} catch (CoreException e) {
			Logger.error("Failed to determine the problem severity for the JAX-RS Web Services", e);
		}
		
	}

	@Override
	public String toString() {
		return "UriPathTemplateCategory on project '" + javaProject.getElementName() + "'";
	}

}
