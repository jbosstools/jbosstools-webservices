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
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.APPLICATION_PATH;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_APPLICATION_HIERARCHY;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_APPLICATION_PATH_VALUE_OVERRIDE;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.Flags;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;

/**
 * This domain element describes a subtype of {@link jvax.ws.rs.Application}
 * annotated with {@link jvax.ws.rs.ApplicationPath}.
 * 
 * @author xcoulon
 */
public class JaxrsJavaApplication extends JaxrsJavaElement<IType> implements IJaxrsApplication {

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
		private Map<String, Annotation> annotations;
		private JaxrsMetamodel metamodel = null;
		private boolean isApplicationSubclass = false;

		private Builder(final IType javaType, final CompilationUnit ast) {
			this.javaType = javaType;
			this.ast = ast;
		}

		public Builder withMetamodel(final JaxrsMetamodel metamodel) {
			this.metamodel = metamodel;
			return this;
		}

		public JaxrsJavaApplication build() throws CoreException {
			if (javaType == null || !javaType.exists()) {
				return null;
			}
			final CompilationUnit compilationUnit = ast != null ? ast : JdtUtils.parse(javaType,
					new NullProgressMonitor());
			final IType applicationSupertype = JdtUtils.resolveType(EnumJaxrsClassname.APPLICATION.qualifiedName,
					javaType.getJavaProject(), new NullProgressMonitor());
			final Annotation applicationPathAnnotation = JdtUtils.resolveAnnotation(javaType, compilationUnit,
					APPLICATION_PATH.qualifiedName);
			isApplicationSubclass = JdtUtils.isTypeOrSuperType(applicationSupertype, javaType);
			if (isApplicationSubclass || applicationPathAnnotation != null) {
				this.annotations = singleToMap(applicationPathAnnotation);
				final JaxrsJavaApplication application = new JaxrsJavaApplication(this);
				// this operation is only performed after creation
				application.joinMetamodel();
				return application;
			}
			return null;
		}

	}

	/**
	 * Indicates whether the underlying Java type is a subclass of
	 * <code>javax.ws.rs.core.Application</code>.
	 */
	private boolean isApplicationSubclass = false;

	/**
	 * The ApplicationPath overriden value that can be configured in the
	 * web.xml.
	 */
	private String applicationPathOverride = null;

	/**
	 * Full constructor.
	 * 
	 * @param builder
	 *            the fluent builder
	 */
	public JaxrsJavaApplication(final Builder builder) {
		super(builder.javaType, builder.annotations, builder.metamodel);
		this.isApplicationSubclass = builder.isApplicationSubclass;
		if (hasMetamodel()) {
			final JaxrsWebxmlApplication webxmlApplication = getMetamodel().findWebxmlApplicationByClassName(
					getJavaClassName());
			if (webxmlApplication != null) {
				this.applicationPathOverride = webxmlApplication.getApplicationPath();
			}
		}
	}

	@Override
	public boolean isMarkedForRemoval() {
		// element should be removed if it's not an application subclass and it
		// has no ApplicationPath annotation
		return !(isApplicationSubclass || getApplicationPathAnnotation() != null);
	}

	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.APPLICATION_JAVA;
	}

	@Override
	public boolean isWebXmlApplication() {
		return false;
	}

	@Override
	public boolean isJavaApplication() {
		return true;
	}

	public boolean isJaxrsCoreApplicationSubclass() {
		return isApplicationSubclass;
	}

	public void setJaxrsCoreApplicationSubclass(final boolean isApplicationSubclass) {
		this.isApplicationSubclass = isApplicationSubclass;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.internal.metamodel.IHttpMethod#getSimpleName
	 * ()
	 */
	@Override
	public String getJavaClassName() {
		return getJavaElement().getFullyQualifiedName();
	}

	/**
	 * Sets the ApplicationPath override that can be configured from web.xml
	 * 
	 * @param applicationPathOverride
	 *            the override value
	 * @throws CoreException
	 */
	public void setApplicationPathOverride(final String applicationPathOverride) throws CoreException {
		Logger.debug("Override @ApplicationPath value with '{}'", applicationPathOverride);
		this.applicationPathOverride = applicationPathOverride;
		if (hasMetamodel()) {
			getMetamodel().update(new JaxrsElementDelta(this, CHANGED, F_APPLICATION_PATH_VALUE_OVERRIDE));
		}
	}

	/**
	 * Unsets the ApplicationPath override that can be configured from web.xml
	 * 
	 * @throws CoreException
	 */
	public void unsetApplicationPathOverride() throws CoreException {
		Logger.debug("Unoverriding @ApplicationPath value");
		this.applicationPathOverride = null;
		if (hasMetamodel()) {
			getMetamodel().update(new JaxrsElementDelta(this, CHANGED, F_APPLICATION_PATH_VALUE_OVERRIDE));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String getApplicationPath() {
		if (applicationPathOverride != null) {
			return applicationPathOverride;
		}
		final Annotation applicationPathAnnotation = getApplicationPathAnnotation();
		if (applicationPathAnnotation != null) {
			return applicationPathAnnotation.getValue();
		}
		return null;
	}

	/**
	 * @return the
	 *         <code>javax.ws.rs.ApplicationPath<code> annotation set on the underlying javatype, or null if none exists.
	 */
	public Annotation getApplicationPathAnnotation() {
		return getAnnotation(APPLICATION_PATH.qualifiedName);
	}

	public boolean isOverriden() {
		if (getMetamodel() != null) {
			return (getMetamodel().findWebxmlApplicationByClassName(this.getJavaClassName()) != null);
		}
		return false;
	}

	/**
	 * Updates the current {@link JaxrsJavaApplication} from the given
	 * {@link IJavaElement} If the given transientApplication is null, this
	 * element will be removed.
	 * 
	 * @param element
	 * @param ast
	 * @return
	 * @throws CoreException
	 */
	public void update(final IJavaElement javaElement, final CompilationUnit ast) throws CoreException {
		final JaxrsJavaApplication transientApplication = from(javaElement, ast).build();
		if (transientApplication == null) {
			remove();
		} else {
			final Flags updateAnnotationsFlag = updateAnnotations(transientApplication.getAnnotations());
			final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, updateAnnotationsFlag);
			if (this.isJaxrsCoreApplicationSubclass() != transientApplication.isJaxrsCoreApplicationSubclass()) {
				this.isApplicationSubclass = transientApplication.isJaxrsCoreApplicationSubclass();
				delta.addFlag(F_APPLICATION_HIERARCHY);
			}
			if (isMarkedForRemoval()) {
				remove();
			}
			// update indexes for this element.
			else if(hasMetamodel()){
				getMetamodel().update(delta);
			}
		}
	}

	@Override
	public String toString() {
		return ("JavaApplication '" + getJavaElement().getElementName() + "': path=" + getApplicationPath());
	}

}
