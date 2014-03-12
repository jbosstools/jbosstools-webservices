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

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;

/**
 * Factory for {@link JaxrsEndpoint} instances
 * 
 * @author xcoulon
 * 
 */
public class JaxrsEndpointFactory {

	/**
	 * Attempts to create one or more {@link IJaxrsEndpoint}s from the given
	 * {@link IJaxrsElement}
	 * 
	 * @param resourceMethod
	 *            the newly created JAX-RS Resource Method
	 * @throws CoreException
	 */
	public static List<IJaxrsEndpoint> createEndpoints(final JaxrsResourceMethod resourceMethod) throws CoreException {
		final List<IJaxrsEndpoint> endpoints = new ArrayList<IJaxrsEndpoint>();
		// create endpoint from resource method or subresource method in root
		// resource
		if (resourceMethod.getParentResource().isRootResource()) {
			switch (resourceMethod.getElementKind()) {
			case RESOURCE_METHOD:
			case SUBRESOURCE_METHOD:
				endpoints.add(createEndpointFromRootResourceMethod(resourceMethod));
				break;
			case SUBRESOURCE_LOCATOR:
				endpoints.addAll(createEndpointsFromSubresourceLocator(resourceMethod));
				break;
			default:
				Logger.info("Unable to create endpoint from undefined JAX-RS Resource Method: " + resourceMethod);
			}
		} else {
			switch (resourceMethod.getElementKind()) {
			case RESOURCE_METHOD:
			case SUBRESOURCE_METHOD:
				endpoints.addAll(createEndpointsFromSubresourceMethod(resourceMethod));
				break;
			default:
				Logger.info("Unable to create endpoint from undefined JAX-RS Resource Method: " + resourceMethod);
			}
		}
		return endpoints;
	}

	/**
	 * @return
	 * @throws CoreException
	 */
	public static IJaxrsEndpoint createEndpointFromRootResourceMethod(final JaxrsResourceMethod resourceMethod)
			throws CoreException {
		final long start = System.currentTimeMillis();
		try {
			final IJaxrsHttpMethod httpMethod = resourceMethod.getMetamodel().findHttpMethodByTypeName(
					resourceMethod.getHttpMethodAnnotation().getFullyQualifiedName());
			final LinkedList<JaxrsResourceMethod> resourceMethods = new LinkedList<JaxrsResourceMethod>();
			resourceMethods.add(resourceMethod);
			final JaxrsEndpoint endpoint = new JaxrsEndpoint(resourceMethod.getMetamodel(), httpMethod, resourceMethods);
			endpoint.joinMetamodel();
			return endpoint;
		} finally {
			final long end = System.currentTimeMillis();
			Logger.tracePerf("Created JAX-RS Endpoint from JAX-RS Resource Method in {}ms", (end - start));
		}
	}

	/**
	 * @return
	 * @throws CoreException
	 */
	public static List<IJaxrsEndpoint> createEndpointsFromSubresourceMethod(final JaxrsResourceMethod resourceMethod)
			throws CoreException {
		final long start = System.currentTimeMillis();
		try {
			final List<IJaxrsEndpoint> endpoints = new ArrayList<IJaxrsEndpoint>();
			final JaxrsMetamodel metamodel = resourceMethod.getMetamodel();
			final IType resourceType = resourceMethod.getParentResource().getJavaElement();
			final List<IType> superTypes = JdtUtils.findSupertypes(resourceType);
			for (IType superType : superTypes) {
				final List<IJaxrsResourceMethod> subresourceLocators = metamodel
						.findResourceMethodsByReturnedType(superType);
				for (IJaxrsResourceMethod subresourceLocator : subresourceLocators) {
					if (subresourceLocator.getParentResource().isRootResource()) {
						final IJaxrsHttpMethod httpMethod = resourceMethod.getMetamodel().findHttpMethodByTypeName(
								resourceMethod.getHttpMethodAnnotation().getFullyQualifiedName());
						final LinkedList<JaxrsResourceMethod> resourceMethods = new LinkedList<JaxrsResourceMethod>();
						resourceMethods.add((JaxrsResourceMethod)subresourceLocator);
						resourceMethods.add(resourceMethod);
						final JaxrsEndpoint endpoint = new JaxrsEndpoint(resourceMethod.getMetamodel(), httpMethod,
								resourceMethods);
						endpoint.joinMetamodel();
						endpoints.add(endpoint);
					}
				}
			}
			return endpoints;
		} finally {
			final long end = System.currentTimeMillis();
			Logger.tracePerf("Created JAX-RS Endpoint from JAX-RS Subresource Method in {}ms", (end - start));
		}

	}

	// FIXME: support chain of subresource locators
	@SuppressWarnings("incomplete-switch")
	public static List<JaxrsEndpoint> createEndpointsFromSubresourceLocator(
			final JaxrsResourceMethod subresourceLocator) throws CoreException {
		final long start = System.currentTimeMillis();
		try {
			final List<JaxrsEndpoint> endpoints = new ArrayList<JaxrsEndpoint>();
			final JaxrsMetamodel metamodel = subresourceLocator.getMetamodel();
			final IType returnedType = subresourceLocator.getReturnedType();
			if (returnedType != null) {
				final List<IType> returnedTypes = JdtUtils.findSubtypes(returnedType);
				for (IType subtype : returnedTypes) {
					final JaxrsResource matchingResource = metamodel.findResource(subtype);
					if (matchingResource != null && matchingResource.isSubresource()) {
						for (IJaxrsResourceMethod method : matchingResource.getAllMethods()) {
							switch (method.getElementKind()) {
							case RESOURCE_METHOD:
							case SUBRESOURCE_METHOD:
								final JaxrsHttpMethod httpMethod = metamodel.findHttpMethodByTypeName(method
										.getHttpMethodClassName());
								final LinkedList<JaxrsResourceMethod> resourceMethods = new LinkedList<JaxrsResourceMethod>(
										Arrays.asList(subresourceLocator, (JaxrsResourceMethod)method));
								final JaxrsEndpoint endpoint = new JaxrsEndpoint(metamodel, httpMethod, resourceMethods);
								endpoint.joinMetamodel();
								endpoints.add(endpoint);
								break;
							}
						}

					}

				}
			}
			return endpoints;
		} finally {
			final long end = System.currentTimeMillis();
			Logger.tracePerf("Created JAX-RS Endpoint from JAX-RS Subresource Locator in {}ms", (end - start));
		}

	}

}
