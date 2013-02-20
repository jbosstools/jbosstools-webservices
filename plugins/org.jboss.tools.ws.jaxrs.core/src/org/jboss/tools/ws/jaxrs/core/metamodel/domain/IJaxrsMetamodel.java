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
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;

public interface IJaxrsMetamodel {

	public abstract Collection<IJaxrsEndpoint> getAllEndpoints();

	/**
	 * Searches and returns a single JAX-RS Java-based Element matching the given
	 * {@link IJavaElement}
	 * 
	 * @param element
	 *            the Java Element
	 * @return the JAX-RS Element or null if none matches.
	 */
	public IJaxrsElement findElement(final IJavaElement javaElement);
	
	public abstract IProject getProject();

	public abstract void addListener(IJaxrsEndpointChangedListener listener);

	void removeListener(IJaxrsEndpointChangedListener listener);

}