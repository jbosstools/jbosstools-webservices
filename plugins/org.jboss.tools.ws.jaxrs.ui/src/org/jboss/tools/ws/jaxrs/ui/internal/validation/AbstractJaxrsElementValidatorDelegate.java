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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsJavaElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.jdt.RangeUtils;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

/**
 * Abstract validator delegate. 
 * 
 * @author Xavier Coulon
 *
 */
public abstract class AbstractJaxrsElementValidatorDelegate<T extends JaxrsBaseElement> implements IJaxrsElementValidator<T> {
	
	/**
	 * The parameter type names that can be annotated with <code>Context</code>.
	 */
	protected final static List<String> CONTEXT_TYPE_NAMES = new ArrayList<String>(Arrays.asList(
			"javax.ws.rs.core.HttpHeaders", "javax.ws.rs.core.UriInfo", "javax.ws.rs.core.Request",
			"javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse",
			"javax.servlet.ServletConfig", "javax.servlet.ServletContext", "javax.ws.rs.core.SecurityContext"));

	protected static final Pattern alphaNumPattern = Pattern.compile("[a-zA-Z1-9]([a-zA-Z1-9]|\\.|-|_)*");

	/** The underlying marker manager.*/
	final IMarkerManager markerManager;

	public AbstractJaxrsElementValidatorDelegate(final IMarkerManager markerManager) {
		this.markerManager = markerManager;
	}

	/**
	 * Validates the given {@link IJaxrsElement} after having removed the JAX-RS Problem markers on the given element.
	 * 
	 * @param element
	 *            the JAX-RS element to validate
	 
	 * @throws CoreException
	 */
	public void validate(final T element, final CompilationUnit ast) throws CoreException {
		validate(element, ast, true);
	}
	
	/**
	 * Validates the given {@link IJaxrsElement}.
	 * 
	 * @param element
	 *            the JAX-RS element to validate
	 * @param removeMarkers
	 *            boolean to indicate if JAX-RS Problem markers related to
	 *            the given element should be removed prior to validation, or
	 *            not (assuming they were already removed).
	 * @throws CoreException
	 */
	public void validate(final T element, final CompilationUnit ast, final boolean removeMarkers) throws CoreException {
		final int previousProblemLevel = element.getMarkerSeverity();
		if(removeMarkers) {
			removeMarkers(element);
		}
		element.resetProblemLevel();
		internalValidate(element, ast);
		final int currentProblemLevel = element.getMarkerSeverity();
		if(currentProblemLevel != previousProblemLevel) {
			Logger.debug("Informing metamodel that problem level changed from {} to {}", previousProblemLevel,
					currentProblemLevel);
			((JaxrsMetamodel)element.getMetamodel()).notifyElementProblemLevelChanged(element);
		}
	}

	abstract void internalValidate(final T element, final CompilationUnit ast) throws CoreException;
	
	/**
	 * Removes the JAX-RS {@link IMarker}s on the underlying {@link IResource}
	 * in the scope of the JAX-RS element of the given element (eg, on the
	 * {@link IMethod} and not the whole {@link ICompilationUnit}) and reset its
	 * internal 'problem level' to {@code 0}.
	 * 
	 * @param element
	 *            the element on which JAX-RS problem markers should be removed.
	 * @throws CoreException
	 */
	void removeMarkers(final T element) throws CoreException {
		switch(element.getElementKind().getCategory()) {
		case RESOURCE_METHOD:
		case RESOURCE_FIELD:
			final ISourceRange sourceRange = ((JaxrsJavaElement<?>) element).getJavaElement().getSourceRange();
			final IMarker[] markers = element.getResource().findMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID, true, IResource.DEPTH_ONE);
			for(IMarker marker : markers) {
				final int markerStartPosition = marker.getAttribute(IMarker.CHAR_START, 0);
				if(RangeUtils.matches(sourceRange, markerStartPosition)) {
					marker.delete();
				}
			}
			break;
		default:
			element.getResource().deleteMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID, true, IResource.DEPTH_ONE);
		}
	}

}
