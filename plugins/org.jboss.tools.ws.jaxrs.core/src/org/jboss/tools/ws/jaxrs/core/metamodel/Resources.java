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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.jboss.tools.ws.jaxrs.core.internal.builder.JAXRSAnnotationsScanner;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionFilterUtil;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.BaseElement.EnumType;
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
	public final void addFrom(final IJavaElement scope, final IProgressMonitor progressMonitor) throws CoreException {
		progressMonitor.beginTask("Adding resources and resourceMethods", 1);
		HTTPMethods httpMethods = metamodel.getHttpMethods();
		List<IType> javaTypes = JAXRSAnnotationsScanner.findResources(scope, httpMethods.getTypeNames(),
				progressMonitor);
		for (IType javaType : javaTypes) {
			try {
				elements.put(javaType.getFullyQualifiedName(),
						new Resource.Builder(javaType, metamodel).build(progressMonitor));
			} catch (InvalidModelElementException e) {
				Logger.warn("Type '" + javaType.getFullyQualifiedName() + "' is not a valid JAX-RS Resource: "
						+ e.getMessage());
			}
		}
	}

	/**
	 * Resolve the URI Mappings in the model, given all root resources,
	 * subresources , resource resourceMethods , subresource resourceMethods and
	 * subresource locators
	 * 
	 * @throws CoreException
	 */
	public final Map<ResolvedUriMapping, Stack<ResourceMethod>> resolveUriMappings(
			final IProgressMonitor progressMonitor) throws CoreException {
		Map<ResolvedUriMapping, Stack<ResourceMethod>> uriMappings = new HashMap<ResolvedUriMapping, Stack<ResourceMethod>>();
		for (Resource resource : getRootResources()) {
			resolveResourcesUriMappings(resource, metamodel.getServiceUri(), uriMappings, new Stack<ResourceMethod>(),
					progressMonitor);
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
		// resource resourceMethods and subresources resourceMethods are treated
		// the same way
		for (ResourceMethod resourceMethod : resource.getAllMethods()) {
			String uriPathTemplate = resolveURIPathTemplate(uriTemplateFragment, resource, resourceMethod);
			MediaTypeCapabilities mediaTypeCapabilities = resolveMediaTypeCapabilities(resource, resourceMethod);
			UriMapping resourceUriMapping = resourceMethod.getUriMapping();
			ResolvedUriMapping uriMapping = new ResolvedUriMapping(resourceUriMapping.getHTTPMethod(), uriPathTemplate,
					resourceUriMapping.getQueryParams(), mediaTypeCapabilities);
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
			if (returnType == null) {
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
	private static final String resolveURIPathTemplate(final String uriTemplateFragment, final Resource resource,
			final ResourceMethod resourceMethod) {
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

	private static final MediaTypeCapabilities resolveMediaTypeCapabilities(final Resource resource,
			final ResourceMethod resourceMethod) {
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
			if (resource.isRootResource() && resource.getUriPathTemplate().endsWith(path)) {
				return resource;
			}
		}
		// not found
		return null;
	}

	public final List<Resource> getRootResources() {
		return CollectionFilterUtil.filterElementsByKind(elements.values(), EnumType.ROOT_RESOURCE);
	}

	public final List<Resource> getSubresources() {
		return CollectionFilterUtil.filterElementsByKind(elements.values(), EnumType.SUBRESOURCE);
	}

}
