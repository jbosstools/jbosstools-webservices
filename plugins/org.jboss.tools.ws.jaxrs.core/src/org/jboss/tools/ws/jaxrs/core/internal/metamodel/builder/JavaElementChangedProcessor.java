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

import static org.eclipse.jdt.core.IJavaElement.ANNOTATION;
import static org.eclipse.jdt.core.IJavaElement.COMPILATION_UNIT;
import static org.eclipse.jdt.core.IJavaElement.FIELD;
import static org.eclipse.jdt.core.IJavaElement.JAVA_PROJECT;
import static org.eclipse.jdt.core.IJavaElement.METHOD;
import static org.eclipse.jdt.core.IJavaElement.PACKAGE_FRAGMENT_ROOT;
import static org.eclipse.jdt.core.IJavaElement.TYPE;
import static org.eclipse.jdt.core.IJavaElementDelta.ADDED;
import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.eclipse.jdt.core.IJavaElementDelta.REMOVED;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_NONE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.HttpMethod;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsElementFactory;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.CompilationUnitsRepository;
import org.jboss.tools.ws.jaxrs.core.jdt.JaxrsAnnotationsScanner;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.JaxrsMetamodelLocator;

public class JavaElementChangedProcessor {

	private final JaxrsElementFactory factory = new JaxrsElementFactory();

	public List<JaxrsElementChangedEvent> processEvents(List<JavaElementChangedEvent> events,
			IProgressMonitor progressMonitor) {
		final List<JaxrsElementChangedEvent> changes = new ArrayList<JaxrsElementChangedEvent>();
		try {
			progressMonitor.beginTask("Processing Java " + events.size() + " change(s)...", events.size());
			Logger.debug("Processing {} change(s)...", events.size());
			for (JavaElementChangedEvent event : events) {
				changes.addAll(processEvent(event, progressMonitor));
				progressMonitor.worked(1);
			}
		} catch (CoreException e) {
			Logger.error("Failed while processing Java changes", e);
			changes.clear();
		} finally {
			progressMonitor.done();
			Logger.debug("Done processing Java changes.");

		}
		return changes;
	}

	private List<JaxrsElementChangedEvent> processEvent(JavaElementChangedEvent event, IProgressMonitor progressMonitor)
			throws CoreException {
		Logger.debug("Processing {} Java change", event);
		final IJavaElement element = event.getElement();
		final CompilationUnit ast = event.getCompilationUnitAST();
		final int deltaKind = event.getDeltaKind();
		// final int[] flags = event.getFlags();
		final int elementType = event.getElement().getElementType();
		// if no metamodel existed for the given project, one is automatically
		// created. Yet, this applies only to project having the JAX-RS Facet
		JaxrsMetamodel metamodel = JaxrsMetamodelLocator.get(element.getJavaProject());
		if (metamodel == null) {
			metamodel = JaxrsMetamodel.create(element.getJavaProject());
		}

		switch (deltaKind) {
		case ADDED:
			switch (elementType) {
			case JAVA_PROJECT:
				return processAddition(element, metamodel, progressMonitor);
			case PACKAGE_FRAGMENT_ROOT:
				return processAddition(element, metamodel, progressMonitor);
			case COMPILATION_UNIT:
				return processAddition((ICompilationUnit) element, ast, metamodel, progressMonitor);
			case TYPE:
				return processAddition((IType) element, ast, metamodel, progressMonitor);
			case METHOD:
				return processAddition((IMethod) element, ast, metamodel, progressMonitor);
			case FIELD:
				return processAddition((IField) element, ast, metamodel, progressMonitor);
			case ANNOTATION:
				return processAddition((IAnnotation) element, ast, metamodel, progressMonitor);
			}
			break;
		case CHANGED:
			switch (elementType) {
			case METHOD:
				return processChange((IMethod) element, ast, metamodel, progressMonitor);
			case ANNOTATION:
				return processChange((IAnnotation) element, ast, metamodel, progressMonitor);
			}
			break;
		case REMOVED:
			switch (elementType) {
			case COMPILATION_UNIT:
				return processRemoval((ICompilationUnit) element, ast, metamodel, progressMonitor);
			case PACKAGE_FRAGMENT_ROOT:
				return processRemoval((IPackageFragmentRoot) element, metamodel, progressMonitor);
			case TYPE:
				return processRemoval((IType) element, ast, metamodel, progressMonitor);
			case METHOD:
				return processRemoval((IMethod) element, ast, metamodel, progressMonitor);
			case ANNOTATION:
				return processRemoval((IAnnotation) element, ast, metamodel, progressMonitor);
			case FIELD:
				return processRemoval((IField) element, ast, metamodel, progressMonitor);
			}
			break;
		}
		return Collections.emptyList();
	}

