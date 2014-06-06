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

import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_METHOD_PARAMETERS;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_METHOD_RETURN_TYPE;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_NONE;
import static org.jboss.tools.ws.jaxrs.core.utils.Annotation.VALUE;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PRODUCES;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.Flags;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils.CollectionComparison;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.jboss.tools.ws.jaxrs.core.utils.Annotation;
import org.jboss.tools.ws.jaxrs.core.utils.JavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.utils.JavaMethodSignature;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;

/** @author xcoulon */
public class JaxrsResourceMethod extends JaxrsResourceElement<IMethod> implements IJaxrsResourceMethod {

	/**
	 * Builder initializer
	 * 
	 * @param javaElement
	 *            the underlying {@link IJavaElement} that on which this JAX-RS
	 *            Element will be built.
	 * @return the Builder
	 * @throws JavaModelException
	 */
	public static Builder from(final IMethod method, final List<IJaxrsHttpMethod> httpMethods)
			throws JavaModelException {
		final CompilationUnit ast = JdtUtils.parse(method, new NullProgressMonitor());
		return new Builder(method, ast, httpMethods); 
	}

	/**
	 * Builder initializer
	 * 
	 * @param method
	 *            the underlying {@link IJavaElement} that on which this JAX-RS
	 *            Element will be built.
	 * @param ast
	 *            the associated AST
	 * @return the Builder
	 * @throws JavaModelException
	 */
	public static Builder from(final IMethod method, final CompilationUnit ast, final List<IJaxrsHttpMethod> httpMethods) {
		return new Builder(method, ast, httpMethods);
	}

	/**
	 * Internal Builder
	 * 
	 * @author xcoulon
	 * 
	 */
	public static class Builder {

		private final IMethod javaMethod;
		private final CompilationUnit ast;;
		private final List<IJaxrsHttpMethod> httpMethods;
		private JaxrsResource parentResource;
		private Map<String, Annotation> annotations;
		private IType returnedJavaType = null;
		private List<JavaMethodParameter> javaMethodParameters = new ArrayList<JavaMethodParameter>();
		private JaxrsMetamodel metamodel;
		private JavaMethodSignature methodSignature;

		public Builder(final IMethod javaMethod, final CompilationUnit ast, final List<IJaxrsHttpMethod> httpMethods) {
			this.javaMethod = javaMethod;
			this.ast = ast;
			this.httpMethods = httpMethods;
		}

		public Builder withMetamodel(final JaxrsMetamodel metamodel) {
			this.metamodel = metamodel;
			return this;
		}
		
		public Builder withJavaMethodSignature(final JavaMethodSignature javaMethodSignature) {
			this.methodSignature = javaMethodSignature;
			return this;
		}

		public Builder withParentResource(final JaxrsResource parentResource) {
			this.parentResource = parentResource;
			return this;
		}

		public JaxrsResourceMethod build() throws CoreException {
			return build(true);
		}
		
		JaxrsResourceMethod build(final boolean joinMetamodel) throws CoreException {
			final long start = System.currentTimeMillis();
			try {
				// skip if element does not exist or if it has compilation errors
				if (javaMethod == null || !javaMethod.exists() || !javaMethod.isStructureKnown()) {
					return null;
				}

				final List<String> httpMethodAnnotationNames = new ArrayList<String>();
				for (IJaxrsHttpMethod httpMethod : httpMethods) {
					httpMethodAnnotationNames.add(httpMethod.getJavaClassName());
				}
				this.annotations = JdtUtils.resolveAllAnnotations(javaMethod, ast);
				Annotation httpMethodAnnotation = null;
				final Annotation pathAnnotation = annotations.get(PATH);
				for (String httpMethodAnnotationName : httpMethodAnnotationNames) {
					if (annotations.containsKey(httpMethodAnnotationName)) {
						httpMethodAnnotation = annotations.get(httpMethodAnnotationName);
						break;
					}
				}
				if (httpMethodAnnotation == null && pathAnnotation == null) {
					Logger.debug("Cannot create ResourceMethod: no Path annotation nor HttpMethod found on method {}.{}()",
							javaMethod.getParent().getElementName(), javaMethod.getElementName());
					return null;
				}
				// if method signature was not provided before.
				if(methodSignature == null) {
					methodSignature = JdtUtils.resolveMethodSignature(javaMethod, ast);
				}
				// avoid creating Resource Method when the Java Method cannot be
				// parsed (ie, syntax/compilation error)
				if (methodSignature == null) {
					return null;
				}
				javaMethodParameters = methodSignature.getMethodParameters();
				returnedJavaType = methodSignature.getReturnedType();
				final JaxrsResourceMethod resourceMethod = new JaxrsResourceMethod(this);
				if(joinMetamodel) {
					resourceMethod.joinMetamodel();
				}
				return resourceMethod;
			} finally {
				final long end = System.currentTimeMillis();
				Logger.tracePerf("Built JAX-RS Resource Method in {}ms", (end - start));
			}
		}

	}

