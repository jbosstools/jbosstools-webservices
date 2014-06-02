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
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.AbstractJaxrsBaseElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.ui.internal.utils.Logger;

/**
 * Abstract validator delegate. 
 * 
 * @author Xavier Coulon
 *
 */
public abstract class AbstractJaxrsElementValidatorDelegate<T extends AbstractJaxrsBaseElement> implements IJaxrsElementValidator<T> {
	
	/**
	 * The parameter type names that can be annotated with <code>Context</code>.
	 */
	protected final static List<String> CONTEXT_TYPE_NAMES = new ArrayList<String>(Arrays.asList(
			"javax.ws.rs.core.HttpHeaders", "javax.ws.rs.core.UriInfo", "javax.ws.rs.core.Request",
			"javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse",
			"javax.servlet.ServletConfig", "javax.servlet.ServletContext", "javax.ws.rs.core.SecurityContext"));

	protected static final Pattern alphaNumPattern = Pattern.compile("[a-zA-Z1-9]([a-zA-Z1-9]|\\.|-|_)*");

	/**
	 * Validates the given {@link IJaxrsElement}.
	 * @param element the JAX-RS element to validate
	 * @throws CoreException
	 */
	public void validate(final T element) throws CoreException {
		final int previousProblemLevel = element.getProblemLevel();
		internalValidate(element);
		final int currentProblemLevel = element.getProblemLevel();
		if(currentProblemLevel != previousProblemLevel) {
			Logger.debug("Informing metamodel that problem level changed from {} to {}", previousProblemLevel,
					currentProblemLevel);
			((JaxrsMetamodel)element.getMetamodel()).notifyElementProblemLevelChanged(element);
		}
	}

	abstract void internalValidate(final T element) throws CoreException;
	
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
		element.resetProblemLevel();
		element.getResource().deleteMarkers(JaxrsMetamodelValidator.JAXRS_PROBLEM_MARKER_ID, true, IResource.DEPTH_ONE);
	}

}
