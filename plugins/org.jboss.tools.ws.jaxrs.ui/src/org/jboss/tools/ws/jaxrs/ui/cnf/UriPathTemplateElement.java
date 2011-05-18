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
import java.util.Stack;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.jboss.tools.ws.jaxrs.core.metamodel.ResolvedUriMapping;
import org.jboss.tools.ws.jaxrs.core.metamodel.ResourceMethod;
import org.jboss.tools.ws.jaxrs.ui.cnf.UriPathTemplateMediaTypeMappingElement.EnumMediaType;

public class UriPathTemplateElement implements ITreeContentProvider {

	private final ResolvedUriMapping resolvedUriMapping;
	
	private final Stack<ResourceMethod> resourceMethods;
	
	private final UriPathTemplateCategory uriPathTemplateCategory;

	public UriPathTemplateElement(ResolvedUriMapping uriMapping, Stack<ResourceMethod> resourceMethods,
			UriPathTemplateCategory uriPathTemplateCategory) {
		super();
		this.resolvedUriMapping = uriMapping;
		this.resourceMethods = resourceMethods;
		this.uriPathTemplateCategory = uriPathTemplateCategory;
	}

	public Object[] getChildren(Object parentElement) {
		List<Object> elements = new ArrayList<Object>(3);
		elements.add(new UriPathTemplateMediaTypeMappingElement(resolvedUriMapping.getMediaTypeCapabilities().getConsumedMimeTypes(), EnumMediaType.CONSUMES));
		elements.add(new UriPathTemplateMediaTypeMappingElement(resolvedUriMapping.getMediaTypeCapabilities().getProducedMimeTypes(), EnumMediaType.PROVIDES));
		elements.add(new UriPathTemplateMethodMappingElement(resourceMethods));
		return elements.toArray();
	}

	public Object getParent(Object element) {
		return uriPathTemplateCategory;
	}

	public boolean hasChildren(Object element) {
		return (getChildren(element).length > 0);
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub

	}

	public boolean hasErrors() {
		return resourceMethods.lastElement().hasErrors();
	}
	/**
	 * @return the uriMapping
	 */
	public ResolvedUriMapping getResolvedUriMapping() {
		return resolvedUriMapping;
	}
	
	public ResourceMethod getLastMethod() {
		return resourceMethods.lastElement();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((resolvedUriMapping == null) ? 0 : resolvedUriMapping.hashCode());
		return result;
	}

	/* (non-Javadoc)
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
		if (resolvedUriMapping == null) {
			if (other.resolvedUriMapping != null)
				return false;
		} else if (!resolvedUriMapping.equals(other.resolvedUriMapping))
			return false;
		return true;
	}
	

}
