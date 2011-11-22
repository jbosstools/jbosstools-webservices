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
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsEndpointChangedEvent;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResourceMethod;

public class JaxrsElementChangedProcessor {

	public List<JaxrsEndpointChangedEvent> processEvents(final List<JaxrsElementChangedEvent> jaxrsElementChanges,
			IProgressMonitor progressMonitor) {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		try {
			for (JaxrsElementChangedEvent jaxrsElementChange : jaxrsElementChanges) {
				Logger.debug("Processing {}", jaxrsElementChange);
				final List<JaxrsEndpointChangedEvent> endpointChanges = processEvent(jaxrsElementChange);
				for (IJaxrsEndpointChangedEvent endpointChange : endpointChanges) {
					Logger.debug("--> {}", endpointChange);
				}
				changes.addAll(endpointChanges);
			}
			Logger.debug("Done processing JAX-RS change(s).");
		} catch (CoreException e) {
			Logger.error("Failed to process JAX-RS element changes", e);
		}
		return changes;
	}

	private List<JaxrsEndpointChangedEvent> processEvent(final JaxrsElementChangedEvent event) throws CoreException {
		Logger.debug("Processing JAX-RS change {}", event);
		final IJaxrsElement<?> element = event.getElement();
		final EnumElementKind elementKind = element.getElementKind();
		final int flags = event.getFlags();
		switch (event.getDeltaKind()) {
		case ADDED:
			switch (elementKind) {
			case HTTP_METHOD:
				return processAddition((IJaxrsHttpMethod) element);
			case RESOURCE:
				return processAddition((IJaxrsResource) element);
			case RESOURCE_METHOD:
				return processAddition((IJaxrsResourceMethod) element);
			}
			break;
		case CHANGED:
			switch (elementKind) {
			case HTTP_METHOD:
				return processChange((IJaxrsHttpMethod) element, flags);
			case RESOURCE:
				return processChange((IJaxrsResource) element, flags);
			case RESOURCE_METHOD:
				return processChange((IJaxrsResourceMethod) element, flags);
			}
			break;
		case REMOVED:
			switch (elementKind) {
			case HTTP_METHOD:
				return processRemoval((IJaxrsHttpMethod) element);
			case RESOURCE:
				return processRemoval((IJaxrsResource) element);
			case RESOURCE_METHOD:
				return processRemoval((IJaxrsResourceMethod) element);
			}
			break;
		}
		return Collections.emptyList();
	}

	private List<JaxrsEndpointChangedEvent> processAddition(final IJaxrsHttpMethod httpMethod) {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		return changes;
	}

	private List<JaxrsEndpointChangedEvent> processAddition(final IJaxrsResource resource) throws CoreException {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		for (IJaxrsResourceMethod resourceMethod : resource.getAllMethods()) {
			changes.addAll(processAddition(resourceMethod));
		}
		return changes;
	}