	/**
	 * Process the addition of a Java Element (can be a JavaProject or a Java Package Fragment root).
	 * 
	 * @param scope
	 *            the java element that may contain JAX-RS items
	 * @param metamodel
	 *            the metamodel associated with the current Java project
	 * @param progressMonitor
	 *            the progress monitor
	 * @return a list of changes (ie, JAX-RS elements that where created)
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	private List<JaxrsElementChangedEvent> processAddition(final IJavaElement scope, final JaxrsMetamodel metamodel,
			final IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> changes = new ArrayList<JaxrsElementChangedEvent>();
		if (metamodel.getElement(scope) == null) {
			// process this type as it is not already known from the metamodel
			// let's see if the given project contains JAX-RS HTTP Methods
			final List<IType> matchingHttpMethodTypes = JaxrsAnnotationsScanner.findHttpMethodTypes(scope,
					progressMonitor);
			for (IType type : matchingHttpMethodTypes) {
				final CompilationUnit ast = JdtUtils.parse(type, progressMonitor);
				final Annotation annotation = JdtUtils.resolveAnnotation(type, ast, HttpMethod.class);
				final JaxrsHttpMethod httpMethod = factory.createHttpMethod(annotation, ast, metamodel);
				metamodel.add(httpMethod);
				if (httpMethod != null) {
					changes.add(new JaxrsElementChangedEvent(httpMethod, ADDED));
				}
			}
			// let's see if the given project contains JAX-RS HTTP Resources
			final List<IType> matchingResourceTypes = JaxrsAnnotationsScanner.findResourceTypes(scope, progressMonitor);
			for (IType matchingType : matchingResourceTypes) {
				final CompilationUnit ast = JdtUtils.parse(matchingType, progressMonitor);
				final JaxrsResource createdResource = factory.createResource(matchingType, ast, metamodel);
				if (createdResource != null) {
					metamodel.add(createdResource);
					changes.add(new JaxrsElementChangedEvent(createdResource, ADDED));
					for (JaxrsResourceMethod resourceMethod : createdResource.getMethods().values()) {
						metamodel.add(resourceMethod);
						changes.add(new JaxrsElementChangedEvent(resourceMethod, ADDED));
					}
					for (JaxrsResourceField resourceField : createdResource.getFields().values()) {
						metamodel.add(resourceField);
						changes.add(new JaxrsElementChangedEvent(resourceField, ADDED));
					}
				}
			}
			// let's see if the given project contains JAX-RS Application
			final List<IType> matchingApplicationTypes = JaxrsAnnotationsScanner.findApplicationTypes(scope,
					progressMonitor);
			for (IType matchingType : matchingApplicationTypes) {
				final CompilationUnit ast = JdtUtils.parse(matchingType, progressMonitor);
				final JaxrsApplication createdApplication = factory.createApplication(matchingType, ast, metamodel);
				if (createdApplication != null) {
					metamodel.add(createdApplication);
					changes.add(new JaxrsElementChangedEvent(createdApplication, ADDED));
				}
			}
		}

		return changes;
	}

	private List<JaxrsElementChangedEvent> processAddition(ICompilationUnit element, CompilationUnit ast,
			final JaxrsMetamodel metamodel, IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> changes = new ArrayList<JaxrsElementChangedEvent>();
		for (IType type : element.getTypes()) {
			changes.addAll(processAddition(type, ast, metamodel, progressMonitor));
		}
		return changes;
	}

	/**
	 * @param type
	 * @param progressMonitor
	 * @throws CoreException
	 */
	private List<JaxrsElementChangedEvent> processAddition(final IType javaType, final CompilationUnit ast,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> changes = new ArrayList<JaxrsElementChangedEvent>();
		// let's see if the given type can be an HTTP Method (ie, is annotated
		// with @HttpMethod)
		final JaxrsHttpMethod httpMethod = factory.createHttpMethod(javaType, ast, metamodel);
		if (httpMethod != null) {
			metamodel.add(httpMethod);
			changes.add(new JaxrsElementChangedEvent(httpMethod, ADDED));
		}
		// now,let's see if the given type can be a Resource (with or without
		// @Path)
		final JaxrsResource resource = factory.createResource(javaType, ast, metamodel);
		if (resource != null) {
			metamodel.add(resource);
			changes.add(new JaxrsElementChangedEvent(resource, ADDED));
		}
		// now,let's see if the given type can be an Application
		final JaxrsApplication application = factory.createApplication(javaType, ast, metamodel);
		if (application != null) {
			metamodel.add(application);
			changes.add(new JaxrsElementChangedEvent(application, ADDED));
		}
		// TODO: now,let's see if the given type can be a Provider

		return changes;
	}

