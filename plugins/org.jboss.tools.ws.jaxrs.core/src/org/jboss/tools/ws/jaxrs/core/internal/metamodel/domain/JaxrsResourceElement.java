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

import java.util.List;

import org.eclipse.jdt.core.IMember;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;

/**
 * @author Xavier Coulon
 * 
 */
public abstract class JaxrsResourceElement<T extends IMember> extends JaxrsElement<T> {

	private final JaxrsResource parentResource;

	public JaxrsResourceElement(T javaElement, List<Annotation> annotations, JaxrsResource parentResource,
			JaxrsMetamodel metamodel) {
		super(javaElement, annotations, metamodel);
		this.parentResource = parentResource;
		this.parentResource.addElement(this);
	}

	public JaxrsResourceElement(T javaElement, Annotation annotation, JaxrsResource parentResource,
			JaxrsMetamodel metamodel) {
		super(javaElement, annotation, metamodel);
		this.parentResource = parentResource;
		this.parentResource.addElement(this);
	}

	public JaxrsResource getParentResource() {
		return parentResource;
	}

}
