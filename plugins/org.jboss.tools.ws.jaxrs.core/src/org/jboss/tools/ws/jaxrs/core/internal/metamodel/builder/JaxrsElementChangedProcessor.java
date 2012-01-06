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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import static org.eclipse.jdt.core.IJavaElementDelta.ADDED;
import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.eclipse.jdt.core.IJavaElementDelta.REMOVED;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_ELEMENT_KIND;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_METHOD_RETURN_TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResourceMethod;

public class JaxrsElementChangedProcessor {

	public List<JaxrsEndpointChangedEvent> processEvents(final List<JaxrsElementChangedEvent> jaxrsElementChanges,
			IProgressMonitor progressMonitor) {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		try {
			Logger.debug("Processing {} JAX-RS element change(s)...", jaxrsElementChanges.size());
			boolean loopAgain = true;
			while (loopAgain) {
				loopAgain = false;
				for (Iterator<JaxrsElementChangedEvent> iterator = jaxrsElementChanges.iterator(); iterator.hasNext();) {
					JaxrsElementChangedEvent jaxrsElementChange = iterator.next();
					Logger.debug("Processing {}", jaxrsElementChange);
					final List<JaxrsEndpointChangedEvent> endpointChanges = processEvent(jaxrsElementChange);
					for (JaxrsEndpointChangedEvent endpointChange : endpointChanges) {
						Logger.debug("--> {}", endpointChange);
					}
					if (!endpointChanges.isEmpty()) {
						changes.addAll(endpointChanges);
						iterator.remove();
						loopAgain = true;
					}
				}
			}
		} catch (CoreException e) {
			Logger.error("Failed to process JAX-RS element changes", e);
		} finally {
			Logger.debug("Done processing JAX-RS element change(s).");
		}
		return changes;
	}

	private List<JaxrsEndpointChangedEvent> processEvent(final JaxrsElementChangedEvent event) throws CoreException {
		final JaxrsElement<?> element = event.getElement();
		final EnumElementKind elementKind = element.getElementKind();
		final int flags = event.getFlags();
		switch (event.getDeltaKind()) {
		case ADDED:
			switch (elementKind) {
			case APPLICATION:
				return processAddition((JaxrsApplication) element);
			case HTTP_METHOD:
				return processAddition((JaxrsHttpMethod) element);
			case RESOURCE:
				return processAddition((JaxrsResource) element);
			case RESOURCE_METHOD:
				return processAddition((JaxrsResourceMethod) element);
			}
		case CHANGED:
			switch (elementKind) {
			case APPLICATION:
				return processChange((JaxrsApplication) element, flags);
			case HTTP_METHOD:
				return processChange((JaxrsHttpMethod) element, flags);
			case RESOURCE:
				return processChange((JaxrsResource) element, flags);
			case RESOURCE_METHOD:
				return processChange((JaxrsResourceMethod) element, flags);
			}
		case REMOVED:
			switch (elementKind) {
			case APPLICATION:
				return processRemoval((JaxrsApplication) element);
			case HTTP_METHOD:
				return processRemoval((JaxrsHttpMethod) element);
			case RESOURCE:
				return processRemoval((JaxrsResource) element);
			case RESOURCE_METHOD:
				return processRemoval((JaxrsResourceMethod) element);
			}
		}
		return Collections.emptyList();
	}

	/**
	 * Process changes in the JAX-RS Metamodel when a new Application element is added. There should be only one,
	 * though...
	 * 
	 * @param application
	 * @return
	 */
	private List<JaxrsEndpointChangedEvent> processAddition(final JaxrsApplication application) {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		final JaxrsMetamodel metamodel = application.getMetamodel();
		// if the given application becomes the used application in the metamodel
		if (application.equals(metamodel.getApplication())) {
			for (Iterator<JaxrsEndpoint> iterator = metamodel.getEndpoints().iterator(); iterator.hasNext();) {
				JaxrsEndpoint endpoint = iterator.next();
				if (endpoint.refresh(application)) {
					changes.add(new JaxrsEndpointChangedEvent(endpoint, CHANGED));
				}
			}
		}
		return changes;
	}

	private List<JaxrsEndpointChangedEvent> processAddition(final JaxrsHttpMethod httpMethod) {
		return Collections.emptyList();
	}

