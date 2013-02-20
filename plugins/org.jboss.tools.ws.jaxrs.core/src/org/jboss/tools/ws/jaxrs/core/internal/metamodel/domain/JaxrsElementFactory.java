/******************************************************************************* 
Le * Copyright (c) 2008 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * real racin
 * Eclipse public static License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import static org.eclipse.jdt.core.IJavaElement.ANNOTATION;
import static org.eclipse.jdt.core.IJavaElement.COMPILATION_UNIT;
import static org.eclipse.jdt.core.IJavaElement.FIELD;
import static org.eclipse.jdt.core.IJavaElement.JAVA_PROJECT;
import static org.eclipse.jdt.core.IJavaElement.METHOD;
import static org.eclipse.jdt.core.IJavaElement.PACKAGE_FRAGMENT_ROOT;
import static org.eclipse.jdt.core.IJavaElement.TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JaxrsElementsSearcher;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;

/**
 * Factory for JAX-RS elements that should be created from Java elements.
 * 
 * @author Xavier Coulon
 * 
 */
public class JaxrsElementFactory {

	/**
	 * Dispatch method.
	 * 
	 * @param element
	 * @param ast
	 * @param metamodel
	 * @param progressMonitor
	 * @return
	 * @throws CoreException
	 */
	public static List<IJaxrsElement> createElements(final IJavaElement element, final CompilationUnit ast,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		final List<IJaxrsElement> elements = new ArrayList<IJaxrsElement>();
		switch (element.getElementType()) {
		case JAVA_PROJECT:
			elements.addAll(createElements(element, metamodel, progressMonitor));
			break;
		case PACKAGE_FRAGMENT_ROOT:
			elements.addAll(createElements(element, metamodel, progressMonitor));
			break;
		case COMPILATION_UNIT:
			final ICompilationUnit compilationUnit = (ICompilationUnit) element;
			for (IType type : compilationUnit.getTypes()) {
				elements.addAll(createElements(type, ast, metamodel, progressMonitor));
			}
			break;
		case TYPE:
			elements.addAll(createElements((IType) element, ast, metamodel, progressMonitor));
			break;
		case METHOD:
			elements.addAll(createElements((IMethod) element, ast, metamodel, progressMonitor));
			break;
		case FIELD:
			elements.addAll(createElements((IField) element, ast, metamodel, progressMonitor));
			break;
		case ANNOTATION:
			elements.addAll(createElements((IAnnotation) element, ast, metamodel,
					progressMonitor));
			break;
		}
		return elements;
	}

	/**
	 * Creates one or more JAX-RS elements from the given {@link IType}
	 * 
	 * @param type
	 *            the java type
	 * @param ast
	 *            the associated {@link CompilationUnit}
	 * @param metamodel
	 *            the metamodel
	 * @param progressMonitor
	 *            the progress monitor
	 * @return
	 * @throws CoreException
	 */
	private static List<IJaxrsElement> createElements(final IType type, final CompilationUnit ast,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		final List<IJaxrsElement> elements = new ArrayList<IJaxrsElement>();
		// let's see if the given type can be an HTTP Method (ie, is annotated
		// with @HttpMethod)
		final JaxrsHttpMethod httpMethod = JaxrsHttpMethod.from(type, ast).withMetamodel(metamodel).build();
		if (httpMethod != null) {
			elements.add(httpMethod);
		}
		// now,let's see if the given type can be a Resource (with or without
		// @Path)
		final JaxrsResource resource = JaxrsResource.from(type, ast, metamodel.findAllHttpMethods()).withMetamodel(metamodel).build();
		if (resource != null) {
			elements.add(resource);
			elements.addAll(resource.getAllMethods());
			elements.addAll(resource.getAllFields());
		}
		// now,let's see if the given type can be an Application
		final JaxrsJavaApplication application = JaxrsJavaApplication.from(type, ast).withMetamodel(metamodel).build();
		if (application != null) {
			elements.add(application);
		}
		// now,let's see if the given type can be a Provider
		final JaxrsProvider provider = JaxrsProvider.from(type, ast).withMetamodel(metamodel).build();
		if (provider != null) {
			elements.add(provider);
		}

		return elements;
	}

