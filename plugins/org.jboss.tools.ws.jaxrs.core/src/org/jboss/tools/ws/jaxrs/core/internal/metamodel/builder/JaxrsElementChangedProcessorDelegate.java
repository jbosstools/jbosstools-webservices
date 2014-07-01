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
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_ELEMENT_KIND;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_METHOD_RETURN_TYPE;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsEndpointFactory;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregator;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregatorField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregatorProperty;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceProperty;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.JavaElementsSearcher;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IAnnotatedSourceType;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsEndpointDelta;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;

public class JaxrsElementChangedProcessorDelegate {

	public static void processEvent(final JaxrsElementDelta event) throws CoreException {
		final IJaxrsElement element = event.getElement();
		final EnumElementCategory elementKind = element.getElementKind().getCategory();
		final Flags flags = event.getFlags();
		switch (event.getDeltaKind()) {
		case ADDED:
			switch (elementKind) {
			case APPLICATION:
				processAddition((IJaxrsApplication) element);
				break;
			case HTTP_METHOD:
				processAddition((JaxrsHttpMethod) element);
				break;
			case RESOURCE_METHOD:
				processAddition((JaxrsResourceMethod) element);
				break;
			case RESOURCE_FIELD:
				processAddition((JaxrsResourceField) element);
				break;
			case RESOURCE_PROPERTY:
				processAddition((JaxrsResourceProperty) element);
				break;
			case PARAMETER_AGGREGATOR_FIELD:
				processAddition((JaxrsParameterAggregatorField) element);
				break;
			case PARAMETER_AGGREGATOR_PROPERTY:
				processAddition((JaxrsParameterAggregatorProperty) element);
				break;
			default:
				Logger.trace("No direct impact on JAX-RS Endpoints after change on element:" + elementKind);
				break;
			}
			break;
		case CHANGED:
			switch (elementKind) {
			case APPLICATION:
				processChange((IJaxrsApplication) element, flags);
				break;
			case HTTP_METHOD:
				processChange((JaxrsHttpMethod) element, flags);
				break;
			case RESOURCE:
				processChange((JaxrsResource) element, flags);
				break;
			case RESOURCE_METHOD:
				processChange((JaxrsResourceMethod) element, flags);
				break;
			case RESOURCE_FIELD:
				processChange((JaxrsResourceField) element, flags);
				break;
			case RESOURCE_PROPERTY:
				processChange((JaxrsResourceProperty) element, flags);
				break;
			case PARAMETER_AGGREGATOR_FIELD:
				processChange((JaxrsParameterAggregatorField) element);
				break;
			case PARAMETER_AGGREGATOR_PROPERTY:
				processChange((JaxrsParameterAggregatorProperty) element);
				break;
			default:
				Logger.trace("No direct impact on JAX-RS Endpoints after change on element:" + elementKind);
				break;
			}
			break;
		case REMOVED:
			switch (elementKind) {
			case APPLICATION:
				processRemoval((IJaxrsApplication) element);
				break;
			case HTTP_METHOD:
				processRemoval((JaxrsHttpMethod) element);
				break;
			case RESOURCE_METHOD:
				processRemoval((JaxrsResourceMethod) element);
				break;
			case RESOURCE_FIELD:
				processRemoval((JaxrsResourceField) element);
				break;
			case RESOURCE_PROPERTY:
				processRemoval((JaxrsResourceProperty) element);
				break;
			case PARAMETER_AGGREGATOR_FIELD:
				processRemoval((JaxrsParameterAggregatorField) element);
				break;
			case PARAMETER_AGGREGATOR_PROPERTY:
				processRemoval((JaxrsParameterAggregatorProperty) element);
				break;
			default:
				Logger.trace("No direct impact on JAX-RS Endpoints after change on element:" + elementKind);
				break;
			}
		}
	}

	/**
	 * Process changes in the JAX-RS Metamodel when a new Application element is
	 * added. There should be only one, though...
	 * 
	 * @param application
	 * @return
	 */
	private static void processAddition(final IJaxrsApplication application) {
		final JaxrsMetamodel metamodel = (JaxrsMetamodel) application.getMetamodel();
		// if the given application becomes the used application in the
		// metamodel
		if (application.equals(metamodel.findApplication())) {
			for (Iterator<IJaxrsEndpoint> iterator = metamodel.getAllEndpoints().iterator(); iterator.hasNext();) {
				JaxrsEndpoint endpoint = (JaxrsEndpoint) iterator.next();
				endpoint.update(application);
			}
		}
	}

	private static void processAddition(final JaxrsHttpMethod httpMethod) {
		return;
	}

