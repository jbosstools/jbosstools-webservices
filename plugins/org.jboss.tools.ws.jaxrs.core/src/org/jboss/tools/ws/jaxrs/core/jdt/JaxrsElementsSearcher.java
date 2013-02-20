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

package org.jboss.tools.ws.jaxrs.core.jdt;

import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.APPLICATION;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.EXCEPTION_MAPPER;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.HTTP_METHOD;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.MESSAGE_BODY_READER;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.MESSAGE_BODY_WRITER;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PROVIDER;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
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
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsHttpMethod;

/**
 * Class that scan the projec's classpath to find JAX-RS Resources and Providers.
 */
public final class JaxrsElementsSearcher {

	/** Private constructor of the utility class. */
	private JaxrsElementsSearcher() {

	}

	/**
	 * Returns all JAX-RS Applications in the given scope (ex : javaProject), ie, types annotated with
	 * <code>javax.ws.rs.ApplicationPath</code> annotation and subtypes of
	 * {@link javax.ws.rs.Application} (even if type hirarchy or annotation is missing).
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
	public static List<IType> findApplicationTypes(final IJavaElement scope, final IProgressMonitor progressMonitor)
			throws CoreException {
		//FIXME: need correct usage of progressmonitor/subprogress monitor
		
		// first, search for type annotated with <code>javax.ws.rs.ApplicationPath</code>
		IJavaSearchScope searchScope = null;
		searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { scope }, IJavaSearchScope.SOURCES
				| IJavaSearchScope.REFERENCED_PROJECTS);
		
		final List<IType> applicationTypes = searchForAnnotatedTypes(APPLICATION_PATH.qualifiedName, searchScope, progressMonitor);
		// the search result also includes all subtypes of javax.ws.rs.core.Application (while avoiding duplicate results)
		final List<IType> applicationSubtypes = findSubtypes(scope, APPLICATION.qualifiedName, progressMonitor);
		applicationTypes.addAll(CollectionUtils.difference(applicationSubtypes, applicationTypes));
		return applicationTypes;
	}

	/**
	 * Returns all JAX-RS providers in the given scope (ex : javaProject), ie, types annotated with
	 * <code>javax.ws.rs.ext.Provider</code> annotation.
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
	public static List<IType> findProviderTypes(final IJavaElement scope, final IProgressMonitor progressMonitor)
			throws CoreException {
		final List<IType> providerTypes = new ArrayList<IType>();
		final IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { scope }, IJavaSearchScope.SOURCES
					| IJavaSearchScope.REFERENCED_PROJECTS);
		final List<IType> annotatedTypes = searchForAnnotatedTypes(PROVIDER.qualifiedName, searchScope, progressMonitor);
		providerTypes.addAll(annotatedTypes);
		// also search for one or more provider subtypes
		final List<IType> messageBodyReaderSubtypes = findSubtypes(scope, MESSAGE_BODY_READER.qualifiedName, progressMonitor);
		providerTypes.addAll(CollectionUtils.difference(messageBodyReaderSubtypes, providerTypes));
		final List<IType> messageBodyWriterSubtypes = findSubtypes(scope, MESSAGE_BODY_WRITER.qualifiedName, progressMonitor);
		providerTypes.addAll(CollectionUtils.difference(messageBodyWriterSubtypes, providerTypes));
		final List<IType> exceptionMapperSubtypes = findSubtypes(scope, EXCEPTION_MAPPER.qualifiedName, progressMonitor);
		providerTypes.addAll(CollectionUtils.difference(exceptionMapperSubtypes, providerTypes));
		
		return providerTypes;
	}

	/**
	 * @param scope
	 * @param progressMonitor
	 * @param typeName TODO
	 * @return
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
	 * Returns all JAX-RS resources resourceMethods (ie, class resourceMethods annotated with an @HttpMethod annotation)
	 * in the given scope (ex : javaProject).
	 * 
	 * @param scope
	 *            the search scope (project, compilation unit, type, etc.)
	 * @param httpMethodNames
	 *            the fully qualified names of the HttpMethod java types.
	 * @param progressMonitor
	 *            the progress monitor
	 * @return JAX-RS resource resourceMethods in a map, indexed by the declaring type of the resourceMethods
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public static List<IType> findResourceTypes(final IJavaElement scope, final IProgressMonitor progressMonitor)
			throws CoreException {
		IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { scope },
				IJavaSearchScope.SOURCES | IJavaSearchScope.REFERENCED_PROJECTS);
		return searchForAnnotatedTypes(PATH.qualifiedName, searchScope, progressMonitor);
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
	public static List<IType> findHttpMethodTypes(final IJavaElement scope, final IProgressMonitor progressMonitor)
			throws CoreException {
		IJavaSearchScope searchScope = null;
		if (scope instanceof IJavaProject) {
			searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { scope },
					IJavaSearchScope.SOURCES | IJavaSearchScope.REFERENCED_PROJECTS);
		} else {
			searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { scope });
		}
		return searchForAnnotatedTypes(HTTP_METHOD.qualifiedName, searchScope, progressMonitor);
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
	private static List<IType> searchForAnnotatedTypes(final String annotationName, final IJavaSearchScope searchScope,
			final IProgressMonitor progressMonitor) throws CoreException {
		JavaMemberSearchResultCollector collector = new JavaMemberSearchResultCollector(IJavaElement.TYPE, searchScope);
		SearchPattern pattern = SearchPattern.createPattern(annotationName, IJavaSearchConstants.ANNOTATION_TYPE,
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
	 * @param list
	 *            the types annotated with <code>javax.ws.rs.HttpMethod</code> annotation
	 * @param progressMonitor
	 *            the progress monitor
	 * @return JAX-RS resource resourceMethods in a map, indexed by the declaring type of the resourceMethods
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public static List<IMethod> findResourceMethods(final IJavaElement scope, final List<IJaxrsHttpMethod> httpMethods,
			final IProgressMonitor progressMonitor) throws CoreException {
		IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { scope },
				IJavaSearchScope.SOURCES | IJavaSearchScope.REFERENCED_PROJECTS);
		List<String> annotations = new ArrayList<String>(httpMethods.size() + 1);
		annotations.add(PATH.qualifiedName);
		for (IJaxrsHttpMethod httpMethod : httpMethods) {
			annotations.add(httpMethod.getJavaClassName());
		}
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
	private static List<IMethod> searchForAnnotatedMethods(final List<String> annotationNames,
			final IJavaSearchScope searchScope, final IProgressMonitor progressMonitor) throws CoreException {
		JavaMemberSearchResultCollector collector = new JavaMemberSearchResultCollector(IJavaElement.METHOD,
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
		// FIXME : wrong scope : returns all the annotated resourceMethods of
		// the enclosing type
		return collector.getResult(IMethod.class);
	}

}