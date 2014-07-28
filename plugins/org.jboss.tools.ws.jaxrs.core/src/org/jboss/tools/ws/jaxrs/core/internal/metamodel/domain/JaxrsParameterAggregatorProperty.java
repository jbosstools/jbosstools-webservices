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
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.DEFAULT_VALUE;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.MATRIX_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.QUERY_PARAM;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.AnnotationUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.Flags;
import org.jboss.tools.ws.jaxrs.core.jdt.FlagsUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.SourceType;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJavaMethodSignature;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsParameterAggregatorProperty;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;

/**
 * JAX-RS Parameter Aggregator Property, ie, a bean accessor annotated with one of {@link JaxrsParamAnnotations#PARAM_ANNOTATIONS}.
 * 
 * @author xcoulon
 */
public class JaxrsParameterAggregatorProperty extends JaxrsParameterAggregatorElement<IMethod> implements IJaxrsParameterAggregatorProperty {

	/**
	 * Builder initializer
	 * 
	 * @param javaElement
	 *            the underlying {@link IJavaElement} that on which this JAX-RS
	 *            Element will be built.
	 * @param ast
	 *            the associated AST
	 * @return the Builder
	 * @throws JavaModelException
	 */
	public static Builder from(final IMethod javaMethod, final CompilationUnit ast) {
		return new Builder(javaMethod, ast);
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
		private Map<String, Annotation> annotations;
		private JaxrsParameterAggregator parentParameterAggregator;
		private JaxrsMetamodel metamodel;
		private SourceType javaMethodParameterType;

		private Builder(final IMethod javaMethod, final CompilationUnit ast) {
			this.javaMethod = javaMethod;
			this.ast = ast;
		}

		public JaxrsParameterAggregatorProperty buildTransient() throws CoreException {
			return buildInParentAggregator(null);
		}
		
		public Builder withAnnotations(final Map<String, Annotation> annotations) {
			this.annotations = annotations;
			return this;
		}
		
		public JaxrsParameterAggregatorProperty buildInParentAggregator(final JaxrsParameterAggregator parentParameterAggregator) throws CoreException {
			final long start = System.currentTimeMillis();
			try {
				// skip if element does not exist or if it has compilation errors
				if (javaMethod == null || !javaMethod.exists() || !javaMethod.isStructureKnown()) {
					return null;
				}
				this.parentParameterAggregator = parentParameterAggregator;
				if(this.parentParameterAggregator != null) {
					this.metamodel = this.parentParameterAggregator.getMetamodel();
				}
				final IJavaMethodSignature methodSignature = JdtUtils.resolveMethodSignature(javaMethod, ast);
				this.javaMethodParameterType = JdtUtils.getPropertyType(methodSignature);
				final IType parentType = (IType) javaMethod.getParent();
				// lookup parent resource in metamodel
				if (parentParameterAggregator == null && metamodel != null) {
					Logger.trace("Skipping {}.{} because parent Parameter Aggregator does not exist", parentType.getFullyQualifiedName(), javaMethod.getElementName());
				}
				if(this.annotations == null) {
					this.annotations = JdtUtils.resolveAllAnnotations(javaMethod, ast);
				}
				if (JaxrsParamAnnotations.matchesAtLeastOne(annotations.keySet())) {
					final JaxrsParameterAggregatorProperty field = new JaxrsParameterAggregatorProperty(this);
					// this operation is only performed after creation
					if(this.metamodel != null) {
						field.joinMetamodel();
					}
					return field;
				}
				return null;
			} finally {
				final long end = System.currentTimeMillis();
				Logger.tracePerf("Built JAX-RS Resource Method in {}ms", (end - start));
			}
		}

	}
	
	/**
	 * Full constructor.
	 * 
	 * @param builder
	 *            the fluent builder.
	 */
	private JaxrsParameterAggregatorProperty(final Builder builder) {
		this(builder.javaMethod, builder.annotations, builder.metamodel, builder.javaMethodParameterType,
				builder.parentParameterAggregator, null);
	}
	
