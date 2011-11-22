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

package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResourceMethod;

/**
 * Manages all the JAX-RS domain classes of the JAX-RS Metamodel. Not only a
 * POJO, but also provides business services.
 * 
 * @author xcoulon
 */
public class JaxrsMetamodel implements IJaxrsMetamodel {

	/**
	 * The qualified name of the metamodel when stored in the project session
	 * properties.
	 */
	public static final QualifiedName METAMODEL_QUALIFIED_NAME = new QualifiedName(JBossJaxrsCorePlugin.PLUGIN_ID,
			"metamodel");

	/** The enclosing JavaProject. */
	private final IJavaProject javaProject;

	/** The Service URI. Default is "/" */
	private String serviceUri = "/";

	/**
	 * All the subclasses of <code>javax.ws.rs.core.Application</code>, although
	 * there should be only one.
	 */
	private final List<IJaxrsApplication> applications = new ArrayList<IJaxrsApplication>();

	/**
	 * All the resources (both rootresources and subresources) available in the
	 * service , indexed by their associated java type fully qualified name.
	 */
	private final List<IJaxrsResource> resources = new ArrayList<IJaxrsResource>();

	/**
	 * The available providers (classes which implement MessageBodyWriter<T>,
	 * MessageBodyReader<T> or ExceptionMapper<T>), , indexed by their
	 * associated java type fully qualified name.
	 */
	private final List<IJaxrsProvider> providers = new ArrayList<IJaxrsProvider>();

	/** The HTTP ResourceMethod elements container. */
	private final List<IJaxrsHttpMethod> httpMethods = new ArrayList<IJaxrsHttpMethod>();

	/** Internal index of all the elements of this metamodel. */
	private final Map<String, Set<IJaxrsElement<?>>> elementsIndex = new HashMap<String, Set<IJaxrsElement<?>>>();

	/** the endpoints, built from the resource methods. */
	private final List<IJaxrsEndpoint> endpoints = new ArrayList<IJaxrsEndpoint>();

	/**
	 * Full constructor.
	 * 
	 * @param javaProject
	 *            the enclosing java project
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	private JaxrsMetamodel(final IJavaProject javaProject) throws CoreException {
		this.javaProject = javaProject;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.IMetamodel#getJavaProject
	 * ()
	 */
	public IJavaProject getJavaProject() {
		return javaProject;
	}

