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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IResource;
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
public class HTTPMethods implements Iterable<Entry<String, HTTPMethod>> {

	/**
	 * The available HTTP Methods including the 5 standard ones (@GET, @POST,
	 * etc.) and the application custom extensions (for WebDAV operations
	 * support, etc.): annotations annotated with <code>@HTTPMethod</code>. The resourceMethods are
	 * indexed by their associated java type fully qualified name.
	 */
	private final Map<String, HTTPMethod> httpMethods = new HashMap<String, HTTPMethod>();

	/** the enclosing metamodel. */
	private final Metamodel metamodel;

	/**
	 * Full constructor.
	 * @param m the enclosing metamodel
	 */
	public HTTPMethods(final Metamodel m) {
		this.metamodel = m;
	}

	/**
	 * Registers all discovered HTTP Methods in the given scope.
	 * 
	 * @param scope the scope from which HTTPMethods should be added
	 * @param progressMonitor the progress monitor
	 * @throws CoreException in case of underlying exception
	 */
	public final void addFrom(final IJavaElement scope, final IProgressMonitor progressMonitor) throws CoreException {
		try {
			progressMonitor.beginTask("HTTP Methods registration", 2);
			// call the scanner
			List<IType> httpMethodTypes = null;
			httpMethodTypes = JAXRSAnnotationsScanner.findHTTPMethodTypes(scope, progressMonitor);
			progressMonitor.worked(1);
			// HTTPMethods
			for (IType httpMethodType : httpMethodTypes) {
				try {
					// FIXME : must retrieve java errors somewhere around here
					this.httpMethods.put(httpMethodType.getFullyQualifiedName(), new HTTPMethod(httpMethodType,
							metamodel, progressMonitor));
				} catch (InvalidModelElementException e) {
					Logger.warn("Type '" + httpMethodType.getFullyQualifiedName()
							+ "' is not a valid JAX-RS HTTP ResourceMethod: " + e.getMessage());
				}
			}
			progressMonitor.worked(1);
		} finally {
			progressMonitor.done();
		}
	}

	/**
	 * Removes the JAX-RS HTTPMethod associated with the given removed resource.
	 * @param removedResource the removed/deleted underlying resource
	 * @param progressMonitor the progress monitor
	 */
	public final void removeElement(final IResource removedResource, final IProgressMonitor progressMonitor) {
		for (Iterator<HTTPMethod> iterator = httpMethods.values().iterator(); iterator.hasNext();) {
			HTTPMethod element = iterator.next();
			if (removedResource.equals(element.getJavaElement().getResource())) {
				iterator.remove();
				return;
			}
		}
	}

	/**
	 * Returns true if an HTTPMethod is based on the given java type.
	 * @param type the underlying java type
	 * @return true of false ;-)
	 */
	public final boolean contains(final IType type) {
		return httpMethods.containsKey(type.getFullyQualifiedName());
	}

	/**
	 * Returns the HTTPMethod based on the given java type.
	 * @param type the underlying java type
	 * @return the matching HTTPMethod
	 */
	public final HTTPMethod getByType(final IType type) {
		return httpMethods.get(type.getFullyQualifiedName());
	}

	/**
	 * Returns the HTTPMethod based on the given java type.
	 * @param typeName the underlying java type fully qualified name
	 * @return the matching HTTPMethod
	 */
	public final HTTPMethod getByTypeName(final String typeName) {
		return httpMethods.get(typeName);
	}

	/**
	 * @return the httpMethods
	 */
	public final List<HTTPMethod> getAll() {
		return Collections.unmodifiableList(new ArrayList<HTTPMethod>(httpMethods.values()));
	}

	/**
	 * @return the list of HTTPMethod types
	 */
	public final Set<String> getTypeNames() {
		return httpMethods.keySet();
	}

	/**
	 * Returns the HTTPMethod matching the given HTTP verb.
	 * @param httpVerb the HTTP verb
	 * @return the httpMethod bound to the given HTTP Verb
	 */
	public final HTTPMethod getByVerb(final String httpVerb) {
		for (Entry<String, HTTPMethod> entry : httpMethods.entrySet()) {
			HTTPMethod httpMethod = entry.getValue();
			if (httpMethod.getHttpVerb() != null && httpMethod.getHttpVerb().equals(httpVerb)) {
				return httpMethod;
			}
		}
		return null;
	}

	/**
	 * @return the number of HTTP Methods in the metamodel.
	 */
	public final Object size() {
		return httpMethods.size();
	}

	@Override
	public final Iterator<Entry<String, HTTPMethod>> iterator() {
		return httpMethods.entrySet().iterator();
	}

	/**
	 * Resets the HTTPMethods list
	 */
	public void reset() {
		this.httpMethods.clear();
	}

}
