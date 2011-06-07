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

package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.jboss.tools.ws.jaxrs.core.internal.builder.JAXRSAnnotationsScanner;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionFilterUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.BaseElement.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.Resource.ResourceBuilder;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;

public class Resources extends BaseElementContainer<Resource> {

	/**
	 * Full constructor
	 * 
	 * @param metamodel
	 */
	public Resources(final Metamodel metamodel) {
		super(metamodel);
	}

	// FIXME deal with interfaces/implementations
	@Override
	public List<Resource> addFrom(final IJavaElement scope, final IProgressMonitor progressMonitor)
			throws CoreException {
		progressMonitor.beginTask("Adding resources and resourceMethods", 1);
		HTTPMethods httpMethods = metamodel.getHttpMethods();
		List<IType> javaTypes = JAXRSAnnotationsScanner.findResources(scope, httpMethods.getTypeNames(),
				progressMonitor);
		List<Resource> addedResources = new ArrayList<Resource>();
		for (IType javaType : javaTypes) {
			try {
				Resource addedResource = new ResourceBuilder(javaType, metamodel).build(progressMonitor);
				elements.put(javaType.getFullyQualifiedName(), addedResource);
				addedResources.add(addedResource);
				// TODO: update the resolved mappings from here
			} catch (InvalidModelElementException e) {
				Logger.warn("Type '" + javaType.getFullyQualifiedName() + "' is not a valid JAX-RS Resource: "
						+ e.getMessage());
			}
		}
		return addedResources;
	}

	@Override
	public Resource removeElement(final IResource removedResource, final IProgressMonitor progressMonitor) {
		Resource resource = super.removeElement(removedResource, progressMonitor);
		return resource;
	}

	public Resource getByResource(IResource resource) {
		if (resource == null) {
			return null;
		}
		for (Entry<String, Resource> entry : elements.entrySet()) {
			Resource r = entry.getValue();
			if (resource.equals(r.getJavaElement().getResource())) {
				return r;
			}
		}
		return null;
	}

	/**
	 * Returns the Root resource for the given Path
	 * 
	 * @param path
	 * @return
	 */
	public final Resource getByPath(final String path) {
		for (Entry<String, Resource> entry : elements.entrySet()) {
			Resource resource = entry.getValue();
			if (resource.getKind() == EnumKind.ROOT_RESOURCE
					&& resource.getMapping().getUriPathTemplateFragment().endsWith(path)) {
				return resource;
			}
		}
		// not found
		return null;
	}

	/**
	 * @return The Subresource locators that match the given return type.
	 * @param returnType
	 *            the return type.
	 * @throws CoreException
	 */
	public final List<ResourceMethod> findSubresourceLocators(IType returnType, IProgressMonitor progressMonitor)
			throws CoreException {
		List<ResourceMethod> locators = new ArrayList<ResourceMethod>();
		for (Resource resource : getAll()) {
			for (ResourceMethod locator : resource.getSubresourceLocators()) {
				ITypeHierarchy returnTypeHierarchy = JdtUtils.resolveTypeHierarchy(locator.getReturnType(), true,
						progressMonitor);
				if (returnTypeHierarchy.contains(returnType)) {
					locators.add(locator);
				}
			}
		}
		return locators;
	}

	public final List<Resource> getRootResources() {
		return CollectionFilterUtils.filterElementsByKind(elements.values(), EnumKind.ROOT_RESOURCE);
	}

	public final List<Resource> getSubresources() {
		return CollectionFilterUtils.filterElementsByKind(elements.values(), EnumKind.SUBRESOURCE);
	}

}