	/**
	 * Full constructor.
	 * 
	 * @param javaMethod
	 *            the java method
	 * @param annotations
	 *            the java element annotations (or null)
	 * @param metamodel
	 *            the metamodel in which this element exist, or null if this
	 *            element is transient.
	 * @param javaMethodParameterType the {@link SourceType} associated with the given javaMethod
	 * @param parentParameterAggregator
	 * 			the parent element
	 * @param primaryCopy
	 *            the associated primary copy element, or {@code null} if this
	 *            instance is already the primary element
	 */
	private JaxrsParameterAggregatorProperty(final IMethod javaMethod, final Map<String, Annotation> annotations,
			final JaxrsMetamodel metamodel, final SourceType javaMethodParameterType,
			final JaxrsParameterAggregator parentParameterAggregator, final JaxrsParameterAggregatorProperty primaryCopy) {
		super(javaMethod, annotations, metamodel, javaMethodParameterType, parentParameterAggregator, primaryCopy);
		if(getParentParameterAggregator() != null) {
			getParentParameterAggregator().addElement(this);
		}
	}

	@Override
	public JaxrsParameterAggregatorProperty createWorkingCopy() {
		synchronized (this) {
			final JaxrsParameterAggregator parentWorkingCopy = getParentParameterAggregator().getWorkingCopy();
			return parentWorkingCopy.getProperties().get(this.javaElement.getHandleIdentifier());
		}
	}
	
	protected JaxrsParameterAggregatorProperty createWorkingCopy(final JaxrsParameterAggregator parentWorkingCopy) {
		return new JaxrsParameterAggregatorProperty(getJavaElement(),
				AnnotationUtils.createWorkingCopies(getAnnotations()), getMetamodel(), getType(),
				parentWorkingCopy, this);
	}
	
	@Override
	public JaxrsParameterAggregatorProperty getWorkingCopy() {
		return (JaxrsParameterAggregatorProperty) super.getWorkingCopy();
	}
	
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
					final IMethod method = primaryType.getMethod(getJavaElement().getElementName(), getJavaElement().getParameterTypes());
					update(method, ast);
				}
				break;
			case IJavaElement.METHOD:
				update(from((IMethod) javaElement, ast).buildTransient());
			}
		} 
	}

	/**
	 * Updates this {@link JaxrsParameterAggregatorProperty} from the given {@code transientProperty}.
	 * @param transientProperty
	 * @throws CoreException
	 */
	void update(final JaxrsParameterAggregatorProperty transientProperty) throws CoreException {
		synchronized (this) {
			final Flags annotationsFlags = FlagsUtils.computeElementFlags(this);
			if (transientProperty == null) {
				remove(annotationsFlags);
			} else {
				final Flags updateAnnotationsFlags = updateAnnotations(transientProperty.getAnnotations());
				final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, updateAnnotationsFlags);
				if (isMarkedForRemoval()) {
					remove(annotationsFlags);
				} else if(hasMetamodel()){
					getMetamodel().update(delta);
				}
			}
		}
	}

	/**
	 * @return {@code true} if this element should be removed (ie, it does not meet the requirements to be a {@link JaxrsParameterAggregatorProperty} anymore) 
	 */
	@Override
	boolean isMarkedForRemoval() {
		final boolean hasPathParamAnnotation = hasAnnotation(PATH_PARAM);
		final boolean hasQueryParamAnnotation = hasAnnotation(QUERY_PARAM);
		final boolean hasMatrixParamAnnotation = hasAnnotation(MATRIX_PARAM);
		// element should be removed if it has neither @PathParam, @QueryParam
		// nor @MatrixParam annotation
		return !(hasPathParamAnnotation || hasQueryParamAnnotation || hasMatrixParamAnnotation);
	}
	
	/**
	 * Remove {@code this} from the parent {@link IJaxrsResource} before calling {@code super.remove()} which deals with removal from the {@link JaxrsMetamodel}. 
	 */
	@Override
	public void remove(final Flags flags) throws CoreException {
		getParentParameterAggregator().removeProperty(this);
		super.remove(flags);
	}

	public Annotation getPathParamAnnotation() {
		return getAnnotation(PATH_PARAM);
	}

	public Annotation getQueryParamAnnotation() {
		return getAnnotation(QUERY_PARAM);
	}

	public Annotation getMatrixParamAnnotation() {
		return getAnnotation(MATRIX_PARAM);
	}

	public Annotation getDefaultValueAnnotation() {
		return getAnnotation(DEFAULT_VALUE);
	}
	
	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.PARAMETER_AGGREGATOR_PROPERTY;
	}

	@Override
	public String toString() {
		return "ResourceField '" + getJavaElement().getParent().getElementName() + "."
			+ getJavaElement().getElementName() + "' | annotations=" + getAnnotations();
	}

}