	/**
	 * return type of the java javaMethod. Null if this is not a subresource
	 * locator.
	 */
	private IType returnedJavaType = null;

	/**
	 * List of method parameters bound to the underlying java method of this
	 * resource method.
	 */
	private final List<JavaMethodParameter> javaMethodParameters = new ArrayList<JavaMethodParameter>();

	/**
	 * Full constructor.
	 * 
	 * @param builder
	 *            the fluent builder.
	 * 
	 */
	private JaxrsResourceMethod(final Builder builder) {
		super(builder.javaMethod, builder.annotations, builder.parentResource, builder.metamodel);
		this.returnedJavaType = builder.returnedJavaType;
		if (javaMethodParameters != null) {
			this.javaMethodParameters.addAll(builder.javaMethodParameters);
		}
	}

	/**
	 * Updates the current {@link AbstractJaxrsJavaElement} from the given
	 * {@link IJavaElement}
	 * 
	 * @param javaElement
	 * @param ast
	 * @return
	 * @throws CoreException
	 */
	// TODO: add support for java method thrown exceptions..
	public void update(final IJavaElement javaElement, final CompilationUnit ast) throws CoreException {
		if (javaElement == null) {
			remove();
		} else {
			// NOTE: the given javaElement may be an ICompilationUnit (after
			// resource change) !!
			switch (javaElement.getElementType()) {
			case IJavaElement.COMPILATION_UNIT:
				final IType primaryType = ((ICompilationUnit) javaElement).findPrimaryType();
				if (primaryType != null) {
					final IMethod method = primaryType.getMethod(getJavaElement().getElementName(), getJavaElement()
							.getParameterNames());
					update(method, ast);
				}
				break;
			case IJavaElement.METHOD:
				update(from((IMethod) javaElement, ast, getMetamodel().findAllHttpMethods()).build(false));
			}
		}
	}

