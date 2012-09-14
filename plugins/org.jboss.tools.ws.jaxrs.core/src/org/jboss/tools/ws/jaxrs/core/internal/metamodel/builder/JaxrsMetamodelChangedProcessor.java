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
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_ELEMENT_KIND;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_METHOD_RETURN_TYPE;

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
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.JaxrsEndpointDelta;
import org.jboss.tools.ws.jaxrs.core.metamodel.JaxrsMetamodelDelta;

public class JaxrsMetamodelChangedProcessor {

	public List<JaxrsMetamodelDelta> processAffectedMetamodels(List<JaxrsMetamodelDelta> affectedMetamodels,
			IProgressMonitor progressMonitor) throws CoreException {
		for (JaxrsMetamodelDelta affectedMetamodel : affectedMetamodels) {
			processAffectedMetamodel(affectedMetamodel, progressMonitor);
		}
		return affectedMetamodels;
	}

	/**
	 * Processes the affected elements from the given metamodelDelta to enrich that later one with affected endpoints.
	 * 
	 * @param metamodelDelta
	 * @param progressMonitor
	 * @return the given metamodelDelta completed with affected endpoints.
	 */
	public JaxrsMetamodelDelta processAffectedMetamodel(final JaxrsMetamodelDelta metamodelDelta,
			IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementDelta> affectedElements = metamodelDelta.getAffectedElements();
		Collections.sort(affectedElements);
		try {
			Logger.debug("Processing {} JAX-RS element change(s)...", affectedElements.size());
			for (Iterator<JaxrsElementDelta> iterator = affectedElements.iterator(); iterator.hasNext();) {
				JaxrsElementDelta jaxrsElementChange = iterator.next();
				Logger.debug("Processing {}", jaxrsElementChange);
				final List<JaxrsEndpointDelta> affectedEndpoints = processEvent(jaxrsElementChange);
				for (JaxrsEndpointDelta endpointDelta : affectedEndpoints) {
					Logger.debug("--> {}", endpointDelta);
				}
				if (!affectedEndpoints.isEmpty()) {
					metamodelDelta.addAffectedEndpoint(affectedEndpoints);
				}
			}
		} finally {
			Logger.debug("Done processing JAX-RS element change(s).");
		}
		return metamodelDelta;
	}

