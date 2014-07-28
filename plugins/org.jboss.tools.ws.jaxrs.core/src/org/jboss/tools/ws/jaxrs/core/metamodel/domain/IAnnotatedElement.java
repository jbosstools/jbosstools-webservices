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

import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;

/**
 * An annotated element.
 * @author xcoulon
 *
 */
public interface IAnnotatedElement {
	
	/**
	 * @return the Map of {@link Annotation} for the given element, indexed by
	 *         their fully qualified java name.
	 */
	public Map<String, Annotation> getAnnotations();
	
	/**
	 * 
	 * @param annotationName the fully qualified name of the {@link Annotation} to find
	 * @return the {@link Annotation} with the given fully qualified name.
	 */
	public abstract Annotation getAnnotation(final String annotationName);

	/**
	 * 
	 * @param annotationName the fully qualified name of the {@link Annotation} to find
	 * @return {@code true} if the {@link Annotation} with the given fully qualified name exists, {@code false} otherwise.
	 */
	public abstract boolean hasAnnotation(String annotationName);

	/**
	 * @return the underlying {@link IResource} of this element.
	 */
	public abstract IResource getResource();



}