	private List<JaxrsEndpointChangedEvent> processAddition(final JaxrsResource resource) throws CoreException {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		for (JaxrsResourceMethod resourceMethod : resource.getMethods().values()) {
			changes.addAll(processAddition(resourceMethod));
		}
		return changes;
	}

	private List<JaxrsEndpointChangedEvent> processAddition(final JaxrsResourceMethod resourceMethod)
			throws CoreException {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		final JaxrsMetamodel metamodel = (JaxrsMetamodel) resourceMethod.getMetamodel();
		final JaxrsResource resource = resourceMethod.getParentResource();
		if (resource == null) {
			Logger.warn("Found an orphan resource method: " + resourceMethod);
		} else {
			switch (resourceMethod.getKind()) {
			case RESOURCE_METHOD:
			case SUBRESOURCE_METHOD:
				switch (resource.getKind()) {
				case ROOT_RESOURCE:
					changes.addAll(processRootResourceMethodAddition(resourceMethod, metamodel));
					break;
				case SUBRESOURCE:
				case UNDEFINED:
					changes.addAll(processSubresourceMethodAddition(resourceMethod, metamodel));
					break;
				}
				break;
			case SUBRESOURCE_LOCATOR:
				// FIXME : support multiple levels of subresource locators
				switch (resource.getKind()) {
				case ROOT_RESOURCE:
					changes.addAll(processSubresourceLocatorAddition(resourceMethod, metamodel));
					break;
				}
				break;
			}
		}
		return changes;
	}

	private List<JaxrsEndpointChangedEvent> processSubresourceMethodAddition(final JaxrsResourceMethod resourceMethod,
			final JaxrsMetamodel metamodel) throws CoreException {
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.getHttpMethod(resourceMethod
				.getHttpMethodAnnotation());
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		final JaxrsResource resource = resourceMethod.getParentResource();
		final IProgressMonitor progressMonitor = new NullProgressMonitor();
		final IType resourceType = resource.getJavaElement();
		final ITypeHierarchy returnTypeHierarchy = JdtUtils.resolveTypeHierarchy(resourceType, false, progressMonitor);
		final List<String> supertypesHandlers = extractHandlers(returnTypeHierarchy.getAllSupertypes(resourceType));
		for (IJaxrsResource otherResource : metamodel.getAllResources()) {
			if (((JaxrsResource) otherResource).isRootResource()) {
				for (JaxrsResourceMethod otherResourceMethod : ((JaxrsResource) otherResource).getMethods().values()) {
					if (otherResourceMethod.getKind() == EnumKind.SUBRESOURCE_LOCATOR) {
						final String returnTypeHandler = (otherResourceMethod.getReturnType() != null) ? otherResourceMethod
								.getReturnType().getHandleIdentifier() : null;
						if (returnTypeHandler != null && supertypesHandlers.contains(returnTypeHandler)) {
							final LinkedList<JaxrsResourceMethod> resourceMethods = new LinkedList<JaxrsResourceMethod>(
									Arrays.asList(otherResourceMethod, resourceMethod));
							final JaxrsEndpoint endpoint = new JaxrsEndpoint(metamodel.getApplication(), httpMethod,
									resourceMethods);
							if (metamodel.add(endpoint)) {
								changes.add(new JaxrsEndpointChangedEvent(endpoint, ADDED));
							}
						}
					}
				}
			}
		}
		return changes;
	}

	private List<JaxrsEndpointChangedEvent> processRootResourceMethodAddition(final JaxrsResourceMethod resourceMethod,
			final JaxrsMetamodel metamodel) {
		final JaxrsHttpMethod httpMethod = metamodel.getHttpMethod(resourceMethod.getHttpMethodAnnotation());
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		final JaxrsEndpoint endpoint = new JaxrsEndpoint(metamodel.getApplication(), httpMethod, resourceMethod);
		if (metamodel.add(endpoint)) {
			changes.add(new JaxrsEndpointChangedEvent(endpoint, ADDED));
		}
		return changes;
	}

