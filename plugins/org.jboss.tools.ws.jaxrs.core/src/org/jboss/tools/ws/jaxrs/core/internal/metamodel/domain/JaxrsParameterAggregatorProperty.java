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
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_ELEMENT_KIND;
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
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.Flags;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
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
public class JaxrsParameterAggregatorProperty extends JaxrsJavaElement<IMethod> implements IJaxrsParameterAggregatorProperty {

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
	
	/** The underlying field type. */
	private final SourceType methodParameterType;

	/** The surrounding parent element. */
	private JaxrsParameterAggregator parentParameterAggregator;

	/**
	 * Full constructor.
	 * 
	 * @param builder
	 *            the fluent builder.
	 */
	private JaxrsParameterAggregatorProperty(final Builder builder) {
		super(builder.javaMethod, builder.annotations, builder.metamodel);
		this.methodParameterType = builder.javaMethodParameterType;
		this.parentParameterAggregator = builder.parentParameterAggregator;
		if(this.parentParameterAggregator != null) {
			this.parentParameterAggregator.addElement(this);
		}
	}
	
	public JaxrsParameterAggregator getParentParameterAggregator() {
		return parentParameterAggregator;
	}

	@Override
	public void update(IJavaElement javaElement, CompilationUnit ast) throws CoreException {
		if (javaElement == null) {
			remove();
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

	void update(final JaxrsParameterAggregatorProperty transientProperty) throws CoreException {
		if (transientProperty == null) {
			remove();
		} else {
			final Flags upateAnnotationsFlags = updateAnnotations(transientProperty.getAnnotations());
			final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, upateAnnotationsFlags);
			if (upateAnnotationsFlags.hasValue(F_ELEMENT_KIND) && isMarkedForRemoval()) {
				remove();
			} else if(hasMetamodel()){
				getMetamodel().update(delta);
			}
		}
	}

	/**
	 * @return {@code true} if this element should be removed (ie, it does not meet the requirements to be a {@link JaxrsParameterAggregatorProperty} anymore) 
	 */
	@Override
	boolean isMarkedForRemoval() {
		//FIXME: this looks wrong
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
	public void remove() throws CoreException {
		getParentParameterAggregator().removeProperty(this);
		super.remove();
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
	
	public SourceType getType() {
		return this.methodParameterType;
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
