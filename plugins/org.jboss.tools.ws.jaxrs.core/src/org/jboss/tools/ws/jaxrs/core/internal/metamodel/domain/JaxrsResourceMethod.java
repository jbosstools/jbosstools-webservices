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

import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_METHOD_PARAMETERS;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_METHOD_RETURN_TYPE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_NONE;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PATH;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.PRODUCES;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils.CollectionComparison;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodSignature;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResourceMethod;

/** @author xcoulon */
public class JaxrsResourceMethod extends JaxrsResourceElement<IMethod>
		implements IJaxrsResourceMethod {

	private final JaxrsResource parentResource;

	/**
	 * return type of the java javaMethod. Null if this is not a subresource
	 * locator.
	 */
	private IType returnedJavaType = null;

	/** List of method parameters bound to the underlying java method of this resource method. */
	private final List<JavaMethodParameter> javaMethodParameters = new ArrayList<JavaMethodParameter>();


	public static class Builder {

		private final IMethod javaMethod;
		private final JaxrsMetamodel metamodel;
		private final JaxrsResource parentResource;
		private Annotation httpMethod = null;
		private Annotation consumesAnnotation = null;
		private Annotation producesAnnotation = null;
		private Annotation pathAnnotation = null;
		private final List<JavaMethodParameter> javaMethodParameters = new ArrayList<JavaMethodParameter>();
		private IType returnedJavaType;

		public Builder(IMethod method, JaxrsResource parentResource,
				JaxrsMetamodel metamodel) {
			assert method != null;
			assert parentResource != null;
			assert metamodel != null;
			this.javaMethod = method;
			this.parentResource = parentResource;
			this.metamodel = metamodel;
		}

		public Builder pathTemplate(Annotation pathTemplateAnnotation) {
			this.pathAnnotation = pathTemplateAnnotation;
			return this;
		}

		public Builder consumes(Annotation consumedMediaTypes) {
			this.consumesAnnotation = consumedMediaTypes;
			return this;
		}

		public Builder produces(Annotation producedMediaTypes) {
			this.producesAnnotation = producedMediaTypes;
			return this;
		}

		public Builder httpMethod(Annotation httpAnnotation) {
			this.httpMethod = httpAnnotation;
			return this;
		}

		public Builder methodParameter(JavaMethodParameter methodParameter) {
			this.javaMethodParameters.add(methodParameter);
			return this;
		}

		public Builder returnType(IType returnedJavaType) {
			this.returnedJavaType = returnedJavaType;
			return this;
		}

		public JaxrsResourceMethod build() throws JavaModelException {
			// needs at least one of {@Path, @HttpMethod} annotations to be a
			// valid resource method
			List<Annotation> annotations = new ArrayList<Annotation>();
			if (httpMethod != null) {
				annotations.add(httpMethod);
			}
			if (pathAnnotation != null) {
				annotations.add(pathAnnotation);
			}
			if (consumesAnnotation != null) {
				annotations.add(consumesAnnotation);
			}
			if (producesAnnotation != null) {
				annotations.add(producesAnnotation);
			}
			JaxrsResourceMethod resourceMethod = new JaxrsResourceMethod(
					javaMethod, parentResource, javaMethodParameters,
					returnedJavaType, annotations, metamodel);

			return resourceMethod;
		}

	}

	/**
	 * Full constructor.
	 * 
	 * @param annotations
	 * @param returnedJavaType
	 * @param javaMethodParameters
	 * 
	 * @param javaMethodSignature
	 * 
	 * @throws CoreException
	 */
	private JaxrsResourceMethod(final IMethod javaMethod,
			final JaxrsResource parentResource,
			List<JavaMethodParameter> javaMethodParameters,
			IType returnedJavaType, List<Annotation> annotations,
			final JaxrsMetamodel metamodel) {
		super(javaMethod, annotations, parentResource, metamodel);
		this.parentResource = parentResource;
		this.returnedJavaType = returnedJavaType;
		if (javaMethodParameters != null) {
			this.javaMethodParameters.addAll(javaMethodParameters);
		}
		this.parentResource.addMethod(this);
	}

	public int update(JavaMethodSignature methodSignature)
			throws JavaModelException {
		int flag = F_NONE;
		// method parameters, including their own annotations
		flag += updateMethodParameters(methodSignature);
		// method return type
		flag += updateReturnedType(methodSignature);
		// TODO: method thrown exceptions..
		return flag;
	}

	/**
	 * @param the methodSignature to use to update this one's returned type.
	 * @return the flag indicating some change ({@link JaxrsElementDelta.F_METHOD_RETURN_TYPE}) or no change ({@link JaxrsElementDelta.F_NONE})
	 */
	private int updateReturnedType(JavaMethodSignature methodSignature) {
		final IType returnedType = methodSignature.getReturnedType();
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
	 * @param the methodSignature to use to update this one's method parameters.
	 * @return the flag indicating some change ({@link JaxrsElementDelta.F_METHOD_PARAMETERS}) or no change ({@link JaxrsElementDelta.F_NONE})
	 */
	private int updateMethodParameters(final JavaMethodSignature methodSignature) {
		final List<JavaMethodParameter> otherMethodParameters = methodSignature.getMethodParameters();
		final CollectionComparison<JavaMethodParameter> comparison = CollectionUtils.compare(this.javaMethodParameters, otherMethodParameters);
		boolean changed = hasChanges(otherMethodParameters, comparison);
		if(changed) {
			this.javaMethodParameters.clear();
			this.javaMethodParameters.addAll(otherMethodParameters);
			return F_METHOD_PARAMETERS;
		}
		return F_NONE;
	}

	private boolean hasChanges(
			final List<JavaMethodParameter> otherMethodParameters,
			final CollectionComparison<JavaMethodParameter> comparison) {
		boolean changed = false; // track changes. "true" means at least 1 method parameter changed
		if(!comparison.getAddedItems().isEmpty()) {
			changed = true;
		}
		if(!comparison.getRemovedItems().isEmpty()) {
			changed = true;
		}
		for (JavaMethodParameter item : comparison.getItemsInCommon()) {
			final JavaMethodParameter thisMethodParameter = this.javaMethodParameters.get(this.javaMethodParameters.indexOf(item));
			final JavaMethodParameter otherMethodParameter = otherMethodParameters.get(otherMethodParameters.indexOf(item));
			if(thisMethodParameter != null && thisMethodParameter.hasChanges(otherMethodParameter)) {
				changed = true;
			}
		}
		return changed;
	}

	public IType getReturnedType() {
		return this.returnedJavaType;
	}

	@Override
	public EnumElementCategory getElementCategory() {
		return EnumElementCategory.RESOURCE_METHOD;
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.IResourceMethod#hasErrors
	 * (boolean)
	 */
	@Override
	public void hasErrors(final boolean h) {
		super.hasErrors(h);
		if (hasErrors()) {
			parentResource.hasErrors(true);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.IResourceMethod#getKind
	 * ()
	 */
	@Override
	public final EnumElementKind getElementKind() {
		final Annotation pathAnnotation = getPathAnnotation();
		final Annotation httpMethodAnnotation = getHttpMethodAnnotation();
		if (pathAnnotation == null && httpMethodAnnotation != null) {
			return EnumElementKind.RESOURCE_METHOD;
		} else if (pathAnnotation != null && httpMethodAnnotation != null) {
			return EnumElementKind.SUBRESOURCE_METHOD;
		} else if (pathAnnotation != null && httpMethodAnnotation == null) {
			return EnumElementKind.SUBRESOURCE_LOCATOR;
		}
		return EnumElementKind.UNDEFINED;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.IResourceMethod#
	 * getParentResource()
	 */
	@Override
	public final JaxrsResource getParentResource() {
		return parentResource;
	}

	public Annotation getPathAnnotation() {
		return getAnnotation(PATH.qualifiedName);
	}

	@Override
	public boolean hasPathTemplate() {
		final Annotation pathAnnotation = getPathAnnotation();
		return pathAnnotation != null && pathAnnotation.getValue("value") != null;
	}

	@Override
	public String getPathTemplate() {
		final Annotation pathAnnotation = getPathAnnotation();
		if (pathAnnotation == null) {
			return null;
		}
		return pathAnnotation.getValue("value");
	}

	public Annotation getHttpMethodAnnotation() {
		for (IJaxrsHttpMethod httpMethod : getMetamodel().getAllHttpMethods()) {
			final Annotation annotation = getAnnotation(httpMethod
					.getJavaClassName());
			if (annotation != null) {
				return annotation;
			}
		}
		return null;
	}

	@Override
	public String getHttpMethod() {
		final Annotation httpMethodAnnotation = getHttpMethodAnnotation();
		if (httpMethodAnnotation == null) {
			return null;
		}
		return httpMethodAnnotation.getValue("value");
	}

	public Annotation getConsumesAnnotation() {
		return getAnnotation(CONSUMES.qualifiedName);
	}

	@Override
	public List<String> getConsumedMediaTypes() {
		final Annotation consumesAnnotation = getConsumesAnnotation();
		if (consumesAnnotation == null) {
			return null;
		}
		return consumesAnnotation.getValues("value");
	}

	public Annotation getProducesAnnotation() {
		return getAnnotation(PRODUCES.qualifiedName);
	}

	@Override
	public List<String> getProducedMediaTypes() {
		final Annotation producesAnnotation = getProducesAnnotation();
		if (producesAnnotation == null) {
			return null;
		}
		return producesAnnotation.getValues("value");
	}

	/** @return the javaMethodParameters */
	@Override
	public List<JavaMethodParameter> getJavaMethodParameters() {
		return Collections.unmodifiableList(this.javaMethodParameters);
	}

	/**
	 * Returns a list of {@link JavaMethodParameter} that have an annotation with the given qualifiedName
	 * @param qualifiedName the expected annotation fully qualified name
	 * @return a list of {@link JavaMethodParameter}, empty if no item matches
	 */
	public List<JavaMethodParameter> getJavaMethodParametersWithAnnotation(
			String qualifiedName) {
		final List<JavaMethodParameter> matchingParameters = new ArrayList<JavaMethodParameter>();
		for (JavaMethodParameter parameter : this.javaMethodParameters) {
			if (parameter.getAnnotation(qualifiedName) != null) {
				matchingParameters.add(parameter);
			}
		}
		return matchingParameters;
	}

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return "ResourceMethod '" + parentResource.getName() + "."
				+ getJavaElement().getElementName() + "' ("
				+ getElementKind().toString() + ")";
	}

	@Override
	public List<String> getPathParamValueProposals() {
		final List<String> proposals = new ArrayList<String>();
		final Annotation methodPathAnnotation = getPathAnnotation();
		if (methodPathAnnotation != null && methodPathAnnotation.getValue("value") != null) {
			final String value = methodPathAnnotation.getValue("value");
			proposals.addAll(extractParamsFromUriTemplateFragment(value));
		}
		final Annotation typePathAnnotation = getParentResource()
				.getPathAnnotation();
		if (typePathAnnotation != null) {
			final String value = typePathAnnotation.getValue("value");
			proposals.addAll(extractParamsFromUriTemplateFragment(value));
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
	private static List<String> extractParamsFromUriTemplateFragment(
			String value) {
		List<String> params = new ArrayList<String>();
		int beginIndex = -1;
		while ((beginIndex = value.indexOf("{", beginIndex + 1)) != -1) {
			int semicolonIndex = value.indexOf(":", beginIndex);
			int closingCurlyBraketIndex = value.indexOf("}", beginIndex);
			int endIndex = (semicolonIndex != -1)? Math.min(semicolonIndex, closingCurlyBraketIndex)
					: closingCurlyBraketIndex;
			params.add(value.substring(beginIndex + 1, endIndex).trim());
		}
		return params;
	}



}
