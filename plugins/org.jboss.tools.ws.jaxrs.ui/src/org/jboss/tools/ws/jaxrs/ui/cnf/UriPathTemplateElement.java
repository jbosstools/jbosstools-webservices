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
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.jboss.tools.ws.jaxrs.core.metamodel.ResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.Route;
import org.jboss.tools.ws.jaxrs.ui.cnf.UriPathTemplateMediaTypeMappingElement.EnumCapabilityType;

public class UriPathTemplateElement implements ITreeContentProvider {

	private final Route route;

	private final UriPathTemplateCategory uriPathTemplateCategory;

	public UriPathTemplateElement(Route route, UriPathTemplateCategory uriPathTemplateCategory) {
		super();
		this.route = route;
		this.uriPathTemplateCategory = uriPathTemplateCategory;
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		List<Object> elements = new ArrayList<Object>(3);
		elements.add(new UriPathTemplateMediaTypeMappingElement(route.getEndpoint().getConsumedMediaTypes(),
				EnumCapabilityType.CONSUMES));
		elements.add(new UriPathTemplateMediaTypeMappingElement(route.getEndpoint().getProducedMediaTypes(),
				EnumCapabilityType.PRODUCES));
		elements.add(new UriPathTemplateMethodMappingElement(route.getResourceMethods()));
		return elements.toArray();
	}

	@Override
	public Object getParent(Object element) {
		return uriPathTemplateCategory;
	}

	@Override
	public boolean hasChildren(Object element) {
		return (getChildren(element).length > 0);
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub

	}

	public boolean hasErrors() {
		return getLastMethod().hasErrors();
	}

	/**
	 * @return the uriMapping
	 */
	public Route getResolvedUriMapping() {
		return route;
	}

	public ResourceMethod getLastMethod() {
		return route.getResourceMethods().getLast();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((route == null) ? 0 : route.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UriPathTemplateElement other = (UriPathTemplateElement) obj;
		if (route == null) {
			if (other.route != null)
				return false;
		} else if (!route.equals(other.route))
			return false;
		return true;
	}

}
