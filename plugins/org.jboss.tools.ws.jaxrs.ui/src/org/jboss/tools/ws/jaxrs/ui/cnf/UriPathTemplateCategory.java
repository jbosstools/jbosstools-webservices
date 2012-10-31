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

package org.jboss.tools.ws.jaxrs.ui.cnf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.JaxrsMetamodelLocator;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

public class UriPathTemplateCategory implements ITreeContentProvider {

	private final IProject project;

	private final UriMappingsContentProvider parent;

	private final Map<IJaxrsEndpoint, UriPathTemplateElement> wrapperCache = new HashMap<IJaxrsEndpoint, UriPathTemplateElement>();

	public UriPathTemplateCategory(UriMappingsContentProvider parent, IProject project) {
		super();
		this.parent = parent;
		this.project = project;
	}

	/** {@inheritDoc} */
	@Override
	public Object[] getChildren(Object parentElement) {
		try {
			final IJaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(project);
			if (metamodel != null) {
				List<IJaxrsEndpoint> endpoints = metamodel.getAllEndpoints();
				Logger.debug("UriPathTemplateCatrogory contains {} endpoints", endpoints.size());
				List<UriPathTemplateElement> uriPathTemplateElements = new ArrayList<UriPathTemplateElement>();
				// Collections.sort(uriMappings);
				for (IJaxrsEndpoint endpoint : endpoints) {
					UriPathTemplateElement element = wrapperCache.get(endpoint);
					// LinkedList<IJaxrsResourceMethod> resourceMethods =
					// endpoint.getResourceMethods();
					if (element == null) {
						Logger.trace("Creating element for endpoint {} ('cause not found in wrapperCache)", endpoint);
						element = new UriPathTemplateElement(endpoint, this);
						wrapperCache.put(endpoint, element);
					}
					// after a clean build, the 'endpoint' reference should be
					// updated
					else if (element.getEndpoint() != endpoint) {
						element.setEndpoint(endpoint);

					}
					Logger.trace("Adding element for endpoint {}", endpoint);
					uriPathTemplateElements.add(element);
				}
				return uriPathTemplateElements.toArray();
			}
		} catch (CoreException e) {
			Logger.error("Failed to retrieve JAX-RS Metamodel in project '" + project.getName() + "'", e);
		}
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		return project;
	}

	/**
	 * @return the project
	 */
	public final IProject getProject() {
		return project;
	}

	@Override
	public boolean hasChildren(Object element) {
		try {
			final IJaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(project);
			if (metamodel != null) {
				return (metamodel.getAllEndpoints().size() > 0);
			}
		} catch (CoreException e) {
			Logger.error("Failed to retrieve JAX-RS Metamodel in project '" + project.getName() + "'", e);
		}
		return false;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		Logger.debug("Input changed in UriPathTemplateCategory");
	}

	public int getProblemLevel() {
		int level = 0;
		try {
			final IJaxrsMetamodel metamodel= JaxrsMetamodelLocator.get(project);
			if (metamodel != null) {
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

	public void refreshContent() {
		parent.refreshContent(project);
	}

	public Object[] getChildren() {
		return getChildren(this);
	}

}
