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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBuiltinHttpMethod.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResourceMethod;

/**
 * Manages all the JAX-RS domain classes of the JAX-RS Metamodel. Not only a POJO, but also provides business services.
 * 
 * @author xcoulon
 */
public class JaxrsMetamodel implements IJaxrsMetamodel {

	/**
	 * The qualified name of the metamodel when stored in the project session properties.
	 */
	public static final QualifiedName METAMODEL_QUALIFIED_NAME = new QualifiedName(JBossJaxrsCorePlugin.PLUGIN_ID,
			"metamodel");

	/** The enclosing JavaProject. */
	private final IJavaProject javaProject;

	/**
	 * All the subclasses of <code>javax.ws.rs.core.Application</code>, although there should be only one.
	 */
	private final List<IJaxrsApplication> applications = new ArrayList<IJaxrsApplication>();

	/**
	 * All the resources (both rootresources and subresources) available in the service , indexed by their associated
	 * java type fully qualified name.
	 */
	private final List<JaxrsResource> resources = new ArrayList<JaxrsResource>();

	/**
	 * The available providers (classes which implement MessageBodyWriter<T>, MessageBodyReader<T> or
	 * ExceptionMapper<T>), , indexed by their associated java type fully qualified name.
	 */
	private final List<JaxrsProvider> providers = new ArrayList<JaxrsProvider>();

	/** The HTTP ResourceMethod elements container. */
	private final List<JaxrsHttpMethod> httpMethods = new ArrayList<JaxrsHttpMethod>();

	/** Internal index of all the elements of this metamodel. */
	private final Map<String, Set<JaxrsBaseElement>> elementsIndex = new HashMap<String, Set<JaxrsBaseElement>>();

