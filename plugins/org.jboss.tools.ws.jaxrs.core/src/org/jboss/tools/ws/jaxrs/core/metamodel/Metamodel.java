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

package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.JBossJaxrsCorePlugin;
import org.jboss.tools.ws.jaxrs.core.internal.builder.JaxrsMetamodelBuilder;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;

/**
 * Manages all the JAX-RS domain classes of the JAX-RS Metamodel. Not only a
 * POJO, but also provides business services.
 * 
 * @author xcoulon
 * 
 */
public class Metamodel {

	/**
	 * The qualified name of the metamodel when stored in the project session
	 * properties.
	 */
	private static final QualifiedName METAMODEL_QUALIFIED_NAME = new QualifiedName(JBossJaxrsCorePlugin.PLUGIN_ID,
			"metamodel");

	/** The enclosing JavaProject. */
	private final IJavaProject javaProject;

	/** The Service URI. Default is "/" */
	private String serviceUri = "/";

	/**
	 * List of JAX-RS annotation qualified names, including both core
	 * annotations (@Path, etc.) and custom annotations (HTTP Methods).
	 * 
	 */
	private final List<String> jaxrsAnnotationNames = new ArrayList<String>();

	/**
	 * All the subclasses of <code>javax.ws.rs.core.Application</code>, although
	 * there should be only one.
	 */
	private final Applications applications;

	/**
	 * All the resources (both rootresources and subresources) available in the
	 * service , indexed by their associated java type fully qualified name.
	 */
	private final Resources resources;

	/**
	 * The available providers (classes which implement MessageBodyWriter<T>,
	 * MessageBodyReader<T> or ExceptionMapper<T>), , indexed by their
	 * associated java type fully qualified name.
	 */
	private final Providers providers;

	/** The HTTP ResourceMethod elements container. */
	private final HTTPMethods httpMethods;

