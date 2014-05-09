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
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;

public interface IJaxrsMetamodel extends IJaxrsStatus {

	/**
	 * @return the identifier for this metamodel, based on its underlying {@link IJavaProject#getHandleIdentifier()}
	 */
	public abstract String getIdentifier();
	/**
	 * @return <code>true</code> if the JAX-RS Metamodel is being initialized, false if it has already been initialized.
	 */
	public abstract boolean isInitializing();
	
	/**
	 * Returns an unmodifiable {@link List} of all the {@link IJaxrsElement}s in this Metamodel.
	 * 
	 * @return
	 */
	public List<IJaxrsElement> getAllElements();
	
	
	/**
	 * Returns an unmodifiable {@link Collection} of all the {@link IJaxrsEndpoint}s in this Metamodel.
	 * 
	 * @return
	 */
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

	public abstract IJavaProject getJavaProject();

	/**
	 * Adds the given {@link IJaxrsEndpointChangedListener} listener to the metamodel. Has no effect if 
	 * the same listener has already been registered.
	 * 
	 * @param listener
	 */
	public abstract void addJaxrsEndpointChangedListener(IJaxrsEndpointChangedListener listener);

	public void removeListener(IJaxrsEndpointChangedListener listener);

	/**
	 * @return {@code true} if this metamodel already has (at least) one JAX-RS Application, {@code false} otherwise.
	 */
	public boolean hasApplication();
}