	public void update(final JaxrsResourceMethod transientMethod) throws CoreException {
		if (transientMethod == null) {
			remove();
		} else {
			Flags flags = internalUpdate(transientMethod);
			final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, flags);
			if (isMarkedForRemoval()) {
				remove();
			}
			// update indexes for this element.
			else if (hasMetamodel()) {
				getMetamodel().update(delta);
			}
		}
	}
	
	/**
	 * Remove {@code this} from the parent {@link IJaxrsResource} before calling {@code super.remove()} which deals with removal from the {@link JaxrsMetamodel}. 
	 */
	@Override
	public void remove() throws CoreException {
		// no need to remove again if this element is not part of the metamodel anymore
		if(getParentResource().hasMethod(this)) {
			getParentResource().removeMethod(this);
		}
		if(getMetamodel().containsElement(this)) {
			super.remove();
		}
	}

	Flags internalUpdate(final JaxrsResourceMethod transientMethod) throws CoreException {
		Flags flags = new Flags();
		flags.addFlags(updateAnnotations(transientMethod.getAnnotations()));
		// method parameters, including their own annotations
		flags.addFlags(updateMethodParameters(transientMethod.getJavaMethodParameters()));
		// method return type
		flags.addFlags(updateReturnedType(transientMethod.getReturnedType()));
		return flags;
	}

	/**
	 * @param the
	 *            methodSignature to use to update this one's returned type.
	 * @return the flag indicating some change (
	 *         {@link JaxrsElementDelta.F_METHOD_RETURN_TYPE}) or no change (
	 *         {@link JaxrsElementDelta.F_NONE})
	 */
	private int updateReturnedType(final IType returnedType) {
		if ((this.returnedJavaType != null && returnedType == null)
				|| (this.returnedJavaType == null && returnedType != null)
				|| (this.returnedJavaType != null && returnedType != null && !this.returnedJavaType
						.equals(returnedType))) {
			this.returnedJavaType = returnedType;
			return F_METHOD_RETURN_TYPE;
		}
		return F_NONE;
	}

	/**
	 * @param the
	 *            methodSignature to use to update this one's method parameters.
	 * @return the flag indicating some change (
	 *         {@link JaxrsElementDelta.F_METHOD_PARAMETERS}) or no change (
	 *         {@link JaxrsElementDelta.F_NONE})
	 */
	private int updateMethodParameters(final List<JavaMethodParameter> otherMethodParameters) {
		final CollectionComparison<JavaMethodParameter> comparison = CollectionUtils.compare(this.javaMethodParameters,
				otherMethodParameters);
		boolean changed = hasChanges(otherMethodParameters, comparison);
		// in any case, let's override to get the latest source ranges, even if
		// 'business' content has not changed.
		this.javaMethodParameters.clear();
		this.javaMethodParameters.addAll(otherMethodParameters);
		if (changed) {
			return F_METHOD_PARAMETERS;
		}
		return F_NONE;
	}

	/**
	 * Checks if the two given collection have differences
	 * 
	 * @param otherMethodParameters
	 * @param comparison
	 * @return true if there are differences, false otherwise
	 */
	private boolean hasChanges(final List<JavaMethodParameter> otherMethodParameters,
			final CollectionComparison<JavaMethodParameter> comparison) {
		if (!comparison.getAddedItems().isEmpty()) {
			return true;
		}
		if (!comparison.getRemovedItems().isEmpty()) {
			return true;
		}
		for (JavaMethodParameter item : comparison.getItemsInCommon()) {
			final JavaMethodParameter thisMethodParameter = this.javaMethodParameters.get(this.javaMethodParameters
					.indexOf(item));
			final JavaMethodParameter otherMethodParameter = otherMethodParameters.get(otherMethodParameters
					.indexOf(item));
			if (thisMethodParameter != null && thisMethodParameter.hasChanges(otherMethodParameter)) {
				return true;
			}
		}
		return false;
	}

	public IType getReturnedType() {
		return this.returnedJavaType;
	}

	public void setReturnedType(final IType returnedType) {
		this.returnedJavaType = returnedType;
	}

	/**
	 * @return {@code true} if this element should be removed (ie, it does not meet the requirements to be a {@link JaxrsResourceMethod} anymore) 
	 */
	@Override
	boolean isMarkedForRemoval() {
		final boolean hasPathAnnotation = hasAnnotation(PATH);
		final boolean hasHttpMethodAnnotation = getHttpMethodAnnotation() != null;
		// element should be removed if it has no @Path annotation and it has no
		// HTTP Method annotation
		return !(hasPathAnnotation || hasHttpMethodAnnotation);
	}

	@Override
	public final EnumElementKind getElementKind() {
		if (hasMetamodel()) {
			final Annotation pathAnnotation = getPathAnnotation();
			final Annotation httpMethodAnnotation = getHttpMethodAnnotation();
			if (pathAnnotation == null && httpMethodAnnotation != null) {
				return EnumElementKind.RESOURCE_METHOD;
			} else if (pathAnnotation != null && httpMethodAnnotation != null) {
				return EnumElementKind.SUBRESOURCE_METHOD;
			} else if (pathAnnotation != null && httpMethodAnnotation == null) {
				return EnumElementKind.SUBRESOURCE_LOCATOR;
			}
		}
		return EnumElementKind.UNDEFINED_RESOURCE_METHOD;
	}

	public Annotation getPathAnnotation() {
		return getAnnotation(PATH);
	}

	@Override
	public boolean hasPathTemplate() {
		final Annotation pathAnnotation = getPathAnnotation();
		return pathAnnotation != null && pathAnnotation.getValue() != null;
	}

	@Override
	public String getPathTemplate() {
		final Annotation pathAnnotation = getPathAnnotation();
		if (pathAnnotation == null) {
			return null;
		}
		return pathAnnotation.getValue();
	}
	
	
	public Annotation getHttpMethodAnnotation() {
		if (hasMetamodel()) {
			for (IJaxrsHttpMethod httpMethod : getMetamodel().findAllHttpMethods()) {
				final Annotation annotation = getAnnotation(httpMethod.getJavaClassName());
				if (annotation != null) {
					return annotation;
				}
			}
		}
		return null;
	}

	@Override
	public String getHttpMethodClassName() {
		final Annotation httpMethodAnnotation = getHttpMethodAnnotation();
		if (httpMethodAnnotation == null) {
			return null;
		}
		return httpMethodAnnotation.getFullyQualifiedName();
	}

	public Annotation getConsumesAnnotation() {
		return getAnnotation(CONSUMES);
	}

	@Override
	public List<String> getConsumedMediaTypes() {
		final Annotation consumesAnnotation = getConsumesAnnotation();
		if (consumesAnnotation != null) {
			return consumesAnnotation.getValues(VALUE);
		}
		return Collections.emptyList();
	}

	public Annotation getProducesAnnotation() {
		return getAnnotation(PRODUCES);
	}

	@Override
	public List<String> getProducedMediaTypes() {
		final Annotation producesAnnotation = getProducesAnnotation();
		if (producesAnnotation != null) {
			return producesAnnotation.getValues(VALUE);
		}
		return Collections.emptyList();
	}

	/** @return the javaMethodParameters */
	@Override
	public List<JavaMethodParameter> getJavaMethodParameters() {
		return Collections.unmodifiableList(this.javaMethodParameters);
	}

	/**
	 * Returns the {@link JavaMethodParameter} whose parameter name is equal to
	 * the given parameterName, null otherwise
	 * 
	 * @param parameterName the name of the parameter to retrieve
	 * @return the {@link JavaMethodParameter} or null if none was found
	 */
	public JavaMethodParameter getJavaMethodParameterByName(final String parameterName) {
		for (JavaMethodParameter javaMethodParameter : this.javaMethodParameters) {
			if (javaMethodParameter.getName().equals(parameterName)) {
				return javaMethodParameter;
			}
		}
		return null;
	}

	/**
	 * Returns the {@link JavaMethodParameter} associated with a {@code @PathParam} annotation.
	 * the given parameterName, null otherwise
	 * 
	 * @param pathParamName the name in the {@code @PathParam} annotation
	 * @return the {@link JavaMethodParameter} or {@code null} if none was found
	 */
	public JavaMethodParameter getJavaMethodParameterByAnnotationBinding(final String pathParamName) {
		for (JavaMethodParameter javaMethodParameter : this.javaMethodParameters) {
			final Annotation pathParamAnnotation = javaMethodParameter.getAnnotation(PATH_PARAM);
			if (pathParamAnnotation != null && pathParamName.equals(pathParamAnnotation.getValue())) {
				return javaMethodParameter;
			}
		}
		return null;
	}

	/**
	 * Returns a list of {@link JavaMethodParameter} that have an annotation
	 * with the given qualifiedName
	 * 
	 * @param annotationName
	 *            the expected annotation fully qualified name
	 * @return a list of {@link JavaMethodParameter}, empty if no item matches
	 */
	public List<JavaMethodParameter> getJavaMethodParametersAnnotatedWith(String annotationName) {
		final List<JavaMethodParameter> matchingParameters = new ArrayList<JavaMethodParameter>();
		for (JavaMethodParameter parameter : this.javaMethodParameters) {
			if (parameter.getAnnotation(annotationName) != null) {
				matchingParameters.add(parameter);
			}
		}
		return matchingParameters;
	}

	/**
	 * Remove the {@link JavaMethodParameter} whose name is the given
	 * parameterName
	 * 
	 * @param parameterName
	 */
	public void removeJavaMethodParameter(String parameterName) {
		for (Iterator<JavaMethodParameter> iterator = this.javaMethodParameters.iterator(); iterator.hasNext();) {
			final JavaMethodParameter javaMethodParameter = iterator.next();
			if (javaMethodParameter.getName().equals(parameterName)) {
				iterator.remove();
				break;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return "ResourceMethod '"
				+ (getParentResource() != null ? getParentResource().getName() : "<No Parent Resource>") + "."
				+ getJavaElement().getElementName()
				+ "' " + (getHttpMethodClassName() != null ? getHttpMethodClassName() : "")
				+ " " + (hasPathTemplate() ? getPathTemplate() : "")
				+ " (" + getElementKind().toString() + ")";
	}

	@Override
	public Map<String, Annotation> getPathParamValueProposals() {
		final Map<String, Annotation> proposals = new HashMap<String, Annotation>();
		proposals.putAll(extractProposals(getPathAnnotation()));
		proposals.putAll(extractProposals(getParentResource().getPathAnnotation()));
		return proposals;
	}

	private Map<String, Annotation> extractProposals(final Annotation pathAnnotation) {
		final Map<String, Annotation> proposals = new HashMap<String, Annotation>();
		if (pathAnnotation != null && pathAnnotation.getValue() != null) {
			final String value = pathAnnotation.getValue();
			List<String> params = extractParamsFromUriTemplateFragment(value);
			for (String param : params) {
				proposals.put(param, pathAnnotation);
			}
		}
		return proposals;
	}

	/**
	 * Extracts all the character sequences inside of curly braces ('{' and '}')
	 * and returns them as a list of strings
	 * 
	 * @param value
	 *            the given value
	 * @return the list of character sequences, or an empty list
	 */
	private static List<String> extractParamsFromUriTemplateFragment(String value) {
		List<String> params = new ArrayList<String>();
		int beginIndex = -1;
		while ((beginIndex = value.indexOf("{", beginIndex + 1)) != -1) {
			int semicolonIndex = value.indexOf(":", beginIndex);
			int closingCurlyBraketIndex = value.indexOf("}", beginIndex);
			int endIndex = (semicolonIndex != -1) ? Math.min(semicolonIndex, closingCurlyBraketIndex)
					: closingCurlyBraketIndex;
			params.add(value.substring(beginIndex + 1, endIndex).trim());
		}
		return params;
	}

	
}
