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
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.NAME_BINDING;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.Flags;
import org.jboss.tools.ws.jaxrs.core.jdt.FlagsUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsNameBinding;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;

/**
 * A JAX-RS 2.0 Interceptor/Filter Name Binding Annotation.
 * <p>
 * "A filter or interceptor can be associated with a resource class or method by declaring a new binding annota- tion à la CDI. 
 * These annotations are declared using the JAX-RS meta-annotation @NameBinding and are used to decorate both the filter (or interceptor) 
 * and the resource method or resource class."
 * (JAX-RS 2.0 Spec, chap 6.)
 * </p>
 * 
 * @author xcoulon
 *
 */
public class JaxrsNameBinding extends JaxrsJavaElement<IType> implements IJaxrsNameBinding {

	

	/**
	 * Builder initializer
	 * 
	 * @param javaElement
	 *            the underlying {@link IJavaElement} that on which this JAX-RS
	 *            Element will be built.
	 * @return the Builder
	 * @throws JavaModelException
	 */
	public static Builder from(final IJavaElement javaElement) throws JavaModelException {
		final CompilationUnit ast = JdtUtils.parse(javaElement, new NullProgressMonitor());
		switch (javaElement.getElementType()) {
		case IJavaElement.COMPILATION_UNIT:
			return new Builder(((ICompilationUnit) javaElement).findPrimaryType(), ast);
		case IJavaElement.TYPE:
			return new Builder((IType) javaElement, ast);
		}
		return null;
	}

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
	public static Builder from(final IJavaElement javaElement, final CompilationUnit ast) {
		switch (javaElement.getElementType()) {
		case IJavaElement.COMPILATION_UNIT:
			return new Builder(((ICompilationUnit) javaElement).findPrimaryType(), ast);
		case IJavaElement.TYPE:
			return new Builder((IType) javaElement, ast);
		}
		return null;
	}

	/**
	 * Internal Builder
	 * 
	 * @author xcoulon
	 * 
	 */
	public static class Builder {

		private final IType javaType;
		private final CompilationUnit ast;
		private JaxrsMetamodel metamodel = null;
		private Map<String, Annotation> annotations;

		private Builder(final IType javaType, final CompilationUnit ast) {
			this.javaType = javaType;
			this.ast = ast;
		}

		public Builder withMetamodel(final JaxrsMetamodel metamodel) {
			this.metamodel = metamodel;
			return this;
		}

		public JaxrsNameBinding build() throws CoreException {
			return build(true);
		}
		
		JaxrsNameBinding build(final boolean joinMetamodel) throws CoreException {
			final long start = System.currentTimeMillis();
			try {
				if (javaType == null || !javaType.exists() || !javaType.isStructureKnown()) {
					return null;
				}
				annotations = JdtUtils.resolveAllAnnotations(javaType, ast);
				// Element *MUST* at least have the @NameBinding annotation to be an HTTP Method.
				// Problems will be reported by validation if other annotations are missing.
				if (annotations == null || annotations.isEmpty() || !annotations.containsKey(NAME_BINDING)) {
					return null;
				}
				final JaxrsNameBinding nameBinding = new JaxrsNameBinding(this);
				// this operation is only performed after creation
				if(joinMetamodel) {
					nameBinding.joinMetamodel();
				}
				return nameBinding;
			} finally {
				final long end = System.currentTimeMillis();
				Logger.tracePerf("Built JAX-RS Name Binding in {}ms", (end - start));
			}

		}
	}

	/**
	 * Full constructor.
	 * 
	 * @param builder
	 *            the fluent builder
	 * 
	 */
	private JaxrsNameBinding(final Builder builder) {
		this(builder.javaType, builder.annotations, builder.metamodel);
	}

	/**
	 * Full constructor that can be reused by {@link JaxrsBuiltinNameBinding}
	 * 
	 * @param javaType
	 *            the underlying java type.
	 * @param annotations
	 *            the relevant annotations.
	 * @param metamodel
	 *            the metamodel or <code>null</code> if the instance is
	 *            transient.
	 */
	protected JaxrsNameBinding(final IType javaType, final Map<String, Annotation> annotations,
			final JaxrsMetamodel metamodel) {
		super(javaType, annotations, metamodel);
	}

	public boolean isBuiltIn() {
		return false;
	}

	/**
	 * @return {@code true} if this element should be removed (ie, it does not meet the requirements to be a {@link JaxrsNameBinding} anymore) 
	 */
	@Override
	boolean isMarkedForRemoval() {
		// element should be removed if NameBinding annotation is missing
		return (getNameBindingAnnotation() == null);
	}

	/** @return the NameBinding Annotation */
	public Annotation getNameBindingAnnotation() {
		return getAnnotation(NAME_BINDING);
	}

	/** @return the Retention Annotation */
	public Annotation getRetentionAnnotation() {
		return getAnnotation(Retention.class.getName());
	}

	/** @return the Target Annotation */
	public Annotation getTargetAnnotation() {
		return getAnnotation(Target.class.getName());
	}

	@Override
	public String getJavaClassName() {
		return getJavaElement().getFullyQualifiedName();
	}

	@Override
	public String toString() {
		return "NameBinding [@" + getJavaClassName() + ":" + getNameBindingAnnotation() + "]";
	}

	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.NAME_BINDING;
	}

	/**
	 * Update this {@link JaxrsNameBinding} with the elements of the given element
	 * 
	 * @param element the element to use to update {@link this}
	 * @param ast the {@link ICompilationUnit} associated with the given element
	 * @throws CoreException
	 */
	@Override
	public void update(final IJavaElement element, final CompilationUnit ast) throws CoreException {
		final JaxrsNameBinding transientNameBinding = JaxrsNameBinding.from(element, ast).build(false);
		final Flags annotationsFlags = FlagsUtils.computeElementFlags(this);
		if (transientNameBinding == null) {
			remove(annotationsFlags);
		} else {
			final Flags updateAnnotationsFlags = updateAnnotations(transientNameBinding.getAnnotations());
			if (isMarkedForRemoval()) {
				remove(annotationsFlags);
			}
			// update indexes for this element.
			else if(hasMetamodel()){
				final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, updateAnnotationsFlags);
				getMetamodel().update(delta);
			}
		}
	}

}