	private List<JaxrsElementChangedEvent> processAddition(IField javaField, CompilationUnit ast,
			JaxrsMetamodel metamodel, IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> changes = new ArrayList<JaxrsElementChangedEvent>();
		// let's see if the added field has some JAX-RS annotation on itself.
		final JaxrsResourceField field = factory.createField(javaField, ast, metamodel);
		if (field != null) {
			metamodel.add(field);
			changes.add(new JaxrsElementChangedEvent(field, ADDED));
		}
		return changes;
	}

	/**
	 * @param type
	 * @param progressMonitor
	 * @throws CoreException
	 */
	private List<JaxrsElementChangedEvent> processAddition(final IMethod javaMethod, final CompilationUnit ast,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> changes = new ArrayList<JaxrsElementChangedEvent>();
		final JaxrsResourceMethod resourceMethod = factory.createResourceMethod(javaMethod, ast, metamodel);
		if (resourceMethod != null) {
			metamodel.add(resourceMethod);
			changes.add(new JaxrsElementChangedEvent(resourceMethod, ADDED));
			// now, check if the parent resource should also be added to the
			// metamodel
			if (!metamodel.containsElement(resourceMethod)) {
				final JaxrsResource parentResource = resourceMethod.getParentResource();
				metamodel.add(parentResource);
				changes.add(new JaxrsElementChangedEvent(parentResource, ADDED));
			}
		}
		return changes;
	}

	/**
	 * @param javaAnnotation
	 * @param progressMonitor
	 * @throws CoreException
	 */
	private List<JaxrsElementChangedEvent> processAddition(final IAnnotation javaAnnotation, final CompilationUnit ast,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> changes = new ArrayList<JaxrsElementChangedEvent>();
		final JaxrsElement<?> existingElement = metamodel.getElement(javaAnnotation.getParent());
		if (existingElement == null) {
			final JaxrsElement<?> createdElement = factory.createElement(javaAnnotation, ast, metamodel);
			if (createdElement != null) {
				metamodel.add(createdElement);
				changes.add(new JaxrsElementChangedEvent(createdElement, ADDED));
				switch (createdElement.getElementKind()) {
				case RESOURCE_FIELD:
				case RESOURCE_METHOD:
					JaxrsResource parentResource = ((JaxrsResourceElement<?>) createdElement).getParentResource();
					if (!metamodel.containsElement(parentResource)) {
						metamodel.add(parentResource);
						changes.add(new JaxrsElementChangedEvent(parentResource, ADDED));
					}
				}
			}
		} else {
			final Annotation annotation = JdtUtils.resolveAnnotation(javaAnnotation, ast);
			final int flags = existingElement.addOrUpdateAnnotation(annotation);
			if (flags > 0) {
				changes.add(new JaxrsElementChangedEvent(existingElement, CHANGED, flags));
			}
		}
		return changes;
	}

	/**
	 * @param javaAnnotation
	 * @param progressMonitor
	 * @throws CoreException
	 */
	// FIXME : same code as method processAddition(annotation, etc..) ?!?
	private List<JaxrsElementChangedEvent> processChange(final IAnnotation javaAnnotation, final CompilationUnit ast,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> changes = new ArrayList<JaxrsElementChangedEvent>();
		Annotation annotation = JdtUtils.resolveAnnotation(javaAnnotation, ast);
		if (annotation != null) {
			final JaxrsElement<?> existingElement = metamodel.getElement(annotation);
			if (existingElement == null) {
				final JaxrsElement<?> createdElement = factory.createElement(javaAnnotation, ast, metamodel);
				if (createdElement != null) {
					metamodel.add(createdElement);
					changes.add(new JaxrsElementChangedEvent(createdElement, ADDED));
					switch (createdElement.getElementKind()) {
					case RESOURCE_FIELD:
					case RESOURCE_METHOD:
						JaxrsResource parentResource = ((JaxrsResourceElement<?>) createdElement).getParentResource();
						if (metamodel.containsElement(parentResource)) {
							metamodel.add(parentResource);
							changes.add(new JaxrsElementChangedEvent(parentResource, ADDED));
						}
					}
				}
			} else {
				final int flags = existingElement.addOrUpdateAnnotation(annotation);
				if (flags > 0) {
					changes.add(new JaxrsElementChangedEvent(existingElement, CHANGED, flags));
				}
			}
		}
		return changes;
	}

