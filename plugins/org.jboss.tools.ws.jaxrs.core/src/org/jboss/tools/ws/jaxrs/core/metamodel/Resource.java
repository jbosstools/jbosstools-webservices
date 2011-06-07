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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.builder.JAXRSAnnotationsScanner;
import org.jboss.tools.ws.jaxrs.core.internal.builder.JaxrsMetamodelBuilder;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionFilterUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;

/**
 * From the spec : A resource class is a Java class that uses JAX-RS annotations
 * to implement a corresponding Web resource. Resource classes are POJOs that
 * have at least one method annotated with @Path or a request method designator.
 * 
 * @author xcoulon
 * 
 */
public class Resource extends BaseElement<IType> {

	/**
	 * A map of all JAX-RS resourceMethods this resource has, including
	 * <ul>
	 * <li>the subresource resourceMethods (ie, annotated with both an @Path and
	 * an
	 * 
	 * @HTTPMethod)</li> <li>the subresource locators (ie, only annotated with
	 *                   an @Path)</li> <li>the resource resourceMethods (ie,
	 *                   only annotated with an @HTTPMethod)</li>
	 *                   </ul>
	 *                   The JAX-RS method are indexed by their corresponding
	 *                   Java Methods name and signature
	 */
	final Map<String, ResourceMethod> resourceMethods = new HashMap<String, ResourceMethod>();

	/** The resource mapping. */
	private final ResourceMapping resourceMapping;

	/** Optional Application. */
	private Application application = null;

	/**
	 * Internal 'Resource' element builder.
	 * 
	 * @author xcoulon
	 * 
	 */
	public static class ResourceBuilder {

		private final Metamodel metamodel;
		private final IType javaType;

		/**
		 * Mandatory attributes of the enclosing 'HTTPMethod' element.
		 * 
		 * @param javaType
		 * @param metamodel
		 */
		public ResourceBuilder(final IType javaType, final Metamodel metamodel) {
			this.javaType = javaType;
			this.metamodel = metamodel;
		}

		/**
		 * Builds and returns the elements. Internally calls the merge() method.
		 * 
		 * @param progressMonitor
		 * @return
		 * @throws InvalidModelElementException
		 * @throws CoreException
		 */
		public Resource build(IProgressMonitor progressMonitor) throws InvalidModelElementException, CoreException {
			Resource resource = new Resource(this);
			resource.merge(javaType, progressMonitor);
			return resource;
		}
	}

	/**
	 * Full constructor using the inner 'MediaTypeCapabilitiesBuilder' static
	 * class.
	 * 
	 * @param builder
	 */
	private Resource(ResourceBuilder builder) {
		super(builder.javaType, builder.metamodel);
		this.resourceMapping = new ResourceMapping(this);
	}

	public final ResourceMapping getMapping() {
		return resourceMapping;
	}

