/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
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
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_PARAM_CONVERTER_PROVIDER_HIERARCHY;

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
import org.jboss.tools.ws.jaxrs.core.jdt.AnnotationUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.Flags;
import org.jboss.tools.ws.jaxrs.core.jdt.FlagsUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsParamConverterProvider;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;

/**
 * Contract for a provider of ParamConverter instances. 
 * 
 * @author xcoulon
 *
 */
public class JaxrsParamConverterProvider extends JaxrsJavaElement<IType> implements IJaxrsParamConverterProvider {

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
		private boolean isParamConvertProviderImpl = false;

		private Builder(final IType javaType, final CompilationUnit ast) {
			this.javaType = javaType;
			this.ast = ast;
		}

		public Builder withMetamodel(final JaxrsMetamodel metamodel) {
			this.metamodel = metamodel;
			return this;
		}

		public JaxrsParamConverterProvider build() throws CoreException {
			return build(true);
		}
		
		JaxrsParamConverterProvider build(final boolean joinMetamodel) throws CoreException {
			final long start = System.currentTimeMillis();
			try {
				if (javaType == null || !javaType.exists() || !javaType.isStructureKnown()) {
					return null;
				}
				JdtUtils.makeConsistentIfNecessary(javaType);
				final IType paramConverterProviderSupertype = JdtUtils.resolveType(JaxrsClassnames.PARAM_CONVERTER_PROVIDER,
						javaType.getJavaProject(), new NullProgressMonitor());
				this.isParamConvertProviderImpl = JdtUtils.isTypeOrSuperType(paramConverterProviderSupertype, javaType);
				this.annotations = JdtUtils.resolveAllAnnotations(javaType, ast);
				if(isParamConvertProviderImpl) {
					final JaxrsParamConverterProvider paramConverterProvider = new JaxrsParamConverterProvider(this);
					// this operation is only performed after creation
					if(joinMetamodel) {
						paramConverterProvider.joinMetamodel();
					}
					return paramConverterProvider;
				}
				return null;
			} finally {
				final long end = System.currentTimeMillis();
				Logger.tracePerf("Built JAX-RS JaxrsParamConverterProvider in {}ms", (end - start));
			}
		}
	}

	/** Flag to indicate if the associated Java type implements the required interface.*/
	private boolean isParamConvertProviderImpl = false;

	/**
	 * Full constructor.
	 * 
	 * @param builder
	 *            the fluent builder
	 * 
	 */
	private JaxrsParamConverterProvider(final Builder builder) {
		this(builder.javaType, builder.annotations, builder.isParamConvertProviderImpl, builder.metamodel, null);
	}
	
	/**
	 * Full constructor
	 * @param javaType
	 *            the underlying java type.
	 * @param annotations
	 *            the relevant annotations.
	 * @param isParamConvertProviderImpl 
	 * 		flag to indicate if the associated Java type implements the required interface
	 * @param metamodel
	 *            the metamodel or <code>null</code> if the instance is
	 *            transient.
	 * @param primaryCopy
	 *            the associated primary copy element, or {@code null} if this
	 *            instance is already the primary element
	 */
	private JaxrsParamConverterProvider(final IType javaType, final Map<String, Annotation> annotations,
			final boolean isParamConvertProviderImpl, final JaxrsMetamodel metamodel, final JaxrsParamConverterProvider primaryCopy) {
		super(javaType, annotations, metamodel, primaryCopy);
		this.isParamConvertProviderImpl = isParamConvertProviderImpl;
	}

	@Override
	public JaxrsParamConverterProvider createWorkingCopy() {
		synchronized (this) {
			return new JaxrsParamConverterProvider(getJavaElement(), AnnotationUtils.createWorkingCopies(getAnnotations()),
					isParamConvertProviderImpl(), getMetamodel(), this);
		}
	}

	@Override
	public JaxrsParamConverterProvider getWorkingCopy() {
		return (JaxrsParamConverterProvider) super.getWorkingCopy();
	}
	
	public boolean isParamConvertProviderImpl() {
		return this.isParamConvertProviderImpl;
	}
	
	/**
	 * @see org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement#getElementKind()
	 */
	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.PARAM_CONVERTER_PROVIDER;
	}

	/**
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaElement#update(org.eclipse.jdt.core.IJavaElement, org.eclipse.jdt.core.dom.CompilationUnit)
	 */
	@Override
	public void update(final IJavaElement javaElement, final CompilationUnit ast) throws CoreException {
		synchronized (this) {
			final JaxrsParamConverterProvider transientProvider = JaxrsParamConverterProvider.from(javaElement, ast).build(false);
			final Flags annotationsFlags = FlagsUtils.computeElementFlags(this);
			// clear this element if the given transient element is null
			if (transientProvider == null) {
				remove(annotationsFlags);
			} else {
				final Flags updateAnnotationsFlags = updateAnnotations(transientProvider.getAnnotations());
				final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, updateAnnotationsFlags);
				if (this.isParamConvertProviderImpl() != transientProvider.isParamConvertProviderImpl()) {
					this.isParamConvertProviderImpl = transientProvider.isParamConvertProviderImpl();
					delta.addFlag(F_PARAM_CONVERTER_PROVIDER_HIERARCHY);
				}
				if (isMarkedForRemoval()) {
					remove(annotationsFlags);
				}
				// update indexes for this element.
				else if(hasMetamodel()){
					getMetamodel().update(delta);
				}
			}
		}
	}

	/**
	 * @see org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement#isMarkedForRemoval()
	 */
	@Override
	boolean isMarkedForRemoval() {
		return !this.isParamConvertProviderImpl;
	}

}
