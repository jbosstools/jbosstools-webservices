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

import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_METHOD_PARAMETERS;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_METHOD_RETURN_TYPE;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_NONE;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.jdt.JavaMethodSignature;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.InvalidModelElementException;

/** @author xcoulon */
public class JaxrsResourceMethod extends JaxrsElement<IMethod> implements IJaxrsResourceMethod {

	private final JaxrsResource parentResource;

	private final EnumKind kind = null;

	/** return type of the java javaMethod. Null if this is not a subresource
	 * locator. */
	private IType returnedJavaType = null;

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

		public Builder(IMethod method, JaxrsResource parentResource, JaxrsMetamodel metamodel) {
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
			JaxrsResourceMethod resourceMethod = new JaxrsResourceMethod(javaMethod, parentResource,
					javaMethodParameters, returnedJavaType, annotations, metamodel);

			return resourceMethod;
		}

	}

	/** Full constructor.
	 * 
	 * @param annotations
	 * @param returnedJavaType
	 * @param javaMethodParameters
	 * 
	 * @param javaMethodSignature
	 * 
	 * @throws CoreException
	 * @throws InvalidModelElementException */
	private JaxrsResourceMethod(final IMethod javaMethod, final JaxrsResource parentResource,
			List<JavaMethodParameter> javaMethodParameters, IType returnedJavaType, List<Annotation> annotations,
			final JaxrsMetamodel metamodel) {
		super(javaMethod, annotations, metamodel);
		this.parentResource = parentResource;
		this.returnedJavaType = returnedJavaType;
		if (javaMethodParameters != null) {
			this.javaMethodParameters.addAll(javaMethodParameters);
		}
		this.parentResource.addMethod(this);
	}

	public int update(JavaMethodSignature methodSignature) throws JavaModelException {
		int flag = F_NONE;
		// method parameters, including annotations
		final List<JavaMethodParameter> methodParameters = methodSignature.getMethodParameters();
		if (!this.javaMethodParameters.equals(methodParameters)) {
			this.javaMethodParameters.clear();
			this.javaMethodParameters.addAll(methodParameters);
			flag = F_METHOD_PARAMETERS;
		}
		// method return type
		final IType returnedType = methodSignature.getReturnedType();
		if ((this.returnedJavaType != null && returnedType == null)
				|| (this.returnedJavaType == null && returnedType != null)
				|| (this.returnedJavaType != null && returnedType != null && !this.returnedJavaType
						.equals(returnedType))) {
			this.returnedJavaType = returnedType;
			flag += F_METHOD_RETURN_TYPE;
		}
		// TODO: method thrown exceptions..
		return flag;
	}

	@Override
	public IType getReturnType() {
		return this.returnedJavaType;
	}

	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.RESOURCE_METHOD;
	}

	/** {@inheritDoc}
	 * 
	 * @throws CoreException */
	@Override
	public void validate(IProgressMonitor progressMonitor) throws CoreException {
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
	public final EnumKind getKind() {
		final Annotation pathAnnotation = getPathAnnotation();
		final Annotation httpMethodAnnotation = getHttpMethodAnnotation();
		if (pathAnnotation == null && httpMethodAnnotation != null) {
			return EnumKind.RESOURCE_METHOD;
		} else if (pathAnnotation != null && httpMethodAnnotation != null) {
			return EnumKind.SUBRESOURCE_METHOD;
		} else if (pathAnnotation != null && httpMethodAnnotation == null) {
			return EnumKind.SUBRESOURCE_LOCATOR;
		}
		return EnumKind.UNDEFINED;
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

	@Override
	public Annotation getPathAnnotation() {
		return getAnnotation(Path.class.getName());
	}

	@Override
	public String getPathTemplate() {
		final Annotation pathAnnotation = getPathAnnotation();
		if (pathAnnotation == null) {
			return null;
		}
		return pathAnnotation.getValue("value");
	}

	@Override
	public Annotation getHttpMethodAnnotation() {
		for (IJaxrsHttpMethod httpMethod : getMetamodel().getAllHttpMethods()) {
			final Annotation annotation = getAnnotation(httpMethod.getJavaElement().getFullyQualifiedName());
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

	@Override
	public Annotation getConsumesAnnotation() {
		return getAnnotation(Consumes.class.getName());
	}

	@Override
	public List<String> getConsumedMediaTypes() {
		final Annotation consumesAnnotation = getConsumesAnnotation();
		if (consumesAnnotation == null) {
			return null;
		}
		return consumesAnnotation.getValues("value");
	}

	@Override
	public Annotation getProducesAnnotation() {
		return getAnnotation(Produces.class.getName());
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
		return javaMethodParameters;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return getKind().toString().toLowerCase() + " " + parentResource.getName() + "."
				+ getJavaElement().getElementName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getJavaElement().hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof JaxrsResourceMethod) {
			return getJavaElement().equals(((JaxrsResourceMethod) obj).getJavaElement());
		}
		return false;
	}

}
