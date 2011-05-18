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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.jboss.tools.ws.jaxrs.core.internal.builder.JAXRSAnnotationsScanner;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionFilterUtil;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.BaseElement.EnumType;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;

public class Resources {

	private final Map<String, Resource> resources = new HashMap<String, Resource>();

	private final Metamodel metamodel;

	public Resources(final Metamodel metamodel) {
		this.metamodel = metamodel;
	}

	/**
	 * Resolve the URI Mappings in the model, given all root resources,
	 * subresources , resource resourceMethods , subresource resourceMethods and subresource
	 * locators
	 * 
	 * @throws CoreException
	 */
	public final Map<ResolvedUriMapping, Stack<ResourceMethod>> resolveUriMappings(final IProgressMonitor progressMonitor) throws CoreException {
		Map<ResolvedUriMapping, Stack<ResourceMethod>> uriMappings = new HashMap<ResolvedUriMapping, Stack<ResourceMethod>>();
		for (Resource resource : getRootResources()) {
			resolveResourcesUriMappings(resource, "/*", uriMappings, new Stack<ResourceMethod>(), progressMonitor);
		}
		return uriMappings;
	}

	/**
	 * @param progressMonitor
	 * @param uriMappings
	 * @param resource
	 * @param methodsStack
	 * @throws CoreException
	 */
	private void resolveResourcesUriMappings(final Resource resource, final String uriTemplateFragment,
			final Map<ResolvedUriMapping, Stack<ResourceMethod>> uriMappings, final Stack<ResourceMethod> methodsStack,
			final IProgressMonitor progressMonitor) throws CoreException {
		// resource resourceMethods and subresources resourceMethods are treated the same way
		for (ResourceMethod resourceMethod : resource.getAllMethods()) {
			String uriPathTemplate = resolveURIPathTemplate(uriTemplateFragment, resource, resourceMethod);
			MediaTypeCapabilities mediaTypeCapabilities = resolveMediaTypeCapabilities(resource, resourceMethod);
			UriMapping resourceUriMapping = resourceMethod.getUriMapping();
			ResolvedUriMapping uriMapping = new ResolvedUriMapping(resourceUriMapping.getHTTPMethod(), uriPathTemplate, resourceUriMapping.getQueryParams(), mediaTypeCapabilities);
			@SuppressWarnings("unchecked")
			Stack<ResourceMethod> stack = (Stack<ResourceMethod>) methodsStack.clone();
			stack.add(resourceMethod);
			uriMappings.put(uriMapping, stack);
		}
		// TODO : verify support chain of subresource locators
		// TODO : stack resourceMethods and detect+prevent cycles
		for (ResourceMethod resourceMethod : resource.getSubresourceLocators()) {
			String uriPathTemplate = resolveURIPathTemplate(uriTemplateFragment, resource, resourceMethod);
			IType returnType = resourceMethod.getReturnType();
			if(returnType == null) {
				continue;
			}
			ITypeHierarchy subresourceTypeHierarchy = JdtUtils.resolveTypeHierarchy(returnType, false, progressMonitor);
			for (IType subresourceType : subresourceTypeHierarchy.getSubtypes(returnType)) {
				Resource subresource = getByType(subresourceType);
				if (subresource != null && !subresource.isRootResource()) {
					@SuppressWarnings("unchecked")
					Stack<ResourceMethod> stack = (Stack<ResourceMethod>) methodsStack.clone();
					stack.add(resourceMethod);
					resolveResourcesUriMappings(subresource, uriPathTemplate, uriMappings, stack, progressMonitor);
				}
			}
		}
	}

	// FIXME : include method parameters if annotated with @QueryParam
	private static final String resolveURIPathTemplate(final String uriTemplateFragment, final Resource resource, final ResourceMethod resourceMethod) {
		StringBuffer uriTemplateBuffer = new StringBuffer(uriTemplateFragment);
		String resourceUriPathTemplate = resource.getUriPathTemplate();
		String methodUriPathTemplate = resourceMethod.getUriMapping().getUriPathTemplateFragment();
		if (resourceUriPathTemplate != null) {
			uriTemplateBuffer.append("/").append(resourceUriPathTemplate);
		}
		if (methodUriPathTemplate != null) {
			uriTemplateBuffer.append("/").append(methodUriPathTemplate);
		}
		return uriTemplateBuffer.toString().replaceAll("/\\*", "/").replaceAll("///", "/").replaceAll("//", "/");
	}