	@SuppressWarnings("incomplete-switch")
	private List<JaxrsEndpointDelta> processEvent(final JaxrsElementDelta event) throws CoreException {
		final JaxrsBaseElement element = event.getElement();
		final EnumElementKind elementKind = element.getElementKind();
		final int flags = event.getFlags();
		switch (event.getDeltaKind()) {
		case ADDED:
			switch (elementKind) {
			case APPLICATION:
				return processAddition((IJaxrsApplication) element);
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
				return processChange((IJaxrsApplication) element, flags);
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
				return processRemoval((IJaxrsApplication) element);
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
	private List<JaxrsEndpointDelta> processAddition(final IJaxrsApplication application) {
		final List<JaxrsEndpointDelta> changes = new ArrayList<JaxrsEndpointDelta>();
		final JaxrsMetamodel metamodel = (JaxrsMetamodel) application.getMetamodel();
		// if the given application becomes the used application in the metamodel
		if (application.equals(metamodel.getApplication())) {
			for (Iterator<JaxrsEndpoint> iterator = metamodel.getEndpoints().iterator(); iterator.hasNext();) {
				JaxrsEndpoint endpoint = iterator.next();
				if (endpoint.refresh(application)) {
					changes.add(new JaxrsEndpointDelta(endpoint, CHANGED));
				}
			}
		}
		return changes;
	}

	private List<JaxrsEndpointDelta> processAddition(final JaxrsHttpMethod httpMethod) {
		return Collections.emptyList();
	}

	private List<JaxrsEndpointDelta> processAddition(final JaxrsResource resource) throws CoreException {
		final List<JaxrsEndpointDelta> changes = new ArrayList<JaxrsEndpointDelta>();
		for (JaxrsResourceMethod resourceMethod : resource.getMethods().values()) {
			changes.addAll(processAddition(resourceMethod));
		}
		return changes;
	}

	@SuppressWarnings("incomplete-switch")
	private List<JaxrsEndpointDelta> processAddition(final JaxrsResourceMethod resourceMethod) throws CoreException {
		final List<JaxrsEndpointDelta> changes = new ArrayList<JaxrsEndpointDelta>();
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

	private List<JaxrsEndpointDelta> processSubresourceMethodAddition(final JaxrsResourceMethod resourceMethod,
			final JaxrsMetamodel metamodel) throws CoreException {
		final JaxrsHttpMethod httpMethod = (JaxrsHttpMethod) metamodel.getHttpMethod(resourceMethod
				.getHttpMethodAnnotation());
		final List<JaxrsEndpointDelta> changes = new ArrayList<JaxrsEndpointDelta>();
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
							final JaxrsEndpoint endpoint = new JaxrsEndpoint(metamodel, httpMethod, resourceMethods);
							if (metamodel.add(endpoint)) {
								changes.add(new JaxrsEndpointDelta(endpoint, ADDED));
							}
						}
					}
				}
			}
		}
		return changes;
	}

	private List<JaxrsEndpointDelta> processRootResourceMethodAddition(final JaxrsResourceMethod resourceMethod,
			final JaxrsMetamodel metamodel) {
		final JaxrsHttpMethod httpMethod = metamodel.getHttpMethod(resourceMethod.getHttpMethodAnnotation());
		final List<JaxrsEndpointDelta> changes = new ArrayList<JaxrsEndpointDelta>();
		final JaxrsEndpoint endpoint = new JaxrsEndpoint(metamodel, httpMethod, resourceMethod);
		if (metamodel.add(endpoint)) {
			changes.add(new JaxrsEndpointDelta(endpoint, ADDED));
		}
		return changes;
	}

	// FIXME: support chain of subresource locators
	@SuppressWarnings("incomplete-switch")
	private List<JaxrsEndpointDelta> processSubresourceLocatorAddition(final JaxrsResourceMethod subresourceLocator,
			final JaxrsMetamodel metamodel) throws CoreException {
		final List<JaxrsEndpointDelta> changes = new ArrayList<JaxrsEndpointDelta>();
		final IProgressMonitor progressMonitor = new NullProgressMonitor();
		final IType returnType = subresourceLocator.getReturnType();
		if (returnType != null) {
			final ITypeHierarchy returnTypeHierarchy = JdtUtils
					.resolveTypeHierarchy(returnType, false, progressMonitor);
			if (returnTypeHierarchy != null) {
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
									final JaxrsEndpoint endpoint = new JaxrsEndpoint(metamodel, httpMethod,
											resourceMethods);
									if (metamodel.add(endpoint)) {
										changes.add(new JaxrsEndpointDelta(endpoint, ADDED));
									}
								}
								break;
							}
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

	private List<JaxrsEndpointDelta> processChange(final IJaxrsApplication application, int flags) {
		final List<JaxrsEndpointDelta> changes = new ArrayList<JaxrsEndpointDelta>();
		final JaxrsMetamodel metamodel = (JaxrsMetamodel) application.getMetamodel();
		if (application.equals(metamodel.getApplication())) {
			for (Iterator<JaxrsEndpoint> iterator = metamodel.getEndpoints().iterator(); iterator.hasNext();) {
				JaxrsEndpoint endpoint = iterator.next();
				if (endpoint.refresh(application)) {
					// just notify changes to the UI, no refresh required
					changes.add(new JaxrsEndpointDelta(endpoint, CHANGED));
				}
			}
		}
		return changes;
	}

	private List<JaxrsEndpointDelta> processChange(final JaxrsHttpMethod httpMethod, int flags) {
		final List<JaxrsEndpointDelta> changes = new ArrayList<JaxrsEndpointDelta>();
		for (Iterator<JaxrsEndpoint> iterator = httpMethod.getMetamodel().getEndpoints().iterator(); iterator.hasNext();) {
			JaxrsEndpoint endpoint = iterator.next();
			if (endpoint.match(httpMethod)) {
				// just notify changes to the UI, no refresh required
				changes.add(new JaxrsEndpointDelta(endpoint, CHANGED));
			}
		}
		return changes;
	}

	private List<JaxrsEndpointDelta> processChange(final JaxrsResource resource, int flags) throws CoreException {
		final List<JaxrsEndpointDelta> changes = new ArrayList<JaxrsEndpointDelta>();
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

	private List<JaxrsEndpointDelta> processChange(final JaxrsResourceMethod changedResourceMethod, int flags)
			throws CoreException {
		final List<JaxrsEndpointDelta> changes = new ArrayList<JaxrsEndpointDelta>();
		if ((flags & F_ELEMENT_KIND) > 0) {
			// remove endpoints using this resoureMethod:
			for (Iterator<JaxrsEndpoint> iterator = changedResourceMethod.getMetamodel().getEndpoints().iterator(); iterator
					.hasNext();) {
				JaxrsEndpoint endpoint = iterator.next();
				if (endpoint.match(changedResourceMethod)) {
					iterator.remove();
					changes.add(new JaxrsEndpointDelta(endpoint, REMOVED));
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
								changes.add(new JaxrsEndpointDelta(endpoint, REMOVED));
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
					changes.add(new JaxrsEndpointDelta(endpoint, CHANGED));
				}
			}
		}
		return changes;
	}

	private List<JaxrsEndpointDelta> processRemoval(final JaxrsHttpMethod httpMethod) {
		final List<JaxrsEndpointDelta> changes = new ArrayList<JaxrsEndpointDelta>();
		for (Iterator<JaxrsEndpoint> iterator = httpMethod.getMetamodel().getEndpoints().iterator(); iterator.hasNext();) {
			JaxrsEndpoint endpoint = iterator.next();
			if (endpoint.match(httpMethod)) {
				iterator.remove();
				changes.add(new JaxrsEndpointDelta(endpoint, REMOVED));
			}
		}
		return changes;
	}

	private List<JaxrsEndpointDelta> processRemoval(final IJaxrsApplication application) {
		final List<JaxrsEndpointDelta> changes = new ArrayList<JaxrsEndpointDelta>();
		final JaxrsMetamodel metamodel = (JaxrsMetamodel) application.getMetamodel();
		for (Iterator<JaxrsEndpoint> iterator = metamodel.getEndpoints().iterator(); iterator.hasNext();) {
			JaxrsEndpoint endpoint = iterator.next();
			if (endpoint.refresh(metamodel.getApplication())) {
				changes.add(new JaxrsEndpointDelta(endpoint, CHANGED));
			}
		}
		return changes;
	}

	private List<JaxrsEndpointDelta> processRemoval(final JaxrsResource resource) {
		final List<JaxrsEndpointDelta> changes = new ArrayList<JaxrsEndpointDelta>();
		for (JaxrsResourceMethod resourceMethod : resource.getMethods().values()) {
			changes.addAll(processRemoval(resourceMethod));
		}
		return changes;
	}

	private List<JaxrsEndpointDelta> processRemoval(final JaxrsResourceMethod resourceMethod) {
		final List<JaxrsEndpointDelta> changes = new ArrayList<JaxrsEndpointDelta>();
		for (Iterator<JaxrsEndpoint> iterator = resourceMethod.getMetamodel().getEndpoints().iterator(); iterator
				.hasNext();) {
			JaxrsEndpoint endpoint = (JaxrsEndpoint) iterator.next();
			if (endpoint.match(resourceMethod)) {
				iterator.remove();
				changes.add(new JaxrsEndpointDelta(endpoint, REMOVED));
			}
		}
		return changes;
	}

}
