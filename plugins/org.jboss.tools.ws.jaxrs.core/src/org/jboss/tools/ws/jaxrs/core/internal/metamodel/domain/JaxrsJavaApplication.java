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

import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_APPLICATION_HIERARCHY;
import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.F_APPLICATION_PATH_ANNOTATION;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.APPLICATION_PATH;

import java.util.Map;

import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsApplication;

/**
 * This domain element describes a subtype of {@link jvax.ws.rs.Application} annotated with
 * {@link jvax.ws.rs.ApplicationPath}.
 * 
 * @author xcoulon
 */
public class JaxrsJavaApplication extends JaxrsJavaElement<IType> implements IJaxrsApplication {

	/** Indicates whether the underlying Java type is a subclass of <code>javax.ws.rs.core.Application</code>. */
	private boolean isApplicationSubclass;
	
	/** The ApplicationPath overriden value that can be configured in the web.xml. */
	private String applicationPathOverride = null;
	
	/**
	 * Full constructor.
	 */
	public JaxrsJavaApplication(final IType javaType, final Map<String, Annotation> annotations, final boolean isApplicationSubclass, final JaxrsMetamodel metamodel) {
		super(javaType, annotations, metamodel);
		this.isApplicationSubclass = isApplicationSubclass;
	}

	/**
	 * Full constructor.
	 */
	public JaxrsJavaApplication(final IType javaType, Annotation applicationPathAnnotation, final boolean isApplicationSubclass, final JaxrsMetamodel metamodel) {
		super(javaType, singleToMap(applicationPathAnnotation), metamodel);
		this.isApplicationSubclass = isApplicationSubclass;
	}
	
	@Override
	public EnumElementCategory getElementCategory() {
		return EnumElementCategory.APPLICATION;
	}
	
	@Override
	public boolean isMarkedForRemoval() {
		// element should be removed if it's not an application subclass and it has no ApplicationPath annotation
		return !(isApplicationSubclass || getApplicationPathAnnotation() != null);
	}

	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.UNDEFINED;
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
	 * @param applicationPathOverride the override value
	 */
	public void setApplicationPathOverride(final String applicationPathOverride) {
		Logger.debug("Override @ApplicationPath value with '{}'", applicationPathOverride);
		this.applicationPathOverride = applicationPathOverride;
	}

	/**
	 * Unsets the ApplicationPath override that can be configured from web.xml
	 */
	public void unsetApplicationPathOverride() {
		Logger.debug("Unoverriding @ApplicationPath value");
		this.applicationPathOverride = null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getApplicationPath() {
		if(applicationPathOverride != null) {
			return applicationPathOverride;
		}
		final Annotation applicationPathAnnotation = getApplicationPathAnnotation();
		if (applicationPathAnnotation != null) {
			return applicationPathAnnotation.getValue("value");
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
		return (metamodel.getWebxmlApplication(this.getJavaClassName()) != null);
	}

	/**
	 * Update this Application with the elements of the given Application
	 * 
	 * @param application
	 * @return the flags indicating the kind of changes that occurred during the
	 *         update.
	 */
	public int update(final JaxrsJavaApplication application) {
		int flags = 0;
		final Annotation annotation = this.getAnnotation(APPLICATION_PATH.qualifiedName);
		final Annotation otherAnnotation = application.getAnnotation(APPLICATION_PATH.qualifiedName);
		// handle case where annotation is added 
		if (annotation == null && otherAnnotation != null) {
			flags += addOrUpdateAnnotation(annotation);
		}
		// handle case where annotation is removed 
		else if(annotation != null && otherAnnotation == null) {
			flags += removeAnnotation(annotation);
		} 
		// handle case where annotation is changed 
		else if(annotation != null && otherAnnotation != null
						&& annotation.update(otherAnnotation)) {
			flags += F_APPLICATION_PATH_ANNOTATION;
		}
		
		if(this.isJaxrsCoreApplicationSubclass() != application.isJaxrsCoreApplicationSubclass()) {
			this.isApplicationSubclass = application.isJaxrsCoreApplicationSubclass();
			flags += F_APPLICATION_HIERARCHY;
		}
		return flags;
	}

	@Override
	public String toString() {
		return ("JavaApplication '" + getJavaElement().getElementName() + "': path=" + getApplicationPath());
	}


}
