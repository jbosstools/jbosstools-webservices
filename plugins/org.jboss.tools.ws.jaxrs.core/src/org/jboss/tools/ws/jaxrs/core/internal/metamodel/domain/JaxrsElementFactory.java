/******************************************************************************* 
Le * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
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
import static org.eclipse.jdt.core.IJavaElement.FIELD;
import static org.eclipse.jdt.core.IJavaElement.METHOD;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.JavaElementsSearcher;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;

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
	 * @param metamodel
	 * @param progressMonitor
	 * @return
	 * @throws CoreException
	 */
	public static List<IJaxrsElement> createElements(final IJavaElement element, final CompilationUnit ast,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		final List<IJaxrsElement> elements = new ArrayList<IJaxrsElement>();
		switch (element.getElementType()) {
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
		default:
			elements.addAll(internalCreateElements(element, ast, metamodel, progressMonitor));
			break;
		}
		Collections.sort(elements, new JaxrsElementsComparator());
		return elements;
	}

	private static Set<IJaxrsElement> internalCreateElements(final IJavaElement scope, final CompilationUnit ast,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		if(scope.getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT && ((IPackageFragmentRoot)scope).isArchive()) {
			Logger.debug("Ignoring archive {}", scope.getElementName());
			return Collections.emptySet();
		}
		final Set<IJaxrsElement> elements = new HashSet<IJaxrsElement>();
		// let's see if the given scope contains JAX-RS Application
		final Set<IType> matchingApplicationTypes = JavaElementsSearcher.findApplicationTypes(scope,
				progressMonitor);
		for (IType type : matchingApplicationTypes) {
			final JaxrsJavaApplication application = JaxrsJavaApplication.from(type).withMetamodel(metamodel).build();
			if (application != null) {
				elements.add(application);
			}
		}
		// let's see if the given scope contains JAX-RS HTTP Methods
		final Set<IType> matchingHttpMethodTypes = JavaElementsSearcher.findHttpMethodTypes(scope,
				progressMonitor);
		for (IType type : matchingHttpMethodTypes) {
			final JaxrsHttpMethod httpMethod = JaxrsHttpMethod.from(type).withMetamodel(metamodel).build();
			if (httpMethod != null) {
				elements.add(httpMethod);
			}
		}
		// let's see if the given scope contains JAX-RS Name Bindings
		final Set<IType> matchingNameBindingsTypes = JavaElementsSearcher.findNameBindingTypes(scope,
				progressMonitor);
		for (IType type : matchingNameBindingsTypes) {
			final JaxrsNameBinding nameBinding = JaxrsNameBinding.from(type).withMetamodel(metamodel).build();
			if (nameBinding != null) {
				elements.add(nameBinding);
			}
		}
		// let's see if the given scope contains JAX-RS Resources
		final Set<IType> matchingResourceTypes = JavaElementsSearcher.findResourceTypes(scope, progressMonitor);
		for (IType type : matchingResourceTypes) {
			final JaxrsResource resource = JaxrsResource.from(type, metamodel.findAllHttpMethodNames()).withMetamodel(metamodel).build();
			if (resource != null) {
				elements.add(resource);
				elements.addAll(resource.getAllMethods());
				elements.addAll(resource.getAllFields());
			}
		}
		// now,let's see if the given type can be a ParamConverterProvider
		final Set<IType> matchingParamConverterProviderTypes = JavaElementsSearcher.findParamConverterProviderTypes(scope, progressMonitor);
		for (IType type : matchingParamConverterProviderTypes) {
			final JaxrsParamConverterProvider paramConverterProvider = JaxrsParamConverterProvider.from(type).withMetamodel(metamodel).build();
			if (paramConverterProvider != null) {
				elements.add(paramConverterProvider);
			}
		}
		// now,let's see if the given type can be a Parameter Aggregator
		final Set<IType> matchingParameterAggregatorTypes = JavaElementsSearcher.findParameterAggregatorTypes(scope, progressMonitor);
		for (IType type : matchingParameterAggregatorTypes) {
			final JaxrsParameterAggregator parameterAggregator = JaxrsParameterAggregator.from(type).buildInMetamodel(metamodel);
			if (parameterAggregator != null) {
				elements.add(parameterAggregator);
				elements.addAll(parameterAggregator.getAllProperties());
				elements.addAll(parameterAggregator.getAllFields());
			}
		}
		// let's see if the given scope contains JAX-RS Providers
		final Set<IType> matchingProviderTypes = JavaElementsSearcher.findProviderTypes(scope, progressMonitor);
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
	private static Set<IJaxrsElement> createElements(final IMethod javaMethod,
			final CompilationUnit ast, final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		final Map<String, Annotation> methodAnnotations = JdtUtils.resolveAllAnnotations(javaMethod, ast);
		final Set<IJaxrsElement> elements = new HashSet<IJaxrsElement>();
		// attempt to create a JaxrsParameterAggregatorProperty or a Resource Property from the give Java method.
		if(JaxrsParamAnnotations.matchesAtLeastOne(methodAnnotations.keySet())) {
			final IType parentType = (IType) javaMethod.getAncestor(IJavaElement.TYPE);
			final IJaxrsElement parentElement = metamodel.findElement(parentType);
			if(parentElement == null) {
				elements.addAll(internalCreateElements(parentType, ast, metamodel, progressMonitor));
			} 
			// Parameter Aggregator Method
			else if(parentElement.getElementKind().getCategory() == EnumElementCategory.PARAMETER_AGGREGATOR){
				final JaxrsParameterAggregator parentParameterAggregator = (JaxrsParameterAggregator) parentElement;
				final JaxrsParameterAggregatorProperty parameterAggregatorMethod = JaxrsParameterAggregatorProperty
						.from(javaMethod, ast).buildInParentAggregator(parentParameterAggregator);
				if (parameterAggregatorMethod != null) {
					elements.add(parameterAggregatorMethod);
					// now, check if the parent resource should also be added to the
					// metamodel
					/*if (resourceMethod.getParentResource() != null && !metamodel.containsElement(resourceMethod.getParentResource())) {
						elements.add(resourceMethod.getParentResource());
					}*/
				}
			}
			// Resource Property
			else if(parentElement.getElementKind().getCategory() == EnumElementCategory.RESOURCE){
				final JaxrsResource parentResource = (JaxrsResource) parentElement;
				final JaxrsResourceProperty resourceProperty = JaxrsResourceProperty
						.from(javaMethod, ast).buildInResource(parentResource);
				if (resourceProperty != null) {
					elements.add(resourceProperty);
				}
			}
		} 
		// attempt to create a JaxrsResourceMethod from the give Java method.
		else if(CollectionUtils.hasIntersection(methodAnnotations.keySet(), metamodel.findAllHttpMethodNames()) || 
				methodAnnotations.keySet().contains(JaxrsClassnames.PATH)) {
			final IType parentType = (IType) javaMethod.getAncestor(IJavaElement.TYPE);
			final IJaxrsElement parentElement = metamodel.findElement(parentType);
			if(parentElement == null) {
				elements.addAll(internalCreateElements(parentType, ast, metamodel, progressMonitor));
			} else if(parentElement.getElementKind().getCategory() == EnumElementCategory.RESOURCE){
				final JaxrsResource parentResource = (JaxrsResource) parentElement;
				final JaxrsResourceMethod resourceMethod = JaxrsResourceMethod
						.from(javaMethod, ast, metamodel.findAllHttpMethodNames()).buildInResource(parentResource);
				if (resourceMethod != null) {
					elements.add(resourceMethod);
					// now, check if the parent resource should also be added to the
					// metamodel
					/*if (resourceMethod.getParentResource() != null && !metamodel.containsElement(resourceMethod.getParentResource())) {
						elements.add(resourceMethod.getParentResource());
					}*/
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
	private static Set<IJaxrsElement> createElements(final IField javaField, final CompilationUnit ast,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		final Map<String, Annotation> fieldAnnotations = JdtUtils.resolveAllAnnotations(javaField, ast);
		if(!JaxrsParamAnnotations.matchesAtLeastOne(fieldAnnotations.keySet())) {
			return Collections.emptySet();
		}
		final Set<IJaxrsElement> elements = new HashSet<IJaxrsElement>();
		final IType parentType = (IType) javaField.getAncestor(IJavaElement.TYPE);
		final IJaxrsElement parentElement = metamodel.findElement(parentType);
		if(parentElement == null) {
			elements.addAll(internalCreateElements(parentType, ast, metamodel, progressMonitor));
		} else if(parentElement.getElementKind().getCategory() == EnumElementCategory.RESOURCE){
			final JaxrsResourceField resourceField = JaxrsResourceField.from(javaField, ast).withMetamodel(metamodel).build();
			if (resourceField != null) {
				elements.add(resourceField);
				// now, check if the parent resource should also be added to the
				// metamodel
				/*if (!metamodel.containsElement(resourceField.getParentResource())) {
					final JaxrsResource parentResource = resourceField.getParentResource();
					elements.add(parentResource);
				}*/
			}
		} else if(parentElement.getElementKind().getCategory() == EnumElementCategory.PARAMETER_AGGREGATOR){
			final JaxrsParameterAggregatorField parameterAggregatorField = JaxrsParameterAggregatorField.from(
					javaField, ast).buildInParentAggregator((JaxrsParameterAggregator) parentElement);
			if (parameterAggregatorField != null) {
				elements.add(parameterAggregatorField);
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
	public static Set<IJaxrsElement> createElements(final IAnnotation javaAnnotation, final CompilationUnit ast,
			final JaxrsMetamodel metamodel, final IProgressMonitor progressMonitor) throws CoreException {
		// unsupported annotation (eg: on package declaration, or underlying java
		// element does not exist or not found) are ignored
		if (javaAnnotation != null) {
			switch (javaAnnotation.getParent().getElementType()) {
			case IJavaElement.TYPE:
				return internalCreateElements((IType) javaAnnotation.getParent(), ast, metamodel, progressMonitor);
			case IJavaElement.METHOD:
				return createElements((IMethod) javaAnnotation.getParent(), ast, metamodel, progressMonitor);
			case IJavaElement.FIELD:
				return createElements((IField) javaAnnotation.getParent(), ast, metamodel, progressMonitor);
			}
		}
		return Collections.emptySet();
	}

}
