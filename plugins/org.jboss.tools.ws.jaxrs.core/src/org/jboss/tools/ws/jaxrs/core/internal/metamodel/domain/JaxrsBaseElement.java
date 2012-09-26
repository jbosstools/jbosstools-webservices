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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import org.jboss.tools.ws.jaxrs.core.metamodel.IJaxrsElement;

public abstract class JaxrsBaseElement implements IJaxrsElement {

	/** The associated metamodel. */
	final JaxrsMetamodel metamodel;

	/** Indicates if the underlying java element has compiltation errors. */
	private boolean hasErrors;

	/**
	 * Full constructor.
	 * 
	 * @param metamodel
	 */
	public JaxrsBaseElement(JaxrsMetamodel metamodel) {
		this.metamodel = metamodel;
	}

	/**
	 * Sets a flag of whether the underlying java element has compilation errors or not.
	 * 
	 * @param h
	 *            : true if the java element has errors, false otherwise
	 */
	public void hasErrors(final boolean h) {
		this.hasErrors = h;
	}

	/** @return true if the java element has errors, false otherwise. */
	public final boolean hasErrors() {
		return hasErrors;
	}

	/** @return the metamodel */
	public final JaxrsMetamodel getMetamodel() {
		return metamodel;
	}

}
