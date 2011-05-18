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
import java.util.Stack;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.jboss.tools.ws.jaxrs.core.metamodel.Metamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.ResolvedUriMapping;
import org.jboss.tools.ws.jaxrs.core.metamodel.ResourceMethod;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

public class UriPathTemplateCategory implements ITreeContentProvider {

	private final Metamodel metamodel;

	private final IProject project;
	
	private final UriMappingsContentProvider parent;

	private Map<ResolvedUriMapping, UriPathTemplateElement> wrapperCache = new HashMap<ResolvedUriMapping, UriPathTemplateElement>();

	public UriPathTemplateCategory(UriMappingsContentProvider parent, Metamodel metamodel, IProject project) {
		super();
		this.parent = parent;
		this.metamodel = metamodel;
		this.project = project;
	}

	public Object[] getChildren(Object parentElement) {
		try {
			Map<ResolvedUriMapping, Stack<ResourceMethod>> resolvedUriMappings = metamodel.getResources().resolveUriMappings(
					new NullProgressMonitor());
			List<UriPathTemplateElement> uriPathTemplateElements = new ArrayList<UriPathTemplateElement>();
			List<ResolvedUriMapping> uriMappings = new ArrayList<ResolvedUriMapping>(resolvedUriMappings.keySet());
			// Collections.sort(uriMappings);
			for (ResolvedUriMapping entry : uriMappings) {
				UriPathTemplateElement element = wrapperCache.get(entry);
				Stack<ResourceMethod> resourceMethods = resolvedUriMappings.get(entry);
				if (element == null || !element.getLastMethod().equals(resourceMethods.lastElement())) {
					element = new UriPathTemplateElement(entry, resourceMethods, this);
					wrapperCache.put(entry, element);
				}
				if (element != null) {
					uriPathTemplateElements.add(element);
				} // else ignore the entry

			}
			return uriPathTemplateElements.toArray();
		} catch (CoreException e) {
			Logger.error("Error while resolving project URI Mappings", e);
		}
		return null;
	}

	public Object getParent(Object element) {
		return project;
	}

	public boolean hasChildren(Object element) {
		return (metamodel.getResources().getAll().size() > 0);
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		Logger.debug("Input changed in UriPathTemplateCategory");
	}

	@Override
	public void dispose() {
	}
	
	public void refreshContent() {
		parent.refreshContent();
	}

	

}