	private static final MediaTypeCapabilities resolveMediaTypeCapabilities(final Resource resource, final ResourceMethod resourceMethod) {
		MediaTypeCapabilities resourceMediaTypeCapabilities = resource.getMediaTypeCapabilities();
		MediaTypeCapabilities methodMediaTypeCapabilities = resourceMethod.getUriMapping().getMediaTypeCapabilities();
		MediaTypeCapabilities mediaTypeCapabilities = new MediaTypeCapabilities();
		if (!methodMediaTypeCapabilities.getConsumedMimeTypes().isEmpty()) {
			mediaTypeCapabilities.setConsumedMimeTypes(methodMediaTypeCapabilities.getConsumedMimeTypes());
		} else if (!resourceMediaTypeCapabilities.getConsumedMimeTypes().isEmpty()) {
			mediaTypeCapabilities.setConsumedMimeTypes(resourceMediaTypeCapabilities.getConsumedMimeTypes());
		} else {
			// leave empty collection
			// mediaTypeCapabilities.setConsumedMimeTypes(Arrays.asList("*/*"));
		}
		if (!methodMediaTypeCapabilities.getProducedMimeTypes().isEmpty()) {
			mediaTypeCapabilities.setProducedMimeTypes(methodMediaTypeCapabilities.getProducedMimeTypes());
		} else if (!resourceMediaTypeCapabilities.getProducedMimeTypes().isEmpty()) {
			mediaTypeCapabilities.setProducedMimeTypes(resourceMediaTypeCapabilities.getProducedMimeTypes());
		} else {
			// leave empty collection
			// mediaTypeCapabilities.setProducedMimeTypes(Arrays.asList("*/*"));
		}
		return mediaTypeCapabilities;
	}

	// FIXME deal with interfaces/implementations
	public final void addFrom(final IJavaElement scope, final SubProgressMonitor progressMonitor) throws CoreException {
		progressMonitor.beginTask("Adding resources and resourceMethods", 1);
		HTTPMethods httpMethods = metamodel.getHttpMethods();
		List<IType> javaTypes = JAXRSAnnotationsScanner.findResources(scope, httpMethods.getTypeNames(),
				progressMonitor);
		for (IType javaType : javaTypes) {
			try {
				resources.put(javaType.getFullyQualifiedName(), new Resource(javaType, metamodel, progressMonitor));
			} catch (InvalidModelElementException e) {
				Logger.warn("Type '" + javaType.getFullyQualifiedName() + "' is not a valid JAX-RS Resource: "
						+ e.getMessage());
			}
		}
	}

	public final void removeElement(final IResource removedResource, final IProgressMonitor progressMonitor) {
		for (Iterator<Resource> iterator = resources.values().iterator(); iterator.hasNext();) {
			Resource r = iterator.next();
			if (removedResource.equals(r.getJavaElement().getResource())) {
				iterator.remove();
			}
		}
	}

	public final Resource getByType(final IType type) {
		if(type == null) {
			return null;
		}
		return resources.get(type.getFullyQualifiedName());
	}
	
	public Resource getByResource(IResource resource) {
		if(resource == null) {
			return null;
		}
		for(Entry<String, Resource> entry : resources.entrySet()) {
			Resource r = entry.getValue();
			if(resource.equals(r.getJavaElement().getResource())) {
				return r;
			}
		}
		return null;
	}

	public final boolean contains(final IType type) {
		if(type == null) {
			return false;
		}
		return resources.containsKey(type.getFullyQualifiedName());
	}

	public final Resource getByTypeName(final String fullyQualifiedName) {
		return resources.get(fullyQualifiedName);
	}

	/**
	 * Returns the Root resource for the given Path
	 * 
	 * @param path
	 * @return
	 */
	public final Resource getByPath(final String path) {
		for (Entry<String, Resource> entry : resources.entrySet()) {
			Resource resource = entry.getValue();
			if (resource.isRootResource() && resource.getUriPathTemplate().endsWith(path)) {
				return resource;
			}
		}
		// not found
		return null;
	}

	public final List<Resource> getAll() {
		return Collections.unmodifiableList(new ArrayList<Resource>(resources.values()));
	}

	public final List<Resource> getRootResources() {
		return CollectionFilterUtil.filterElementsByKind(resources.values(), EnumType.ROOT_RESOURCE);
	}

	public final List<Resource> getSubresources() {
		return CollectionFilterUtil.filterElementsByKind(resources.values(), EnumType.SUBRESOURCE);
	}

	/**
	 * Resets the HTTPMethods list
	 */
	public void reset() {
		this.resources.clear();
	}


}
