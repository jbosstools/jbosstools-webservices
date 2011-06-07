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

package org.jboss.tools.ws.jaxrs.core.metamodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.internal.builder.JAXRSAnnotationsScanner;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;

/**
 * Container for JAX-RS HTTP ResourceMethod elements.
 * 
 * @author xcoulon
 * 
 */
public class HTTPMethods extends BaseElementContainer<HTTPMethod> {

	/**
	 * Full constructor.
	 * 
	 * @param m
	 *            the enclosing metamodel
	 */
	public HTTPMethods(final Metamodel m) {
		super(m);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.jboss.tools.ws.jaxrs.core.metamodel.IElementContainer#addFrom(org
	 * .eclipse.jdt.core.IJavaElement,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public final List<HTTPMethod> addFrom(final IJavaElement scope, final IProgressMonitor progressMonitor)
			throws CoreException {
		try {
			progressMonitor.beginTask("HTTP Methods registration", 2);
			// call the scanner
			List<IType> httpMethodTypes = null;
			httpMethodTypes = JAXRSAnnotationsScanner.findHTTPMethodTypes(scope, progressMonitor);
			progressMonitor.worked(1);
			// HTTPMethods
			List<HTTPMethod> addedHttpMethods = new ArrayList<HTTPMethod>();
			for (IType httpMethodType : httpMethodTypes) {
				try {
					// FIXME : must retrieve java errors somewhere around here
					HTTPMethod httpMethod = new HTTPMethod.Builder(httpMethodType, metamodel).build(progressMonitor);
					elements.put(httpMethodType.getFullyQualifiedName(), httpMethod);
					addedHttpMethods.add(httpMethod);
				} catch (InvalidModelElementException e) {
					Logger.warn("Type '" + httpMethodType.getFullyQualifiedName()
							+ "' is not a valid JAX-RS HTTP ResourceMethod: " + e.getMessage());
				}
			}
			progressMonitor.worked(1);
			return addedHttpMethods;
		} finally {
			progressMonitor.done();
		}
	}

	/**
	 * Returns the HTTPMethod matching the given HTTP verb.
	 * 
	 * @param httpVerb
	 *            the HTTP verb
	 * @return the httpMethod bound to the given HTTP Verb
	 */
	public final HTTPMethod getByVerb(final String httpVerb) {
		for (Entry<String, HTTPMethod> entry : elements.entrySet()) {
			HTTPMethod httpMethod = entry.getValue();
			if (httpMethod.getHttpVerb() != null && httpMethod.getHttpVerb().equals(httpVerb)) {
				return httpMethod;
			}
		}
		return null;
	}

}
