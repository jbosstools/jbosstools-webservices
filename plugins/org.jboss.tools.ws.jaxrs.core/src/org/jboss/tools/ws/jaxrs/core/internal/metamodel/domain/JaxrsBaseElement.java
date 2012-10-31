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

	/** Indicates if the element has problems. */
	private int problemLevel;

	/**
	 * Full constructor.
	 * 
	 * @param metamodel
	 */
	public JaxrsBaseElement(JaxrsMetamodel metamodel) {
		this.metamodel = metamodel;
	}

	/**
	 * Resets the problem level for this given element.
	 */
	public void resetProblemLevel() {
		this.problemLevel = 0;
	}

	/**
	 * Sets the problem level for this element. If this element already has a problem level, the highest value is kept.
	 * 
	 * @param problem level: the incoming new problem level.
	 */
	public void setProblemLevel(final int problemLevel) {
		this.problemLevel = Math.max(this.problemLevel, problemLevel);
	}
	
	/** @return the problem level. 
	 * @see IMarker for the severity level (value "0" meaning "no problem, dude")
	 */
	public final int getProblemLevel() {
		return problemLevel;
	}

	/** @return the metamodel */
	public final JaxrsMetamodel getMetamodel() {
		return metamodel;
	}

}