	/** the endpoints, built from the resource methods. */
	private final List<JaxrsEndpoint> endpoints = new ArrayList<JaxrsEndpoint>();

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
		preloadHttpMethods();
	}

	/**
	 * Preload the HttpMethods collection with 6 items from the specification:
	 * <ul>
	 * <li>@GET</li>
	 * <li>@POST</li>
	 * <li>@PUT</li>
	 * <li>@DELETE</li>
	 * <li>@OPTIONS</li>
	 * <li>@HEAD</li>
	 * </ul>
	 */
	private void preloadHttpMethods() {
		httpMethods.addAll(Arrays.asList(GET, POST, PUT, DELETE, HEAD, OPTIONS));
		elementsIndex.put(GET.getFullyQualifiedName(), new HashSet<JaxrsBaseElement>(Arrays.asList(GET)));
		elementsIndex.put(POST.getFullyQualifiedName(), new HashSet<JaxrsBaseElement>(Arrays.asList(POST)));
		elementsIndex.put(PUT.getFullyQualifiedName(), new HashSet<JaxrsBaseElement>(Arrays.asList(PUT)));
		elementsIndex.put(DELETE.getFullyQualifiedName(), new HashSet<JaxrsBaseElement>(Arrays.asList(DELETE)));
		elementsIndex.put(OPTIONS.getFullyQualifiedName(), new HashSet<JaxrsBaseElement>(Arrays.asList(OPTIONS)));
		elementsIndex.put(HEAD.getFullyQualifiedName(), new HashSet<JaxrsBaseElement>(Arrays.asList(HEAD)));
	}

	/*
	 * (non-Javadoc)
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.IMetamodel#getJavaProject ()
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
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public final void remove() throws CoreException {
		Logger.debug("JAX-RS Metamodel removed for project " + javaProject.getElementName());
		javaProject.getProject().setSessionProperty(METAMODEL_QUALIFIED_NAME, null);
	}

	public void add(JaxrsJavaElement<?> element) {
		switch (element.getElementKind()) {
		case APPLICATION:
			this.applications.add((JaxrsJavaApplication) element);
			break;
		case HTTP_METHOD:
			this.httpMethods.add((JaxrsHttpMethod) element);
			break;
		case PROVIDER:
			// this.providers.add(element);
			break;
		case RESOURCE:
			final JaxrsResource resource = (JaxrsResource) element;
			this.resources.add(resource);
			break;
		case RESOURCE_FIELD:
			break;
		case RESOURCE_METHOD:
			break;
		default:
			break;
		}
		indexElement(element);
	}

	public void add(JaxrsWebxmlApplication application) {
		this.applications.add(application);
		Collections.sort(this.applications, new Comparator<IJaxrsApplication>() {
			@Override
			public int compare(IJaxrsApplication app1, IJaxrsApplication app2) {
				return app1.getKind().compareTo(app2.getKind());
			}
		});
		indexElement(application, javaProject);
	}

	/** @param jaxrsElement */
	protected void indexElement(final JaxrsJavaElement<?> jaxrsElement) {
		Logger.trace("Indexing {}", jaxrsElement);
		final IJavaElement javaElement = jaxrsElement.getJavaElement();
		// first, unindex element to clear previous state
		unindexElement(jaxrsElement);
		// then, index for good
		indexElement(jaxrsElement, javaElement);
		// index element that are bound to a java type, not a field or a method
		// if (element.getJavaElement().getElementType() == IJavaElement.TYPE) {
		indexElement(jaxrsElement, JdtUtils.getCompilationUnit(javaElement));
		indexElement(jaxrsElement, JdtUtils.getPackageFragmentRoot(javaElement));
		indexElement(jaxrsElement, javaElement.getJavaProject());
		// }
		for (Entry<String, Annotation> entry : jaxrsElement.getAnnotations().entrySet()) {
			indexElement(jaxrsElement, entry.getValue());
		}
		if (jaxrsElement.getElementKind() == EnumElementKind.RESOURCE) {
			JaxrsResource resource = (JaxrsResource) jaxrsElement;
			for (JaxrsResourceMethod resourceMethod : resource.getMethods().values()) {
				indexElement(resourceMethod);
			}
			for (JaxrsResourceField resourceField : resource.getFields().values()) {
				indexElement(resourceField);
			}
		}

	}

	protected void indexElement(final JaxrsJavaElement<?> element, final Annotation annotation) {
		if (annotation != null) {
			indexElement(element, annotation.getJavaAnnotation());
		}
	}

	/**
	 * @param jaxrsElement
	 * @param javaElement
	 */
	private void indexElement(final JaxrsBaseElement jaxrsElement, final IJavaElement javaElement) {
		if (javaElement == null) {
			return;
		}
		final String key = javaElement.getHandleIdentifier();
		if (!elementsIndex.containsKey(key)) {
			elementsIndex.put(key, new HashSet<JaxrsBaseElement>(Arrays.asList(jaxrsElement)));
		} else {
			elementsIndex.get(key).add(jaxrsElement);
		}
	}

	/** @param jaxrsElement */
	protected void unindexElement(final JaxrsBaseElement jaxrsElement) {
		// if the given element is a JAX-RS Resource, also unindex its children
		// ResourceMethod
		if (jaxrsElement.getElementKind() == EnumElementKind.RESOURCE) {
			final JaxrsResource resource = (JaxrsResource) jaxrsElement;
			for (JaxrsResourceMethod resourceMethod : resource.getMethods().values()) {
				unindexElement(resourceMethod);
			}
			for (JaxrsResourceField resourceField : ((JaxrsResource) jaxrsElement).getFields().values()) {
				unindexElement(resourceField);
			}
		}
		// unindex the given element, whatever its kind
		for (Iterator<Entry<String, Set<JaxrsBaseElement>>> indexIterator = elementsIndex.entrySet().iterator(); indexIterator
				.hasNext();) {
			final Entry<String, Set<JaxrsBaseElement>> indexEntry = indexIterator.next();
			final Set<JaxrsBaseElement> indexEntryElements = indexEntry.getValue();
			// because the elements.remove(jaxrsElement); does not work here
			// (hashcode has changed between the time the jaxrsElement was added
			// and now !)
			for (Iterator<JaxrsBaseElement> indexEntryElementsIterator = indexEntryElements.iterator(); indexEntryElementsIterator.hasNext();) {
				JaxrsBaseElement element = indexEntryElementsIterator.next();
				if (element.equals(jaxrsElement)) {
					Logger.trace(" Removing {} from index", element);
					indexEntryElementsIterator.remove();
				}
			}

			if (indexEntryElements.isEmpty()) {
				indexIterator.remove();
			}
		}
	}

	/**
	 * Unindex the given JAX-RS Element so that it cannot be retrieved when searching for elements with the given
	 * handleIdentifier. This does not mean that the given JAX-RS Element won't be findable anymore.
	 * 
	 * @param jaxrsElement
	 * @param handleIdentifier
	 */
	protected void unindexElement(final JaxrsBaseElement jaxrsElement, final String handleIdentifier) {
		Set<JaxrsBaseElement> jaxrsElements = elementsIndex.get(handleIdentifier);
		if (jaxrsElements != null) {
			jaxrsElements.remove(jaxrsElement);
		}
	}

	/**
	 * @return the application that is used to compute the Endpoint's URI Path Templates, or null if no application was
	 *         specified in the code. An invalid application may be returned, though (ie, a Type annotated with
	 *         {@link javax.ws.rs.ApplicationPath} but not extending the {@link javax.ws.rs.Application} type).
	 */
	public final IJaxrsApplication getApplication() {
		if (applications.isEmpty()) {
			return null;
		}
		return applications.get(0);
	}

	public final List<IJaxrsApplication> getAllApplications() {
		return Collections.unmodifiableList(new ArrayList<IJaxrsApplication>(applications));
	}

	public final List<IJaxrsProvider> getAllProviders() {
		return Collections.unmodifiableList(new ArrayList<IJaxrsProvider>(providers));
	}

	public final List<IJaxrsHttpMethod> getAllHttpMethods() {
		return Collections.unmodifiableList(new ArrayList<IJaxrsHttpMethod>(httpMethods));
	}

	public final List<IJaxrsResource> getAllResources() {
		return Collections.unmodifiableList(new ArrayList<IJaxrsResource>(resources));
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
	public final JaxrsJavaElement<?> find(final IJavaElement element) throws JavaModelException {
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
	 * @param annotation
	 *            (GET, POST, etc..)
	 * @param metamodel
	 * @param annotationName
	 * @return
	 * @throws CoreException
	 */
	public IJaxrsHttpMethod getHttpMethod(final String annotationName) throws CoreException {
		IType annotationType = JdtUtils.resolveType(annotationName, javaProject, new NullProgressMonitor());
		if (annotationType != null) {
			// look for custom HTTP Methods
			JaxrsBaseElement element = getElement(annotationType);
			if (element != null && element.getElementKind() == EnumElementKind.HTTP_METHOD) {
				return (IJaxrsHttpMethod) element;
			}
			// if not found, look for built-in HTTP Methods
			else if(element == null) {
				element = getElement(annotationType.getFullyQualifiedName());
				if (element != null && element.getElementKind() == EnumElementKind.HTTP_METHOD) {
					return (IJaxrsHttpMethod) element;
				}
			}
		}
		return null;
	}


	/**
	 * returns true if this metamodel already contains the given element.
	 * 
	 * @param element
	 * @return
	 */
	public boolean containsElement(JaxrsJavaElement<?> element) {
		return (getElement(element.getJavaElement()) != null);
	}

	public JaxrsBaseElement getElement(IJavaElement element) {
		if (element == null) {
			return null;
		}
		return getElement(element.getHandleIdentifier());
	}
	
	protected JaxrsBaseElement getElement(final String elementName) {
		if (elementName == null) {
			return null;
		}
		final Set<JaxrsBaseElement> elements = elementsIndex.get(elementName);
		if (elements == null || elements.isEmpty()) {
			return null;
		}
		return elements.iterator().next();
	}

	public JaxrsBaseElement getElement(Annotation annotation) {
		return getElement(annotation.getJavaAnnotation());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getElement(IJavaElement element, Class<T> clazz) {
		final JaxrsBaseElement jaxrsElement = getElement(element);
		if (jaxrsElement != null && clazz.isAssignableFrom(jaxrsElement.getClass())) {
			return (T) jaxrsElement;
		}
		return null;
	}

	public List<JaxrsBaseElement> getElements(final IJavaElement javaElement) {
		final String key = javaElement.getHandleIdentifier();
		final List<JaxrsBaseElement> result = new ArrayList<JaxrsBaseElement>();
		if (elementsIndex.containsKey(key)) {
			final Set<JaxrsBaseElement> indexedElements = elementsIndex.get(key);
			result.addAll(indexedElements);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public <T extends JaxrsBaseElement> List<T> getElements(final IJavaElement javaElement,
			Class<? extends JaxrsBaseElement> T) {
		final String key = javaElement.getHandleIdentifier();
		final List<T> elements = new ArrayList<T>();
		if (elementsIndex.containsKey(key)) {
			for (JaxrsBaseElement element : elementsIndex.get(key)) {
				if (element.getClass().isAssignableFrom(T) || T.isAssignableFrom(element.getClass())) {
					elements.add((T) element);
				}
			}
		}
		return elements;
	}

	public void remove(JaxrsBaseElement element) {
		switch (element.getKind()) {
		case APPLICATION_WEBXML:
			remove((JaxrsWebxmlApplication) element);
			break;
		default:
			remove((JaxrsJavaElement<?>)element);
			break;
		}
	}

	/**
	 * Remove the given JAX-RS Element from the metamodel.
	 * 
	 * @param resource
	 * @return true if the resource was actually removed, false otherwise.
	 */
	public void remove(JaxrsJavaElement<?> element) {
		if (element == null) {
			return;
		}
		switch (element.getElementKind()) {
		case APPLICATION:
			this.applications.remove(element);
			break;
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
			final JaxrsResource fieldResourceParent = ((JaxrsResourceField) element).getParentResource();
			fieldResourceParent.removeField((JaxrsResourceField) element);
			break;
		}
		unindexElement(element);
	}

	public void remove(JaxrsWebxmlApplication application) {
		this.applications.remove(application);
		unindexElement(application);
	}

	public JaxrsHttpMethod getHttpMethod(Annotation httpMethodAnnotation) {
		if (httpMethodAnnotation != null) {
			for (IJaxrsHttpMethod httpMethod : httpMethods) {
				final String handleIdentifier1 = httpMethod.getFullyQualifiedName();
				final String handleIdentifier2 = httpMethodAnnotation.getName();
				if (handleIdentifier1.equals(handleIdentifier2)) {
					return (JaxrsHttpMethod) httpMethod;
				}
			}
		}
		return null;
	}

	public boolean add(JaxrsEndpoint endpoint) {
		if (this.endpoints.contains(endpoint)) {
			return false;
		}
		this.endpoints.add(endpoint);
		return true;
	}

	public List<JaxrsEndpoint> getEndpoints() {
		return this.endpoints;
	}

	@Override
	public List<IJaxrsEndpoint> getAllEndpoints() {
		return Collections.unmodifiableList(new ArrayList<IJaxrsEndpoint>(endpoints));
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((javaProject == null) ? 0 : javaProject.getHandleIdentifier().hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		JaxrsMetamodel other = (JaxrsMetamodel) obj;
		if (javaProject == null && other.javaProject != null) {
			return false;
		} else if (javaProject != null && other.javaProject == null) {
			return false;
		} else if (javaProject != null && other.javaProject != null && !javaProject.getHandleIdentifier().equals(other.javaProject.getHandleIdentifier())) {
			return false;
		}
		return true;
	}

	@Override
	public IProject getProject() {
		if (javaProject == null) {
			return null;
		}
		return javaProject.getProject();
	}


}
