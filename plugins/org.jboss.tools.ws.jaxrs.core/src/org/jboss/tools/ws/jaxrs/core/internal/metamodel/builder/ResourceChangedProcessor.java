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
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_FINE_GRAINED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsElementFactory;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.JaxrsAnnotationsScanner;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.JaxrsMetamodelLocator;

public class ResourceChangedProcessor {

	private final JaxrsElementFactory factory = new JaxrsElementFactory();

	public List<JaxrsElementChangedEvent> processEvents(final List<ResourceChangedEvent> events,
			final IProgressMonitor progressMonitor) {
		final List<JaxrsElementChangedEvent> results = new ArrayList<JaxrsElementChangedEvent>();
		try {
			progressMonitor.beginTask("Processing Resource " + events.size() + " change(s)...", events.size());
			Logger.debug("Processing {} Resource change(s)...", events.size());
			for (ResourceChangedEvent event : events) {
				results.addAll(processEvent(event, progressMonitor));
				progressMonitor.worked(1);
			}

		} catch (CoreException e) {
			Logger.error("Failed while processing Resource results", e);
			results.clear();
		} finally {
			progressMonitor.done();
			Logger.debug("Done processing Resource results.");

		}
		return results;
	}

	public List<JaxrsElementChangedEvent> processEvent(ResourceChangedEvent event, IProgressMonitor progressMonitor)
			throws CoreException {
		Logger.debug("Processing {}", event);
		final List<JaxrsElementChangedEvent> results = new ArrayList<JaxrsElementChangedEvent>();
		final IResource resource = event.getResource();
		final IJavaElement scope = JavaCore.create(resource);
		if (scope != null) {
			final JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(scope.getJavaProject());
			final int deltaKind = event.getDeltaKind();
			switch (deltaKind) {
			case ADDED:
			case CHANGED:
				results.addAll(processHttpMethodChangesOnScopeAdditionOrChange(scope, metamodel, progressMonitor));
				results.addAll(processResourceChangesOnScopeAdditionOrChange(scope, metamodel, deltaKind,
						progressMonitor));
				break;
			case REMOVED:
				results.addAll(processHttpMethodChangesOnScopeRemoval(scope, metamodel, progressMonitor));
				results.addAll(processResourceChangesOnScopeRemoval(scope, metamodel, progressMonitor));
				break;
			}
		}

		return results;
	}

	private List<JaxrsElementChangedEvent> processHttpMethodChangesOnScopeAdditionOrChange(IJavaElement scope,
			JaxrsMetamodel metamodel, IProgressMonitor progressMonitor) throws JavaModelException, CoreException {
		final List<JaxrsElementChangedEvent> results = new ArrayList<JaxrsElementChangedEvent>();
		final List<JaxrsElementChangedEvent> changes = preprocessHttpMethodChangesOnScopeAdditionOrChange(scope,
				metamodel, progressMonitor);
		for (JaxrsElementChangedEvent change : changes) {
			results.addAll(postProcessHttpMethod(change, progressMonitor));
		}
		return results;
	}

	private List<JaxrsElementChangedEvent> processResourceChangesOnScopeAdditionOrChange(IJavaElement scope,
			JaxrsMetamodel metamodel, int deltaKind, IProgressMonitor progressMonitor) throws JavaModelException,
			CoreException {
		final List<JaxrsElementChangedEvent> results = new ArrayList<JaxrsElementChangedEvent>();
		final List<JaxrsElementChangedEvent> changes = preprocessResourceChangesOnScopeAdditionOrChange(scope,
				metamodel, progressMonitor);
		for (JaxrsElementChangedEvent change : changes) {
			results.addAll(postProcessResource(change, progressMonitor));
		}
		return results;
	}

	private List<JaxrsElementChangedEvent> processResourceChangesOnScopeRemoval(IJavaElement scope,
			JaxrsMetamodel metamodel, IProgressMonitor progressMonitor) throws JavaModelException, CoreException {
		final List<JaxrsElementChangedEvent> results = new ArrayList<JaxrsElementChangedEvent>();
		final List<JaxrsElementChangedEvent> changes = preprocessHttpMethodChangesOnScopeRemoval(scope, metamodel,
				progressMonitor);
		for (JaxrsElementChangedEvent change : changes) {
			results.addAll(postProcessHttpMethod(change, progressMonitor));
		}
		return results;
	}

	private List<JaxrsElementChangedEvent> processHttpMethodChangesOnScopeRemoval(IJavaElement scope,
			JaxrsMetamodel metamodel, IProgressMonitor progressMonitor) throws JavaModelException, CoreException {
		final List<JaxrsElementChangedEvent> results = new ArrayList<JaxrsElementChangedEvent>();
		final List<JaxrsElementChangedEvent> changes = preprocessResourceChangesOnScopeRemoval(scope, metamodel,
				progressMonitor);
		for (JaxrsElementChangedEvent change : changes) {
			results.addAll(postProcessResource(change, progressMonitor));
		}
		return results;
	}

