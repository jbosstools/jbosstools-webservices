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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.validation;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jboss.tools.common.validation.TempMarkerManager;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;

/**
 * Abstract validator delegate with bits of generics for better readability in subclasses
 * 
 * @author Xavier Coulon
 *
 */
public abstract class AbstractJaxrsElementValidatorDelegate<T extends Object> {
	
	private final T element;
	
	private final TempMarkerManager markerManager;
	
	public AbstractJaxrsElementValidatorDelegate(TempMarkerManager markerManager, T element) {
		this.markerManager = markerManager;
		this.element = element;
	}
	
	public abstract void validate() throws CoreException;

	public T getElement() {
		return element;
	}

	TempMarkerManager getMarkerManager() {
		return markerManager;
	}


	public IMarker addProblem(String message, String preferenceKey, String[] messageArguments, int length, int offset, IResource target) {
		Logger.debug("Reporting problem '{}' on resource '{}'", message, target.getFullPath().toString());
		return markerManager.addProblem(message, preferenceKey, messageArguments, length, offset, target);
	}

	public IMarker addProblem(String message, String preferenceKey, String[] messageArguments, int length, int offset, IResource target, int quickFixId) {
		Logger.debug("Reporting problem '{}' on resource '{}'", message, target.getFullPath().toString());
		return markerManager.addProblem(message, preferenceKey, messageArguments, length, offset, target, quickFixId);
	}
	
	public static void deleteJaxrsMarkers(final IResource resource) throws CoreException {
		if (resource == null) {
			return;
		}
		Logger.debug("Clearing JAX-RS markers for resource " + resource.getName());
		resource.deleteMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_TYPE, true, IResource.DEPTH_ONE);
	}

}
