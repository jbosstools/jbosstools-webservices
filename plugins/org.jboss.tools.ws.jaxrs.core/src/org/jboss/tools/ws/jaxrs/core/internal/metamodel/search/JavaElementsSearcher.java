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

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.search;

import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.APPLICATION;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.CONTAINER_REQUEST_FILTER;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.CONTAINER_RESPONSE_FILTER;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.ENTITY_READER_INTERCEPTOR;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.ENTITY_WRITER_INTERCEPTOR;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.EXCEPTION_MAPPER;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.MESSAGE_BODY_READER;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.MESSAGE_BODY_WRITER;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.NAME_BINDING;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PARAM_CONVERTER_PROVIDER;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PROVIDER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParamAnnotations;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;

/**
 * Class that scan the projec's classpath to find Java Elements that have JAX-RS annotations or supertypes/superinterfaces.
 */
public final class JavaElementsSearcher {

	/** Private constructor of the utility class. */
	private JavaElementsSearcher() {

	}

	/**
	 * Returns all JAX-RS Applications types in the given scope (ex. : javaProject), ie., types annotated with
	 * <code>javax.ws.rs.ApplicationPath</code> annotation and subtypes of
	 * {@link javax.ws.rs.Application} (even if type hierarchy or annotation is missing).
	 * 
	 * 
	 * @param scope
	 *            the search scope (project, compilation unit, type, etc.)
	 * @param progressMonitor
	 *            the progress monitor
	 * @return the JAX-RS Application types
	 * @throws CoreException
	 *             in case of exception
	 */
	public static Set<IType> findApplicationTypes(final IJavaElement scope, final IProgressMonitor progressMonitor)
			throws CoreException {
		final long start = System.currentTimeMillis();
		try {
			// FIXME: need correct usage of progressmonitor/subprogress monitor

			// first, search for type annotated with
			// <code>javax.ws.rs.ApplicationPath</code>
			final IJavaSearchScope searchScope = createSearchScope(scope);
			final Set<IType> applicationTypes = searchForAnnotatedTypes(APPLICATION_PATH, searchScope,
					progressMonitor);
			// the search result also includes all subtypes of
			// javax.ws.rs.core.Application (while avoiding duplicate results)
			final List<IType> applicationSubtypes = findSubtypes(scope, APPLICATION, progressMonitor);
			applicationTypes.addAll(CollectionUtils.difference(applicationSubtypes, applicationTypes));
			return applicationTypes;
		} finally {
			final long end = System.currentTimeMillis();
			Logger.tracePerf("Found Application types in scope {} in {}ms", scope.getElementName(), (end - start));
		}
	}

	/**
	 * Returns all potential JAX-RS providers in the given scope (ex : javaProject), ie, types annotated with
	 * <code>javax.ws.rs.ext.Provider</code> annotation or extending some expected interfaces.
	 * 
	 * 
	 * @param scope
	 *            the search scope (project, compilation unit, type, etc.)
	 * @param includeLibraries
	 *            include project libraries in search scope or not
	 * @param progressMonitor
	 *            the progress monitor
	 * @return providers the JAX-RS provider types
	 * @throws CoreException
	 *             in case of exception
	 */
	public static Set<IType> findProviderTypes(final IJavaElement scope, final IProgressMonitor progressMonitor)
			throws CoreException {
		final long start = System.currentTimeMillis();
		try {
			final Set<IType> providerTypes = new HashSet<IType>();
			final IJavaSearchScope searchScope = createSearchScope(scope);
			final Set<IType> annotatedTypes = searchForAnnotatedTypes(PROVIDER, searchScope,
					progressMonitor);
			providerTypes.addAll(annotatedTypes);
			// also search for one or more provider subtypes
			providerTypes.addAll(findSubtypes(scope, MESSAGE_BODY_READER, progressMonitor));
			providerTypes.addAll(findSubtypes(scope, MESSAGE_BODY_WRITER, progressMonitor));
			providerTypes.addAll(findSubtypes(scope, EXCEPTION_MAPPER, progressMonitor));
			providerTypes.addAll(findSubtypes(scope, CONTAINER_REQUEST_FILTER, progressMonitor));
			providerTypes.addAll(findSubtypes(scope, CONTAINER_RESPONSE_FILTER, progressMonitor));
			providerTypes.addAll(findSubtypes(scope, ENTITY_READER_INTERCEPTOR, progressMonitor));
			providerTypes.addAll(findSubtypes(scope, ENTITY_WRITER_INTERCEPTOR, progressMonitor));
			return providerTypes;
		} finally {
			final long end = System.currentTimeMillis();
			Logger.tracePerf("Found Provider types in scope {} in {}ms", scope.getElementName(), (end - start));
		}
	}