	private static void processAddition(final JaxrsResourceMethod resourceMethod) throws CoreException {
		final JaxrsMetamodel metamodel = (JaxrsMetamodel) resourceMethod.getMetamodel();
		final JaxrsResource resource = resourceMethod.getParentResource();
		if (resource == null) {
			Logger.warn("Found an orphan resource method: " + resourceMethod);
		} else {
			switch(resourceMethod.getElementKind()) {
			case RESOURCE_METHOD:
			case SUBRESOURCE_METHOD:
				if(resource.isRootResource()) {
					processRootResourceMethodAddition(resourceMethod);
				} else {
					processSubresourceMethodAddition(resourceMethod, metamodel);
				}
				break;
			case SUBRESOURCE_LOCATOR:
				// FIXME : support multiple levels of subresource locators
				if(resource.isRootResource()) {
					processSubresourceLocatorAddition(resourceMethod, metamodel);
				}
				break;
			default:
				break;
			}
		}
	}

	private static void processAddition(final JaxrsResourceField resourceField) throws CoreException {
		final JaxrsResource resource = resourceField.getParentResource();
		if (resource == null) {
			Logger.warn("Found an orphan resource field: " + resourceField);
		} else {
			final Set<JaxrsEndpoint> resourceEndpoints = resource.getMetamodel().findEndpoints(resource);
			final Flags flags = new Flags();
			for(Entry<String, Annotation> entry : resourceField.getAnnotations().entrySet()) {
				if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.QUERY_PARAM)) {
					flags.addFlags(JaxrsElementDelta.F_QUERY_PARAM_ANNOTATION);
				} else if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.MATRIX_PARAM)) {
					flags.addFlags(JaxrsElementDelta.F_MATRIX_PARAM_ANNOTATION);
				} else if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.PATH)) {
					flags.addFlags(JaxrsElementDelta.F_PATH_ANNOTATION);
				} else if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.BEAN_PARAM)) {
					flags.addFlags(JaxrsElementDelta.F_BEAN_PARAM_ANNOTATION);
				}
			}
			for(JaxrsEndpoint endpoint : resourceEndpoints) {
				endpoint.update(flags);
			}
		}
	}

	private static void processAddition(final JaxrsResourceProperty resourceProperty) throws CoreException {
		final JaxrsResource resource = resourceProperty.getParentResource();
		if (resource == null) {
			Logger.warn("Found an orphan resource property: " + resourceProperty);
		} else {
			final Set<JaxrsEndpoint> resourceEndpoints = resource.getMetamodel().findEndpoints(resource);
			final Flags flags = new Flags();
			for(Entry<String, Annotation> entry : resourceProperty.getAnnotations().entrySet()) {
				if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.QUERY_PARAM)) {
					flags.addFlags(JaxrsElementDelta.F_QUERY_PARAM_ANNOTATION);
				} else if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.MATRIX_PARAM)) {
					flags.addFlags(JaxrsElementDelta.F_MATRIX_PARAM_ANNOTATION);
				} else if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.PATH)) {
					flags.addFlags(JaxrsElementDelta.F_PATH_ANNOTATION);
				} else if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.BEAN_PARAM)) {
					flags.addFlags(JaxrsElementDelta.F_BEAN_PARAM_ANNOTATION);
				}
			}
			for(JaxrsEndpoint endpoint : resourceEndpoints) {
				endpoint.update(flags);
			}
		}
	}

	private static void processAddition(final JaxrsParameterAggregatorProperty resourceProperty) throws CoreException {
		final JaxrsParameterAggregator parentAggregator = resourceProperty.getParentParameterAggregator();
		if (parentAggregator == null) {
			Logger.warn("Found an orphan resource property: " + resourceProperty);
		} else {
			processCascadeChange(resourceProperty, parentAggregator);
		}
	}
	
	private static void processAddition(final JaxrsParameterAggregatorField resourceField) throws CoreException {
		final JaxrsParameterAggregator parentAggregator = resourceField.getParentParameterAggregator();
		if (parentAggregator == null) {
			Logger.warn("Found an orphan resource property: " + resourceField);
		} else {
			processCascadeChange(resourceField, parentAggregator);
		}
	}
	
	private static void processChange(final JaxrsParameterAggregatorProperty resourceProperty) throws CoreException {
		final JaxrsParameterAggregator parentAggregator = resourceProperty.getParentParameterAggregator();
		if (parentAggregator == null) {
			Logger.warn("Found an orphan resource property: " + resourceProperty);
		} else {
			processCascadeChange(resourceProperty, parentAggregator);
		}
	}
	
	private static void processChange(final JaxrsParameterAggregatorField resourceField) throws CoreException {
		final JaxrsParameterAggregator parentAggregator = resourceField.getParentParameterAggregator();
		if (parentAggregator == null) {
			Logger.warn("Found an orphan resource property: " + resourceField);
		} else {
			processCascadeChange(resourceField, parentAggregator);
		}
	}
	
	private static void processRemoval(final JaxrsParameterAggregatorProperty resourceProperty) throws CoreException {
		final JaxrsParameterAggregator parentAggregator = resourceProperty.getParentParameterAggregator();
		if (parentAggregator == null) {
			Logger.warn("Found an orphan resource property: " + resourceProperty);
		} else {
			processCascadeChange(resourceProperty, parentAggregator);
		}
	}
	
	private static void processRemoval(final JaxrsParameterAggregatorField resourceField) throws CoreException {
		final JaxrsParameterAggregator parentAggregator = resourceField.getParentParameterAggregator();
		if (parentAggregator == null) {
			Logger.warn("Found an orphan resource property: " + resourceField);
		} else {
			processCascadeChange(resourceField, parentAggregator);
		}
	}
	
	private static void processCascadeChange(final IAnnotatedSourceType parameterAggregatorChildElement, final JaxrsParameterAggregator parentAggregator) throws CoreException {
		final JaxrsMetamodel metamodel = parentAggregator.getMetamodel();
		final List<IType> knownTypes = metamodel.getAllJavaElements(IJavaElement.TYPE);
		final Set<IType> relatedTypes = JavaElementsSearcher.findRelatedTypes(parentAggregator.getJavaElement(), knownTypes, null);
		final Set<JaxrsEndpoint> resourceEndpoints = new HashSet<JaxrsEndpoint>();
		for(IType relatedType : relatedTypes) {
			resourceEndpoints.addAll(parentAggregator.getMetamodel().findEndpoints(relatedType));
		}
		
		final Flags flags = new Flags();
		for(Entry<String, Annotation> entry : parameterAggregatorChildElement.getAnnotations().entrySet()) {
			if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.QUERY_PARAM)) {
				flags.addFlags(JaxrsElementDelta.F_QUERY_PARAM_ANNOTATION);
			} else if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.MATRIX_PARAM)) {
				flags.addFlags(JaxrsElementDelta.F_MATRIX_PARAM_ANNOTATION);
			} else if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.PATH_PARAM)) {
				flags.addFlags(JaxrsElementDelta.F_PATH_PARAM_ANNOTATION);
			} else if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.BEAN_PARAM)) {
				flags.addFlags(JaxrsElementDelta.F_BEAN_PARAM_ANNOTATION);
			}
		}
		for(JaxrsEndpoint endpoint : resourceEndpoints) {
			endpoint.update(flags);
		}
	}

	private static void processRootResourceMethodAddition(final JaxrsResourceMethod resourceMethod)
			throws CoreException {
		JaxrsEndpointFactory.createEndpoints(resourceMethod);
	}

	// FIXME: support chain of subresource locators
	private static void processSubresourceLocatorAddition(final JaxrsResourceMethod subresourceLocator,
			final JaxrsMetamodel metamodel) throws CoreException {
		JaxrsEndpointFactory.createEndpointsFromSubresourceLocator(subresourceLocator);
	}
	
	private static void processSubresourceMethodAddition(final JaxrsResourceMethod resourceMethod,
			final JaxrsMetamodel metamodel) throws CoreException {
		JaxrsEndpointFactory.createEndpointsFromSubresourceMethod(resourceMethod);
	}

	private static void processChange(final IJaxrsApplication application, final Flags flags) {
		final JaxrsMetamodel metamodel = (JaxrsMetamodel) application.getMetamodel();
		if (application.equals(metamodel.findApplication())) {
			for (Iterator<IJaxrsEndpoint> iterator = metamodel.getAllEndpoints().iterator(); iterator.hasNext();) {
				JaxrsEndpoint endpoint = (JaxrsEndpoint) iterator.next();
				if (endpoint.update(application)) {
					// just notify changes to the UI, no refresh required
					new JaxrsEndpointDelta(endpoint, CHANGED);
				}
			}
		}
	}

	private static void processChange(final JaxrsHttpMethod httpMethod, final Flags flags) {
		final Set<JaxrsEndpoint> endpoints = ((JaxrsMetamodel) httpMethod.getMetamodel()).findEndpoints(httpMethod);
		for (JaxrsEndpoint endpoint : endpoints) {
			endpoint.update(httpMethod);
		}
	}

	private static void processChange(final JaxrsResource resource, final Flags flags) throws CoreException {
		// no structural change in the resource: refresh its methods
		if (!flags.hasValue(F_ELEMENT_KIND)) {
			for (JaxrsResourceMethod resourceMethod : resource.getMethods().values()) {
				processChange(resourceMethod, flags);
			}
		}
		// structural change : remove all endpoints associated with its methods
		// and create new ones
		else {
			for (JaxrsResourceMethod resourceMethod : resource.getMethods().values()) {
				processRemoval(resourceMethod);
				processAddition(resourceMethod);
			}
		}
	}

	private static void processChange(final JaxrsResourceMethod changedResourceMethod, final Flags flags) throws CoreException {
		final JaxrsMetamodel metamodel = changedResourceMethod.getMetamodel();
		if (flags.hasValue(F_ELEMENT_KIND)
				|| (changedResourceMethod.getElementKind() == EnumElementKind.SUBRESOURCE_LOCATOR && flags
						.hasValue(F_METHOD_RETURN_TYPE))) {
			// remove endpoints using this resoureMethod:
			metamodel.removeEndpoints(changedResourceMethod);
			// create endpoints using this resourceMethod:
			processAddition(changedResourceMethod);
		}
		// simply refresh all endpoints using this resourceMethod
		else {
			final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(changedResourceMethod);
			for (JaxrsEndpoint endpoint : endpoints) {
				endpoint.update(flags);
			}
		}
	}

	private static void processChange(final JaxrsResourceField changedResourceField, final Flags flags) throws CoreException {
		final JaxrsMetamodel metamodel = changedResourceField.getMetamodel();
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(changedResourceField.getParentResource());
		for (JaxrsEndpoint endpoint : endpoints) {
			endpoint.update(flags);
		}
	}
	
	private static void processChange(final JaxrsResourceProperty changedResourceProperty, final Flags flags) throws CoreException {
		final JaxrsMetamodel metamodel = changedResourceProperty.getMetamodel();
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(changedResourceProperty.getParentResource());
		for (JaxrsEndpoint endpoint : endpoints) {
			endpoint.update(flags);
		}
	}
	
	private static void processRemoval(final JaxrsHttpMethod httpMethod) {
		httpMethod.getMetamodel().removeEndpoints(httpMethod);
	}

	private static void processRemoval(final IJaxrsApplication application) {
		final Collection<JaxrsEndpoint> endpoints = ((JaxrsMetamodel) application.getMetamodel()).findEndpoints(application);
		for (JaxrsEndpoint endpoint : endpoints) {
			endpoint.remove(application);
		}
	}

	private static void processRemoval(final JaxrsResourceMethod resourceMethod) {
		final JaxrsMetamodel metamodel = resourceMethod.getMetamodel();
		if(metamodel != null) {
			metamodel.removeEndpoints(resourceMethod);
		}
	}

	private static void processRemoval(final JaxrsResourceField resourceField) throws CoreException {
		final JaxrsMetamodel metamodel = resourceField.getMetamodel();
		if(metamodel != null) {
			final Collection<JaxrsEndpoint> affectedEndpoints = metamodel.findEndpoints(resourceField.getParentResource());
			final Flags flags = new Flags();
			for(Entry<String, Annotation> entry : resourceField.getAnnotations().entrySet()) {
				if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.QUERY_PARAM)) {
					flags.addFlags(JaxrsElementDelta.F_QUERY_PARAM_ANNOTATION);
				} else if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.MATRIX_PARAM)) {
					flags.addFlags(JaxrsElementDelta.F_MATRIX_PARAM_ANNOTATION);
				} else if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.BEAN_PARAM)) {
					flags.addFlags(JaxrsElementDelta.F_BEAN_PARAM_ANNOTATION);
				} 
			}
			if(flags.hasValue()) {
				for(JaxrsEndpoint endpoint : affectedEndpoints) {
					endpoint.update(flags);
				}
			}
		}
	}

	private static void processRemoval(final JaxrsResourceProperty resourceProperty) throws CoreException {
		final JaxrsMetamodel metamodel = resourceProperty.getMetamodel();
		if(metamodel != null) {
			final Collection<JaxrsEndpoint> affectedEndpoints = metamodel.findEndpoints(resourceProperty.getParentResource());
			final Flags flags = new Flags();
			for(Entry<String, Annotation> entry : resourceProperty.getAnnotations().entrySet()) {
				if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.QUERY_PARAM)) {
					flags.addFlags(JaxrsElementDelta.F_QUERY_PARAM_ANNOTATION);
				} else if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.MATRIX_PARAM)) {
					flags.addFlags(JaxrsElementDelta.F_MATRIX_PARAM_ANNOTATION);
				} else if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.BEAN_PARAM)) {
					flags.addFlags(JaxrsElementDelta.F_BEAN_PARAM_ANNOTATION);
				} 
			}
			if(flags.hasValue()) {
				for(JaxrsEndpoint endpoint : affectedEndpoints) {
					endpoint.update(flags);
				}
			}
		}
	}

}
