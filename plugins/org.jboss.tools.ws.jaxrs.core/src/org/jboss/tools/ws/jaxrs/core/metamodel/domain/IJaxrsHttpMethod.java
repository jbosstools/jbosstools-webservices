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

import org.eclipse.jdt.core.IType;

public interface IJaxrsHttpMethod extends IJaxrsElement, Comparable<IJaxrsHttpMethod> {
	/** @return the httpVerb */
	String getHttpVerb();

	/**
	 * @return {@code true} if this HTTP Method is 'built-in' (ie, part of the
	 *         mandatory HTTP Methods that a certified JAX-RS implementation
	 *         must provide: {@code GET}, {@code PUT}, {@code POST},
	 *         {@code DELETE}, {@code HEAD}, {@code OPTIONS}), {@code false}
	 *         otherwise .
	 */
	boolean isBuiltIn();

	/**
	 * @return the fully qualified name of the underlying {@link IType} of this
	 *         JAX-RS element
	 */
	public String getJavaClassName();
}