	private static List<IJaxrsElement> createElements(final IJavaElement scope,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		if(scope.getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT && ((IPackageFragmentRoot)scope).isArchive()) {
			Logger.debug("Ignoring archive {}", scope.getElementName());
			return Collections.emptyList();
		}
		final List<IJaxrsElement> elements = new ArrayList<IJaxrsElement>();
		// let's see if the given scope contains JAX-RS HTTP Methods
		final List<IType> matchingHttpMethodTypes = JaxrsElementsSearcher.findHttpMethodTypes(scope,
				progressMonitor);
		for (IType type : matchingHttpMethodTypes) {
			final JaxrsHttpMethod httpMethod = JaxrsHttpMethod.from(type).withMetamodel(metamodel).build();
			if (httpMethod != null) {
				elements.add(httpMethod);
			}
		}
		// let's see if the given scope contains JAX-RS HTTP Resources
		final List<IType> matchingResourceTypes = JaxrsElementsSearcher.findResourceTypes(scope, progressMonitor);
		for (IType type : matchingResourceTypes) {
			final JaxrsResource resource = JaxrsResource.from(type, metamodel.findAllHttpMethods()).withMetamodel(metamodel).build();
			if (resource != null) {
				elements.add(resource);
				elements.addAll(resource.getAllMethods());
				elements.addAll(resource.getAllFields());
			}
		}
		// let's see if the given scope contains JAX-RS Application
		final List<IType> matchingApplicationTypes = JaxrsElementsSearcher.findApplicationTypes(scope,
				progressMonitor);
		for (IType type : matchingApplicationTypes) {
			final JaxrsJavaApplication application = JaxrsJavaApplication.from(type).withMetamodel(metamodel).build();
			if (application != null) {
				elements.add(application);
			}
		}
		// let's see if the given scope contains JAX-RS Providers
		final List<IType> matchingProviderTypes = JaxrsElementsSearcher.findProviderTypes(scope, progressMonitor);
		for (IType type : matchingProviderTypes) {
			final JaxrsProvider provider = JaxrsProvider.from(type).withMetamodel(metamodel).build();
			if (provider != null) {
				elements.add(provider);
			}
		}
		return elements;
	}

	/**
	 * Attempts to create a JAX-RS Resource Method from the given
	 * {@link IMethod}, and if needed, also creates the parent JAX-RS Resource.
	 * 
	 * @param javaMethod
	 * @param ast
	 * @param metamodel
	 * @param progressMonitor
	 * @return
	 * @throws CoreException
	 */
	private static List<IJaxrsElement> createElements(final IMethod javaMethod,
			final CompilationUnit ast, final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		final List<IJaxrsElement> elements = new ArrayList<IJaxrsElement>();
		final IType parentType = (IType) javaMethod.getAncestor(IJavaElement.TYPE);
		if(metamodel.findElement(parentType) == null) {
			elements.addAll(createElements(parentType, ast, metamodel, progressMonitor));
		} else {
			final JaxrsResource parentResource = metamodel.findResource((IType)javaMethod.getAncestor(IJavaElement.TYPE));
			final JaxrsResourceMethod resourceMethod = JaxrsResourceMethod
					.from(javaMethod, ast, metamodel.findAllHttpMethods()).withParentResource(parentResource).withMetamodel(metamodel).build();
			if (resourceMethod != null) {
				elements.add(resourceMethod);
				// now, check if the parent resource should also be added to the
				// metamodel
				if (resourceMethod.getParentResource() != null && !metamodel.containsElement(resourceMethod.getParentResource())) {
					elements.add(resourceMethod.getParentResource());
				}
			}
		}
		return elements;
	}

	/**
	 * Attempts to create a JAX-RS Resource Method from the given
	 * {@link IMethod}, and if needed, also creates the parent JAX-RS Resource.
	 * 
	 * @param javaField
	 * @param ast
	 * @param metamodel
	 * @param progressMonitor
	 * @return
	 * @throws CoreException
	 */
	private static List<IJaxrsElement> createElements(IField javaField, CompilationUnit ast,
			JaxrsMetamodel metamodel, IProgressMonitor progressMonitor) throws CoreException {
		final List<IJaxrsElement> elements = new ArrayList<IJaxrsElement>();
		final IType parentType = (IType) javaField.getAncestor(IJavaElement.TYPE);
		if(metamodel.findElement(parentType) == null) {
			elements.addAll(createElements(parentType, ast, metamodel, progressMonitor));
		} else {
			final JaxrsResourceField resourceField = JaxrsResourceField.from(javaField, ast).withMetamodel(metamodel).build();
			if (resourceField != null) {
				elements.add(resourceField);
				// now, check if the parent resource should also be added to the
				// metamodel
				if (!metamodel.containsElement(resourceField.getParentResource())) {
					final JaxrsResource parentResource = resourceField.getParentResource();
					elements.add(parentResource);
				}
			}
		}
		return elements;
	}

	/**
	 * Attempts to create a new JAX-RS element from the given {@link Annotation}
	 * . Multiple elements can be returned, for example if a java method is
	 * annotated with a JAX-RS annotation (ex: <code>@Path()</code>), the parent
	 * type becomes a JAX-RS Subresource.
	 * 
	 * @param element
	 * @param ast
	 * @param metamodel
	 * @return the created JAX-RS element or null if the given Java annotation
	 *         is not a valid one.
	 * @throws CoreException
	 */
	public static List<IJaxrsElement> createElements(final IAnnotation javaAnnotation, final CompilationUnit ast,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		// unsupported annotation (eg: on package declaration, or underlying java
		// element does not exist or not found) are ignored
		if (javaAnnotation != null) {
			switch (javaAnnotation.getParent().getElementType()) {
			case IJavaElement.TYPE:
				return createElements((IType) javaAnnotation.getParent(), ast, metamodel, progressMonitor);
			case IJavaElement.METHOD:
				return createElements((IMethod) javaAnnotation.getParent(), ast, metamodel, progressMonitor);
			case IJavaElement.FIELD:
				return createElements((IField) javaAnnotation.getParent(), ast, metamodel, progressMonitor);
			}
		}
		return Collections.emptyList();
	}

}