	/**
	 * See if HttpMethods exist in the given scope. The exact kind of the
	 * {@link JaxrsElementChangedEvent} event is not determined at this stage,
	 * it's the responsibility of the
	 * {@link ResourceChangedProcessor#postProcessHttpMethod(JaxrsElementChangedEvent, IProgressMonitor)}
	 * method.
	 * 
	 * @param scope
	 * @param metamodel
	 * @param progressMonitor
	 * @return events containing the new HttpMethods (already added to the
	 *         metamodel)
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	private List<JaxrsElementChangedEvent> preprocessHttpMethodChangesOnScopeAdditionOrChange(final IJavaElement scope,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException,
			JavaModelException {
		final List<JaxrsElementChangedEvent> results = new ArrayList<JaxrsElementChangedEvent>();
		// see if there may be elements to add/change from the given scope
		final List<JaxrsHttpMethod> matchingHttpMethods = new ArrayList<JaxrsHttpMethod>();
		final List<IType> matchingHttpMethodTypes = JaxrsAnnotationsScanner.findHTTPMethodTypes(scope, progressMonitor);
		for (IType matchingHttpMethodType : matchingHttpMethodTypes) {
			final CompilationUnit ast = JdtUtils.parse(matchingHttpMethodType, progressMonitor);
			final JaxrsHttpMethod matchingHttpMethod = factory.createHttpMethod(matchingHttpMethodType, ast, metamodel);
			if (matchingHttpMethod != null) {
				matchingHttpMethods.add(matchingHttpMethod);
			}
		}
		// retrieve the existing elements from the Metamodel
		final List<JaxrsHttpMethod> existingHttpMethods = metamodel.getElements(scope, JaxrsHttpMethod.class);
		// compute the differences, with a 'fuzzy' case when the kind is
		// 'changed'
		final Collection<JaxrsHttpMethod> addedHttpMethods = CollectionUtils.difference(matchingHttpMethods,
				existingHttpMethods);
		for (JaxrsHttpMethod httpMethod : addedHttpMethods) {
			results.add(new JaxrsElementChangedEvent(httpMethod, ADDED));
		}
		final Collection<JaxrsHttpMethod> changedHttpMethods = CollectionUtils.intersection(matchingHttpMethods,
				existingHttpMethods);
		for (JaxrsHttpMethod httpMethod : changedHttpMethods) {
			results.add(new JaxrsElementChangedEvent(httpMethod, CHANGED, F_FINE_GRAINED));
		}
		final Collection<JaxrsHttpMethod> removedHttpMethods = CollectionUtils.difference(existingHttpMethods,
				matchingHttpMethods);
		for (JaxrsHttpMethod httpMethod : removedHttpMethods) {
			results.add(new JaxrsElementChangedEvent(httpMethod, REMOVED));
		}
		return results;
	}

	/**
	 * See if HttpMethods existed in the given scope. These elements can only be
	 * marked as removed.
	 * 
	 * @param scope
	 * @param metamodel
	 * @param progressMonitor
	 * @return events containing the new HttpMethods (already added to the
	 *         metamodel)
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	private List<JaxrsElementChangedEvent> preprocessHttpMethodChangesOnScopeRemoval(final IJavaElement scope,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException,
			JavaModelException {
		final List<JaxrsElementChangedEvent> results = new ArrayList<JaxrsElementChangedEvent>();
		// retrieve the existing elements from the Metamodel
		final List<JaxrsHttpMethod> existingHttpMethods = metamodel.getElements(scope, JaxrsHttpMethod.class);
		for (JaxrsHttpMethod httpMethod : existingHttpMethods) {
			results.add(new JaxrsElementChangedEvent(httpMethod, REMOVED));
		}
		return results;
	}

	/**
	 * See if new JAX-RS Resources exist in the given scope. The exact kind of
	 * the {@link JaxrsElementChangedEvent} event is not determined at this
	 * stage, it's the responsibility of the
	 * {@link ResourceChangedProcessor#postProcessResource(JaxrsElementChangedEvent, IProgressMonitor)}
	 * method.
	 * 
	 * @param scope
	 * @param metamodel
	 * @param progressMonitor
	 * @return events containing the JAX-RS Resources
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	private List<JaxrsElementChangedEvent> preprocessResourceChangesOnScopeAdditionOrChange(final IJavaElement scope,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException,
			JavaModelException {
		final List<JaxrsElementChangedEvent> results = new ArrayList<JaxrsElementChangedEvent>();
		// see if there may be elements to add/change from the given scope
		final List<JaxrsResource> matchingResources = new ArrayList<JaxrsResource>();
		final List<IType> matchingResourceTypes = JaxrsAnnotationsScanner.findResources(scope, progressMonitor);
		for (IType matchingResourceType : matchingResourceTypes) {
			final CompilationUnit ast = JdtUtils.parse(matchingResourceType, progressMonitor);
			final JaxrsResource matchingResource = factory.createResource(matchingResourceType, ast, metamodel);
			if (matchingResource != null) {
				matchingResources.add(matchingResource);
			}
		}
		// retrieve the existing elements from the Metamodel
		final List<JaxrsResource> existingResources = metamodel.getElements(scope, JaxrsResource.class);
		// compute the differences, with a 'fuzzy' case when the kind is
		// 'changed'
		final Collection<JaxrsResource> addedResources = CollectionUtils.difference(matchingResources,
				existingResources);
		for (JaxrsResource resource : addedResources) {
			results.add(new JaxrsElementChangedEvent(resource, ADDED));
		}
		final Collection<JaxrsResource> changedResources = CollectionUtils.intersection(matchingResources,
				existingResources);
		for (JaxrsResource resource : changedResources) {
			results.add(new JaxrsElementChangedEvent(resource, CHANGED, F_FINE_GRAINED));
		}
		final Collection<JaxrsResource> removedResources = CollectionUtils.difference(existingResources,
				matchingResources);
		for (JaxrsResource resource : removedResources) {
			results.add(new JaxrsElementChangedEvent(resource, REMOVED));
		}

		return results;
	}

	/**
	 * See if JAX-RS Resources existed in the given scope. These elements can
	 * only be marked as removed.
	 * 
	 * @param scope
	 * @param metamodel
	 * @param progressMonitor
	 * @return events containing the JAX-RS Resources
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	private List<JaxrsElementChangedEvent> preprocessResourceChangesOnScopeRemoval(final IJavaElement scope,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException,
			JavaModelException {
		final List<JaxrsElementChangedEvent> results = new ArrayList<JaxrsElementChangedEvent>();
		// retrieve the existing elements from the Metamodel
		final List<JaxrsResource> existingResources = metamodel.getElements(scope, JaxrsResource.class);
		for (JaxrsResource resource : existingResources) {
			results.add(new JaxrsElementChangedEvent(resource, REMOVED));
		}
		return results;
	}

	private List<JaxrsElementChangedEvent> postProcessResource(final JaxrsElementChangedEvent event,
			final IProgressMonitor progressMonitor) {
		final List<JaxrsElementChangedEvent> results = new ArrayList<JaxrsElementChangedEvent>();
		final JaxrsResource eventResource = (JaxrsResource) event.getElement();
		final JaxrsMetamodel metamodel = eventResource.getMetamodel();
		switch (event.getDeltaKind()) {
		case ADDED:
			metamodel.add(eventResource);
			results.add(event);
			break;
		case REMOVED:
			metamodel.remove(eventResource);
			results.add(event);
			break;
		case CHANGED:
			final JaxrsResource existingResource = metamodel.getElement(eventResource.getJavaElement(),
					JaxrsResource.class);
			// compare at the fields level
			results.addAll(mergeResourceFields(existingResource, eventResource, metamodel));
			// and compare at the methods level
			results.addAll(mergeResourceMethods(existingResource, eventResource, metamodel));
			// finally, compare at the annotations level
			results.addAll(mergeResourceAnnotations(existingResource, eventResource));
			break;
		}
		return results;
	}

	private final List<JaxrsElementChangedEvent> mergeResourceAnnotations(final JaxrsResource existingResource,
			final JaxrsResource matchingResource) {
		final List<JaxrsElementChangedEvent> changes = new ArrayList<JaxrsElementChangedEvent>();
		final int flags = existingResource.mergeAnnotations(matchingResource.getAnnotations());
		if (flags > 0) {
			changes.add(new JaxrsElementChangedEvent(existingResource, CHANGED, flags));
		}
		return changes;
	}

	/**
	 * Update this Resource fields with the ones of the given resource
	 * 
	 * @param metamodel
	 * 
	 * @param otherResource
	 * @return the flags indicating the kind of changes that occurred during the
	 *         update.
	 */
	private List<JaxrsElementChangedEvent> mergeResourceFields(final JaxrsResource existingResource,
			final JaxrsResource matchingResource, final JaxrsMetamodel metamodel) {
		final List<JaxrsElementChangedEvent> changes = new ArrayList<JaxrsElementChangedEvent>();
		final Map<String, JaxrsResourceField> addedFields = CollectionUtils.difference(matchingResource.getFields(),
				existingResource.getFields());
		final Map<String, JaxrsResourceField> removedFields = CollectionUtils.difference(existingResource.getFields(),
				matchingResource.getFields());
		final Map<String, JaxrsResourceField> changedFields = CollectionUtils.intersection(
				existingResource.getFields(), matchingResource.getFields());

		for (Entry<String, JaxrsResourceField> entry : addedFields.entrySet()) {
			final JaxrsResourceField addedField = entry.getValue();
			existingResource.addElement(addedField);
			metamodel.add(addedField);
			changes.add(new JaxrsElementChangedEvent(addedField, ADDED));
		}
		for (Entry<String, JaxrsResourceField> entry : changedFields.entrySet()) {
			final JaxrsResourceField existingField = entry.getValue();
			final JaxrsResourceField matchingField = matchingResource.getFields().get(entry.getKey());
			int flags = existingField.mergeAnnotations(matchingField.getAnnotations());
			if ((flags & F_ELEMENT_KIND) > 0 && existingField.getKind() == EnumKind.UNDEFINED) {
				metamodel.remove(existingField);
				changes.add(new JaxrsElementChangedEvent(existingField, REMOVED));
			} else if (flags > 0) {
				changes.add(new JaxrsElementChangedEvent(existingField, CHANGED, flags));
			}
		}
		for (Entry<String, JaxrsResourceField> entry : removedFields.entrySet()) {
			final JaxrsResourceField removedField = entry.getValue();
			existingResource.removeField(removedField);
			metamodel.remove(removedField);
			changes.add(new JaxrsElementChangedEvent(removedField, REMOVED));
		}

		return changes;
	}