	private List<JaxrsElementChangedEvent> processChange(IMethod javaMethod, CompilationUnit ast,
			JaxrsMetamodel metamodel, IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> changes = new ArrayList<JaxrsElementChangedEvent>();
		final JaxrsElement<?> jaxrsElement = metamodel.getElement(javaMethod);
		if (jaxrsElement != null && jaxrsElement.getElementKind() == EnumElementKind.RESOURCE_METHOD) {
			final int flag = ((JaxrsResourceMethod) jaxrsElement).update(CompilationUnitsRepository.getInstance()
					.getMethodSignature(javaMethod));
			if (flag != F_NONE) {
				changes.add(new JaxrsElementChangedEvent(jaxrsElement, CHANGED, flag));
			}
		}
		return changes;
	}

	/**
	 * @param element
	 * @param progressMonitor
	 * @return
	 * @throws CoreException
	 */
	private List<JaxrsElementChangedEvent> processRemoval(IPackageFragmentRoot packageFragmentRoot,
			final JaxrsMetamodel metamodel, IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> changes = new ArrayList<JaxrsElementChangedEvent>();
		final List<JaxrsElement<?>> elements = metamodel.getElements(packageFragmentRoot);
		for (JaxrsElement<?> element : elements) {
			metamodel.remove(element);
			changes.add(new JaxrsElementChangedEvent(element, REMOVED));
		}
		return changes;
	}

	/**
	 * @param element
	 * @param ast
	 * @param progressMonitor
	 * @return
	 * @throws CoreException
	 */
	private List<JaxrsElementChangedEvent> processRemoval(ICompilationUnit compilationUnit, CompilationUnit ast,
			final JaxrsMetamodel metamodel, IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> changes = new ArrayList<JaxrsElementChangedEvent>();
		final List<JaxrsElement<?>> elements = metamodel.getElements(compilationUnit);
		for (JaxrsElement<?> element : elements) {
			metamodel.remove(element);
			CompilationUnitsRepository.getInstance().removeAST(compilationUnit);
			changes.add(new JaxrsElementChangedEvent(element, REMOVED));
		}
		return changes;
	}

	/**
	 * @param javaType
	 * @param progressMonitor
	 * @throws CoreException
	 */
	private List<JaxrsElementChangedEvent> processRemoval(final IType javaType, final CompilationUnit ast,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> changes = new ArrayList<JaxrsElementChangedEvent>();
		final JaxrsElement<?> element = metamodel.getElement(javaType);
		// if item does not exist yet, then don't care about the removed type
		if (element != null) {
			metamodel.remove(element);
			changes.add(new JaxrsElementChangedEvent(element, REMOVED));
		}
		return changes;
	}

	/**
	 * @param javaAnnotation
	 * @param progressMonitor
	 * @throws CoreException
	 */
	private List<JaxrsElementChangedEvent> processRemoval(final IAnnotation javaAnnotation, final CompilationUnit ast,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		final List<JaxrsElementChangedEvent> changes = new ArrayList<JaxrsElementChangedEvent>();
		final JaxrsElement<?> element = metamodel.getElement(javaAnnotation);
		if (element != null) {
			// The logic is the same for all the kinds of elements
			final int flag = element.removeAnnotation(javaAnnotation.getHandleIdentifier());
			if (element.getKind() == EnumKind.UNDEFINED) {
				metamodel.remove(element);
				changes.add(new JaxrsElementChangedEvent(element, REMOVED));
			} else {
				changes.add(new JaxrsElementChangedEvent(element, CHANGED, flag));
			}
		}
		return changes;
	}

	private List<JaxrsElementChangedEvent> processRemoval(IField field, CompilationUnit ast, JaxrsMetamodel metamodel,
			IProgressMonitor progressMonitor) {
		final List<JaxrsElementChangedEvent> changes = new ArrayList<JaxrsElementChangedEvent>();
		final List<JaxrsElement<?>> elements = metamodel.getElements(field);
		for (JaxrsElement<?> element : elements) {
			metamodel.remove(element);
			changes.add(new JaxrsElementChangedEvent(element, REMOVED));
		}
		return changes;
	}

	private List<JaxrsElementChangedEvent> processRemoval(IMethod method, CompilationUnit ast,
			JaxrsMetamodel metamodel, IProgressMonitor progressMonitor) {
		final List<JaxrsElementChangedEvent> changes = new ArrayList<JaxrsElementChangedEvent>();
		final List<JaxrsElement<?>> elements = metamodel.getElements(method);
		for (JaxrsElement<?> element : elements) {
			if (element.getElementKind() == EnumElementKind.RESOURCE_METHOD) {
				metamodel.remove(element);
				changes.add(new JaxrsElementChangedEvent(element, REMOVED));
			}
		}
		return changes;
	}

}