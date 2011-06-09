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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.jboss.tools.ws.jaxrs.core.metamodel.Metamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.ResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.Route;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

public class UriPathTemplateCategory implements ITreeContentProvider {

	private final Metamodel metamodel;

	private final IProject project;

	private final UriMappingsContentProvider parent;

	private final Map<Route, UriPathTemplateElement> wrapperCache = new HashMap<Route, UriPathTemplateElement>();

	public UriPathTemplateCategory(UriMappingsContentProvider parent, Metamodel metamodel, IProject project) {
		super();
		this.parent = parent;
		this.metamodel = metamodel;
		this.project = project;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (metamodel == null) {
			return new Object[0];
		}
		List<Route> routes = metamodel.getRoutes().getAll();
		List<UriPathTemplateElement> uriPathTemplateElements = new ArrayList<UriPathTemplateElement>();
		// Collections.sort(uriMappings);
		for (Route entry : routes) {
			UriPathTemplateElement element = wrapperCache.get(entry);
			LinkedList<ResourceMethod> resourceMethods = entry.getResourceMethods();
			if (element == null || !element.getLastMethod().equals(resourceMethods.getLast())) {
				element = new UriPathTemplateElement(entry, this);
				wrapperCache.put(entry, element);
			}
			if (element != null) {
				uriPathTemplateElements.add(element);
			} // else ignore the entry

		}
		return uriPathTemplateElements.toArray();
	}

	@Override
	public Object getParent(Object element) {
		return project;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (metamodel == null) {
			return false;
		}
		return (metamodel.getResources().getAll().size() > 0);
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		Logger.debug("Input changed in UriPathTemplateCategory");
	}

	public boolean hasErrors() {
		for (Route route : metamodel.getRoutes().getAll()) {
			for (ResourceMethod resourceMethod : route.getResourceMethods()) {
				if (resourceMethod.hasErrors()) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void dispose() {
	}

	public void refreshContent() {
		parent.refreshContent();
	}

}
