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

import java.util.Map;

import org.eclipse.jdt.core.IMember;
import org.jboss.tools.ws.jaxrs.core.utils.Annotation;

/**
 * Abstract class holding the parent JAX-RS Resource.
 * 
 * @author Xavier Coulon
 * 
 */
public abstract class JaxrsResourceElement<T extends IMember> extends AbstractJaxrsJavaElement<T> {

	/** The parent JAX-RS Resource for this element. */
	private final JaxrsResource parentResource;

	/**
	 * Full constructor for elements having multiple annotations.
	 * 
	 * @param javaElement
	 * @param annotations
	 * @param parentResource
	 */
	protected JaxrsResourceElement(final T javaElement, final Map<String, Annotation> annotations,
			final JaxrsResource parentResource, final JaxrsMetamodel metamodel) {
		super(javaElement, annotations, metamodel);
		this.parentResource = parentResource;
		if(this.parentResource != null) {
			this.parentResource.addElement(this);
		}
	}

	/**
	 * @return the parent JAX-RS Resource
	 */
	public JaxrsResource getParentResource() {
		return parentResource;
	}

}
