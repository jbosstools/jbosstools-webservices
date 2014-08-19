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
package org.jboss.tools.ws.jaxrs.core.metamodel.domain;

import org.eclipse.core.resources.IResource;

/**
 * Base interface for all JAX-RS Elements.
 * 
 * @author Xavier Coulon
 * 
 */
public interface IJaxrsElement extends IJaxrsStatus {

	public abstract IJaxrsMetamodel getMetamodel();

	public abstract EnumElementKind getElementKind();

	/**
	 * The unique identifier for the JAX-RS Element is:
	 * <ul>
	 * <li>the underlying Java Element handleIdentifier if the JAX-RS Element is
	 * based on a Java Element</li>
	 * <li>the underlying resource full path if the JAX-RS Element is not
	 * based on a Java Element</li>
	 * </ul>
	 * </p>
	 * <p>
	 * This identifier should remain the same for most of the case, otherwise it
	 * means that the element has been removed and re-created (ex: renaming a
	 * Java Method results in removing the old one and creating a new one).
	 * </p>
	 * 
	 * @return the unique identifier for the JAX-RS Element.
	 */
	public abstract String getIdentifier();

	public abstract IResource getResource();

	public abstract String getName();

	public abstract boolean isBinary();

}