	public final boolean isRootResource() {
		return getKind() == EnumKind.ROOT_RESOURCE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final EnumKind getKind() {
		if (this.resourceMapping.getUriPathTemplateFragment() != null) {
			return EnumKind.ROOT_RESOURCE;
		}
		return EnumKind.SUBRESOURCE;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @return
	 * 
	 * @throws CoreException
	 */
	@Override
	public final Set<EnumElementChange> merge(final IType javaType, final IProgressMonitor progressMonitor)
			throws CoreException, InvalidModelElementException {
		if (!JdtUtils.isTopLevelType(javaType)) {
			throw new InvalidModelElementException("Type is not a top-level type");
		}
		Set<EnumElementChange> changes = new HashSet<EnumElementChange>();
		CompilationUnit compilationUnit = getCompilationUnit(progressMonitor);
		// TODO : base64.decode()
		Set<IProblem> problems = JdtUtils.resolveErrors(javaType, compilationUnit);
		if (problems != null && problems.size() > 0) {
			for (IProblem problem : problems) {
				Logger.debug("Problem found: " + problem.getMessage());
			}
			return changes;
		}
		Set<EnumElementChange> resourceChanges = this.resourceMapping.merge(compilationUnit);
		mergeMethods(javaType, compilationUnit, resourceChanges, progressMonitor);
		return changes;
	}

	// FIXME deal with interfaces/implementations
	private final void mergeMethods(final IJavaElement scope, final CompilationUnit compilationUnit,
			final Set<EnumElementChange> resourceChanges, final IProgressMonitor progressMonitor) throws CoreException {
		Set<String> httpMethodNames = getMetamodel().getHttpMethods().getTypeNames();
		List<IMethod> javaMethods = JAXRSAnnotationsScanner
				.findResourceMethods(scope, httpMethodNames, progressMonitor);
		Map<IMethod, String> keys = new HashMap<IMethod, String>();
		for (IMethod javaMethod : javaMethods) {
			keys.put(javaMethod, computeKey(javaMethod));
		}

		for (Iterator<String> iterator = resourceMethods.keySet().iterator(); iterator.hasNext();) {
			String key = iterator.next();
			if (!keys.containsValue(key)) {
				ResourceMethod resourceMethod = resourceMethods.get(key);
				Logger.debug("Removed " + resourceMethod.toString());
				iterator.remove();
				getMetamodel().getRoutes().removeFrom(resourceMethod, progressMonitor);
			}
		}
		for (IMethod javaMethod : javaMethods) {
			try {
				String key = keys.get(javaMethod);
				if (resourceMethods.containsKey(key)) {
					ResourceMethod resourceMethod = resourceMethods.get(key);

					Set<EnumElementChange> resourceMethodChanges = resourceMethod.merge(javaMethod, progressMonitor);
					Set<EnumElementChange> changes = new HashSet<EnumElementChange>();
					changes.addAll(resourceChanges);
					changes.addAll(resourceMethodChanges);
					for (EnumElementChange change : changes) {
						switch (change) {
						case KIND:
							getMetamodel().getRoutes().merge(resourceMethod, progressMonitor);
							break;
						case MAPPING:
							List<Route> routes = this.getMetamodel().getRoutes().getByResourceMethod(resourceMethod);
							if (routes != null) {
								for (Route route : routes) {
									route.getEndpoint().merge();
								}
							}
							break;
						}
					}
					Logger.debug("Updated " + resourceMethod.toString());
				} else {
					ResourceMethod resourceMethod = new ResourceMethod(javaMethod, this, getMetamodel(),
							progressMonitor);
					resourceMethods.put(key, resourceMethod);
					getMetamodel().getRoutes().merge(resourceMethod, progressMonitor);
					Logger.debug("Added " + resourceMethod.toString());
				}
			} catch (InvalidModelElementException e) {
				Logger.warn("ResourceMethod '" + javaMethod.getElementName()
						+ "' is not a valid JAX-RS ResourceMethod: " + e.getMessage());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws CoreException
	 */
	@Override
	public void validate(IProgressMonitor progressMonitor) throws CoreException {
		Logger.debug("Validating " + super.getJavaElement().getFullyQualifiedName());
		super.getJavaElement().getResource()
				.deleteMarkers(JaxrsMetamodelBuilder.JAXRS_PROBLEM, true, IResource.DEPTH_INFINITE);
		for (Entry<String, ResourceMethod> entry : resourceMethods.entrySet()) {
			entry.getValue().validate(progressMonitor);
		}
		Logger.debug("Validation done.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void hasErrors(final boolean hasErrors) {
		super.hasErrors(hasErrors);
		if (!hasErrors) {
			for (ResourceMethod resourceMethod : resourceMethods.values()) {
				resourceMethod.hasErrors(hasErrors);
			}
		}
	}

	private static String computeKey(final IMethod method) throws JavaModelException {
		StringBuffer key = new StringBuffer(method.getElementName()).append('(');
		for (String parameterType : method.getParameterTypes()) {
			key.append(parameterType);
		}
		return key.append(')').toString();
	}

	public final String getName() {
		return getJavaElement().getElementName();
	}

	public final ResourceMethod getByJavaMethod(final IMethod javaMethod) throws JavaModelException {
		return resourceMethods.get(computeKey(javaMethod));
	}

	public final Application getApplication() {
		return application;
	}

	public final List<ResourceMethod> getResourceMethods() {
		return CollectionFilterUtils.filterElementsByKind(resourceMethods.values(), EnumKind.RESOURCE_METHOD);
	}

	public final List<ResourceMethod> getSubresourceMethods() {
		return CollectionFilterUtils.filterElementsByKind(resourceMethods.values(), EnumKind.SUBRESOURCE_METHOD);
	}

	/**
	 * Returns both resource resourceMethods and subresource resourceMethods
	 * 
	 * @return a list of resources of mixed kinds RESOURCE_METHOD and
	 *         SUBRESOURCE_METHOD
	 */
	public final List<ResourceMethod> getAllMethods() {
		// return
		// CollectionFilterUtils.filterElementsByKind(resourceMethods.values(),
		// EnumKind.RESOURCE_METHOD,
		// EnumKind.SUBRESOURCE_METHOD);
		return new ArrayList<ResourceMethod>(resourceMethods.values());
	}

	/**
	 * @return the subresourceLocators
	 */
	public final List<ResourceMethod> getSubresourceLocators() {
		return CollectionFilterUtils.filterElementsByKind(resourceMethods.values(), EnumKind.SUBRESOURCE_LOCATOR);
	}

	public final ResourceMethod getByMapping(final HTTPMethod httpMethod, final String uriPathTemplateFragment,
			final String consumes, final String produces) {
		for (ResourceMethod resourceMethod : resourceMethods.values()) {
			if (resourceMethod.getMapping().matches(httpMethod, uriPathTemplateFragment, consumes, produces)) {
				return resourceMethod;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return new StringBuffer().append(getName()).append(" (root:").append(isRootResource()).append(") ")
				.append(resourceMapping).toString();
	}
}