	/**
	 * Full constructor.
	 * 
	 * @param javaProject
	 *            the enclosing java project
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public Metamodel(final IJavaProject javaProject) throws CoreException {
		this.javaProject = javaProject;
		applications = new Applications(this);
		providers = new Providers(javaProject, this);
		resources = new Resources(this);
		httpMethods = new HTTPMethods(this);
		jaxrsAnnotationNames.addAll(Arrays.asList(new String[] { Provider.class.getName(), Consumes.class.getName(),
				Produces.class.getName(), Path.class.getName(), HttpMethod.class.getName() }));
		javaProject.getProject().setSessionProperty(METAMODEL_QUALIFIED_NAME, this);
	}

	/**
	 * @return the javaProject
	 */
	public IJavaProject getJavaProject() {
		return javaProject;
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
	public static Metamodel get(final IProject project) throws CoreException {
		return (Metamodel) project.getSessionProperty(METAMODEL_QUALIFIED_NAME);
	}

	/**
	 * 
	 * @throws CoreException
	 *             in case of underlying exception
	 */
	public final void remove() throws CoreException {
		Logger.info("JAX-RS Metamodel removed for project " + javaProject.getElementName());
		javaProject.getProject().setSessionProperty(METAMODEL_QUALIFIED_NAME, null);
	}

	/**
	 * @return the resources
	 */
	public final Resources getResources() {
		return resources;
	}

	/**
	 * @return the providers
	 */
	public final Providers getProviders() {
		return providers;
	}

	/**
	 * @return the httpMethods
	 */
	public final HTTPMethods getHttpMethods() {
		return httpMethods;
	}

	/**
	 * @return the serviceUri
	 */
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
	 * Registers all discovered JAX-RS elements within the given scope.
	 * 
	 * @param scope
	 *            the scope in which any kind of JAX-RS should be searched.
	 * @param progressMonitor
	 *            the progress monitor
	 * @throws CoreException
	 *             in case of underlying exception
	 * @throws InvalidModelElementException
	 */
	public final void addElements(final IJavaElement scope, final IProgressMonitor progressMonitor)
			throws CoreException {
		try {
			progressMonitor.beginTask("Computing JAX-RS metamodel", 4);
			applications.addFrom(scope, progressMonitor);
			progressMonitor.worked(1);
			httpMethods.addFrom(scope, progressMonitor);
			progressMonitor.worked(1);
			providers.addFrom(scope, new SubProgressMonitor(progressMonitor, 1,
					SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
			progressMonitor.worked(1);
			resources.addFrom(scope, new SubProgressMonitor(progressMonitor, 1,
					SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
			progressMonitor.worked(1);
		} finally {
			progressMonitor.done();
		}
	}

	/**
	 * Remove any type of JAX-RS Element mapped to the given resource in the
	 * workspace.
	 * 
	 * @param resource
	 *            the resource to which the JAX-RS element is mapped
	 * @param progressMonitor
	 *            the progress monitor
	 */
	public final void remove(final IResource resource, final IProgressMonitor progressMonitor) {
		try {
			progressMonitor.beginTask("Computing JAX-RS metamodel", 4);
			applications.removeElement(resource, progressMonitor);
			progressMonitor.worked(1);
			httpMethods.removeElement(resource, progressMonitor);
			progressMonitor.worked(1);
			resources.removeElement(resource, progressMonitor);
			progressMonitor.worked(1);
			providers.removeElement(resource, progressMonitor);
			progressMonitor.worked(1);
		} finally {
			progressMonitor.done();
		}
	}

	/**
	 * Returns the JAX-RS Element associated with the given java element.
	 * 
	 * @param element
	 *            the underlying java element (can be IType or IMethod)
	 * @return the associated JAX-RS element, or null if none found
	 * @throws JavaModelException
	 *             in case of underlying exception
	 */
	public final BaseElement<?> find(final IJavaElement element) throws JavaModelException {
		switch (element.getElementType()) {
		case IJavaElement.TYPE:
			return findElement((IType) element);
		case IJavaElement.METHOD:
			return findElement((IMethod) element);
		default:
			break;
		}
		return null;
	}

	/**
	 * Returns the JAX-RS Element associated with the given java method.
	 * 
	 * @param method
	 *            the underlying java method
	 * @return the associated JAX-RS element, or null if none found
	 * @throws JavaModelException
	 *             in case of underlying exception
	 */
	private BaseElement<IMethod> findElement(final IMethod method) throws JavaModelException {
		Resource resource = resources.getByType((IType) (method.getParent()));
		if (resource != null) {
			return resource.getByJavaMethod(method);
		}
		return null;
	}

	/**
	 * Returns the JAX-RS Element associated with the given java type.
	 * 
	 * @param type
	 *            the underlying java type
	 * @return the associated JAX-RS element, or null if none found
	 */
	private BaseElement<IType> findElement(final IType type) {
		if (resources.contains(type)) {
			return resources.getByType(type);
		}
		if (providers.contains(type)) {
			return providers.getByType(type);
		}
		if (httpMethods.contains(type)) {
			return httpMethods.getByType(type);
		}
		return null;
	}

	/**
	 * Applies the resource delta to this metamodel.
	 * 
	 * @param delta
	 *            the resource delta (addition, change or removal of a project
	 *            resource)
	 * @param progressMonitor
	 *            the progress monitor
	 * @throws CoreException
	 *             in case of underlying exception
	 * @throws InvalidModelElementException
	 */
	public final void applyDelta(final IResourceDelta delta, final IProgressMonitor progressMonitor)
			throws CoreException {
		IResource resource = delta.getResource();
		ICompilationUnit compilationUnit = JdtUtils.getCompilationUnit(resource);
		if (resource.exists()) {
			IMarker[] markers = resource
					.findMarkers(JaxrsMetamodelBuilder.JAVA_PROBLEM, true, IResource.DEPTH_INFINITE);
			if (reportErrors(compilationUnit, markers)) {
				Logger.warn("Resource '" + resource.getName() + "' contains errors.");
				return;
			}
		}
		switch (delta.getKind()) {
		case IResourceDelta.ADDED:
			addElements(compilationUnit, progressMonitor);
			break;
		case IResourceDelta.REMOVED:
			remove(resource, progressMonitor);
			break;
		case IResourceDelta.CHANGED:
			if ((delta.getFlags() & IResourceDelta.CONTENT) != 0) {
				mergeElement(compilationUnit, progressMonitor);
				break;
			}
		default:
			break;
		}
	}

	/**
	 * Merges the given compilation unit within this metamodel.
	 * 
	 * @param compilationUnit
	 *            the changed compilation unit
	 * @param progressMonitor
	 *            the progress monitor
	 * @throws CoreException
	 *             in case of underlying exception
	 * @throws InvalidModelElementException
	 */
	protected final void mergeElement(final ICompilationUnit compilationUnit, final IProgressMonitor progressMonitor)
			throws CoreException {
		try {
			progressMonitor.beginTask("Updating JAX-RS metamodel", 1);
			IType primaryType = compilationUnit.findPrimaryType();
			if (primaryType != null) {
				BaseElement<IType> jaxrsElement = findElement(primaryType);
				if (jaxrsElement != null) {
					try {
						jaxrsElement.hasErrors(false);
						jaxrsElement.merge(jaxrsElement.getJavaElement(), progressMonitor);
					} catch (InvalidModelElementException e) {
						Logger.warn("Failed to merge " + jaxrsElement.getJavaElement().getElementName()
								+ ", removing element from JAX-RS metamodel");
						remove(compilationUnit.getResource(), progressMonitor);
					}
				} else {
					addElements(compilationUnit, progressMonitor);
				}
			} else {
				remove(compilationUnit.getResource(), progressMonitor);
			}
		} finally {
			progressMonitor.done();
		}
	}

	public void validate(IProgressMonitor progressMonitor) throws CoreException {
		for (Resource resource : resources.getAll()) {
			resource.validate(progressMonitor);
		}

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
			BaseElement<?> jaxrsElement = find(compilationUnit.getElementAt(marker.getAttribute(IMarker.CHAR_START, 0)));
			if (jaxrsElement != null) {
				jaxrsElement.hasErrors(true);
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
		this.httpMethods.reset();
		this.providers.reset();
		this.resources.reset();

	}

}
