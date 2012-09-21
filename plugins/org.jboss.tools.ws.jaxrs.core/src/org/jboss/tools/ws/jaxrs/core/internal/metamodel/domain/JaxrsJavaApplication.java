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

import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementDelta.*;
import static org.jboss.tools.ws.jaxrs.core.jdt.EnumJaxrsClassname.APPLICATION_PATH;

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
	 * 
	 * @param javaType
	 * @param applicationPathAnnocation
	 * @param metamodel
	 */
	public JaxrsJavaApplication(final IType javaType, final Annotation applicationPathAnnocation, final boolean isApplicationSubclass, final JaxrsMetamodel metamodel) {
		super(javaType, applicationPathAnnocation, metamodel);
		this.isApplicationSubclass = isApplicationSubclass;
	}

	@Override
	public EnumElementCategory getElementCategory() {
		return EnumElementCategory.APPLICATION;
	}
	
	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.APPLICATION_JAVA;
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
		final Annotation applicationPathAnnotation = getAnnotation(APPLICATION_PATH.qualifiedName);
		if (applicationPathAnnotation != null) {
			return applicationPathAnnotation.getValue("value");
		}
		return null;
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
	public int update(JaxrsJavaApplication application) {
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
			flags += F_APPLICATION_PATH_VALUE;
		}
		
		if(this.isJaxrsCoreApplicationSubclass() != application.isJaxrsCoreApplicationSubclass()) {
			flags += F_APPLICATION_HIERARCHY;
		}
		
		return flags;
	}
	
	@Override
	public String toString() {
		return ("JavaApplication '" + getJavaElement().getElementName() + "': " + getApplicationPath());
	}

}
