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

import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBuiltinHttpMethod.DELETE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBuiltinHttpMethod.GET;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBuiltinHttpMethod.HEAD;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBuiltinHttpMethod.OPTIONS;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBuiltinHttpMethod.POST;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBuiltinHttpMethod.PUT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsProvider;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResourceField;
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

	/**
	 * All the subclasses of <code>javax.ws.rs.core.Application</code>, although
	 * there should be only one.
	 */
	private final List<IJaxrsApplication> applications = new ArrayList<IJaxrsApplication>();

	/**
	 * All the resources (both rootresources and subresources) available in the
	 * service , indexed by their associated java type fully qualified name.
	 */
	private final List<JaxrsResource> resources = new ArrayList<JaxrsResource>();

	/**
	 * The available providers (classes which implement MessageBodyWriter<T>,
	 * MessageBodyReader<T> or ExceptionMapper<T>), , indexed by their
	 * associated java type fully qualified name.
	 */
	private final List<JaxrsProvider> providers = new ArrayList<JaxrsProvider>();

	/** The HTTP ResourceMethod elements container. */
	private final List<JaxrsHttpMethod> httpMethods = new ArrayList<JaxrsHttpMethod>();

	/**
	 * Internal index of all the elements of this metamodel (by handleIdentifier
	 * of their associated java element).
	 */
	private final Map<String, Set<IJaxrsElement>> elementsIndex = new HashMap<String, Set<IJaxrsElement>>();

	/**
	 * Internal index of all the elements of this metamodel (by fullpath of
	 * their underlying resource).
	 */
	private final Map<String, Set<IJaxrsElement>> resourcesIndex = new HashMap<String, Set<IJaxrsElement>>();

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
		init();
	}

	@Override
	public IJaxrsMetamodel getMetamodel() {
		return this;
	}

	@Override
	public boolean isBinary() {
		// Metamodel is never binary
		return false;
	}

	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.METAMODEL;
	}

	@Override
	public EnumElementCategory getElementCategory() {
		return EnumElementCategory.METAMODEL;
	}

	@Override
	public boolean isMarkedForRemoval() {
		// this element should not be removed 
		return false;
	}

	@Override
	public IResource getResource() {
		return getProject();
	}

	@Override
	public String getName() {
		return "JAX-RS Metamodel for project " + getProject().getName();
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
	private void init() {
		httpMethods.addAll(Arrays.asList(GET, POST, PUT, DELETE, HEAD, OPTIONS));
		elementsIndex.put(GET.getJavaClassName(), new HashSet<IJaxrsElement>(Arrays.asList(GET)));
		elementsIndex.put(POST.getJavaClassName(), new HashSet<IJaxrsElement>(Arrays.asList(POST)));
		elementsIndex.put(PUT.getJavaClassName(), new HashSet<IJaxrsElement>(Arrays.asList(PUT)));
		elementsIndex.put(DELETE.getJavaClassName(), new HashSet<IJaxrsElement>(Arrays.asList(DELETE)));
		elementsIndex.put(OPTIONS.getJavaClassName(), new HashSet<IJaxrsElement>(Arrays.asList(OPTIONS)));
		elementsIndex.put(HEAD.getJavaClassName(), new HashSet<IJaxrsElement>(Arrays.asList(HEAD)));

		indexElement(this, getProject());
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
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public final void remove() throws CoreException {
		Logger.debug("JAX-RS Metamodel removed for project " + javaProject.getElementName());
		javaProject.getProject().setSessionProperty(METAMODEL_QUALIFIED_NAME, null);
	}

	public void add(JaxrsJavaElement<?> element) {
		if(element instanceof JaxrsJavaApplication) {
			this.applications.add((JaxrsJavaApplication) element);
		} else if(element instanceof JaxrsHttpMethod) {
			this.httpMethods.add((JaxrsHttpMethod) element);
		} else if(element instanceof JaxrsProvider) {
			this.providers.add((JaxrsProvider) element);
		} else if(element instanceof JaxrsResource) {
			this.resources.add((JaxrsResource) element);
		}
		indexElement(element);
	}

	public void add(JaxrsWebxmlApplication application) {
		this.applications.add(application);
		/*
		Collections.sort(this.applications, new Comparator<IJaxrsApplication>() {
			@Override
			public int compare(IJaxrsApplication app1, IJaxrsApplication app2) {
				return app1.getElementKind().compareTo(app2.getElementKind());
			}
		});*/
		indexElement(application, this.javaProject);
		indexElement(application, application.getResource());
	}

	/** @param jaxrsElement */
	protected void indexElement(final JaxrsJavaElement<?> jaxrsElement) {
		Logger.trace("Indexing {}", jaxrsElement);
		final IJavaElement javaElement = jaxrsElement.getJavaElement();
		// first, unindex element to clear previous state
		unindexElement(jaxrsElement);
		// then, index for good
		indexElement(jaxrsElement, javaElement);
		indexElement(jaxrsElement, javaElement.getResource());
		// index element that are bound to a java type, not a field or a method
		// if (element.getJavaElement().getElementType() == IJavaElement.TYPE) {
		indexElement(jaxrsElement, JdtUtils.getCompilationUnit(javaElement));
		indexElement(jaxrsElement, JdtUtils.getPackageFragmentRoot(javaElement));
		indexElement(jaxrsElement, javaElement.getJavaProject());
		// }
		for (Entry<String, Annotation> entry : jaxrsElement.getAnnotations().entrySet()) {
			indexElement(jaxrsElement, entry.getValue());
		}
		if (jaxrsElement.getElementCategory() == EnumElementCategory.RESOURCE) {
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
		if (annotation != null && annotation.getJavaAnnotation() != null) {
			indexElement(element, annotation.getJavaAnnotation());
			indexElement(element, annotation.getJavaAnnotation().getResource());
		}
	}

	/**
	 * @param jaxrsElement
	 *            the JAX-RS element of the metamodel to index
	 * @param javaElement
	 *            the associated Java Element
	 */
	private void indexElement(final JaxrsBaseElement jaxrsElement, final IJavaElement javaElement) {
		if (javaElement == null) {
			return;
		}
		final String key = javaElement.getHandleIdentifier();
		if (!elementsIndex.containsKey(key)) {
			elementsIndex.put(key, new HashSet<IJaxrsElement>(Arrays.asList(jaxrsElement)));
		} else {
			elementsIndex.get(key).add(jaxrsElement);
		}
	}

	/**
	 * @param jaxrsElement
	 *            the JAX-RS element of the metamodel to index
	 * @param resource
	 *            the underlying resource
	 */
	private void indexElement(final IJaxrsElement jaxrsElement, final IResource resource) {
		if (resource == null) {
			return;
		}
		final String key = resource.getFullPath().toPortableString();
		if (!resourcesIndex.containsKey(key)) {
			resourcesIndex.put(key, new HashSet<IJaxrsElement>(Arrays.asList(jaxrsElement)));
		} else {
			resourcesIndex.get(key).add(jaxrsElement);
		}
	}

	/** @param jaxrsElement */
	protected void unindexElement(final IJaxrsElement jaxrsElement) {
		// if the given element is a JAX-RS Resource, also unindex its children
		// ResourceMethod
		if (jaxrsElement.getElementCategory() == EnumElementCategory.RESOURCE) {
			final JaxrsResource resource = (JaxrsResource) jaxrsElement;
			for (JaxrsBaseElement resourceMethod : resource.getMethods().values()) {
				unindexElement(resourceMethod);
			}
			for (JaxrsResourceField resourceField : ((JaxrsResource) jaxrsElement).getFields().values()) {
				unindexElement(resourceField);
			}
		}
		// unindex the given element, whatever its kind
		unindex(jaxrsElement, elementsIndex);
		unindex(jaxrsElement, resourcesIndex);
	}

	/**
	 * @param jaxrsElement
	 * @param index
	 */
	private void unindex(final IJaxrsElement jaxrsElement, Map<String, Set<IJaxrsElement>> index) {
		for (Iterator<Entry<String, Set<IJaxrsElement>>> indexIterator = index.entrySet().iterator(); indexIterator
				.hasNext();) {
			final Entry<String, Set<IJaxrsElement>> indexEntry = indexIterator.next();
			final Set<IJaxrsElement> indexEntryElements = indexEntry.getValue();
			// iterating because the elements.remove(jaxrsElement); does not
			// work here
			// (hashcode has changed between the time the jaxrsElement was added
			// and now !)
			for (Iterator<IJaxrsElement> indexEntryElementsIterator = indexEntryElements.iterator(); indexEntryElementsIterator
					.hasNext();) {
				IJaxrsElement element = indexEntryElementsIterator.next();
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
	 * Unindex the given JAX-RS Element so that it cannot be retrieved when
	 * searching for elements with the given handleIdentifier. This does not
	 * mean that the given JAX-RS Element won't be findable anymore.
	 * 
	 * @param jaxrsElement
	 * @param handleIdentifier
	 */
	protected void unindexElement(final JaxrsBaseElement jaxrsElement, final String handleIdentifier) {
		Set<IJaxrsElement> jaxrsElements = elementsIndex.get(handleIdentifier);
		if (jaxrsElements != null) {
			jaxrsElements.remove(jaxrsElement);
		}
	}

	/**
	 * Returns an unmodifiable set of all the elements in the Metamodel.
	 * 
	 * @return
	 */
	public Set<IJaxrsElement> getAllElements() {
		// using a set automatically remove duplicates (because elements are
		// indexed under several criteria)
		final Collection<Set<IJaxrsElement>> values = elementsIndex.values();
		final Set<IJaxrsElement> elements = new HashSet<IJaxrsElement>();
		for (Set<IJaxrsElement> subSet : values) {
			elements.addAll(subSet);
		}
		return Collections.unmodifiableSet(elements);
	}

	/**
	 * @return the application that is used to compute the Endpoint's URI Path
	 *         Templates, or null if no application was specified in the code.
	 *         An invalid application may be returned, though (ie, a Type
	 *         annotated with {@link javax.ws.rs.ApplicationPath} but not
	 *         extending the {@link javax.ws.rs.Application} type). If multiple
	 *         applications have been defined, the pure web.xml one is returned.
	 */
	public final IJaxrsApplication getApplication() {
		// try to return pure web.xml first
		final JaxrsWebxmlApplication webxmlApplication = getWebxmlApplication();
		if (webxmlApplication != null) {
			return webxmlApplication;
		}
		final List<JaxrsJavaApplication> javaApplications = getJavaApplications();
		if (javaApplications.isEmpty()) {
			return null;
		}
		// otherwise, return first java-based application
		return javaApplications.get(0);
	}

	/**
	 * Returns the Application (Java or Webxml) those underlying resource
	 * matches the given resource, or null if not found
	 * 
	 * @param changedResource
	 * @return the associated application or null
	 */
	public final IJaxrsApplication getApplication(IResource changedResource) {
		for (IJaxrsApplication application : this.applications) {
			if (application.getResource().equals(changedResource)) {
				return application;
			}
		}
		return null;
	}

	/**
	 * @return the java application that matches the given classname, or null if
	 *         none was found.
	 */
	public final JaxrsJavaApplication getJavaApplication(final String className) {
		if (className != null) {
			for (IJaxrsApplication application : this.applications) {
				if (application.isJavaApplication() && className.equals(application.getJavaClassName())) {
					return (JaxrsJavaApplication) application;
				}
			}
		}
		return null;
	}

	/**
	 * @return the webxml application that matches the given classname, or null
	 *         if none was found.
	 */
	public final JaxrsWebxmlApplication getWebxmlApplication(final String className) {
		if (className != null) {
			for (IJaxrsApplication application : this.applications) {
				if (application.isWebXmlApplication() && className.equals(application.getJavaClassName())) {
					return (JaxrsWebxmlApplication) application;
				}
			}
		}
		return null;
	}

	/**
	 * @return all the JAX-RS Application in the Metamodel The result is a
	 *         separate unmodifiable list
	 */
	public final List<IJaxrsApplication> getAllApplications() {
		return Collections.unmodifiableList(new ArrayList<IJaxrsApplication>(this.applications));
	}

	/**
	 * @return true if the metamodel holds more than 1 <strong>real</strong>
	 *         applicatio, that is, excluding all application overrides
	 *         configured in the web deployment descriptor. Returns false
	 *         otherwise.
	 */
	public boolean hasMultipleApplications() {
		final List<IJaxrsApplication> realApplications = new ArrayList<IJaxrsApplication>();
		for (IJaxrsApplication application : this.applications) {
			if (application.isJavaApplication()) {
				realApplications.add(application);
			} else if (!((JaxrsWebxmlApplication) application).isOverride()) {
				realApplications.add(application);
			}
		}
		return realApplications.size() > 1;
	}

	/**
	 * @return the <strong>pure JEE</strong> web.xml based application. There
	 *         can be only one (at most), defined with the
	 *         <code>javax.ws.rs.core.Application</code> class name. Other
	 *         web.xml application declarations are just to override the
	 *         java-based application <code>@ApplicationPath</code> value and
	 *         <strong>will not be returned</strong> by this method.
	 */
	public final JaxrsWebxmlApplication getWebxmlApplication() {
		for (IJaxrsApplication application : this.applications) {
			if (application.isWebXmlApplication()
					&& EnumJaxrsClassname.APPLICATION.equals(application.getJavaClassName())) {
				return (JaxrsWebxmlApplication) application;
			}
		}
		return null;
	}

	/**
	 * @return the web.xml based application and all the java-based application
	 *         overrides, or an empty collection if none exist in the metamodel.
	 */
	public final List<JaxrsWebxmlApplication> getWebxmlApplications() {
		final List<JaxrsWebxmlApplication> webxmlApplications = new ArrayList<JaxrsWebxmlApplication>();
		for (IJaxrsApplication application : this.applications) {
			if (application.isWebXmlApplication()) {
				webxmlApplications.add((JaxrsWebxmlApplication) application);
			}
		}
		return Collections.unmodifiableList(webxmlApplications);
	}

	/**
	 * @return the web.xml based application and all the java-based application
	 *         overrides, or an empty collection if none exist in the metamodel.
	 */
	public final List<JaxrsJavaApplication> getJavaApplications() {
		final List<JaxrsJavaApplication> javaApplications = new ArrayList<JaxrsJavaApplication>();
		for (IJaxrsApplication application : this.applications) {
			if (application.isJavaApplication()) {
				javaApplications.add((JaxrsJavaApplication) application);
			}
		}
		return Collections.unmodifiableList(javaApplications);
	}

	public final List<IJaxrsProvider> getAllProviders() {
		return Collections.unmodifiableList(new ArrayList<IJaxrsProvider>(providers));
	}

	public IJaxrsProvider getProvider(IType providerType) {
		if (providerType == null) {
			return null;
		}
		final IJaxrsElement result = getElementByIdentifier(providerType.getHandleIdentifier());
		if (result != null && result.getElementCategory() == EnumElementCategory.PROVIDER) {
			return (IJaxrsProvider) result;
		}
		return null;
	}

	public final List<IJaxrsHttpMethod> getAllHttpMethods() {
		return Collections.unmodifiableList(new ArrayList<IJaxrsHttpMethod>(httpMethods));
	}

	public final List<IJaxrsResource> getAllResources() {
		return Collections.unmodifiableList(new ArrayList<IJaxrsResource>(resources));
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
			IJaxrsElement element = getElement(annotationType);
			if (element != null && element.getElementCategory() == EnumElementCategory.HTTP_METHOD) {
				return (IJaxrsHttpMethod) element;
			}
			// if not found, look for built-in HTTP Methods
			else if (element == null) {
				element = getElementByIdentifier(annotationType.getFullyQualifiedName());
				if (element != null && element.getElementCategory() == EnumElementCategory.HTTP_METHOD) {
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

	/**
	 * Return the JAX-RS element matching the given Java Element
	 * 
	 * @param element
	 *            the java element
	 * @return the JAX-RS element or null if none found
	 */
	public IJaxrsElement getElement(IJavaElement element) {
		if (element == null) {
			return null;
		}
		return getElementByIdentifier(element.getHandleIdentifier());
	}

	public List<IJaxrsElement> getElements(final IResource resource) {
		if (resource == null) {
			return null;
		}
		final Set<IJaxrsElement> elements = resourcesIndex.get(resource.getFullPath().toOSString());
		if (elements == null || elements.isEmpty()) {
			return Collections.emptyList();
		}
		return new ArrayList<IJaxrsElement>(elements);
	}

	/**
	 * Return the JAX-RS element matching the given Java Element Handle
	 * Identifier
	 * 
	 * @param element
	 *            the java element handle identifier
	 * @return the JAX-RS element or null if none found
	 */
	private IJaxrsElement getElementByIdentifier(final String elementHandleIdentifier) {
		if (elementHandleIdentifier == null) {
			return null;
		}
		final Set<IJaxrsElement> elements = elementsIndex.get(elementHandleIdentifier);
		if (elements == null || elements.isEmpty()) {
			return null;
		}
		return elements.iterator().next();
	}

	/**
	 * Return the JAX-RS element matching the given Java Annotation
	 * 
	 * @param element
	 *            the java annotation
	 * @return the JAX-RS element or null if none found
	 */
	public IJaxrsElement getElement(Annotation annotation) {
		return getElement(annotation.getJavaAnnotation());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getElement(IJavaElement element, Class<T> clazz) {
		final IJaxrsElement jaxrsElement = getElement(element);
		if (jaxrsElement != null && clazz.isAssignableFrom(jaxrsElement.getClass())) {
			return (T) jaxrsElement;
		}
		return null;
	}

	public List<IJaxrsElement> getElements(final IJavaElement javaElement) {
		if (javaElement == null) {
			return Collections.emptyList();
		}
		final String key = javaElement.getHandleIdentifier();
		final List<IJaxrsElement> result = new ArrayList<IJaxrsElement>();
		if (elementsIndex.containsKey(key)) {
			final Set<IJaxrsElement> indexedElements = elementsIndex.get(key);
			result.addAll(indexedElements);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public <T extends IJaxrsElement> List<T> getElements(final IJavaElement javaElement,
			Class<? extends JaxrsBaseElement> T) {
		final String key = javaElement.getHandleIdentifier();
		final List<T> elements = new ArrayList<T>();
		if (elementsIndex.containsKey(key)) {
			for (IJaxrsElement element : elementsIndex.get(key)) {
				if (element.getClass().isAssignableFrom(T) || T.isAssignableFrom(element.getClass())) {
					elements.add((T) element);
				}
			}
		}
		return elements;
	}

	public void remove(IJaxrsElement element) {
		if (element == null) {
			return;
		} 
		if(element instanceof IJaxrsApplication) {
			this.applications.remove(element);
		} else if(element instanceof IJaxrsHttpMethod) {
			this.httpMethods.remove(element);
		} else if(element instanceof IJaxrsResource) {
			this.resources.remove(element);
		} else if(element instanceof IJaxrsResourceMethod) {
			final JaxrsResource parentResource = ((JaxrsResourceMethod) element).getParentResource();
			parentResource.removeMethod((IJaxrsResourceMethod) element);
		} else if(element instanceof IJaxrsResourceField) {
			final JaxrsResource fieldResourceParent = ((JaxrsResourceField) element).getParentResource();
			fieldResourceParent.removeField((JaxrsResourceField) element);
		} else if(element instanceof IJaxrsProvider) {
			this.providers.remove(element);
		}
		unindexElement(element);
	}

	public JaxrsHttpMethod getHttpMethod(Annotation httpMethodAnnotation) {
		if (httpMethodAnnotation != null) {
			for (IJaxrsHttpMethod httpMethod : httpMethods) {
				final String handleIdentifier1 = httpMethod.getJavaClassName();
				final String handleIdentifier2 = httpMethodAnnotation.getFullyQualifiedName();
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
	 * 
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
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		return false;
	}

	@Override
	public IProject getProject() {
		if (javaProject == null) {
			return null;
		}
		return javaProject.getProject();
	}

}
