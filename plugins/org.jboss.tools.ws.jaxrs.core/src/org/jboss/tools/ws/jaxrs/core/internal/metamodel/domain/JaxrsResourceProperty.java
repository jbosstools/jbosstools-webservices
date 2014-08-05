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
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.BEAN_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.DEFAULT_VALUE;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.MATRIX_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.QUERY_PARAM;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.Flags;
import org.jboss.tools.ws.jaxrs.core.jdt.FlagsUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.SourceType;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJavaMethodSignature;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceProperty;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;

/**
 * JAX-RS Resource Field.
 * 
 * @author xcoulon
 */
public class JaxrsResourceProperty extends JaxrsResourceElement<IMethod> implements IJaxrsResourceProperty {

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
	public static Builder from(final IMethod method, final CompilationUnit ast) {
		return new Builder(method, ast);
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
		private JaxrsResource parentResource;
		private JaxrsMetamodel metamodel;
		private SourceType javaPropertyType;
		private IJavaMethodSignature methodSignature;
		
		private Builder(final IMethod javaMethod, final CompilationUnit ast) {
			this.javaMethod = javaMethod;
			this.ast = ast;
		}

		public Builder withAnnotations(final Map<String, Annotation> annotations) {
			this.annotations = annotations;
			return this;
		}

		public Builder withJavaMethodSignature(final IJavaMethodSignature javaMethodSignature) {
			this.methodSignature = javaMethodSignature;
			return this;
		}
		
		public JaxrsResourceProperty buildTransient() throws CoreException {
			return buildInResource(null);
		}
		
		JaxrsResourceProperty buildInResource(final JaxrsResource parentResource) throws CoreException {
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
				if(!JaxrsParamAnnotations.matchesAtLeastOne(annotations.keySet())) {
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
				
				this.javaPropertyType = JdtUtils.getPropertyType(methodSignature);
				if(this.javaPropertyType == null) {
					return null;
				}
				final JaxrsResourceProperty resourceProperty = new JaxrsResourceProperty(this);
				if(parentResource != null) {
					resourceProperty.joinMetamodel();
				}
				return resourceProperty;
			} finally {
				final long end = System.currentTimeMillis();
				Logger.tracePerf("Built JAX-RS Resource Method in {}ms", (end - start));
			}
		}

	}
	
	/** the underlying method signature. */
	private IJavaMethodSignature methodSignature;
	
	/**
	 * Full constructor.
	 * 
	 * @param builder
	 *            the fluent builder.
	 */
	private JaxrsResourceProperty(final Builder builder) {
		super(builder.javaMethod, builder.annotations, builder.metamodel, builder.javaPropertyType, builder.parentResource);
		this.methodSignature = builder.methodSignature;
		if(getParentResource() != null) {
			getParentResource().addProperty(this);
		}
	}

	/**
	 * @return the parent JAX-RS Resource
	 */
	public JaxrsResource getParentResource() {
		return parentResource;
	}
	
	public IJavaMethodSignature getMethodSignature() {
		return methodSignature;
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
					final IField field = primaryType.getField(getJavaElement().getElementName());
					update(field, ast);
				}
				break;
			case IJavaElement.METHOD:
				update(from((IMethod) javaElement, ast).buildTransient());
			}
		} 
	}

	/**
	 * Updates this {@link JaxrsResourceProperty} from the given {@code transientProperty}.  
	 * @param transientProperty
	 * @param flags
	 * @throws CoreException
	 */
	void update(final JaxrsResourceProperty transientProperty) throws CoreException {
		final Flags annotationsFlags = FlagsUtils.computeElementFlags(this);
		if (transientProperty == null) {
			// give a hint about the existing JAX-RS annotations before the element is removed.
			remove(annotationsFlags);
		} else {
			final Flags updateAnnotationsFlags = updateAnnotations(transientProperty.getAnnotations());
			final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, updateAnnotationsFlags);
			if (updateAnnotationsFlags.hasValue(F_ELEMENT_KIND) && isMarkedForRemoval()) {
				remove(annotationsFlags);
			} else if(hasMetamodel()){
				getMetamodel().update(delta);
			}
		}
	}

	/**
	 * @return {@code true} if this element should be removed (ie, it does not meet the requirements to be a {@link JaxrsResourceProperty} anymore) 
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
		getParentResource().removeProperty(this);
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
	
	public Annotation getBeanParamAnnotation() {
		return getAnnotation(BEAN_PARAM);
	}
	
	@Override
	public EnumElementKind getElementKind() {
		if (getPathParamAnnotation() != null) {
			return EnumElementKind.PATH_PARAM_PROPERTY;
		} 
		if (getQueryParamAnnotation() != null) {
			return EnumElementKind.QUERY_PARAM_PROPERTY;
		}
		if (getMatrixParamAnnotation() != null) {
			return EnumElementKind.MATRIX_PARAM_PROPERTY;
		}
		if (getBeanParamAnnotation() != null) {
			return EnumElementKind.BEAN_PARAM_PROPERTY;
		}
		return EnumElementKind.UNDEFINED_RESOURCE_PROPERTY;
	}

	@Override
	public String toString() {
		return "ResourceProperty'" + getJavaElement().getParent().getElementName() + "."
			+ getJavaElement().getElementName() + "' | annotations=" + getAnnotations();
	}

}