	/**
	 * Accessor to the metamodel from the given project's session properties.
	 * 
	 * @param javaProject
	 *            the java project
	 * @return the metamodel or null if none was found
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public static JaxrsMetamodel create(final IJavaProject javaProject) throws CoreException {
		if (javaProject == null || javaProject.getProject() == null) {
			return null;
		}
		Logger.debug("JAX-RS Metamodel created for project " + javaProject.getElementName());
		JaxrsMetamodel metamodel = new JaxrsMetamodel(javaProject);
		javaProject.getProject().setSessionProperty(METAMODEL_QUALIFIED_NAME, metamodel);
		return metamodel;
	}

	/**
	 * Accessor to the metamodel from the given project's session properties.
	 * 
	 * @param javaProject
	 *            the java project
	 * @return the metamodel or null if none was found
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public static JaxrsMetamodel get(final IJavaProject javaProject) throws CoreException {
		if (javaProject == null || javaProject.getProject() == null) {
			return null;
		}
		JaxrsMetamodel metamodel = (JaxrsMetamodel) javaProject.getProject().getSessionProperty(
				METAMODEL_QUALIFIED_NAME);

		return metamodel;
	}

	/**
	 * Accessor to the metamodel from the given project's session properties.
	 * 
	 * @param project
	 *            the project
	 * @return the metamodel or null if none was found
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public static JaxrsMetamodel get(final IProject project) throws CoreException {
		return get(JavaCore.create(project));
	}

	/**
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public final void remove() throws CoreException {
		Logger.debug("JAX-RS Metamodel removed for project " + javaProject.getElementName());
		javaProject.getProject().setSessionProperty(METAMODEL_QUALIFIED_NAME, null);
	}

	public void add(IJaxrsElement<?> element) {
		switch (element.getElementKind()) {
		case APPLICATION:
			break;
		case HTTP_METHOD:
			this.httpMethods.add((IJaxrsHttpMethod) element);
			break;
		case PROVIDER:
			// this.providers.add(element);
			break;
		case RESOURCE:
			this.resources.add((IJaxrsResource) element);
			break;
		/*
		 * case RESOURCE_FIELD: final JaxrsResource fieldParent =
		 * findResource((IType) element.getJavaElement().getParent()); if
		 * (fieldParent != null) { fieldParent.addField((JaxrsParamField)
		 * element); } break; case RESOURCE_METHOD: final JaxrsResource
		 * methodParent = findResource((IType)
		 * element.getJavaElement().getParent()); if (methodParent != null) {
		 * methodParent.addMethod((IJaxrsResourceMethod) element); } break;
		 */
		}
		indexElement(element);
	}

	/** @param element */
	protected void indexElement(final IJaxrsElement<?> element) {
		final IJavaElement javaElement = element.getJavaElement();
		indexElement(element, javaElement);
		// index element that are bound to a java type, not a field or a method
		if (element.getJavaElement().getElementType() == IJavaElement.TYPE) {
			indexElement(element, JdtUtils.getCompilationUnit(javaElement));
			indexElement(element, JdtUtils.getPackageFragmentRoot(javaElement));
			indexElement(element, javaElement.getJavaProject());
		}
		for (Annotation annotation : element.getAnnotations()) {
			indexElement(element, annotation);
		}
	}

	protected void indexElement(final IJaxrsElement<?> element, final Annotation annotation) {
		if (annotation != null) {
			indexElement(element, annotation.getJavaAnnotation());
		}
	}

	/**
	 * @param jaxrsElement
	 * @param javaElement
	 */
	@SuppressWarnings("unchecked")
	private void indexElement(final IJaxrsElement<?> jaxrsElement, final IJavaElement javaElement) {
		if (javaElement == null) {
			return;
		}
		final String key = javaElement.getHandleIdentifier();
		if (!elementsIndex.containsKey(key)) {
			elementsIndex.put(key, new HashSet<IJaxrsElement<?>>(Arrays.asList(jaxrsElement)));
		} else {
			elementsIndex.get(key).add(jaxrsElement);
		}
	}

	/** @param jaxrsElement */
	protected void unindexElement(final IJaxrsElement<?> jaxrsElement) {
		for (Iterator<Entry<String, Set<IJaxrsElement<?>>>> iterator = elementsIndex.entrySet().iterator(); iterator
				.hasNext();) {
			final Entry<String, Set<IJaxrsElement<?>>> entry = iterator.next();
			final Set<IJaxrsElement<?>> elements = entry.getValue();
			if (elements.contains(jaxrsElement)) {
				elements.remove(jaxrsElement);
				if (elements.isEmpty()) {
					iterator.remove();
				}
			}
		}
	}

	/** @param jaxrsElement */
	protected void unindexElement(final IJaxrsElement<?> jaxrsElement, final String handleIdentifier) {
		Set<IJaxrsElement<?>> jaxrsElements = elementsIndex.get(handleIdentifier);
		if (jaxrsElements != null) {
			jaxrsElements.remove(jaxrsElement);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.IMetamodel#getProviders
	 * ()
	 */
	@Override
	public final List<IJaxrsProvider> getAllProviders() {
		return Collections.unmodifiableList(providers);
	}

	/**
	 * @param field
	 * @param iJavaElement
	 * @return private JaxrsResource findResource(final IType javaElement) {
	 *         final String targetIdentifier =
	 *         javaElement.getHandleIdentifier(); for (IJaxrsResource resource :
	 *         resources) { final String resourceIdentifier =
	 *         resource.getJavaElement().getHandleIdentifier(); if
	 *         (resourceIdentifier.equals(targetIdentifier)) { return
	 *         (JaxrsResource) resource; } } return null; }
	 */

	@Override
	public final List<IJaxrsHttpMethod> getAllHttpMethods() {
		return Collections.unmodifiableList(httpMethods);
	}

	public final List<IJaxrsResource> getAllResources() {
		return Collections.unmodifiableList(resources);
	}

	@Override
	public final String getServiceUri() {
		return serviceUri;
	}

	/**
	 * Sets the Base URI for the URI mapping templates.
	 * 
	 * @param uri
	 *            the serviceUri to set
	 */
	public final void setServiceUri(final String uri) {
		// remove trailing "*" character, if present.
		if (uri.endsWith("*")) {
			this.serviceUri = uri.substring(0, uri.length() - 1);
		} else {
			this.serviceUri = uri;
		}
	}

	/**
	 * Returns the JAX-RS ElementKind associated with the given java element.
	 * 
	 * @param element
	 *            the underlying java element (can be IType or IMethod)
	 * @return the associated JAX-RS element, or null if none found
	 * @throws JavaModelException
	 *             in case of underlying exception
	 */
	public final IJaxrsElement<?> find(final IJavaElement element) throws JavaModelException {
		switch (element.getElementType()) {
		case IJavaElement.TYPE:
			// return findElement((IType) element);
		case IJavaElement.METHOD:
			// return findElement((IMethod) element);
		default:
			break;
		}
		return null;
	}

	/**
	 * Report errors from the given markers into the JAX-RS element(s)
	 * associated with the given compiltation unit.
	 * 
	 * @param compilationUnit
	 *            the compilation unit
	 * @param markers
	 *            the markers
	 * @return true if errors were found and reported, false otherwise
	 * @throws JavaModelException
	 *             in case of underlying exception
	 */
	public final boolean reportErrors(final ICompilationUnit compilationUnit, final IMarker[] markers)
			throws JavaModelException {
		boolean hasErrors = false;
		for (IMarker marker : markers) {
			if (marker.getAttribute(IMarker.SEVERITY, 0) != IMarker.SEVERITY_ERROR) {
				continue;
			}
			Logger.debug("Error found: " + marker.getAttribute(IMarker.MESSAGE, ""));
			JaxrsElement<?> element = (JaxrsElement<?>) find(compilationUnit.getElementAt(marker.getAttribute(
					IMarker.CHAR_START, 0)));
			if (element != null) {
				element.hasErrors(true);
			}
			hasErrors = true;
		}
		return hasErrors;
	}

	/**
	 * Resets this metamodel for further re-use (ie, before a new 'full/clean'
	 * build). Keeping the same instance of Metamodel in the project's session
	 * properties is a convenient thing, especially on the UI side, where some
	 * caching system is use to maintain the state of nodes in the Common
	 * Navigator (framework).
	 */
	public void reset() {
		Logger.debug("Reseting the JAX-RS Metamodel fpr project {}", this.javaProject.getElementName());
		this.applications.clear();
		this.httpMethods.clear();
		this.providers.clear();
		this.resources.clear();
		this.elementsIndex.clear();
	}

	/**
	 * @param annotation
	 * @param metamodel
	 * @param annotationName
	 * @return
	 * @throws CoreException
	 */
	public IJaxrsHttpMethod getHttpMethod(final String annotationName) throws CoreException {
		IType annotationType = JdtUtils.resolveType(annotationName, javaProject, new NullProgressMonitor());
		if (annotationType != null) {
			final IJaxrsElement<?> element = getElement(annotationType);
			if (element != null && element.getElementKind() == EnumElementKind.HTTP_METHOD) {
				return (IJaxrsHttpMethod) element;
			}
		}
		return null;
	}

	@Override
	public IJaxrsElement<?> getElement(IJavaElement element) {
		if (element == null) {
			return null;
		}
		final String handleIdentifier = element.getHandleIdentifier();
		final Set<IJaxrsElement<?>> elements = elementsIndex.get(handleIdentifier);
		if (elements == null || elements.isEmpty()) {
			return null;
		}
		return elements.iterator().next();
	}

	public IJaxrsElement<?> getElement(Annotation annotation) {
		return getElement(annotation.getJavaAnnotation());
	}

	@SuppressWarnings("unchecked")
	public <T> T getElement(IJavaElement element, Class<T> clazz) {
		return (T) getElement(element);
	}

	public List<IJaxrsElement<?>> getElements(final IJavaElement javaElement) {
		final String key = javaElement.getHandleIdentifier();
		final List<IJaxrsElement<?>> elements = new ArrayList<IJaxrsElement<?>>();
		if (elementsIndex.containsKey(key)) {
			for (IJaxrsElement<?> element : elementsIndex.get(key)) {
				elements.add(element);
			}
		}
		return elements;
	}

	@SuppressWarnings("unchecked")
	public <T extends IJaxrsElement<?>> List<T> getElements(final IJavaElement javaElement, Class<?> T) {
		final String key = javaElement.getHandleIdentifier();
		final List<T> elements = new ArrayList<T>();
		if (elementsIndex.containsKey(key)) {
			for (IJaxrsElement<?> element : elementsIndex.get(key)) {
				// TODO avoid classcast exceptions
				elements.add((T) element);
			}
		}
		return elements;
	}

	/**
	 * Remove the given JAX-RS Resource from the metamodel.
	 * 
	 * @param resource
	 * @return true if the resource was actually removed, false otherwise.
	 */
	public void remove(IJaxrsElement<?> element) {
		if (element == null) {
			return;
		}
		switch (element.getElementKind()) {
		case HTTP_METHOD:
			this.httpMethods.remove(element);
			break;
		case RESOURCE:
			this.resources.remove(element);
			break;
		case RESOURCE_METHOD:
			final JaxrsResource parentResource = ((JaxrsResourceMethod) element).getParentResource();
			parentResource.removeMethod((IJaxrsResourceMethod) element);
			break;
		case RESOURCE_FIELD:
			final JaxrsResource fieldResourceParent = ((JaxrsParamField) element).getParentResource();
			fieldResourceParent.removeField((JaxrsParamField) element);
			break;
		}
		unindexElement(element);
	}

	@Override
	public List<IJaxrsEndpoint> getAllEndpoints() {
		return endpoints;
	}

	public IJaxrsHttpMethod getHttpMethod(Annotation httpMethodAnnotation) {
		if (httpMethodAnnotation != null) {
			for (IJaxrsHttpMethod httpMethod : httpMethods) {
				final String handleIdentifier1 = httpMethod.getJavaElement().getFullyQualifiedName();
				final String handleIdentifier2 = httpMethodAnnotation.getName();
				if (handleIdentifier1.equals(handleIdentifier2)) {
					return httpMethod;
				}
			}
		}
		return null;
	}

	public boolean add(IJaxrsEndpoint endpoint) {
		if (this.endpoints.contains(endpoint)) {
			return false;
		}
		this.endpoints.add(endpoint);
		return true;
	}

	public IJaxrsElement<?> getElement(IResource resource) {

		return null;
	}

}