	// FIXME: support chain of subresource locators
	private List<JaxrsEndpointChangedEvent> processSubresourceLocatorAddition(
			final JaxrsResourceMethod subresourceLocator, final JaxrsMetamodel metamodel) throws CoreException {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		final IProgressMonitor progressMonitor = new NullProgressMonitor();
		final IType returnType = subresourceLocator.getReturnType();
		if (returnType != null) {
			final ITypeHierarchy returnTypeHierarchy = JdtUtils
					.resolveTypeHierarchy(returnType, false, progressMonitor);
			final List<String> subtypesHandlers = extractHandlers(returnTypeHierarchy.getAllSubtypes(returnType));
			for (Iterator<IJaxrsResource> iterator = metamodel.getAllResources().iterator(); iterator.hasNext();) {
				JaxrsResource resource = (JaxrsResource) iterator.next();
				if (resource.isSubresource()) {
					final String resourceHandleIdentifier = resource.getJavaElement().getHandleIdentifier();
					if (resourceHandleIdentifier.equals(returnType.getHandleIdentifier())
							|| subtypesHandlers.contains(resourceHandleIdentifier)) {
						for (JaxrsResourceMethod resourceMethod : resource.getMethods().values()) {
							switch (resourceMethod.getKind()) {
							case RESOURCE_METHOD:
							case SUBRESOURCE_METHOD:
								final JaxrsHttpMethod httpMethod = metamodel.getHttpMethod(resourceMethod
										.getHttpMethodAnnotation());
								final LinkedList<JaxrsResourceMethod> resourceMethods = new LinkedList<JaxrsResourceMethod>(
										Arrays.asList(subresourceLocator, resourceMethod));
								final JaxrsEndpoint endpoint = new JaxrsEndpoint(metamodel.getApplication(),
										httpMethod, resourceMethods);
								if (metamodel.add(endpoint)) {
									changes.add(new JaxrsEndpointChangedEvent(endpoint, ADDED));
								}
							}
							break;
						}
					}
				}
			}
		}
		return changes;
	}

	private List<String> extractHandlers(final IType[] supertypes) {
		final List<String> supertypesHandlers = new ArrayList<String>();
		for (IType supertype : supertypes) {
			supertypesHandlers.add(supertype.getHandleIdentifier());
		}
		return supertypesHandlers;
	}

	private List<JaxrsEndpointChangedEvent> processChange(final JaxrsApplication application, int flags) {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		final JaxrsMetamodel metamodel = application.getMetamodel();
		if (application.equals(metamodel.getApplication())) {
			for (Iterator<JaxrsEndpoint> iterator = metamodel.getEndpoints().iterator(); iterator.hasNext();) {
				JaxrsEndpoint endpoint = iterator.next();
				if (endpoint.refresh(application)) {
					// just notify changes to the UI, no refresh required
					changes.add(new JaxrsEndpointChangedEvent(endpoint, CHANGED));
				}
			}
		}
		return changes;
	}

	private List<JaxrsEndpointChangedEvent> processChange(final JaxrsHttpMethod httpMethod, int flags) {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		for (Iterator<JaxrsEndpoint> iterator = httpMethod.getMetamodel().getEndpoints().iterator(); iterator.hasNext();) {
			JaxrsEndpoint endpoint = iterator.next();
			if (endpoint.match(httpMethod)) {
				// just notify changes to the UI, no refresh required
				changes.add(new JaxrsEndpointChangedEvent(endpoint, CHANGED));
			}
		}
		return changes;
	}

	private List<JaxrsEndpointChangedEvent> processChange(final JaxrsResource resource, int flags) throws CoreException {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		// no structural change in the resource: refresh its methods
		if ((flags & F_ELEMENT_KIND) == 0) {
			for (JaxrsResourceMethod resourceMethod : resource.getMethods().values()) {
				changes.addAll(processChange(resourceMethod, flags));
			}
		}
		// structural change : remove all endpoints associated with its methods
		// and creat new ones
		else {
			for (JaxrsResourceMethod resourceMethod : resource.getMethods().values()) {
				changes.addAll(processRemoval(resourceMethod));
				changes.addAll(processAddition(resourceMethod));
			}

		}
		return changes;
	}

