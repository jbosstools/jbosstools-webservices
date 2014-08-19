/******************************************************************************* 
 * Copyright (c) 2011 - 2014 Red Hat, Inc. and others. 
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
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.SourceType;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IAnnotatedSourceType;

public abstract class JaxrsResourceElement<T extends IMember> extends JaxrsJavaElement<T> implements IAnnotatedSourceType {

	/** The underlying field type. */
	protected final SourceType javaType;
	/** The parent JAX-RS Resource for this element. */
	protected final JaxrsResource parentResource;

	/**
	 * Constructor
	 * @param element
	 * @param annotations
	 * @param metamodel
	 * @param javaType
	 * @param parentResource
	 * @param primaryCopy
	 *            the associated primary copy element, or {@code null} if this
	 *            instance is already the primary element
	 */
	JaxrsResourceElement(final T element, final Map<String, Annotation> annotations, final JaxrsMetamodel metamodel,
			final SourceType javaType, final JaxrsResource parentResource, final JaxrsResourceElement<T> primaryCopy) {
		super(element, annotations, metamodel, primaryCopy);
		this.javaType = javaType;
		this.parentResource = parentResource;

	}

	/**
	 * @return the parent JAX-RS Resource
	 */
	public JaxrsResource getParentResource() {
		return parentResource;
	}

	public SourceType getType() {
		return this.javaType;
	}

}