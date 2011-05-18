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
	 * Constructor
	 * 
	 * @param javaMethod
	 * @param metamodel
	 * @param problems
	 * @throws CoreException
	 * @throws InvalidModelElementException
	 */
	public ResourceMethod(final IMethod javaMethod, final Resource parentResource, final Metamodel metamodel,
			final IProgressMonitor progressMonitor) throws InvalidModelElementException, CoreException {
		super(metamodel, javaMethod);
		this.parentResource = parentResource;
		setState(EnumState.CREATING);
		merge(javaMethod, progressMonitor);
		setState(EnumState.CREATED);
	}

	@Override
	public final void merge(final IMethod javaMethod, final IProgressMonitor progressMonitor)
			throws InvalidModelElementException, CoreException {
		CompilationUnit compilationUnit = getCompilationUnit(javaMethod, progressMonitor);
		if (getState() == EnumState.CREATED) {
			Set<IProblem> problems = JdtUtils.resolveErrors(javaMethod, compilationUnit);
			if (problems != null && problems.size() > 0) {
				// metamodel.reportErrors(javaType, problems);
				return;
			}
		}
		IMethodBinding methodBinding = JdtUtils.resolveMethodBinding(javaMethod, compilationUnit);
		if (uriMapping == null) {
			this.uriMapping = new UriMapping(javaMethod, compilationUnit, getMetamodel());
		} else {
			this.uriMapping.merge(javaMethod, compilationUnit);
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