	private List<JaxrsElementChangedEvent> mergeResourceMethods(final JaxrsResource existingResource,
			final JaxrsResource matchingResource, final JaxrsMetamodel metamodel) {
		final List<JaxrsElementChangedEvent> changes = new ArrayList<JaxrsElementChangedEvent>();
		final Map<String, JaxrsResourceMethod> addedMethods = CollectionUtils.difference(matchingResource.getMethods(),
				existingResource.getMethods());
		final Map<String, JaxrsResourceMethod> removedMethods = CollectionUtils.difference(
				existingResource.getMethods(), matchingResource.getMethods());
		final Map<String, JaxrsResourceMethod> changedMethods = CollectionUtils.intersection(
				existingResource.getMethods(), matchingResource.getMethods());

		for (Entry<String, JaxrsResourceMethod> entry : addedMethods.entrySet()) {
			final JaxrsResourceMethod addedMethod = entry.getValue();
			existingResource.addMethod(addedMethod);
			metamodel.add(addedMethod);
			changes.add(new JaxrsElementChangedEvent(addedMethod, ADDED));
		}
		for (Entry<String, JaxrsResourceMethod> entry : changedMethods.entrySet()) {
			final JaxrsResourceMethod existingMethod = entry.getValue();
			final JaxrsResourceMethod matchingMethod = matchingResource.getMethods().get(entry.getKey());
			int flags = existingMethod.mergeAnnotations(matchingMethod.getAnnotations());
			if ((flags & F_ELEMENT_KIND) > 0 && existingMethod.getKind() == EnumKind.UNDEFINED) {
				metamodel.remove(existingMethod);
				changes.add(new JaxrsElementChangedEvent(existingMethod, REMOVED));
			} else if (flags > 0) {
				changes.add(new JaxrsElementChangedEvent(existingMethod, CHANGED, flags));
			}
		}
		for (Entry<String, JaxrsResourceMethod> entry : removedMethods.entrySet()) {
			final JaxrsResourceMethod removedMethod = entry.getValue();
			existingResource.removeMethod(removedMethod);
			metamodel.remove(removedMethod);
			changes.add(new JaxrsElementChangedEvent(removedMethod, REMOVED));
		}
		return changes;
	}

	private List<JaxrsElementChangedEvent> postProcessHttpMethod(JaxrsElementChangedEvent event,
			IProgressMonitor progressMonitor) {
		final List<JaxrsElementChangedEvent> results = new ArrayList<JaxrsElementChangedEvent>();
		final JaxrsHttpMethod eventHttpMethod = (JaxrsHttpMethod) event.getElement();
		final JaxrsMetamodel metamodel = eventHttpMethod.getMetamodel();
		switch (event.getDeltaKind()) {
		case ADDED:
			metamodel.add(eventHttpMethod);
			results.add(event);
			break;
		case REMOVED:
			metamodel.remove(eventHttpMethod);
			results.add(event);
			// FIXME: also remove all resourceMethods / update all subresource
			// methods using this HttpMethod
			break;
		case CHANGED:
			final JaxrsHttpMethod existingHttpMethod = metamodel.getElement(eventHttpMethod.getJavaElement(),
					JaxrsHttpMethod.class);
			final int flags = existingHttpMethod.update(eventHttpMethod);
			if (flags != 0) {
				results.add(new JaxrsElementChangedEvent(existingHttpMethod, CHANGED, flags));
			}
			break;
		}
		return results;
	}
}
