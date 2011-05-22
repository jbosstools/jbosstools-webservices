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

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;

/**
 * @author xcoulon
 * 
 */
public class ResourceMethod extends BaseElement<IMethod> {

	private final Resource parentResource;

	private UriMapping uriMapping = null;

	private EnumType kind = null;

	/**
	 * return type of the java method. Null if this is not a subresource
	 * locator.
	 */
	private IType returnType = null;

	/**
	 * Internal 'Resource' element builder.
	 * 
	 * @author xcoulon
	 * 
	 */
	public static class Builder {

		private final Metamodel metamodel;
		private final IMethod javaMethod;
		private final Resource parentResource;

		/**
		 * Mandatory attributes of the enclosing 'ResourceMethod' element.
		 * 
		 * @param javaMethod
		 * @param metamodel
		 * @param parentResource
		 */
		public Builder(final IMethod javaMethod, final Resource parentResource, final Metamodel metamodel) {
			this.javaMethod = javaMethod;
			this.metamodel = metamodel;
			this.parentResource = parentResource;
		}

		/**
		 * Builds and returns the elements. Internally calls the merge() method.
		 * 
		 * @param progressMonitor
		 * @return
		 * @throws InvalidModelElementException
		 * @throws CoreException
		 */
		public ResourceMethod build(IProgressMonitor progressMonitor) throws InvalidModelElementException,
				CoreException {
			ResourceMethod resourceMethod = new ResourceMethod(this);
			resourceMethod.merge(javaMethod, progressMonitor);
			return resourceMethod;
		}
	}

	/**
	 * Full constructor using the inner 'Builder' static class.
	 * 
	 * @param builder
	 */
	public ResourceMethod(final Builder builder) throws InvalidModelElementException, CoreException {
		super(builder.javaMethod, builder.metamodel);
		this.parentResource = builder.parentResource;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void merge(final IMethod javaMethod, final IProgressMonitor progressMonitor)
			throws InvalidModelElementException, CoreException {
		CompilationUnit compilationUnit = getCompilationUnit(progressMonitor);
		if (state == EnumState.CREATED) {
			Set<IProblem> problems = JdtUtils.resolveErrors(javaMethod, compilationUnit);
			if (problems != null && problems.size() > 0) {
				// metamodel.reportErrors(javaMethod, problems);
				return;
			}
		}
		IMethodBinding methodBinding = JdtUtils.resolveMethodBinding(javaMethod, compilationUnit);
		if (uriMapping == null) {
			this.uriMapping = new UriMapping.Builder(javaMethod, getMetamodel()).build(compilationUnit);
		} else {
			this.uriMapping.merge(compilationUnit);
		}
		HTTPMethod httpMethod = uriMapping.getHTTPMethod();
		String uriPathTemplateFragment = uriMapping.getUriPathTemplateFragment();
		if (uriPathTemplateFragment == null && httpMethod != null) {
			this.kind = EnumType.RESOURCE_METHOD;
		} else if (uriPathTemplateFragment != null && httpMethod != null) {
			this.kind = EnumType.SUBRESOURCE_METHOD;
		} else if (uriPathTemplateFragment != null && httpMethod == null) {
			this.kind = EnumType.SUBRESOURCE_LOCATOR;
		} else {
			throw new InvalidModelElementException(
					"ResourceMethod has no valid @Path annotation and no HTTP ResourceMethod annotation");
		}
		ITypeBinding javaReturnType = methodBinding.getReturnType();
		if (javaReturnType != null) {
			this.returnType = (IType) javaReturnType.getJavaElement();
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws CoreException
	 */
	@Override
	public void validate(IProgressMonitor progressMonitor) throws CoreException {
		getUriMapping().validate();
	}

	@Override
	public final BaseElement.EnumType getKind() {
		return kind;
	}

	/**
	 * @return the parentResource
	 */
	public final Resource getParentResource() {
		return parentResource;
	}

	/**
	 * @return the uriMapping
	 */
	public final UriMapping getUriMapping() {
		return uriMapping;
	}

	/**
	 * @return the returnType
	 */
	public final IType getReturnType() {
		return returnType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return "ResourceMethod [" + parentResource.getName() + "." + getJavaElement().getElementName() + "] -> "
				+ uriMapping.toString() + ", kind=" + kind + "]";
	}

	/*
	 * private static UriMapping computeUriMapping(IMethod javaMethod,
	 * CompilationUnit compilationUnit, List<HTTPMethod> httpMethods,
	 * IProgressMonitor progressMonitor) throws JavaModelException,
	 * CoreException { // look for any @HTTPMethod annotation HTTPMethod
	 * httpMethod = lookupHTTPMethod(httpMethods, javaMethod, compilationUnit,
	 * progressMonitor); // look for @Path annotation String
	 * methodLevelUriPathTemplate = (String)
	 * JdtUtils.resolveAnnotationAttributeValue(javaMethod, compilationUnit,
	 * ANNOTATION_PATH, "value"); MediaTypeCapabilities mediaTypeCapabilities =
	 * JAXRSAnnotationsScanner.resolveMediaTypeCapabilities(javaMethod,
	 * compilationUnit, progressMonitor); return new UriMapping(httpMethod,
	 * methodLevelUriPathTemplate, mediaTypeCapabilities); }
	 */

}
