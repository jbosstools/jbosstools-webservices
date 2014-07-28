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

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
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
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsParameterAggregatorField;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;

/**
 * JAX-RS Parameter Aggregator Field.
 * 
 * @author xcoulon
 */
public class JaxrsParameterAggregatorField extends JaxrsParameterAggregatorElement<IField> implements IJaxrsParameterAggregatorField {

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
		private JaxrsParameterAggregator parentParameterAggregator;
		private JaxrsMetamodel metamodel;
		private SourceType javaFieldType;

		private Builder(final IField javaField, final CompilationUnit ast) {
			this.javaField = javaField;
			this.ast = ast;
		}

		public Builder withAnnotations(final Map<String, Annotation> annotations) {
			this.annotations = annotations;
			return this;
		}
		
		public JaxrsParameterAggregatorField buildTransient() throws CoreException {
			return buildInParentAggregator(null);
		}
		
		JaxrsParameterAggregatorField buildInParentAggregator(final JaxrsParameterAggregator parentParameterAggregator) throws CoreException {
			final long start = System.currentTimeMillis();
			try {
				// skip if element does not exist or if it has compilation errors
				if (javaField == null || !javaField.exists() || !javaField.isStructureKnown()) {
					return null;
				}
				this.parentParameterAggregator = parentParameterAggregator;
				if(this.parentParameterAggregator != null) {
					this.metamodel = this.parentParameterAggregator.getMetamodel();
				}
				javaFieldType = JdtUtils.resolveFieldType(javaField, ast);
				final IType parentType = (IType) javaField.getParent();
				// lookup parent resource in metamodel
				if (parentParameterAggregator == null && metamodel != null) {
					Logger.trace("Skipping {}.{} because parent Parameter Aggregator does not exist", parentType.getFullyQualifiedName(), javaField.getElementName());
				}
				if(this.annotations == null) {
					this.annotations = JdtUtils.resolveAllAnnotations(javaField, ast);
				}
				if (JaxrsParamAnnotations.matchesAtLeastOne(annotations.keySet())) {
					final JaxrsParameterAggregatorField field = new JaxrsParameterAggregatorField(this);
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
	private JaxrsParameterAggregatorField(final Builder builder) {
		this(builder.javaField, builder.annotations, builder.metamodel, builder.javaFieldType,
				builder.parentParameterAggregator, null);
	}
	
	/**
	 * Full constructor.
	 * 
	 * @param javaField
	 *            the java field
	 * @param annotations
	 *            the java element annotations (or null)
	 * @param metamodel
	 *            the metamodel in which this element exist, or null if this
	 *            element is transient.
	 * @param javaFieldType the {@link SourceType} associated with the given javaField
	 * @param parentParameterAggregator
	 * 			the parent element
	 * @param primaryCopy
	 *            the associated primary copy element, or {@code null} if this
	 *            instance is already the primary element
	 */
	private JaxrsParameterAggregatorField(final IField javaField, final Map<String, Annotation> annotations,
			final JaxrsMetamodel metamodel, final SourceType javaFieldType,
			final JaxrsParameterAggregator parentParameterAggregator, final JaxrsParameterAggregatorField primaryCopy) {
		super(javaField, annotations, metamodel, javaFieldType, parentParameterAggregator, primaryCopy);
		if(getParentParameterAggregator() != null) {
			getParentParameterAggregator().addElement(this);
		}
	}

	@Override
	public JaxrsParameterAggregatorField createWorkingCopy() {
		synchronized (this) {
			final JaxrsParameterAggregator parentWorkingCopy = getParentParameterAggregator().getWorkingCopy();
			return parentWorkingCopy.getFields().get(this.javaElement.getHandleIdentifier());
		}
	}
	
	protected JaxrsParameterAggregatorField createWorkingCopy(final JaxrsParameterAggregator parentWorkingCopy) {
		return new JaxrsParameterAggregatorField(getJavaElement(),
				AnnotationUtils.createWorkingCopies(getAnnotations()), getMetamodel(), getType().createWorkingCopy(),
				parentWorkingCopy, this);
	}
	
	@Override
	public JaxrsParameterAggregatorField getWorkingCopy() {
		return (JaxrsParameterAggregatorField) super.getWorkingCopy();
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
			case IJavaElement.FIELD:
				update(from((IField) javaElement, ast).buildTransient());
			}
		} 
	}

	void update(final JaxrsParameterAggregatorField transientField) throws CoreException {
		synchronized (this) {
			final Flags annotationsFlags = FlagsUtils.computeElementFlags(this);
			if (transientField == null) {
				remove(annotationsFlags);
			} else {
				final Flags updateAnnotationsFlags = updateAnnotations(transientField.getAnnotations());
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
	 * @return {@code true} if this element should be removed (ie, it does not meet the requirements to be a {@link JaxrsParameterAggregatorField} anymore) 
	 */
	@Override
	boolean isMarkedForRemoval() {
		for(String annotationName : JaxrsParamAnnotations.PARAM_ANNOTATIONS) {
			if(hasAnnotation(annotationName)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Remove {@code this} from the parent {@link IJaxrsResource} before calling {@code super.remove()} which deals with removal from the {@link JaxrsMetamodel}. 
	 */
	@Override
	public void remove(final Flags flags) throws CoreException {
		getParentParameterAggregator().removeField(this);
		super.remove(flags);
	}

	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.PARAMETER_AGGREGATOR_FIELD;
	}

	@Override
	public String toString() {
		return "ResourceField '" + getJavaElement().getParent().getElementName() + "."
			+ getJavaElement().getElementName() + "' (" + getType().getDisplayableTypeName() + ") | annotations=" + getAnnotations();
	}

}
