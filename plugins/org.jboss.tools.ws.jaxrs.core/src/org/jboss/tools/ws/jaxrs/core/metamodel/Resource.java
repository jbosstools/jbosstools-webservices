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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.jboss.tools.ws.jaxrs.core.internal.builder.JAXRSAnnotationsScanner;
import org.jboss.tools.ws.jaxrs.core.internal.builder.JaxrsMetamodelBuilder;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionFilterUtil;
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
	 * indicates if the resource is a root resource (type annotated with @Path)
	 * or not. Can change
	 */
	boolean isRootResource = false;

	/** The URI path template. As with annotations, it can change */
	private String uriPathTemplate;

	/**
	 * the default media type capabilities offered by this resource. May be
	 * overridden at method level
	 */
	final MediaTypeCapabilities mediaTypeCapabilities = new MediaTypeCapabilities();

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

	/**
	 * Full constructor
	 * 
	 * @param javaType
	 * @param metamodel
	 * @param compilationUnit
	 * @param mediaTypeCapabilities
	 * @throws CoreException
	 * @throws InvalidModelElementException
	 */
	public Resource(final IType javaType, final Metamodel metamodel, final IProgressMonitor progressMonitor)
			throws CoreException, InvalidModelElementException {
		super(metamodel, javaType);
		setState(EnumState.CREATING);
		merge(javaType, progressMonitor);
		setState(EnumState.CREATED);
	}

	public final boolean isRootResource() {
		return isRootResource;
	}

	@Override
	public final EnumType getKind() {
		if (isRootResource) {
			return EnumType.ROOT_RESOURCE;
		}
		return EnumType.SUBRESOURCE;
	}

	@Override
	public final void merge(final IType javaType, final IProgressMonitor progressMonitor) throws CoreException,
			InvalidModelElementException {
		if (!JdtUtils.isTopLevelType(javaType)) {
			throw new InvalidModelElementException("Type is not a top-level type");
		}
		CompilationUnit compilationUnit = getCompilationUnit(javaType, progressMonitor);
		// TODO : base64.decode()
		if (getState() == EnumState.CREATED) {
			Set<IProblem> problems = JdtUtils.resolveErrors(javaType, compilationUnit);
			if (problems != null && problems.size() > 0) {
				// metamodel.reportErrors(javaType, problems);
				return;
			}
		}

		// String serviceURI = container.getMetamodel().getServiceURI();
		IAnnotationBinding pathAnnotationBinding = JdtUtils.resolveAnnotationBinding(javaType, compilationUnit,
				Path.class);
		if (pathAnnotationBinding != null) {
			isRootResource = true;
			this.uriPathTemplate = (String) JdtUtils.resolveAnnotationAttributeValue(pathAnnotationBinding, "value");
		} else {
			isRootResource = false;
			this.uriPathTemplate = null;
		}

		mediaTypeCapabilities.setConsumedMimeTypes(JAXRSAnnotationsScanner.resolveMediaTypeCapabilities(javaType,
				compilationUnit, Consumes.class));
		mediaTypeCapabilities.setProducedMimeTypes(JAXRSAnnotationsScanner.resolveMediaTypeCapabilities(javaType,
				compilationUnit, Produces.class));

		mergeMethods(javaType, compilationUnit, progressMonitor);
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

	// FIXME deal with interfaces/implementations
	public final void mergeMethods(final IJavaElement scope, final CompilationUnit compilationUnit,
			final IProgressMonitor progressMonitor) throws CoreException {
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
				iterator.remove();
			}
		}
		for (IMethod javaMethod : javaMethods) {
			try {
				String key = keys.get(javaMethod);
				if (resourceMethods.containsKey(key)) {
					ResourceMethod resourceMethod = resourceMethods.get(key);
					resourceMethod.merge(javaMethod, progressMonitor);
					Logger.debug("Updated " + resourceMethod.toString());
				} else {
					ResourceMethod resourceMethod = new ResourceMethod(javaMethod, this, getMetamodel(),
							progressMonitor);
					resourceMethods.put(key, resourceMethod);
					Logger.debug("Added " + resourceMethod.toString());
				}
			} catch (InvalidModelElementException e) {
				Logger.warn("ResourceMethod '" + javaMethod.getElementName()
						+ "' is not a valid JAX-RS ResourceMethod: " + e.getMessage());
			}
		}
	}

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

	/**
	 * @return the mediaTypeCapabilities
	 */
	public final MediaTypeCapabilities getMediaTypeCapabilities() {
		return mediaTypeCapabilities;
	}

	public final String getName() {
		return getJavaElement().getElementName();
	}

	/**
	 * @return the uriPathTemplate
	 */
	public final String getUriPathTemplate() {
		return uriPathTemplate;
	}

	public final ResourceMethod getByJavaMethod(final IMethod javaMethod) throws JavaModelException {
		return resourceMethods.get(computeKey(javaMethod));
	}

	public final List<ResourceMethod> getResourceMethods() {
		return CollectionFilterUtil.filterElementsByKind(resourceMethods.values(), EnumType.RESOURCE_METHOD);
	}

	public final List<ResourceMethod> getSubresourceMethods() {
		return CollectionFilterUtil.filterElementsByKind(resourceMethods.values(), EnumType.SUBRESOURCE_METHOD);
	}

	/**
	 * Returns both resource resourceMethods and subresource resourceMethods
	 * 
	 * @return a list of resources of mixed kinds RESOURCE_METHOD and
	 *         SUBRESOURCE_METHOD
	 */
	public final List<ResourceMethod> getAllMethods() {
		return CollectionFilterUtil.filterElementsByKind(resourceMethods.values(), EnumType.RESOURCE_METHOD,
				EnumType.SUBRESOURCE_METHOD);
	}

	/**
	 * @return the subresourceLocators
	 */
	public final List<ResourceMethod> getSubresourceLocators() {
		return CollectionFilterUtil.filterElementsByKind(resourceMethods.values(), EnumType.SUBRESOURCE_LOCATOR);
	}

	public final ResourceMethod getByURIMapping(final HTTPMethod httpMethod, final String uriPathTemplateFragment, final String consumes,
			final String produces) {
		for (ResourceMethod resourceMethod : resourceMethods.values()) {
			if (resourceMethod.getUriMapping().matches(httpMethod, uriPathTemplateFragment, consumes, produces)) {
				return resourceMethod;
			}
		}
		return null;
	}

}