	/**
	 * Returns all potential JAX-RS ParamConverter Providers in the given scope (ex : javaProject), ie, types annotated with
	 * <code>javax.ws.rs.ext.Provider</code> annotation or implementing {@code java.ws.rs.ext.ParamConverterProvider}.
	 * 
	 * 
	 * @param scope
	 *            the search scope (project, compilation unit, type, etc.)
	 * @param includeLibraries
	 *            include project libraries in search scope or not
	 * @param progressMonitor
	 *            the progress monitor
	 * @return the JAX-RS ParamConverter types
	 * @throws CoreException
	 *             in case of exception
	 */
	public static Set<IType> findParamConverterProviderTypes(final IJavaElement scope, final IProgressMonitor progressMonitor)
			throws CoreException {
		final long start = System.currentTimeMillis();
		try {
			final Set<IType> paramConverterTypes = new HashSet<IType>();
			final IJavaSearchScope searchScope = createSearchScope(scope);
			final Set<IType> annotatedTypes = searchForAnnotatedTypes(PROVIDER, searchScope,
					progressMonitor);
			paramConverterTypes.addAll(annotatedTypes);
			// also search for subtypes 
			final List<IType> paramConverterProviderSubtypes = findSubtypes(scope, PARAM_CONVERTER_PROVIDER,
					progressMonitor);
			paramConverterTypes.addAll(CollectionUtils.difference(paramConverterProviderSubtypes, paramConverterTypes));
			return paramConverterTypes;
		} finally {
			final long end = System.currentTimeMillis();
			Logger.tracePerf("Found Provider types in scope {} in {}ms", scope.getElementName(), (end - start));
		}
	}
	
	/**
	 * Returns all potential JAX-RS Parameter Aggregators in the given scope (ex : javaProject), ie, types with fields or methods annotated with .
	 * 
	 * 
	 * @param scope
	 *            the search scope (project, compilation unit, type, etc.)
	 * @param includeLibraries
	 *            include project libraries in search scope or not
	 * @param progressMonitor
	 *            the progress monitor
	 * @return providers the JAX-RS provider types
	 * @throws CoreException
	 *             in case of exception
	 */
	public static Set<IType> findParameterAggregatorTypes(IJavaElement scope, IProgressMonitor progressMonitor) throws CoreException {
		final long start = System.currentTimeMillis();
		try {
			final Set<IType> types = new HashSet<IType>();
			final IJavaSearchScope searchScope = createSearchScope(scope);
			final Set<IMethod> annotatedMethods = searchForAnnotatedMethods(JaxrsParamAnnotations.PARAM_ANNOTATIONS, searchScope, progressMonitor);
			for(IMethod annotatedMethod : annotatedMethods) {
				types.add((IType) annotatedMethod.getAncestor(IJavaElement.TYPE));
			}
			final Set<IField> annotatedFields = searchForAnnotatedFields(JaxrsParamAnnotations.PARAM_ANNOTATIONS, searchScope, progressMonitor);
			for(IField annotatedField : annotatedFields) {
				types.add((IType) annotatedField.getAncestor(IJavaElement.TYPE));
			}
			return types;
		} finally {
			final long end = System.currentTimeMillis();
			Logger.tracePerf("Found Provider types in scope {} in {}ms", scope.getElementName(), (end - start));
		}
	}
	
	/**
	 * @param scope the search scope
	 * @param typeName the fully qualified name of the type whose subtypes should be found
	 * @param progressMonitor the progress monitor
	 * @return the subtypes of the given type, in the given scope, or empty list if none was found.
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	private static List<IType> findSubtypes(final IJavaElement scope, final String typeName, final IProgressMonitor progressMonitor)
			throws CoreException, JavaModelException {
		final IType superType = JdtUtils.resolveType(typeName, scope.getJavaProject(), progressMonitor);
		final List<IType> subtypes = JdtUtils.findSubtypes(scope, superType, progressMonitor);
		return subtypes;
	}

	/**
	 * Returns all JAX-RS resources types
	 * in the given scope (ex : javaProject).
	 * 
	 * @param scope
	 *            the search scope (project, compilation unit, type, etc.)
	 * @param progressMonitor
	 *            the progress monitor
	 * @return Set of JAX-RS resource types
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public static Set<IType> findResourceTypes(final IJavaElement scope, final IProgressMonitor progressMonitor)
			throws CoreException {
		final long start = System.currentTimeMillis();
		try {
			final IJavaSearchScope searchScope = createSearchScope(scope);
			return searchForAnnotatedTypes(PATH, searchScope, progressMonitor);
		} finally {
			final long end = System.currentTimeMillis();
			Logger.tracePerf("Found Resource types in scope {} in {}ms", scope.getElementName(), (end - start));
		}
	}

	/**
	 * Returns all HTTP Methods (ie, annotation meta-annotated with the <code>javax.ws.rs.HttpMethod</code> annotation)
	 * in the given scope (ex : javaProject).
	 * 
	 * @param scope
	 *            the search scope (project, compilation unit, type, etc.)
	 * @param progressMonitor
	 *            the progress monitor
	 * @return The found types
	 * @throws CoreException
	 *             in case of underlying exceptions.
	 */
	public static Set<IType> findHttpMethodTypes(final IJavaElement scope, final IProgressMonitor progressMonitor)
			throws CoreException {
		final long start = System.currentTimeMillis();
		try {
			final IJavaSearchScope searchScope = createSearchScope(scope);
			return searchForAnnotatedTypes(HTTP_METHOD, searchScope, progressMonitor);
		} finally {
			final long end = System.currentTimeMillis();
			Logger.tracePerf("Found HTTP Method types in scope {} in {}ms", scope.getElementName(), (end - start));
		}
	}
	
