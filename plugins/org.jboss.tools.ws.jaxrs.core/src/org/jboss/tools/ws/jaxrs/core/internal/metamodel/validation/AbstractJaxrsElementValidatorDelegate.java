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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ISourceRange;
import org.jboss.tools.common.validation.TempMarkerManager;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;

/**
 * Abstract validator delegate with bits of generics for better readability in subclasses
 * 
 * @author Xavier Coulon
 *
 */
public abstract class AbstractJaxrsElementValidatorDelegate<T> {
	
	private final TempMarkerManager markerManager;
	
	public AbstractJaxrsElementValidatorDelegate(final TempMarkerManager markerManager) {
		this.markerManager = markerManager;
	}
	
	public abstract void validate(final T element) throws CoreException;

	TempMarkerManager getMarkerManager() {
		return markerManager;
	}

	public IMarker addProblem(final String message, final String preferenceKey, final String[] messageArguments, final JaxrsMetamodel metamodel) {
		final IProject project = metamodel.getProject();
		Logger.debug("Reporting problem '{}' on project '{}'", message, project.getName());
		final IMarker marker = markerManager.addProblem(message, preferenceKey, messageArguments, 0, 0, project);
		metamodel.setProblemLevel(marker.getAttribute(IMarker.SEVERITY, 0));
		return marker;
		
	}
	public IMarker addProblem(final String message, final String preferenceKey, final String[] messageArguments, final ISourceRange range, final JaxrsBaseElement element) {
		final IResource resource = element.getResource();
		Logger.debug("Reporting problem '{}' on resource '{}'", message, resource.getFullPath().toString());
		final IMarker marker = markerManager.addProblem(message, preferenceKey, messageArguments, range.getLength(), range.getOffset(), resource);
		element.setProblemLevel(marker.getAttribute(IMarker.SEVERITY, 0));
		return marker;
	}

	public IMarker addProblem(final String message, final String preferenceKey, final String[] messageArguments, final ISourceRange range, final JaxrsBaseElement element, final int quickFixId) {
		final IResource resource = element.getResource();
		Logger.debug("Reporting problem '{}' on resource '{}'", message, resource.getFullPath().toString());
		final IMarker marker = markerManager.addProblem(message, preferenceKey, messageArguments, range.getLength(), range.getOffset(), resource, quickFixId);
		element.setProblemLevel(marker.getAttribute(IMarker.SEVERITY, 0));
		return marker;
	}

}
