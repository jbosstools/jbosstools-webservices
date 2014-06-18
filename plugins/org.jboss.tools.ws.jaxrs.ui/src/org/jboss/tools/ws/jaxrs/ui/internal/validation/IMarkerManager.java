/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.ui.internal.validation;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ISourceRange;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;

/**
 * Interface for JAX-RS Marker generation
 * 
 * @author Xavier Coulon
 *
 */
public interface IMarkerManager {

	/**
	 * Adds an {@link IMarker} on the given {@link JaxrsMetamodel}
	 * @param metamodel the metamodel that whose underlying project will receive the created marker
	 * @param message the marker message
	 * @param messageArguments the message arguments
	 * @param preferenceKey the preference key, to configure the marker severity
	 * @return the created marker
	 * @throws CoreException 
	 */
	public abstract IMarker addMarker(final JaxrsMetamodel metamodel, final String message, final String[] messageArguments,
			final String preferenceKey) throws CoreException;

	/**
	 * Adds an {@link IMarker} on the given {@link JaxrsBaseElement}
	 * @param element the JAX-RS element that whose underlying resource will receive the created marker
	 * @param message the marker message
	 * @param messageArguments the message arguments
	 * @param preferenceKey the preference key, to configure the marker severity
	 * @return the created marker
	 * @throws CoreException 
	 */
	public abstract IMarker addMarker(final JaxrsBaseElement element, final ISourceRange range, final String message,
			final String[] messageArguments, final String preferenceKey) throws CoreException;
	
	/**
	 * Adds an {@link IMarker} on the given {@link JaxrsBaseElement}
	 * @param element the JAX-RS element that whose underlying resource will receive the created marker
	 * @param message the marker message
	 * @param messageArguments the message arguments
	 * @param preferenceKey the preference key, to configure the marker severity
	 * @param quickFixId the id of the quickfix that should be proposed to the user
	 * @return the created marker
	 * @throws CoreException 
	 */
	public abstract IMarker addMarker(final JaxrsBaseElement element, final ISourceRange range, final String message,
			final String[] messageArguments, final String preferenceKey, final int quickFixId) throws CoreException;

}