	/**
	 * Returns all Name Bindings (ie, annotation meta-annotated with the <code>javax.ws.rs.NameBinding</code> annotation)
	 * in the given scope (ex : javaProject).
	 * 
	 * @param scope
	 *            the search scope (project, compilation unit, type, etc.)
	 * @param progressMonitor
	 *            the progress monitor
	 * @return The found types
	 * @throws CoreException
	 *             in case of underlying exceptions.
	 */
	public static Set<IType> findNameBindingTypes(final IJavaElement scope, final IProgressMonitor progressMonitor)
			throws CoreException {
		final long start = System.currentTimeMillis();
		try {
			final IJavaSearchScope searchScope = createSearchScope(scope);
			return searchForAnnotatedTypes(NAME_BINDING, searchScope, progressMonitor);
		} finally {
			final long end = System.currentTimeMillis();
			Logger.tracePerf("Found NameBinding types in scope {} in {}ms", scope.getElementName(), (end - start));
		}
	}
	
	/**
	 * Finds all {@link IMethod} annotated with an annotation whose fully qualified name is the one given 
	 * @param scope the scope in which the search should be performed
	 * @param fullyQualifiedName the fully qualified name of the annotation to look for
	 * @return the Set of annotated Java elements.
	 * @throws CoreException 
	 */
	public static Set<IMethod> findAnnotatedMethods(final IJavaProject scope, final String fullyQualifiedName, final IProgressMonitor progressMonitor) throws CoreException {
		final long start = System.currentTimeMillis();
		try {
			final IJavaSearchScope searchScope = createSearchScope(scope);
			return searchForAnnotatedMethods(Arrays.asList(fullyQualifiedName), searchScope, progressMonitor);
		} finally {
			final long end = System.currentTimeMillis();
			Logger.tracePerf("Found HTTP Method types in scope {} in {}ms", scope.getElementName(), (end - start));
		}
	}

	