	private List<JaxrsEndpointChangedEvent> processAddition(final IJaxrsResourceMethod resourceMethod)
			throws CoreException {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		final JaxrsMetamodel metamodel = (JaxrsMetamodel) resourceMethod.getMetamodel();
		final IJaxrsResource resource = resourceMethod.getParentResource();
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

	private List<JaxrsEndpointChangedEvent> processSubresourceMethodAddition(final IJaxrsResourceMethod resourceMethod,
			final JaxrsMetamodel metamodel) throws CoreException {
		final IJaxrsHttpMethod httpMethod = metamodel.getHttpMethod(resourceMethod.getHttpMethodAnnotation());
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		final IJaxrsResource resource = resourceMethod.getParentResource();
		final IProgressMonitor progressMonitor = new NullProgressMonitor();
		final IType resourceType = resource.getJavaElement();
		final ITypeHierarchy returnTypeHierarchy = JdtUtils.resolveTypeHierarchy(resourceType, false, progressMonitor);
		final List<String> supertypesHandlers = extractHandlers(returnTypeHierarchy.getAllSupertypes(resourceType));
		for (IJaxrsResource otherResource : metamodel.getAllResources()) {
			if (otherResource.isRootResource()) {
				for (IJaxrsResourceMethod otherResourceMethod : otherResource.getAllMethods()) {
					if (otherResourceMethod.getKind() == EnumKind.SUBRESOURCE_LOCATOR) {
						final String returnTypeHandler = (otherResourceMethod.getReturnType() != null) ? otherResourceMethod
								.getReturnType().getHandleIdentifier() : null;
						if (returnTypeHandler != null && supertypesHandlers.contains(returnTypeHandler)) {
							final LinkedList<IJaxrsResourceMethod> resourceMethods = new LinkedList<IJaxrsResourceMethod>(
									Arrays.asList(otherResourceMethod, resourceMethod));
							final IJaxrsEndpoint endpoint = new JaxrsEndpoint(httpMethod, resourceMethods);
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

	private List<JaxrsEndpointChangedEvent> processRootResourceMethodAddition(
			final IJaxrsResourceMethod resourceMethod, final JaxrsMetamodel metamodel) {
		final IJaxrsHttpMethod httpMethod = metamodel.getHttpMethod(resourceMethod.getHttpMethodAnnotation());
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		final IJaxrsEndpoint endpoint = new JaxrsEndpoint(httpMethod, resourceMethod);
		if (metamodel.add(endpoint)) {
			changes.add(new JaxrsEndpointChangedEvent(endpoint, ADDED));
		}
		return changes;
	}

	// FIXME: support chain of subresource locators
	private List<JaxrsEndpointChangedEvent> processSubresourceLocatorAddition(
			final IJaxrsResourceMethod subresourceLocator, final JaxrsMetamodel metamodel) throws CoreException {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		final IProgressMonitor progressMonitor = new NullProgressMonitor();
		final IType returnType = subresourceLocator.getReturnType();
		if (returnType != null) {
			final ITypeHierarchy returnTypeHierarchy = JdtUtils
					.resolveTypeHierarchy(returnType, false, progressMonitor);
			final List<String> subtypesHandlers = extractHandlers(returnTypeHierarchy.getAllSubtypes(returnType));
			for (IJaxrsResource resource : metamodel.getAllResources()) {
				if (resource.isSubresource()) {
					final String resourceHandleIdentifier = resource.getJavaElement().getHandleIdentifier();
					if (resourceHandleIdentifier.equals(returnType.getHandleIdentifier())
							|| subtypesHandlers.contains(resourceHandleIdentifier)) {
						for (IJaxrsResourceMethod resourceMethod : resource.getAllMethods()) {
							switch (resourceMethod.getKind()) {
							case RESOURCE_METHOD:
							case SUBRESOURCE_METHOD:
								final IJaxrsHttpMethod httpMethod = metamodel.getHttpMethod(resourceMethod
										.getHttpMethodAnnotation());
								final LinkedList<IJaxrsResourceMethod> resourceMethods = new LinkedList<IJaxrsResourceMethod>(
										Arrays.asList(subresourceLocator, resourceMethod));
								final IJaxrsEndpoint endpoint = new JaxrsEndpoint(httpMethod, resourceMethods);
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

	private List<JaxrsEndpointChangedEvent> processChange(final IJaxrsHttpMethod httpMethod, int flags) {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		for (Iterator<IJaxrsEndpoint> iterator = httpMethod.getMetamodel().getAllEndpoints().iterator(); iterator
				.hasNext();) {
			JaxrsEndpoint endpoint = (JaxrsEndpoint) iterator.next();
			if (endpoint.match(httpMethod)) {
				// just notify changes to the UI, no refresh required
				changes.add(new JaxrsEndpointChangedEvent(endpoint, CHANGED));
			}
		}
		return changes;
	}

	private List<JaxrsEndpointChangedEvent> processChange(final IJaxrsResource resource, int flags)
			throws CoreException {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		// no structural change in the resource: refresh its methods
		if ((flags & F_ELEMENT_KIND) == 0) {
			for (IJaxrsResourceMethod resourceMethod : resource.getAllMethods()) {
				changes.addAll(processChange(resourceMethod, flags));
			}
		}
		// structural change : remove all endpoints associated with its methods
		// and creat new ones
		else {
			for (IJaxrsResourceMethod resourceMethod : resource.getAllMethods()) {
				changes.addAll(processRemoval(resourceMethod));
				changes.addAll(processAddition(resourceMethod));
			}

		}
		return changes;
	}

	private List<JaxrsEndpointChangedEvent> processChange(final IJaxrsResourceMethod changedResourceMethod, int flags)
			throws CoreException {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		if ((flags & F_ELEMENT_KIND) > 0) {
			// remove endpoints using this resoureMethod:
			for (Iterator<IJaxrsEndpoint> iterator = changedResourceMethod.getMetamodel().getAllEndpoints().iterator(); iterator
					.hasNext();) {
				JaxrsEndpoint endpoint = (JaxrsEndpoint) iterator.next();
				if (endpoint.match(changedResourceMethod)) {
					iterator.remove();
					changes.add(new JaxrsEndpointChangedEvent(endpoint, REMOVED));
				}
			}
			// create endpoints using this resourceMethod:
			changes.addAll(processAddition(changedResourceMethod));
		} else if (changedResourceMethod.getKind() == EnumKind.SUBRESOURCE_LOCATOR
				&& (flags & F_METHOD_RETURN_TYPE) > 0) {

			for (Iterator<IJaxrsEndpoint> endpointIterator = changedResourceMethod.getMetamodel().getAllEndpoints()
					.iterator(); endpointIterator.hasNext();) {
				JaxrsEndpoint endpoint = (JaxrsEndpoint) endpointIterator.next();
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
			for (Iterator<IJaxrsEndpoint> iterator = changedResourceMethod.getMetamodel().getAllEndpoints().iterator(); iterator
					.hasNext();) {
				JaxrsEndpoint endpoint = (JaxrsEndpoint) iterator.next();
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

	private List<JaxrsEndpointChangedEvent> processRemoval(final IJaxrsHttpMethod httpMethod) {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		for (Iterator<IJaxrsEndpoint> iterator = httpMethod.getMetamodel().getAllEndpoints().iterator(); iterator
				.hasNext();) {
			JaxrsEndpoint endpoint = (JaxrsEndpoint) iterator.next();
			if (endpoint.match(httpMethod)) {
				iterator.remove();
				changes.add(new JaxrsEndpointChangedEvent(endpoint, REMOVED));
			}
		}
		return changes;
	}

	private List<JaxrsEndpointChangedEvent> processRemoval(final IJaxrsResource resource) {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		for (IJaxrsResourceMethod resourceMethod : resource.getAllMethods()) {
			changes.addAll(processRemoval(resourceMethod));
		}
		return changes;
	}

	private List<JaxrsEndpointChangedEvent> processRemoval(final IJaxrsResourceMethod resourceMethod) {
		final List<JaxrsEndpointChangedEvent> changes = new ArrayList<JaxrsEndpointChangedEvent>();
		for (Iterator<IJaxrsEndpoint> iterator = resourceMethod.getMetamodel().getAllEndpoints().iterator(); iterator
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
