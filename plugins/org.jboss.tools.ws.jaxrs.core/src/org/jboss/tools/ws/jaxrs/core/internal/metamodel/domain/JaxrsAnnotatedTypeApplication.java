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

import static org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder.JaxrsElementChangedEvent.F_APPLICATION_PATH_VALUE;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.ApplicationPath;

import org.eclipse.jdt.core.IType;
import org.eclipse.wst.validation.ValidatorMessage;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumKind;

/**
 * This domain element describes a subtype of {@link jvax.ws.rs.Application} annotated with
 * {@link jvax.ws.rs.ApplicationPath}.
 * 
 * @author xcoulon
 */
public class JaxrsAnnotatedTypeApplication extends JaxrsApplication {

	/**
	 * Full constructor.
	 * 
	 * @param javaType
	 * @param applicationPathAnnocation
	 * @param metamodel
	 */
	public JaxrsAnnotatedTypeApplication(IType javaType, Annotation applicationPathAnnocation, JaxrsMetamodel metamodel) {
		super(javaType, applicationPathAnnocation, metamodel);
	}

	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.APPLICATION;
	}

	@Override
	public EnumKind getKind() {
		if (getAnnotation(ApplicationPath.class.getName()) != null) {
			return EnumKind.APPLICATION;
		}
		return EnumKind.UNDEFINED;
	}

	@Override
	public List<ValidatorMessage> validate() {
		List<ValidatorMessage> messages = new ArrayList<ValidatorMessage>();
		return messages;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getApplicationPath() {
		final Annotation applicationPathAnnotation = getAnnotation(ApplicationPath.class.getName());
		if (applicationPathAnnotation != null) {
			return applicationPathAnnotation.getValue("value");
		}
		return null;
	}
	
	/**
	 * Update this Application with the elements of the given Application
	 * 
	 * @param application
	 * @return the flags indicating the kind of changes that occurred during the
	 *         update.
	 */
	@Override
	public int update(JaxrsApplication application) {
		int flags = 0;
		final Annotation annotation = this.getAnnotation(ApplicationPath.class.getName());
		final Annotation otherAnnotation = application.getAnnotation(ApplicationPath.class.getName());
		if (annotation != null && otherAnnotation != null && !annotation.equals(otherAnnotation)
				&& annotation.update(otherAnnotation)) {
			flags += F_APPLICATION_PATH_VALUE;
		}
		return flags;
	}


}
