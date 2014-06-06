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
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.COOKIE_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.DEFAULT_VALUE;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.HEADER_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.MATRIX_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.QUERY_PARAM;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.Flags;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.jboss.tools.ws.jaxrs.core.utils.Annotation;
import org.jboss.tools.ws.jaxrs.core.utils.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.utils.ParameterType;

/**
 * JAX-RS Resource Field.
 * 
 * @author xcoulon
 */
public class JaxrsResourceField extends JaxrsResourceElement<IField> implements IJaxrsResourceField {

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
	public static Builder from(final IField field, final CompilationUnit ast) {
		return new Builder(field, ast);
	}

	/**
	 * Internal Builder
	 * 
	 * @author xcoulon
	 * 
	 */
	public static class Builder {

		private final IField javaField;
		private final CompilationUnit ast;
		private Map<String, Annotation> annotations;
		private JaxrsResource parentResource;
		private JaxrsMetamodel metamodel;
		private ParameterType javaFieldType;

		private Builder(final IField javaField, final CompilationUnit ast) {
			this.javaField = javaField;
			this.ast = ast;
		}

		public Builder withParentResource(final JaxrsResource parentResource) {
			this.parentResource = parentResource;
			return this;
		}

		public Builder withMetamodel(final JaxrsMetamodel metamodel) {
			this.metamodel = metamodel;
			return this;
		}

		public JaxrsResourceField build() throws CoreException {
			return build(true);
		}
		
		JaxrsResourceField build(final boolean joinMetamodel) throws CoreException {
			final long start = System.currentTimeMillis();
			try {
				// skip if element does not exist or if it has compilation errors
				if (javaField == null || !javaField.exists() || !javaField.isStructureKnown()) {
					return null;
				}
				javaFieldType = JdtUtils.resolveFieldType(javaField, ast);
				final IType parentType = (IType) javaField.getParent();
				// lookup parent resource in metamodel
				if (parentResource == null && metamodel != null) {
					final AbstractJaxrsJavaElement<?> parentElement = (AbstractJaxrsJavaElement<?>) metamodel.findElement(parentType);
					if (parentElement != null
							&& parentElement.getElementKind().getCategory() == EnumElementCategory.RESOURCE) {
						parentResource = (JaxrsResource) parentElement;
					}
				}
				final List<String> expectedFieldAnnotations = Arrays.asList(MATRIX_PARAM,
						QUERY_PARAM, PATH_PARAM, COOKIE_PARAM,
						HEADER_PARAM);
				this.annotations = JdtUtils.resolveAllAnnotations(javaField, ast);
				final boolean matchesExpectedAnnotation = CollectionUtils.hasIntersection(expectedFieldAnnotations, annotations.keySet());
				if (matchesExpectedAnnotation) {
					final JaxrsResourceField field = new JaxrsResourceField(this);
					// this operation is only performed after creation
					if(joinMetamodel) {
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
	private final ParameterType fieldType;

	/**
	 * Full constructor.
	 * 
	 * @param builder
	 *            the fluent builder.
	 */
	private JaxrsResourceField(final Builder builder) {
		super(builder.javaField, builder.annotations, builder.parentResource, builder.metamodel);
		this.fieldType = builder.javaFieldType;
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
					final IField field = primaryType.getField(getJavaElement().getElementName());
					update(field, ast);
				}
				break;
			case IJavaElement.METHOD:
				update(from((IField) javaElement, ast).build(false));
			}
		} 
	}

	void update(final JaxrsResourceField transientField) throws CoreException {
		if (transientField == null) {
			remove();
		} else {
			final Flags upateAnnotationsFlags = updateAnnotations(transientField.getAnnotations());
			final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, upateAnnotationsFlags);
			if (upateAnnotationsFlags.hasValue(F_ELEMENT_KIND) && isMarkedForRemoval()) {
				remove();
			} else if(hasMetamodel()){
				getMetamodel().update(delta);
			}
		}
	}

	/**
	 * @return {@code true} if this element should be removed (ie, it does not meet the requirements to be a {@link JaxrsResourceField} anymore) 
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
	
	public ParameterType getType() {
		return this.fieldType;
	}

	@Override
	public String getTypeName() {
		return this.fieldType.getQualifiedName();
	}
	
	@Override
	public EnumElementKind getElementKind() {
		if (getPathParamAnnotation() != null) {
			return EnumElementKind.PATH_PARAM_FIELD;
		}
		if (getQueryParamAnnotation() != null) {
			return EnumElementKind.QUERY_PARAM_FIELD;
		}
		if (getMatrixParamAnnotation() != null) {
			return EnumElementKind.MATRIX_PARAM_FIELD;
		}
		return EnumElementKind.UNDEFINED_RESOURCE_FIELD;
	}

	@Override
	public String toString() {
		return "ResourceField '" + getJavaElement().getParent().getElementName() + "."
			+ getJavaElement().getElementName() + "' | annotations=" + getAnnotations();
	}

}