	/**
	 * Creates and returns an IJavaSearchScope from the given {@link IJavaElement} scope
	 * @param scope the Java Element that will serve as a scope for an upcoming search
	 * @return the search scope
	 * @throws JavaModelException
	 */
	private static IJavaSearchScope createSearchScope(final IJavaElement scope) throws JavaModelException {
		IJavaSearchScope searchScope;
		if (scope instanceof IJavaProject) {
			searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { scope },
					IJavaSearchScope.SOURCES | IJavaSearchScope.REFERENCED_PROJECTS);
		} else {
			searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { scope });
		}
		return searchScope;
	}

	/**
	 * Creates and returns an IJavaSearchScope from the given {@link IJavaElement}s scope
	 * @param scope the Java Elements that will serve as a scope for an upcoming search
	 * @return the search scope
	 * @throws JavaModelException
	 */
	private static IJavaSearchScope createSearchScope(final List<? extends IJavaElement> scope) throws JavaModelException {
		return SearchEngine.createJavaSearchScope(scope.toArray(new IJavaElement[scope.size()]));
	}

	/**
	 * Search for types that are annotated with the given annotation name, in the given search scope.
	 * 
	 * @param annotationName
	 *            the annotation type name
	 * @param searchScope
	 *            the search scope
	 * @param progressMonitor
	 *            the progress monitor
	 * @return the found types
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	private static Set<IType> searchForAnnotatedTypes(final String annotationName, final IJavaSearchScope searchScope,
			final IProgressMonitor progressMonitor) throws CoreException {
		final JavaMemberSearchResultCollector collector = new JavaMemberSearchResultCollector(IJavaElement.TYPE, searchScope);
		final SearchPattern pattern = SearchPattern.createPattern(annotationName, IJavaSearchConstants.ANNOTATION_TYPE,
				IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE | IJavaSearchConstants.TYPE, SearchPattern.R_EXACT_MATCH
						| SearchPattern.R_CASE_SENSITIVE);
		// perform search, results are added/filtered by the custom
		// searchRequestor defined above
		new SearchEngine().search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() },
				searchScope, collector, progressMonitor);
		return collector.getResult(IType.class);
	}

	/**
	 * Returns all JAX-RS resources resourceMethods (ie, class resourceMethods annotated with an @HttpMethod annotation)
	 * in the given scope (ex : javaProject).
	 * 
	 * @param scope
	 *            the search scope
	 * @param httpMethodNames
	 *            the fully qualified names of the existing types annotated with <code>javax.ws.rs.HttpMethod</code> annotation
	 * @param progressMonitor
	 *            the progress monitor
	 * @return the set of matching {@link IMethod}s
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public static Set<IMethod> findResourceMethods(final IJavaElement scope, final Set<String> httpMethodNames,
			final IProgressMonitor progressMonitor) throws CoreException {
		final IJavaSearchScope searchScope = createSearchScope(scope);
		final List<String> annotations = new ArrayList<String>(httpMethodNames.size() + 1);
		annotations.add(PATH);
		annotations.addAll(httpMethodNames);
		return searchForAnnotatedMethods(annotations, searchScope, progressMonitor);
	}

	/**
	 * Search for methods annotated with one of the given annotations, in the search scope.
	 * 
	 * @param annotationNames
	 *            the annotations fully qualified names
	 * @param searchScope
	 *            the search scope
	 * @param progressMonitor
	 *            the progress monitor
	 * @return the matching methods
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	private static Set<IMethod> searchForAnnotatedMethods(final List<String> annotationNames,
			final IJavaSearchScope searchScope, final IProgressMonitor progressMonitor) throws CoreException {
		final JavaMemberSearchResultCollector collector = new JavaMemberSearchResultCollector(IJavaElement.METHOD,
				searchScope);
		SearchPattern pattern = null;
		for (String annotationName : annotationNames) {
			// TODO : apply on METHOD instead of TYPE ?
			SearchPattern subPattern = SearchPattern.createPattern(annotationName,
					IJavaSearchConstants.ANNOTATION_TYPE, IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE,
					SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
			if (pattern == null) {
				pattern = subPattern;
			} else {
				pattern = SearchPattern.createOrPattern(pattern, subPattern);
			}
		}
		// perform search, results are added/filtered by the custom
		// searchRequestor defined above
		new SearchEngine().search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() },
				searchScope, collector, progressMonitor);
		return collector.getResult(IMethod.class);
	}
	

	/**
	 * Search for fields annotated with one of the given annotations, in the search scope.
	 * 
	 * @param annotationNames
	 *            the annotations fully qualified names
	 * @param searchScope
	 *            the search scope
	 * @param progressMonitor
	 *            the progress monitor
	 * @return the matching fields
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	private static Set<IField> searchForAnnotatedFields(final List<String> annotationNames,
			final IJavaSearchScope searchScope, final IProgressMonitor progressMonitor) throws CoreException {
		final JavaMemberSearchResultCollector collector = new JavaMemberSearchResultCollector(IJavaElement.FIELD,
				searchScope);
		SearchPattern pattern = null;
		for (String annotationName : annotationNames) {
			// TODO : apply on METHOD instead of TYPE ?
			SearchPattern subPattern = SearchPattern.createPattern(annotationName,
					IJavaSearchConstants.ANNOTATION_TYPE, IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE,
					SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
			if (pattern == null) {
				pattern = subPattern;
			} else {
				pattern = SearchPattern.createOrPattern(pattern, subPattern);
			}
		}
		// perform search, results are added/filtered by the custom
		// searchRequestor defined above
		new SearchEngine().search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() },
				searchScope, collector, progressMonitor);
		return collector.getResult(IField.class);
	}
	
	/**
	 * Returns all related {@link IType}s from the given 'scope' argument that reference the given {@link IType}
	 * @param type the type that is referenced
	 * @param types the search scope
	 * @param progressMonitor the progress monitor
	 * @return the set of related types
	 * @throws CoreException 
	 */
	public static Set<IType> findRelatedTypes(final IType type,
			final List<IType> types, final IProgressMonitor progressMonitor) throws CoreException {
		if(type == null || types == null || types.isEmpty()) {
			return Collections.emptySet();
		}
		final IJavaSearchScope searchScope = createSearchScope(types);
		final JavaMemberSearchResultCollector collector = new JavaMemberSearchResultCollector(IJavaElement.TYPE,
				searchScope);
		final SearchPattern pattern = SearchPattern.createPattern(type,
				IJavaSearchConstants.ALL_OCCURRENCES ,
				SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
		// perform search, results are added/filtered by the custom
		// searchRequestor defined above
		new SearchEngine().search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() },
				searchScope, collector, progressMonitor);
		return collector.getResult(IType.class);
	}

}