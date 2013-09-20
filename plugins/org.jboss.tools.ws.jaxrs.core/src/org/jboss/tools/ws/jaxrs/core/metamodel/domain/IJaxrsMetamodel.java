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
package org.jboss.tools.ws.jaxrs.core.metamodel.domain;

import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;

public interface IJaxrsMetamodel extends IJaxrsStatus {

	/**
	 * @return <code>true</code> if the JAX-RS Metamodel is being initialized, false if it has already been initialized.
	 */
	public abstract boolean isInitializing();
	
	public abstract Collection<IJaxrsEndpoint> getAllEndpoints();

	/**
	 * Searches and returns a single JAX-RS Java-based Element matching the given
	 * {@link IJavaElement}
	 * 
	 * @param element
	 *            the Java Element
	 * @return the JAX-RS Element or null if none matches.
	 */
	public abstract IJaxrsStatus findElement(final IJavaElement javaElement);
	
	public abstract IProject getProject();

	/**
	 * Adds the given {@link IJaxrsEndpointChangedListener} listener to the metamodel. Has no effect if 
	 * the same listener has already been registered.
	 * 
	 * @param listener
	 */
	public abstract void addListener(IJaxrsEndpointChangedListener listener);

	public void removeListener(IJaxrsEndpointChangedListener listener);

}