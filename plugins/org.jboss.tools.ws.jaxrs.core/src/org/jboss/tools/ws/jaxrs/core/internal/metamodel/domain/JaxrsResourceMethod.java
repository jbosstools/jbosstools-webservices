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
import static org.jboss.tools.ws.jaxrs.core.jdt.Annotation.VALUE;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_METHOD_PARAMETERS;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_METHOD_RETURN_TYPE;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.BEAN_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.CONSUMES;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PRODUCES;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils.CollectionComparison;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.AnnotationUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.Flags;
import org.jboss.tools.ws.jaxrs.core.jdt.FlagsUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.SourceType;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IAnnotatedSourceType;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJavaMethodParameter;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJavaMethodSignature;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;

/** @author xcoulon */
public class JaxrsResourceMethod extends JaxrsJavaElement<IMethod> implements IJaxrsResourceMethod {

	/**
	 * Builder initializer
	 * 
	 * @param javaElement
	 *            the underlying {@link IJavaElement} that on which this JAX-RS
	 *            Element will be built.
	 * @return the Builder
	 * @throws JavaModelException
	 */
	public static Builder from(final IMethod method, final Set<String> httpMethodNames)
			throws JavaModelException {
		final CompilationUnit ast = JdtUtils.parse(method, new NullProgressMonitor());
		return new Builder(method, ast, httpMethodNames); 
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
	public static Builder from(final IMethod method, final CompilationUnit ast, final Set<String> httpMethodNames) {
		return new Builder(method, ast, httpMethodNames);
	}

	/**
	 * Internal Builder
	 * 
	 * @author xcoulon
	 * 
	 */
	public static class Builder {

		private final IMethod javaMethod;
		private final CompilationUnit ast;
		private final Set<String> httpMethodNames;
		private JaxrsResource parentResource;
		private JaxrsMetamodel metamodel;
		private Map<String, Annotation> annotations;
		private IJavaMethodSignature methodSignature;
		private SourceType returnedJavaType = null;
		private List<IJavaMethodParameter> javaMethodParameters = new ArrayList<IJavaMethodParameter>();

		public Builder(final IMethod javaMethod, final CompilationUnit ast, final Set<String> httpMethodNames) {
			this.javaMethod = javaMethod;
			this.ast = ast;
			this.httpMethodNames = httpMethodNames;
		}

		public Builder withJavaMethodSignature(final IJavaMethodSignature javaMethodSignature) {
			this.methodSignature = javaMethodSignature;
			return this;
		}
		
		public Builder withAnnotations(final Map<String, Annotation> annotations) {
			this.annotations = annotations;
			return this;
		}

		/**
		 * Builds a <strong>transient<strong> instance of {@link JaxrsResourceMethod}, ie, not attached to any parent element nor included in the {@link JaxrsMetamodel}.
		 * @return a transient instance 
		 * @throws CoreException
		 */
		public JaxrsResourceMethod buildTransient() throws CoreException {
			return buildInResource(null);
		}
		
		/**
		 * Builds an  instance of {@link JaxrsResourceMethod}, ie, attached to the given parent element and included in the {@link JaxrsMetamodel}.
		 * @param parentResource the parent resource
		 * @return the created instance 
		 * @throws CoreException
		 */
		public JaxrsResourceMethod buildInResource(final JaxrsResource parentResource) throws CoreException {
			final long start = System.currentTimeMillis();
			try {
				// skip if element does not exist or if it has compilation errors
				if (javaMethod == null || !javaMethod.exists() || !javaMethod.isStructureKnown()) {
					return null;
				}
				this.parentResource = parentResource;
				if(parentResource != null) {
					this.metamodel = parentResource.getMetamodel();
				}
				if(this.annotations == null) {
					this.annotations = JdtUtils.resolveAllAnnotations(javaMethod, ast);
				}
				Annotation httpMethodAnnotation = null;
				final Annotation pathAnnotation = annotations.get(PATH);
				for (String httpMethodAnnotationName : httpMethodNames) {
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
				if(parentResource != null) {
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
	private SourceType returnedJavaType = null;

	/**
	 * List of method parameters bound to the underlying java method of this
	 * resource method.
	 */
	private final List<IJavaMethodParameter> javaMethodParameters = new ArrayList<IJavaMethodParameter>();

	/** The parent JAX-RS Resource for this element. */
	private final JaxrsResource parentResource;

	/**
	 * Full constructor.
	 * 
	 * @param builder
	 *            the fluent builder.
	 * 
	 */
	private JaxrsResourceMethod(final Builder builder) {
		super(builder.javaMethod, builder.annotations, builder.metamodel);
		this.parentResource = builder.parentResource;
		if(this.parentResource != null) {
			this.parentResource.addMethod(this);
		}
		this.returnedJavaType = builder.returnedJavaType;
		if (javaMethodParameters != null) {
			this.javaMethodParameters.addAll(builder.javaMethodParameters);
		}
	}

	/**
	 * @return the parent JAX-RS Resource
	 */
	public JaxrsResource getParentResource() {
		return parentResource;
	}
	
	/**
	 * Updates the current {@link JaxrsJavaElement} from the given
	 * {@link IJavaElement}
	 * 
	 * @param javaElement
	 * @param ast
	 * @return
	 * @throws CoreException
	 */
	// TODO: add support for java method thrown exceptions..
	@Override
	public void update(final IJavaElement javaElement, final CompilationUnit ast) throws CoreException {
		if (javaElement == null) {
			remove(FlagsUtils.computeElementFlags(this));
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
				update(from((IMethod) javaElement, ast, getMetamodel().findAllHttpMethodNames()).buildTransient());
			}
		}
	}

	public void update(final JaxrsResourceMethod transientMethod) throws CoreException {
		if (transientMethod == null) {
			remove(FlagsUtils.computeElementFlags(this));
		} else {
			final Flags updateFlags = internalUpdate(transientMethod);
			if (isMarkedForRemoval()) {
				remove(updateFlags);
			}
			// update indexes for this element.
			else if (hasMetamodel()) {
				final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, updateFlags);
				getMetamodel().update(delta);
			}
		}
	}
	
	/**
	 * Remove {@code this} from the parent {@link IJaxrsResource} before calling {@code super.remove()} which deals with removal from the {@link JaxrsMetamodel}. 
	 */
	@Override
	public void remove(final Flags flags) throws CoreException {
		getParentResource().removeMethod(this);
		super.remove(flags);
	}

	Flags internalUpdate(final JaxrsResourceMethod transientMethod) throws CoreException {
		final Flags flags = new Flags();
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
	private Flags updateReturnedType(final SourceType returnedType) {
		if ((this.returnedJavaType != null && returnedType == null)
				|| (this.returnedJavaType == null && returnedType != null)
				|| (this.returnedJavaType != null && returnedType != null && !this.returnedJavaType
						.equals(returnedType))) {
			this.returnedJavaType = returnedType;
			return new Flags(F_METHOD_RETURN_TYPE);
		}
		return Flags.NONE;
	}

	/**
	 * @param the
	 *            methodSignature to use to update this one's method parameters.
	 * @return the flag indicating some change (
	 *         {@link JaxrsElementDelta.F_METHOD_PARAMETERS}) or no change (
	 *         {@link JaxrsElementDelta.F_NONE})
	 */
	private Flags updateMethodParameters(final List<IJavaMethodParameter> otherMethodParameters) {
		final CollectionComparison<IJavaMethodParameter> comparison = CollectionUtils.compare(this.javaMethodParameters,
				otherMethodParameters);
		boolean changed = hasChanges(otherMethodParameters, comparison);
		// in any case, let's override to get the latest source ranges, even if
		// 'business' content has not changed.
		this.javaMethodParameters.clear();
		this.javaMethodParameters.addAll(otherMethodParameters);
		if (changed) {
			return new Flags(F_METHOD_PARAMETERS);
		}
		return Flags.NONE;
	}

	/**
	 * Checks if the two given collection have differences
	 * 
	 * @param otherMethodParameters
	 * @param comparison
	 * @return true if there are differences, false otherwise
	 */
	private boolean hasChanges(final List<IJavaMethodParameter> otherMethodParameters,
			final CollectionComparison<IJavaMethodParameter> comparison) {
		if (!comparison.getAddedItems().isEmpty()) {
			return true;
		}
		if (!comparison.getRemovedItems().isEmpty()) {
			return true;
		}
		for (IJavaMethodParameter item : comparison.getItemsInCommon()) {
			final IJavaMethodParameter thisMethodParameter = this.javaMethodParameters.get(this.javaMethodParameters
					.indexOf(item));
			final IJavaMethodParameter otherMethodParameter = otherMethodParameters.get(otherMethodParameters
					.indexOf(item));
			if (thisMethodParameter != null && ((JavaMethodParameter)thisMethodParameter).hasChanges(otherMethodParameter)) {
				return true;
			}
		}
		return false;
	}

	public SourceType getReturnedType() {
		return this.returnedJavaType;
	}

	public void setReturnedType(final SourceType returnedType) {
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
	public List<IJavaMethodParameter> getJavaMethodParameters() {
		return Collections.unmodifiableList(this.javaMethodParameters);
	}

	/**
	 * Returns the {@link JavaMethodParameter} whose parameter name is equal to
	 * the given parameterName, null otherwise
	 * 
	 * @param parameterName the name of the parameter to retrieve
	 * @return the {@link JavaMethodParameter} or null if none was found
	 */
	public IJavaMethodParameter getJavaMethodParameterByName(final String parameterName) {
		for (IJavaMethodParameter javaMethodParameter : this.javaMethodParameters) {
			if (javaMethodParameter.getName().equals(parameterName)) {
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
	public List<IJavaMethodParameter> getJavaMethodParametersAnnotatedWith(final String annotationName) {
		final List<IJavaMethodParameter> matchingParameters = new ArrayList<IJavaMethodParameter>();
		for (IJavaMethodParameter parameter : this.javaMethodParameters) {
			if (parameter.getAnnotation(annotationName) != null) {
				matchingParameters.add(parameter);
			}
		}
		return matchingParameters;
	}

	/**
	 * Returns the {@link JavaMethodParameter} that have an annotation
	 * with the given qualifiedName and with the given annotationValue, or {@code null} if none matches. 
	 * 
	 * @param annotationName
	 *            the expected annotation fully qualified name
	 * @return an{@link JavaMethodParameter}, or {@code null} if no item matches
	 */
	public IJavaMethodParameter getJavaMethodParameterAnnotatedWith(final String annotationName, final String annotationValue) {
		for (IJavaMethodParameter parameter : this.javaMethodParameters) {
			final Annotation annotation = parameter.getAnnotation(annotationName);
			if (annotation != null && annotationValue.equals(annotation.getValue())) {
				return parameter;
			}
		}
		return null;
	}
	
	/**
	 * Remove the {@link JavaMethodParameter} whose name is the given
	 * parameterName
	 * 
	 * @param parameterName
	 */
	public void removeJavaMethodParameter(String parameterName) {
		for (Iterator<IJavaMethodParameter> iterator = this.javaMethodParameters.iterator(); iterator.hasNext();) {
			final IJavaMethodParameter javaMethodParameter = iterator.next();
			if (javaMethodParameter.getName().equals(parameterName)) {
				iterator.remove();
				break;
			}
		}
	}

	@Override
	public Map<String, Annotation> getPathTemplateParameters() {
		final Map<String, Annotation> proposals = new HashMap<String, Annotation>();
		proposals.putAll(AnnotationUtils.extractTemplateParameters(getPathAnnotation()));
		return proposals;
	}

	/**
	 * Retrieves <strong>all</strong>:
	 * <ul>
	 * <li> {@link JaxrsResource}'s {@link JaxrsResourceField} and {@link JaxrsResourceProperty} annotated with the given annotationName,</li>
	 * <li> {@code this} {@link JavaMethodParameter} annotated with the given annotationName,</li>
	 * <li> {@link JaxrsParameterAggregatorField} and {@link JaxrsParameterAggregatorProperty} annotated with the given
	 * annotationName in a {@link JaxrsParameterAggregator} if the parent {@link JaxrsResource} has any
	 * {@link JaxrsResourceField} or {@link JaxrsResourceProperty} of the {@link JaxrsParameterAggregator} type annotated with {@code javax.ws.rs.BeanParam},</li>
	 * <li> {@link JaxrsParameterAggregatorField} and {@link JaxrsParameterAggregatorProperty} annotated with the given
	 * annotationName in a {@link JaxrsParameterAggregator} if any {@link JavaMethodParameter} of the {@link JaxrsParameterAggregator} type annotated with with {@code javax.ws.rs.BeanParam}.</li>
	 * </ul>
	 * @param annotationName
	 * @return all {@link IAnnotatedSourceType} matching the description above
	 *         ;-)
	 */
	public List<IAnnotatedSourceType> getRelatedTypesAnnotatedWith(final String annotationName) {
		final List<IAnnotatedSourceType> annotatedSourceTypes = new ArrayList<IAnnotatedSourceType>();
		// 1 - retrieve all fields and methods annotated with annotationName
		annotatedSourceTypes.addAll(getParentResource().getFieldsAnnotatedWith(annotationName));
		annotatedSourceTypes.addAll(getParentResource().getPropertiesAnnotatedWith(annotationName));
		// 2 - method parameters annotated with annotationName
		annotatedSourceTypes.addAll(getJavaMethodParametersAnnotatedWith(annotationName));
		// 3, 4 - all fields and properties annotated with 'annotationName' of parent resource fields and properties and of method parameters type annotated with @BeanParam 
		final List<IAnnotatedSourceType> beanParameters = new ArrayList<IAnnotatedSourceType>(); 
		beanParameters.addAll(getParentResource().getFieldsAnnotatedWith(BEAN_PARAM));
		beanParameters.addAll(getParentResource().getPropertiesAnnotatedWith(BEAN_PARAM));
		beanParameters.addAll(getJavaMethodParametersAnnotatedWith(BEAN_PARAM));
		for(IAnnotatedSourceType beanParameter : beanParameters) {
			final SourceType parameterAggregatorType = beanParameter.getType();
			if(parameterAggregatorType != null) {
				final JaxrsParameterAggregator parameterAggregator = (JaxrsParameterAggregator) getMetamodel().findElement(parameterAggregatorType.getErasureName(), EnumElementCategory.PARAMETER_AGGREGATOR);
				if(parameterAggregator!= null) {
					annotatedSourceTypes.addAll(parameterAggregator.getFieldsAnnotatedWith(annotationName));
					annotatedSourceTypes.addAll(parameterAggregator.getPropertiesAnnotatedWith(annotationName));
				}
			}
		}
		return annotatedSourceTypes;
	}

	/**
	 * Retrieves <strong>the first</strong> of:
	 * <ul>
	 * <li> {@link JaxrsResource}'s {@link JaxrsResourceField} and {@link JaxrsResourceProperty} annotated with the given {@code annotationName} and whose value matches the given {@code annotationValue},</li>
	 * <li> {@code this} {@link JavaMethodParameter} annotated with the given {@code annotationName} and whose value matches the given {@code annotationValue},</li>
	 * <li> {@link JaxrsParameterAggregatorField} and {@link JaxrsParameterAggregatorProperty} annotated with the given
	 * {@code annotationName} and whose value matches the given {@code annotationValue} in a {@link JaxrsParameterAggregator} if the parent {@link JaxrsResource} has any
	 * {@link JaxrsResourceField} or {@link JaxrsResourceProperty} of the {@link JaxrsParameterAggregator} type annotated with {@code javax.ws.rs.BeanParam},</li>
	 * <li> {@link JaxrsParameterAggregatorField} and {@link JaxrsParameterAggregatorProperty} annotated with the given
	 * {@code annotationName} and whose value matches the given {@code annotationValue} in a {@link JaxrsParameterAggregator} if any {@link JavaMethodParameter} of the {@link JaxrsParameterAggregator} type annotated with with {@code javax.ws.rs.BeanParam}.</li>
	 * </ul>
	 * @param annotationName the annotation fully qualified name to match
	 * @param annotationValue the annotation value to match
	 * @return all {@link IAnnotatedSourceType} matching the description above
	 *         ;-)
	 */
	public IAnnotatedSourceType getRelatedTypeAnnotatedWith(final String annotationName, final String annotationValue) {
		// 1 - method parameters annotated with annotationName
		final IJavaMethodParameter methodParameter = getJavaMethodParameterAnnotatedWith(annotationName, annotationValue);
		if(methodParameter != null) {
			return methodParameter;
		}
		// 2 - retrieve all fields and methods annotated with annotationName
		final JaxrsResourceField resourceField = getParentResource().getFieldAnnotatedWith(annotationName, annotationValue);
		if(resourceField != null) {
			return resourceField;
		}
		final JaxrsResourceProperty resourceProperty = getParentResource().getPropertyAnnotatedWith(annotationName, annotationValue);
		if(resourceProperty != null) {
			return resourceProperty;
		}
		// 3, 4 - all fields and properties annotated with 'annotationName' of parent resource fields and properties and of method parameters type annotated with @BeanParam 
		final List<IAnnotatedSourceType> beanParameters = new ArrayList<IAnnotatedSourceType>(); 
		beanParameters.addAll(getParentResource().getFieldsAnnotatedWith(BEAN_PARAM));
		beanParameters.addAll(getParentResource().getPropertiesAnnotatedWith(BEAN_PARAM));
		beanParameters.addAll(getJavaMethodParametersAnnotatedWith(BEAN_PARAM));
		for(IAnnotatedSourceType beanParameter : beanParameters) {
			final SourceType parameterAggregatorType = beanParameter.getType();
			if(parameterAggregatorType != null) {
				final JaxrsParameterAggregator parameterAggregator = (JaxrsParameterAggregator) getMetamodel().findElement(parameterAggregatorType.getErasureName(), EnumElementCategory.PARAMETER_AGGREGATOR);
				if(parameterAggregator!= null) {
					final JaxrsParameterAggregatorField aggregatorField = parameterAggregator.getFieldAnnotatedWith(annotationName, annotationValue);
					if(aggregatorField != null) {
						return aggregatorField;
					}
					final JaxrsParameterAggregatorProperty aggregatorProperty = parameterAggregator.getPropertyAnnotatedWith(annotationName, annotationValue);
					if(aggregatorProperty != null) {
						return aggregatorProperty;
					}
				}
			}
		}
		return null;
	}
	
	/**
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

}