	private List<JaxrsEndpointChangedEvent> processChange(final JaxrsResourceMethod changedResourceMethod, int flags)
			throws CoreException {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		if ((flags & F_ELEMENT_KIND) > 0) {
			// remove endpoints using this resoureMethod:
			for (Iterator<JaxrsEndpoint> iterator = changedResourceMethod.getMetamodel().getEndpoints().iterator(); iterator
					.hasNext();) {
				JaxrsEndpoint endpoint = iterator.next();
				if (endpoint.match(changedResourceMethod)) {
					iterator.remove();
					changes.add(new JaxrsEndpointChangedEvent(endpoint, REMOVED));
				}
			}
			// create endpoints using this resourceMethod:
			changes.addAll(processAddition(changedResourceMethod));
		} else if (changedResourceMethod.getKind() == EnumKind.SUBRESOURCE_LOCATOR
				&& (flags & F_METHOD_RETURN_TYPE) > 0) {

			for (Iterator<JaxrsEndpoint> endpointIterator = changedResourceMethod.getMetamodel().getEndpoints()
					.iterator(); endpointIterator.hasNext();) {
				JaxrsEndpoint endpoint = endpointIterator.next();
				if (endpoint.match(changedResourceMethod)) {
					for (Iterator<IJaxrsResourceMethod> resourceMethodIterator = endpoint.getResourceMethods()
							.iterator(); resourceMethodIterator.hasNext();) {
						JaxrsResourceMethod endpointResourceMethod = (JaxrsResourceMethod) resourceMethodIterator
								.next();
						if (endpointResourceMethod.equals(changedResourceMethod)) {
							IType returnType = endpointResourceMethod.getReturnType();
							JaxrsResourceMethod nextEndpointResourceMethod = (JaxrsResourceMethod) resourceMethodIterator
									.next();
							IType nextEndpointResourceMethodType = (IType) nextEndpointResourceMethod.getJavaElement()
									.getParent();
							boolean match = JdtUtils.isTypeOrSuperType(returnType, nextEndpointResourceMethodType);
							if (!match) {
								endpointIterator.remove();
								changes.add(new JaxrsEndpointChangedEvent(endpoint, REMOVED));
								// stop the iteration over the resourceMethods
								// of the current endpoint, in order to move to
								// the next one.
								break;
							}
						}
					}
				}
			}
			// create missing endpoints using this resourceMethod:
			changes.addAll(processAddition(changedResourceMethod));
		}
		// simply refresh all endpoints using this resourceMethod
		else {
			for (Iterator<JaxrsEndpoint> iterator = changedResourceMethod.getMetamodel().getEndpoints().iterator(); iterator
					.hasNext();) {
				JaxrsEndpoint endpoint = iterator.next();
				if (endpoint.match(changedResourceMethod)) {
					// refresh the endpoint after the changes
					endpoint.refresh(changedResourceMethod, flags);
					// check if the endpoint is still valid:
					changes.add(new JaxrsEndpointChangedEvent(endpoint, CHANGED));
				}
			}
		}
		return changes;
	}

	private List<JaxrsEndpointChangedEvent> processRemoval(final JaxrsHttpMethod httpMethod) {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		for (Iterator<JaxrsEndpoint> iterator = httpMethod.getMetamodel().getEndpoints().iterator(); iterator.hasNext();) {
			JaxrsEndpoint endpoint = iterator.next();
			if (endpoint.match(httpMethod)) {
				iterator.remove();
				changes.add(new JaxrsEndpointChangedEvent(endpoint, REMOVED));
			}
		}
		return changes;
	}

	private List<JaxrsEndpointChangedEvent> processRemoval(final JaxrsApplication application) {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		final JaxrsMetamodel metamodel = application.getMetamodel();
		for (Iterator<JaxrsEndpoint> iterator = metamodel.getEndpoints().iterator(); iterator
				.hasNext();) {
			JaxrsEndpoint endpoint = iterator.next();
			if (endpoint.refresh(metamodel.getApplication())) {
				changes.add(new JaxrsEndpointChangedEvent(endpoint, CHANGED));
			}
		}
		return changes;
	}

	private List<JaxrsEndpointChangedEvent> processRemoval(final JaxrsResource resource) {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		for (JaxrsResourceMethod resourceMethod : resource.getMethods().values()) {
			changes.addAll(processRemoval(resourceMethod));
		}
		return changes;
	}

	private List<JaxrsEndpointChangedEvent> processRemoval(final JaxrsResourceMethod resourceMethod) {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		for (Iterator<JaxrsEndpoint> iterator = resourceMethod.getMetamodel().getEndpoints().iterator(); iterator
				.hasNext();) {
			JaxrsEndpoint endpoint = (JaxrsEndpoint) iterator.next();
			if (endpoint.match(resourceMethod)) {
				iterator.remove();
				changes.add(new JaxrsEndpointChangedEvent(endpoint, REMOVED));
			}
		}
		return changes;
	}
}
