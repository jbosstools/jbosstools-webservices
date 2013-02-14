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
import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsElement;

/**
 * Abstract validator delegate with bits of generics for better readability in subclasses
 * 
 * @author Xavier Coulon
 *
 */
public abstract class AbstractJaxrsElementValidatorDelegate<T extends IJaxrsElement> {
	
	private final TempMarkerManager markerManager;
	
	public AbstractJaxrsElementValidatorDelegate(final TempMarkerManager markerManager) {
		this.markerManager = markerManager;
	}
	
	public abstract void validate(final T element) throws CoreException;

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

}
