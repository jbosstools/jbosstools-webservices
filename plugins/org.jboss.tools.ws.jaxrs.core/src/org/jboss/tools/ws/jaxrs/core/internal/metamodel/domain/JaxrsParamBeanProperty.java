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

import org.eclipse.jdt.core.IMethod;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsParamBeanProperty;

/**
 * @author xcoulon
 * 
 */
public class JaxrsParamBeanProperty extends JaxrsJavaElement<IMethod> implements IJaxrsParamBeanProperty {

	public JaxrsParamBeanProperty(IMethod element, Annotation annotation, JaxrsMetamodel metamodel) {
		super(element, singleToMap(annotation), metamodel);
	}

	@Override
	public EnumElementCategory getElementCategory() {
		return EnumElementCategory.RESOURCE_FIELD;
	}

	@Override
	public boolean isMarkedForRemoval() {
		// TODO: implement
		return false;
	}

	@Override
	public EnumElementKind getElementKind() {
		return null;
	}

}
